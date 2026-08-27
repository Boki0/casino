package com.boki0.casino.payment.history;

import com.boki0.casino.payment.entity.DepositOrder;
import com.boki0.casino.payment.entity.DepositStatus;
import com.boki0.casino.payment.entity.PaymentProviderType;
import com.boki0.casino.payment.repository.DepositOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryQueryServiceTest {

    @Mock
    private DepositOrderRepository depositOrderRepository;

    @InjectMocks
    private PaymentHistoryQueryService paymentHistoryQueryService;

    @Test
    void shouldRestrictHistoryToAuthenticatedUserAndRequestedDateRange() {
        UUID authenticatedUserId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 31, 23, 59, 59);
        DepositOrder depositOrder = depositOrder(authenticatedUserId, LocalDateTime.of(2026, 8, 15, 12, 0));

        when(depositOrderRepository.findPaymentHistory(
                eq(authenticatedUserId),
                eq(from),
                eq(to),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(depositOrder)));

        Page<PaymentHistoryItemResponse> result = paymentHistoryQueryService.findHistory(
                authenticatedUserId,
                new PaymentHistoryQuery(PaymentHistoryType.DEPOSIT, from, to),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(depositOrder.getId());
        verify(depositOrderRepository).findPaymentHistory(
                eq(authenticatedUserId),
                eq(from),
                eq(to),
                any(Pageable.class)
        );
    }

    @Test
    void shouldUseNewestFirstSortingWhenPageableIsUnsorted() {
        UUID authenticatedUserId = UUID.randomUUID();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(depositOrderRepository.findPaymentHistory(
                eq(authenticatedUserId),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        paymentHistoryQueryService.findHistory(
                authenticatedUserId,
                PaymentHistoryQuery.all(),
                PageRequest.of(0, 20)
        );

        verify(depositOrderRepository).findPaymentHistory(
                eq(authenticatedUserId),
                eq(null),
                eq(null),
                pageableCaptor.capture()
        );
        Sort.Order createdAtOrder = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertThat(createdAtOrder).isNotNull();
        assertThat(createdAtOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldPreserveDepositValuesInHistoryResponse() {
        UUID authenticatedUserId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 10, 9, 30);
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 10, 9, 32);
        DepositOrder depositOrder = depositOrder(authenticatedUserId, createdAt);
        depositOrder.setCompletedAt(completedAt);

        when(depositOrderRepository.findPaymentHistory(
                eq(authenticatedUserId),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(depositOrder)));

        PaymentHistoryItemResponse item = paymentHistoryQueryService.findHistory(
                authenticatedUserId,
                PaymentHistoryQuery.all(),
                PageRequest.of(0, 20)
        ).getContent().getFirst();

        assertThat(item.id()).isEqualTo(depositOrder.getId());
        assertThat(item.type()).isEqualTo(PaymentHistoryType.DEPOSIT);
        assertThat(item.amount()).isEqualByComparingTo("125.50");
        assertThat(item.currency()).isEqualTo("EUR");
        assertThat(item.status()).isEqualTo(PaymentHistoryStatus.COMPLETED);
        assertThat(item.provider()).isEqualTo(PaymentProviderType.STRIPE);
        assertThat(item.createdAt()).isEqualTo(createdAt);
        assertThat(item.completedAt()).isEqualTo(completedAt);
    }

    @Test
    void shouldReturnEmptyHistoryForUnsupportedWithdrawalRecords() {
        Page<PaymentHistoryItemResponse> result = paymentHistoryQueryService.findHistory(
                UUID.randomUUID(),
                new PaymentHistoryQuery(PaymentHistoryType.WITHDRAWAL, null, null),
                PageRequest.of(0, 20)
        );

        assertThat(result).isEmpty();
        verify(depositOrderRepository, never()).findPaymentHistory(any(), any(), any(), any());
    }

    @Test
    void shouldRejectInvertedDateRange() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 31, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 1, 0, 0);

        assertThatThrownBy(() -> new PaymentHistoryQuery(null, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fromCreatedAt must not be after toCreatedAt");
    }

    private DepositOrder depositOrder(UUID authUserId, LocalDateTime createdAt) {
        DepositOrder depositOrder = new DepositOrder();
        depositOrder.setId(UUID.randomUUID());
        depositOrder.setAuthUserId(authUserId);
        depositOrder.setAmount(new BigDecimal("125.50"));
        depositOrder.setCurrency("EUR");
        depositOrder.setCreditsAmount(new BigDecimal("125.50"));
        depositOrder.setStatus(DepositStatus.COMPLETED);
        depositOrder.setProvider(PaymentProviderType.STRIPE);
        depositOrder.setCreatedAt(createdAt);
        return depositOrder;
    }
}
