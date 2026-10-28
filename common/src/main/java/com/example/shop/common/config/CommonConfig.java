package com.example.shop.common.config;

import com.example.shop.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CommonConfig {

    @Bean
    KafkaAdmin.NewTopics shopTopics() {
        NewTopic[] topics = Topics.ALL.stream()
                .map(name -> TopicBuilder.name(name).partitions(3).replicas(1).build())
                .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }

    /**
     * Consumer failures: retry with exponential backoff (0.5s, 1s, 2s, 4s, 8s), then park the record in
     * "<topic>.DLT" so one poison message cannot block the partition.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        var backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(8000);
        var handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
