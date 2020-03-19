package com.sample;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.Properties;


public class SimpleStream implements Runnable {

    public static final Logger log = LoggerFactory.getLogger(SimpleStream.class);

    private Properties streamProperties;

    public SimpleStream(Properties properties) {
        Objects.requireNonNull(properties);

        this.streamProperties = new Properties();
        streamProperties.putAll(properties);

        // Define if not set
        streamProperties.putIfAbsent(StreamsConfig.APPLICATION_ID_CONFIG, "simple-stream");
        streamProperties.putIfAbsent(StreamsConfig.CLIENT_ID_CONFIG, "simple-stream-client");

        streamProperties.putIfAbsent(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);
        streamProperties.putIfAbsent(StreamsConfig.STATE_DIR_CONFIG, "/tmp");
        streamProperties.putIfAbsent(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 3);
        streamProperties.putIfAbsent(StreamsConfig.consumerPrefix(ConsumerConfig.MAX_POLL_RECORDS_CONFIG), 500);
        streamProperties.putIfAbsent(StreamsConfig.consumerPrefix(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG), 100);

        // Fixed properties
        streamProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    }

    private String getMachine(String event) {
        return event.split("\\|")[0];
    }

    public Properties getStreamProperties() {
        return this.streamProperties;
    }

    public Topology getTopology() {
        var builder = new StreamsBuilder();

        var productionStream = builder.stream(Topics.production, Consumed.with(Serdes.String(), Serdes.String()));

        // Count the nb item produced within 1m per machine
        productionStream
                .peek((itemType, event) -> log.info("Receiving key = {}, value = {}", itemType, event))
                .groupBy((itemType, event) -> getMachine(event))
                .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(
                        "machine-1m-count-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long())
                )
                .toStream()
                .map((Windowed<String> machine, Long count) -> new KeyValue(machine.key(), count))
                .peek((machine, oneMinuteCount) -> log.info("Sending key = {}, value = {}", machine, oneMinuteCount))
                .to(Topics.machine1mProductionPerformance, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }

    @Override
    public void run() {
        log.info("Starting SimpleStream");

        var topology = getTopology();
        log.info(topology.describe().toString());

        var streams = new KafkaStreams(topology, streamProperties);

        // Gracefully close the stream when the application shut down
        // Fixme CountLatch
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Gracefully closing the stream");
            streams.close();
        }));

        // Catch any exception, close the stream  and stop the application
        streams.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            log.error(e.getMessage());
            streams.close();
            System.exit(1);
        });

        streams.start();
    }
}
