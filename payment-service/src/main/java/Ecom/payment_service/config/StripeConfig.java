package Ecom.payment_service.config;

import com.stripe.Stripe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "stripe.api.key", matchIfMissing = false)
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe SDK initialised — key prefix: {}",
                stripeApiKey.length() > 10 ? stripeApiKey.substring(0, 10) + "..." : "***");
    }
}
