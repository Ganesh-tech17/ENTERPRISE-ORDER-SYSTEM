package com.example.shop.payment;

import com.example.shop.common.events.Events.PaymentCompleted;
import com.example.shop.common.events.Events.PaymentFailed;
import com.example.shop.common.events.Topics;
import com.example.shop.common.outbox.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    public record Result(Payment payment, boolean created) {}

    private final PaymentRepository repository;
    private final OutboxService outbox;
    private final BigDecimal maxAmount;

    public PaymentService(PaymentRepository repository, OutboxService outbox,
                          @Value("${payment.max-amount:5000}") BigDecimal maxAmount) {
        this.repository = repository;
        this.outbox = outbox;
        this.maxAmount = maxAmount;
    }

    /**
     * Idempotent: calling it again for the same order returns the existing payment and publishes nothing.
     * A concurrent duplicate loses on the UNIQUE(order_id) constraint (DataIntegrityViolationException).
     */
    @Transactional
    public Result process(UUID orderId, BigDecimal amount) {
        Optional<Payment> existing = repository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return new Result(existing.get(), false);
        }
        Payment payment = new Payment(orderId, amount);
        // Stand-in for a real PSP call (Stripe, Razorpay, ...). Here: amounts above the limit are declined.
        if (amount.compareTo(maxAmount) > 0) {
            payment.fail("Amount exceeds limit of " + maxAmount);
            repository.saveAndFlush(payment);
            outbox.publish(Topics.PAYMENT_FAILED, orderId, new PaymentFailed(orderId, payment.getFailureReason()));
        } else {
            payment.complete();
            repository.saveAndFlush(payment);
            outbox.publish(Topics.PAYMENT_COMPLETED, orderId, new PaymentCompleted(orderId, payment.getId()));
        }
        return new Result(payment, true);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId);
    }
}
