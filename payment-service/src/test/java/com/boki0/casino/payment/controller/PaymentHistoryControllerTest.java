package com.boki0.casino.payment.controller;

import com.boki0.casino.payment.entity.PaymentProviderType;
import com.boki0.casino.payment.exception.GlobalExceptionHandler;
import com.boki0.casino.payment.history.PaymentHistoryItemResponse;
import com.boki0.casino.payment.history.PaymentHistoryQuery;
import com.boki0.casino.payment.history.PaymentHistoryQueryService;
import com.boki0.casino.payment.history.PaymentHistoryStatus;
import com.boki0.casino.payment.history.PaymentHistoryType;
import com.boki0.casino.payment.security.GatewayInternalAuthFilter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryControllerTest {

    private static final String INTERNAL_SECRET = "test-gateway-secret";
    private static final String HEADER_INTERNAL_SECRET = "X-Internal-Gateway-Secret";
    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";

    @Mock
    private PaymentHistoryQueryService paymentHistoryQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentHistoryController controller = new PaymentHistoryController(paymentHistoryQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new GatewayInternalAuthFilter(INTERNAL_SECRET))
                .build();
    }

    @Test
    void authenticatedUserShouldReceiveOwnPaymentHistory() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        PaymentHistoryItemResponse item = historyItem();
        when(paymentHistoryQueryService.findHistory(
                eq(authenticatedUserId),
                any(PaymentHistoryQuery.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(item.id().toString()))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].amount").value(75.50))
                .andExpect(jsonPath("$.content[0].currency").value("EUR"))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].provider").value("STRIPE"))
                .andExpect(jsonPath("$.content[0].providerSessionId").doesNotExist())
                .andExpect(jsonPath("$.content[0].checkoutUrl").doesNotExist())
                .andExpect(jsonPath("$.content[0].idempotencyKey").doesNotExist());

        verify(paymentHistoryQueryService).findHistory(
                eq(authenticatedUserId),
                any(PaymentHistoryQuery.class),
                any(Pageable.class)
        );
    }

    @Test
    void emptyHistoryShouldReturnOkWithEmptyContent() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        when(paymentHistoryQueryService.findHistory(
                eq(authenticatedUserId),
                any(PaymentHistoryQuery.class),
                any(Pageable.class)
        )).thenReturn(Page.empty(PageRequest.of(0, 20)));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldTranslateTypeAndInclusiveDateRangeFilters() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        ArgumentCaptor<PaymentHistoryQuery> queryCaptor = ArgumentCaptor.forClass(PaymentHistoryQuery.class);
        when(paymentHistoryQueryService.findHistory(
                eq(authenticatedUserId),
                any(PaymentHistoryQuery.class),
                any(Pageable.class)
        )).thenReturn(Page.empty(PageRequest.of(0, 20)));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("type", "DEPOSIT")
                        .queryParam("from", "2026-08-01")
                        .queryParam("to", "2026-08-31"))
                .andExpect(status().isOk());

        verify(paymentHistoryQueryService).findHistory(
                eq(authenticatedUserId),
                queryCaptor.capture(),
                any(Pageable.class)
        );
        PaymentHistoryQuery query = queryCaptor.getValue();
        assertThat(query.type()).isEqualTo(PaymentHistoryType.DEPOSIT);
        assertThat(query.fromCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 0, 0));
        assertThat(query.toCreatedAt()).isEqualTo(LocalDate.of(2026, 8, 31).atTime(LocalTime.MAX));
    }

    @Test
    void shouldSupportFromAndToFiltersIndependently() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        ArgumentCaptor<PaymentHistoryQuery> queryCaptor = ArgumentCaptor.forClass(PaymentHistoryQuery.class);
        when(paymentHistoryQueryService.findHistory(
                eq(authenticatedUserId),
                any(PaymentHistoryQuery.class),
                any(Pageable.class)
        )).thenReturn(Page.empty(PageRequest.of(0, 20)));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("from", "2026-08-10"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("to", "2026-08-20"))
                .andExpect(status().isOk());

        verify(paymentHistoryQueryService, org.mockito.Mockito.times(2)).findHistory(
                eq(authenticatedUserId),
                queryCaptor.capture(),
                any(Pageable.class)
        );
        assertThat(queryCaptor.getAllValues().get(0).fromCreatedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 10, 0, 0));
        assertThat(queryCaptor.getAllValues().get(0).toCreatedAt()).isNull();
        assertThat(queryCaptor.getAllValues().get(1).fromCreatedAt()).isNull();
        assertThat(queryCaptor.getAllValues().get(1).toCreatedAt())
                .isEqualTo(LocalDate.of(2026, 8, 20).atTime(LocalTime.MAX));
    }

    @Test
    void shouldApplyPaginationDefaultsAndRequestedPageSize() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(paymentHistoryQueryService.findHistory(
                eq(authenticatedUserId),
                any(PaymentHistoryQuery.class),
                any(Pageable.class)
        )).thenReturn(Page.empty(PageRequest.of(0, 20)));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("page", "2")
                        .queryParam("size", "5"))
                .andExpect(status().isOk());

        verify(paymentHistoryQueryService, org.mockito.Mockito.times(2)).findHistory(
                eq(authenticatedUserId),
                any(PaymentHistoryQuery.class),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getAllValues().get(0).getPageNumber()).isZero();
        assertThat(pageableCaptor.getAllValues().get(0).getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getAllValues().get(1).getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getAllValues().get(1).getPageSize()).isEqualTo(5);
    }

    @Test
    void invalidRangeAndQueryParametersShouldReturnBadRequest() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("from", "2026-08-31")
                        .queryParam("to", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromCreatedAt must not be after toCreatedAt"));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("type", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid query parameter: type"));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid query parameter: from"));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page must not be negative"));

        mockMvc.perform(authenticatedGet(authenticatedUserId, "/payments/history")
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size must be between 1 and 100; default is 20"));
    }

    @Test
    void requestWithoutTrustedGatewaySecretShouldBeRejected() throws Exception {
        mockMvc.perform(get("/payments/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized internal gateway request"));
    }

    private MockHttpServletRequestBuilder authenticatedGet(UUID authenticatedUserId, String path) {
        return get(path)
                .header(HEADER_INTERNAL_SECRET, INTERNAL_SECRET)
                .header(HEADER_AUTH_USER_ID, authenticatedUserId.toString());
    }

    private PaymentHistoryItemResponse historyItem() {
        return new PaymentHistoryItemResponse(
                UUID.randomUUID(),
                PaymentHistoryType.DEPOSIT,
                new BigDecimal("75.50"),
                "EUR",
                PaymentHistoryStatus.COMPLETED,
                PaymentProviderType.STRIPE,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                LocalDateTime.of(2026, 8, 20, 10, 1)
        );
    }
}
