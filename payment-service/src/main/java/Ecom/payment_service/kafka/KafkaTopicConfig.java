package Ecom.payment_service.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String PAYMENT_SUCCESS = "payment.success";
    public static final String PAYMENT_FAILED  = "payment.failed";

    @Bean public NewTopic paymentSuccess() { return TopicBuilder.name(PAYMENT_SUCCESS).partitions(3).replicas(1).build(); }
    @Bean public NewTopic paymentFailed()  { return TopicBuilder.name(PAYMENT_FAILED).partitions(3).replicas(1).build(); }
}
