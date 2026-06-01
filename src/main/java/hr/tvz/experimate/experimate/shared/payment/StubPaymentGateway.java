package hr.tvz.experimate.experimate.shared.payment;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub implementation of {@link PaymentGateway} used until a real payment provider is integrated.
 *
 * <p>Always returns a successful result with a generated reference. Swap this out by
 * writing a new {@code @Service} that implements {@link PaymentGateway} and removing
 * this bean — every calling service will pick up the new implementation automatically.
 */
@Service
public class StubPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(ChargeRequest request) {
        return new PaymentResult(true, "STUB-" + UUID.randomUUID());
    }
}
