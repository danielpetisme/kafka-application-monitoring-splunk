package com.sample;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;


public class SimpleConsumer implements Runnable {

    public static final Logger log = LoggerFactory.getLogger(SimpleConsumer.class);

    private final KafkaConsumer<Integer, Integer> kafkaConsumer;

    public SimpleConsumer(Properties properties) {
        Objects.requireNonNull(properties);

        var consumerProperties = new Properties();
        consumerProperties.putAll(properties);

        // Define if not set
        consumerProperties.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "simple-consumer");
        consumerProperties.putIfAbsent(ConsumerConfig.CLIENT_ID_CONFIG, "simple-consumer-client");

        // Fixed properties
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);

        this.kafkaConsumer = new KafkaConsumer<>(consumerProperties);
    }

    @Override
    public void run() {
        log.info("Starting SimpleConsumer");
        this.kafkaConsumer.subscribe(List.of(Topics.machine1mProductionPerformance));
        while (true) {
            ConsumerRecords<Integer, Integer> records = kafkaConsumer.poll(Duration.ofMillis(400));
            for (ConsumerRecord<Integer, Integer> record : records) {
                log.info("Topic: {}, timestamp = {},  offset = {}, key = {}, value = {}", record.topic(), record.timestamp(), record.offset(), record.key(), record.value());
            }
        }
    }

}
