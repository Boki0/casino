# Payment Service Flow

`payment-service` owns deposit attempts through `DepositOrder`. `wallet-service` owns wallet balance through `Wallet` and `WalletTransaction`.

The payment service never directly changes wallet balances. When a deposit becomes `COMPLETED`, it publishes `PaymentDepositCompletedEvent`; wallet-service consumes that event and credits the wallet idempotently.

This project uses virtual demo credits only. It has no withdrawals, no cash-out, and is not intended for real-money gambling production use.

## Responsibilities

- `payment-service`: creates and completes deposit orders.
- `wallet-service`: owns wallet balances and wallet transactions.
- `api-gateway`: validates JWT for user-facing payment endpoints and forwards trusted headers.
- `RabbitMQ`: delivers deposit completion events from payment-service to wallet-service.
- `Stripe`: sandbox Checkout provider for card-like demo deposits.

## Manual Deposit Flow

Manual deposits are a development-only way to test the deposit and wallet-credit pipeline without Stripe.

1. Client calls `POST /api/payments/deposits` with `provider = MANUAL`.
2. `api-gateway` validates the JWT.
3. `api-gateway` forwards trusted headers, including `X-Auth-User-Id` and `X-Internal-Gateway-Secret`.
4. `payment-service` creates a `DepositOrder` with status `PENDING`.
5. Client calls `POST /api/payments/deposits/{depositId}/complete-manual`.
6. `payment-service` marks the deposit `COMPLETED`.
7. `payment-service` publishes `PaymentDepositCompletedEvent`.
8. `wallet-service` consumes the event.
9. `wallet-service` credits the user's wallet idempotently.

### Manual Curl Examples

Create a manual deposit:

```bash
curl -X POST http://localhost:8080/api/payments/deposits \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10.00,
    "currency": "EUR",
    "creditsAmount": 1000.00,
    "provider": "MANUAL",
    "idempotencyKey": "manual-deposit-test-1"
  }'
```

Complete it:

```bash
curl -X POST http://localhost:8080/api/payments/deposits/DEPOSIT_ID/complete-manual \
  -H "Authorization: Bearer TOKEN"
```

Check wallet:

```bash
curl http://localhost:8080/api/wallet/me \
  -H "Authorization: Bearer TOKEN"
```

## Stripe Deposit Flow

Stripe deposits use Stripe Checkout in sandbox mode.

1. Client calls `POST /api/payments/deposits` with `provider = STRIPE`.
2. `api-gateway` validates the JWT.
3. `payment-service` creates a `DepositOrder` with status `PENDING`.
4. `StripePaymentProvider` creates a Stripe Checkout Session.
5. `payment-service` returns `checkoutUrl`.
6. User opens `checkoutUrl` and pays with a Stripe test card.
7. Stripe sends `checkout.session.completed` webhook.
8. `payment-service` verifies `Stripe-Signature` using `STRIPE_WEBHOOK_SECRET`.
9. `payment-service` marks the matching `DepositOrder` as `COMPLETED`.
10. `payment-service` publishes `PaymentDepositCompletedEvent`.
11. `wallet-service` consumes the event and credits the wallet.

`success-url` and `cancel-url` are frontend redirects only. They are not proof of payment. The trusted proof of payment is the verified Stripe webhook.

Example Stripe deposit request:

```bash
curl -X POST http://localhost:8080/api/payments/deposits \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10.00,
    "currency": "EUR",
    "creditsAmount": 1000.00,
    "provider": "STRIPE",
    "idempotencyKey": "stripe-checkout-test-1"
  }'
```

The response should contain a Stripe `checkoutUrl`. Open it in a browser and use a Stripe test card.

## Local Stripe Setup

Install and authenticate Stripe CLI:

```bash
stripe login
```

Create a root `.env` file:

```bash
STRIPE_SECRET_KEY=sk_test_your_key_here
```

`.env` is ignored by Git and must not be committed.

`scripts/start-dev.sh` can capture `STRIPE_WEBHOOK_SECRET` automatically from Stripe CLI output. If you need to set values manually:

```bash
export STRIPE_SECRET_KEY="sk_test_..."
export STRIPE_WEBHOOK_SECRET="whsec_..."
```

Do not commit real Stripe keys or webhook secrets.

## start-dev.sh Behavior

`./scripts/start-dev.sh` starts local infrastructure and services:

- Docker infrastructure
- `api-gateway`
- `auth-service`
- `user-service`
- `wallet-service`
- `payment-service`

If Stripe CLI is installed and `STRIPE_SECRET_KEY` is set, the script also opens a visible terminal/tab titled `stripe-cli` and runs:

```bash
stripe listen --forward-to localhost:8080/api/payments/webhooks/stripe
```

Stripe CLI prints a webhook signing secret. The script captures the `whsec_...` value from local CLI output and passes it to payment-service as `STRIPE_WEBHOOK_SECRET`.

`payment-service` receives:

- `STRIPE_SECRET_KEY` from `.env` or the current shell environment.
- `STRIPE_WEBHOOK_SECRET` captured from Stripe CLI.

## Important Config

Safe local config in `payment-service/src/main/resources/application.properties`:

```properties
stripe.secret-key=${STRIPE_SECRET_KEY:}
stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET:}
stripe.success-url=${STRIPE_SUCCESS_URL:http://localhost:4200/deposit/success?session_id={CHECKOUT_SESSION_ID}}
stripe.cancel-url=${STRIPE_CANCEL_URL:http://localhost:4200/deposit/cancel}
```

Do not place real `sk_test`, `sk_live`, or `whsec` values in committed files.

## RabbitMQ Event

When a deposit is completed, payment-service publishes:

- exchange: `casino.events`
- routing key: `payment.deposit.completed`
- event: `PaymentDepositCompletedEvent`

Event fields:

- `eventId`
- `eventType`
- `eventVersion`
- `depositOrderId`
- `authUserId`
- `amount`
- `currency`
- `creditsAmount`
- `provider`
- `occurredAt`

Wallet-service consumes the event and credits the wallet using:

- `referenceId = depositOrderId`
- `idempotencyKey = payment-deposit-{depositOrderId}`

Duplicate events must not double-credit the wallet. Wallet-service relies on existing idempotency checks for the idempotency key and payment reference.

## Troubleshooting

### Stripe Checkout returns 500: Stripe secret key is not configured

Fix:

- Set `STRIPE_SECRET_KEY`.
- Restart `payment-service`.

### checkout.session.completed returns 400 in Stripe CLI

Fix:

- Check `STRIPE_WEBHOOK_SECRET`.
- Check `api-gateway` treats `/api/payments/webhooks/stripe` as public.
- Check `payment-service` allows `/payments/webhooks/stripe` without `X-Internal-Gateway-Secret`.
- Check `payment-service` logs for signature, metadata, amount, currency, and deposit lookup messages.

### Wallet balance does not increase

Fix:

- Check the `DepositOrder` status is `COMPLETED`.
- Check RabbitMQ queue `wallet-service.payment-deposit-completed.queue`.
- Check wallet-service logs.
- Check the `wallet_transactions` table.

### GitHub blocks push because a Stripe secret was committed

Fix:

- Remove the secret from the file.
- Rewrite local Git history.
- Rotate the exposed Stripe test key in Stripe Dashboard.
- Use `.env` or environment variables instead.

## Production Notes

- Use live Stripe keys only as production environment variables.
- Configure Stripe Dashboard webhook endpoint to the public backend URL.
- Do not use Stripe CLI in production.
- Use HTTPS.
- Rotate exposed keys immediately.
- This portfolio project uses virtual credits only.
