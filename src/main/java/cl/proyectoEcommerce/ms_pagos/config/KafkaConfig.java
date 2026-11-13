package cl.proyectoEcommerce.ms_pagos.config;

import cl.proyectoEcommerce.ms_pagos.dtos.IntentoPagoDTO;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    ProducerFactory<String, IntentoPagoDTO> intentoPagoProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);
        JacksonJsonSerializer<IntentoPagoDTO> serializer = new JacksonJsonSerializer<>();
        serializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(properties, new StringSerializer(), serializer);
    }

    @Bean
    KafkaTemplate<String, IntentoPagoDTO> intentoPagoKafkaTemplate(
            ProducerFactory<String, IntentoPagoDTO> intentoPagoProducerFactory) {
        return new KafkaTemplate<>(intentoPagoProducerFactory);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, IntentoPagoDTO> kafkaListenerContainerFactory(
            ConsumerFactory<String, IntentoPagoDTO> intentoPagoConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, IntentoPagoDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(intentoPagoConsumerFactory);
        return factory;
    }

    @Bean
    ConsumerFactory<String, IntentoPagoDTO> intentoPagoConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:payment-velocity-group}") String groupId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        JacksonJsonDeserializer<IntentoPagoDTO> deserializer =
                new JacksonJsonDeserializer<>(IntentoPagoDTO.class).ignoreTypeHeaders();
        return new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(), deserializer);
    }

    @Bean
    NewTopic paymentAttemptsTopic(
            @Value("${pagos.kafka.topic:payment-attempts}") String topic,
            @Value("${pagos.kafka.partitions:3}") int partitions,
            @Value("${pagos.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas).build();
    }
}
