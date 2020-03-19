package com.sample;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.apache.kafka.streams.test.OutputVerifier;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleStreamTest {

    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, Long> outputTopic;

    TopologyTestDriver testDriver;

    @BeforeEach
    public void setup() {
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, SimpleStream.class.getName());
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");

        SimpleStream stream = new SimpleStream(properties);
        this.testDriver = new TopologyTestDriver(stream.getTopology(), stream.getStreamProperties());

        inputTopic = testDriver.createInputTopic(Topics.production, new StringSerializer(), new StringSerializer());
        outputTopic = testDriver.createOutputTopic(Topics.machine1mProductionPerformance, new StringDeserializer(), new LongDeserializer());
    }

    @AfterEach
    public void tearDown() {
        testDriver.close();
    }

    @Test
    public void testStream() {
        var records = List.of(
                new TestRecord<>("ItemType#1", "Machine#1|Item#1"),
                new TestRecord<>("ItemType#2", "Machine#2|Item#2"),
                new TestRecord<>("ItemType#3", "Machine#1|Item#3"),
                new TestRecord<>("ItemType#1", "Machine#2|Item#4"),
                new TestRecord<>("ItemType#2", "Machine#1|Item#5"),
                new TestRecord<>("ItemType#3", "Machine#1|Item#6")
        );

        var expectedMachine1mPerformance = Map.of(
                "Machine#1", 4L,
                "Machine#2", 2L
        );
        inputTopic.pipeRecordList(records);

        final Map<String, Long> actualMachine1mPerformance = outputTopic.readKeyValuesToMap();

        assertThat(actualMachine1mPerformance).isEqualTo(expectedMachine1mPerformance);
    }
}
