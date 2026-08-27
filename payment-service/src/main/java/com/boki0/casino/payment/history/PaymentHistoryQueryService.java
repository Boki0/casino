package com.boki0.casino.payment.history;

import com.boki0.casino.payment.entity.DepositOrder;
import com.boki0.casino.payment.repository.DepositOrderRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentHistoryQueryService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final DepositOrderRepository depositOrderRepository;

    public PaymentHistoryQueryService(DepositOrderRepository depositOrderRepository) {
        this.depositOrderRepository = depositOrderRepository;
    }

    @Transactional(readOnly = true)
    public Page<PaymentHistoryItemResponse> findHistory(
            UUID authenticatedUserId,
            PaymentHistoryQuery query,
            Pageable pageable
    ) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");

        Pageable resolvedPageable = withDefaultSort(pageable);

        if (query.type() == PaymentHistoryType.WITHDRAWAL) {
            return Page.empty(resolvedPageable);
        }

        return depositOrderRepository.findPaymentHistory(
                        authenticatedUserId,
                        query.fromCreatedAt(),
                        query.toCreatedAt(),
                        resolvedPageable
                )
                .map(this::toResponse);
    }

    private Pageable withDefaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        if (pageable.isUnpaged()) {
            return Pageable.unpaged(DEFAULT_SORT);
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
    }

    private PaymentHistoryItemResponse toResponse(DepositOrder depositOrder) {
        return new PaymentHistoryItemResponse(
                depositOrder.getId(),
                PaymentHistoryType.DEPOSIT,
                depositOrder.getAmount(),
                depositOrder.getCurrency(),
                PaymentHistoryStatus.valueOf(depositOrder.getStatus().name()),
                depositOrder.getProvider(),
                depositOrder.getCreatedAt(),
                depositOrder.getCompletedAt()
        );
    }
}
