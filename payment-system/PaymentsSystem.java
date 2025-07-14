import java.util.*;

/**
 * Simplified Payment System for Movie Booking (Single Domain)
 */
public class MoviePaymentSystem {

    // --- Domain Model ---
    public static class MovieBooking {
        private final String userId;
        private final double baseAmount;
        private final String movieId;
        private final String showId;
        private final String couponCode;

        public MovieBooking(String userId, double baseAmount, String movieId, String showId, String couponCode) {
            this.userId = userId;
            this.baseAmount = baseAmount;
            this.movieId = movieId;
            this.showId = showId;
            this.couponCode = couponCode;
        }

        public String getUserId() { return userId; }
        public double getBaseAmount() { return baseAmount; }
        public String getMovieId() { return movieId; }
        public String getShowId() { return showId; }
        public String getCouponCode() { return couponCode; }
    }

    // --- Payment Request/Response ---
    public static class PaymentRequest {
        private final String userId;
        private final double amount;
        private final String currency;
        private final Map<String, String> metadata;

        public PaymentRequest(String userId, double amount, String currency, Map<String, String> metadata) {
            this.userId = userId;
            this.amount = amount;
            this.currency = currency;
            this.metadata = metadata;
        }

        public String getUserId() { return userId; }
        public double getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public Map<String, String> getMetadata() { return metadata; }

        @Override
        public String toString() {
            return "PaymentRequest{" +
                    "userId='" + userId + '\'' +
                    ", amount=" + amount +
                    ", currency='" + currency + '\'' +
                    ", metadata=" + metadata +
                    '}';
        }
    }

    public static class PaymentResponse {
        private final boolean success;
        private final String transactionId;
        private final String message;

        public PaymentResponse(boolean success, String transactionId, String message) {
            this.success = success;
            this.transactionId = transactionId;
            this.message = message;
        }

        @Override
        public String toString() {
            return "PaymentResponse{" +
                    "success=" + success +
                    ", transactionId='" + transactionId + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    // --- Payment Gateway ---
    public interface PaymentGateway {
        PaymentResponse process(PaymentRequest request);
    }

    public static class StripeGateway implements PaymentGateway {
        @Override
        public PaymentResponse process(PaymentRequest request) {
            System.out.println("Processing with Stripe: " + request);
            return new PaymentResponse(true, UUID.randomUUID().toString(), "Paid via Stripe");
        }
    }

    // --- Pricing Rules & Strategy ---
    public interface PricingRule {
        double apply(double amount, Map<String, String> metadata);
    }

    public static class DiscountRule implements PricingRule {
        private final double discountPercent;

        public DiscountRule(double discountPercent) {
            this.discountPercent = discountPercent;
        }

        @Override
        public double apply(double amount, Map<String, String> metadata) {
            System.out.println("Applying discount: " + discountPercent + "%");
            return amount * (1 - discountPercent / 100.0);
        }
    }

    public static class CouponRule implements PricingRule {
        @Override
        public double apply(double amount, Map<String, String> metadata) {
            String coupon = metadata.getOrDefault("couponCode", "");
            if ("SAVE50".equalsIgnoreCase(coupon)) {
                System.out.println("Applying coupon SAVE50: -50");
                return Math.max(0, amount - 50);
            }
            return amount;
        }
    }

    public static class TaxRule implements PricingRule {
        private final double taxPercent;

        public TaxRule(double taxPercent) {
            this.taxPercent = taxPercent;
        }

        @Override
        public double apply(double amount, Map<String, String> metadata) {
            System.out.println("Applying tax: " + taxPercent + "%");
            return amount * (1 + taxPercent / 100.0);
        }
    }

    public interface PricingStrategy {
        double calculate(double baseAmount, Map<String, String> metadata);
    }

    public static class PricingPipeline implements PricingStrategy {
        private final List<PricingRule> rules;

        public PricingPipeline(List<PricingRule> rules) {
            this.rules = rules;
        }

        @Override
        public double calculate(double baseAmount, Map<String, String> metadata) {
            double amount = baseAmount;
            for (PricingRule rule : rules) {
                amount = rule.apply(amount, metadata);
            }
            return amount;
        }
    }

    // --- Adapter ---
    public static class MovieBookingAdapter {
        private final String currency;

        public MovieBookingAdapter(String currency) {
            this.currency = currency;
        }

        public PaymentRequest adapt(MovieBooking booking, PricingStrategy pricingStrategy) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("movieId", booking.getMovieId());
            metadata.put("showId", booking.getShowId());
            metadata.put("couponCode", booking.getCouponCode());

            double finalAmount = pricingStrategy.calculate(booking.getBaseAmount(), metadata);

            return new PaymentRequest(
                    booking.getUserId(),
                    finalAmount,
                    currency,
                    metadata
            );
        }
    }

    // --- Payment Processor ---
    public static class PaymentProcessor {
        private final PaymentGateway gateway;
        private final PricingStrategy globalPricingStrategy;

        public PaymentProcessor(PaymentGateway gateway, PricingStrategy globalPricingStrategy) {
            this.gateway = gateway;
            this.globalPricingStrategy = globalPricingStrategy;
        }

        public PaymentResponse process(PaymentRequest request) {
            double finalAmount = globalPricingStrategy.calculate(request.getAmount(), request.getMetadata());
            PaymentRequest finalRequest = new PaymentRequest(
                    request.getUserId(),
                    finalAmount,
                    request.getCurrency(),
                    request.getMetadata()
            );
            return gateway.process(finalRequest);
        }
    }

    // --- Main Demo ---
    public static void main(String[] args) {
        System.out.println("=== Movie Booking Payment Demo ===");

        // 1. Create domain booking
        MovieBooking booking = new MovieBooking(
                "user123",
                500.0,
                "MOV123",
                "SHOW456",
                "SAVE50"
        );

        // 2. Domain pricing rules (discount + coupon)
        PricingStrategy domainPricing = new PricingPipeline(List.of(
                new DiscountRule(10),  // 10% discount
                new CouponRule()       // Apply coupon
        ));

        // 3. Adapt to PaymentRequest
        MovieBookingAdapter adapter = new MovieBookingAdapter("INR");
        PaymentRequest domainPricedRequest = adapter.adapt(booking, domainPricing);
        System.out.println("\nPaymentRequest after domain pricing: " + domainPricedRequest);

        // 4. Global pricing rules (e.g., tax)
        PricingStrategy globalPricing = new PricingPipeline(List.of(
                new TaxRule(18)  // 18% GST
        ));

        // 5. Setup payment gateway
        PaymentGateway gateway = new StripeGateway();

        // 6. Process payment
        PaymentProcessor processor = new PaymentProcessor(gateway, globalPricing);
        PaymentResponse response = processor.process(domainPricedRequest);

        System.out.println("\nFinal PaymentResponse: " + response);
    }
}
