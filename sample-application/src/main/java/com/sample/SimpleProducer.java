package com.sample;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class SimpleProducer implements Runnable {

    public static final Logger log = LoggerFactory.getLogger(SimpleProducer.class);

    private final int nbMachines;
    private final int nbItemTypes;
    private final KafkaProducer<String, String> kafkaProducer;

    public SimpleProducer(Properties properties, int nbMachines, int nbItemTypes) {
        Objects.requireNonNull(properties);

        var producerProperties = new Properties();
        producerProperties.putAll(properties);

        // Define if not set
        producerProperties.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, "simple-producer-client");

        // Fixed properties
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        this.kafkaProducer = new KafkaProducer<>(producerProperties);
        this.nbMachines = nbMachines;
        this.nbItemTypes = nbItemTypes;
    }

    @Override
    public void run() {
        log.info("Starting SimpleProducer with {} item type(s) and {} machines", nbItemTypes, nbMachines);
        var random = new Random();
        while (true) {
            var itemType = "ItemType#" + random.nextInt(nbItemTypes);
            var machine = "Machine#" + random.nextInt(nbMachines);
            var item = "Item#" + UUID.randomUUID();
            ProducerRecord<String, String> record = new ProducerRecord<>(Topics.production, itemType, machine + "-" + item);
            log.info("Sending {}: {}", record.key(), record.value());
            try {
                // FIXME add error management (callback, timeout, whatever)
                kafkaProducer.send(record).get();
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Producer exception", e);
                Thread.interrupted();
                break;
            }
        }
    }

}