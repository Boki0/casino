# Wallet Service Flow

## Purpose

`wallet-service` manages user wallets and wallet balances.

It is responsible for:

- creating a wallet for each registered user
- storing the current wallet balance
- storing wallet transaction records
- providing the authenticated user with their wallet balance

The wallet uses virtual credits only. It does not manage real money directly.

---

## Service Port

Local development port:

```text
8083