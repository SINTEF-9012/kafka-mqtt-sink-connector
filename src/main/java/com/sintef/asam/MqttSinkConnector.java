package com.sintef.asam;

import com.sintef.asam.util.Version;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MqttSinkConnector extends SinkConnector {

    private static final Logger logger = LogManager.getLogger(MqttSinkConnector.class);
    private Map<String, String> connectorProperties;

    @Override
    public void start(Map<String, String> map) {
        connectorProperties = map;
        logger.debug("STARTING mqtt sink connector");
    }

    @Override
    public Class<? extends Task> taskClass() {
        return MqttSinkConnectorTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int i) {
        List<Map<String, String>> taskConfigs = new ArrayList<>(1);
        taskConfigs.add(new HashMap<>(connectorProperties));
        return taskConfigs;
    }

    @Override
    public void stop() {
        logger.debug("STOPPING mqtt sink connector.");
    }

    @Override
    public ConfigDef config() {
        return MqttSinkConnectorConfig.configuration;
    }

    @Override
    public String version() {
        return Version.getVersion();
    }

}
