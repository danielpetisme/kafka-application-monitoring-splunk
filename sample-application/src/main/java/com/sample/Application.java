package com.sample;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.sample.ApplicationConfig.*;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static final String KAFKA_ENV_PREFIX = "KAFKA_";
    private static final String APPLICATION_ENV_PREFIX = "APPLICATION_";

    public static Properties environmentVariablesToProperties(String prefix) {
        var envVars = System.getenv().entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(
                        e -> e.getKey()
                                .replace(prefix, "")
                                .toLowerCase()
                                .replace("_", ".")
                        , e -> e.getValue())
                );

        var properties = new Properties();
        properties.putAll(envVars);
        return properties;
    }

    public static void createTopic(AdminClient adminClient, String topic, int nbPartitions, int replicationFactor) throws InterruptedException, ExecutionException {
        if (!adminClient.listTopics().names().get().contains(topic)) {
            log.info("Creating topic {}", topic);
            final NewTopic newTopic = new NewTopic(topic, nbPartitions, (short) replicationFactor);
            try {
                CreateTopicsResult topicsCreationResult = adminClient.createTopics(Collections.singleton(newTopic));
                topicsCreationResult.all().get();
            } catch (TopicExistsException e) {
                //silent ignore if topic already exists
            }
        }
    }

    private static boolean isValidMode(String value) {
        for (Mode mode : Mode.values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // Retrieve properties
        var kafkaProperties = environmentVariablesToProperties(KAFKA_ENV_PREFIX);
        var applicationProperties = environmentVariablesToProperties(APPLICATION_ENV_PREFIX);

        // Define a default bootstrap server for Kafka
        kafkaProperties.putIfAbsent(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // application properties
        var nbMachines = (int) applicationProperties.getOrDefault(NB_MACHINES_CONFIG, DEFAULT_NB_MACHINES);
        var nbItemTypes = (int) applicationProperties.getOrDefault(NB_ITEM_TYPES_CONFIG, DEFAULT_NB_ITEM_TYPES);
        var createTopics = Boolean.parseBoolean((String) applicationProperties.getOrDefault(CREATE_TOPICS_CONFIG, "false"));

        // Initialize topics if needed
        if (createTopics) {
            var adminClient = AdminClient.create(kafkaProperties);
            createTopic(adminClient, Topics.production, nbItemTypes, 1);
            createTopic(adminClient, Topics.machine1mProductionPerformance, nbMachines, 1);
        }

        // Get the application mode
        var modeProperty = (String) applicationProperties.getOrDefault(MODE_CONFIG, DEFAULT_MODE);
        if (!isValidMode(modeProperty)) {
            log.error("{} is not a recognized mode, accepted values: all(default)|producer|consumer|stream", modeProperty);
            System.exit(1);
        }
        var mode = Mode.valueOf(modeProperty.toUpperCase());

        // Start the application
        var threads = new ArrayList<Thread>();

        if (mode == Mode.PRODUCER || mode == Mode.ALL) {
            threads.add(new Thread(new SimpleProducer(kafkaProperties, nbMachines, nbItemTypes), "producer"));
        }
        if (mode == Mode.CONSUMER || mode == Mode.ALL) {
            threads.add(new Thread(new SimpleConsumer(kafkaProperties), "consumer"));
        }
        if (mode == Mode.STREAM || mode == Mode.ALL) {
            threads.add(new Thread(new SimpleStream(kafkaProperties), "stream"));
        }

        for (var thread : threads) {
            thread.start();
        }
    }

}
