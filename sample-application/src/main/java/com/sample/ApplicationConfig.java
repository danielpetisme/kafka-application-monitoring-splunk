package com.sample;

public interface ApplicationConfig {

    enum Mode {
        ALL,
        PRODUCER,
        CONSUMER,
        STREAM
    }

    String MODE_CONFIG = "mode";
    String CREATE_TOPICS_CONFIG = "create.topics";
    String NB_MACHINES_CONFIG = "nb.machines";
    String NB_ITEM_TYPES_CONFIG = "nb.item-types";

    String DEFAULT_MODE = "all";
    int DEFAULT_NB_MACHINES = 10;
    int DEFAULT_NB_ITEM_TYPES = 10;
}
