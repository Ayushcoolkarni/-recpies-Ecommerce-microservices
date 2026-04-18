package Ecom.payment_service.kafka;

import Ecom.payment_service.event.PaymentFailedEvent;
import Ecom.payment_service.event.PaymentSuccessEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaProducerConfig for payment-service.
 *
 * FIXES:
 *   - retries=3: recovers from transient broker unavailability (e.g. startup race)
 *   - reconnect backoff: prevents hammering a recovering broker
 *   - delivery.timeout.ms: bounded wait so the thread doesn't hang forever
 *   - Generic KafkaTemplate<Object,Object> bean added for DLT error handler in consumers
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka:29092}")
    private String bootstrapServers;

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        // FIX: retry on transient errors (broker not yet ready at startup)
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        props.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 5000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);
        return props;
    }

    @Bean
    public KafkaTemplate<String, PaymentSuccessEvent> successTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(baseProps()));
    }

    @Bean
    public KafkaTemplate<String, PaymentFailedEvent> failedTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(baseProps()));
    }

    /**
     * Generic template required by DeadLetterPublishingRecoverer in error handlers.
     */
    @Bean
    public KafkaTemplate<Object, Object> genericKafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(baseProps()));
    }
}
