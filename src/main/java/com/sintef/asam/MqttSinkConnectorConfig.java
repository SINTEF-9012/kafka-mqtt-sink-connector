package com.sintef.asam;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


public class MqttSinkConnectorConfig extends AbstractConfig {

    private static final Logger logger = LogManager.getLogger(MqttSinkConnectorConfig.class);
    static ConfigDef configuration = baseConfigDef();

    public MqttSinkConnectorConfig(Map<String, String> properties) {
        super(configuration, properties);
    }

    public static ConfigDef baseConfigDef() {
        ConfigDef configDef = new ConfigDef();
        configDef.define("mqtt.connector.broker.uri", Type.STRING,
                "tcp://localhost:1883", Importance.HIGH,
                "Full uri with port to mqtt broker")
                .define("mqtt.connector.client.id", Type.STRING, "kafka_sink_connector", Importance.MEDIUM,
        "Client id used by connector to subscribe to mqtt broker")
                .define("mqtt.connector.clean_session", Type.BOOLEAN, true, Importance.MEDIUM,
        "If connection should begin with clean session")
                .define("mqtt.connector.connection_timeout", Type.INT, 30, Importance.LOW,
        "Timeout limit for mqtt broker connection")
                .define("mqtt.connector.keep_alive", Type.INT, 60, Importance.LOW,
                "The interval to keep alive")
                .define("mqtt.connector.qos", Type.INT, 1, Importance.LOW,
                        "Which qos to use for paho client connection")
                .define("mqtt.connector.ssl", Type.BOOLEAN, false, Importance.LOW,
                        "which qos to use for paho client connection")
                .define("mqtt.connector.ssl.ca", Type.STRING, "./ca.crt", Importance.HIGH,
                        "If secure (SSL) then path to CA is needed.")
                .define("mqtt.connector.ssl.crt", Type.STRING, "./client.crt", Importance.HIGH,
                        "If secure (SSL) then path to client crt is needed.")
                .define("mqtt.connector.ssl.key", Type.STRING, "./client.key", Importance.HIGH,
                        "If secure (SSL) then path to client key is needed.")
                .define("topics.regex", Type.STRING, "upstream*", Importance.MEDIUM,
                        "Kafka topic to publish on. This depends on processing unit.")
                .define("mqtt.connector.kafka.name", Type.STRING, "mqtt_downstream", Importance.MEDIUM,
                        "Name used by conenctor to Kafka connection api.")
                .define("mqtt.connector.mqtt_topic_key", Type.STRING, "topic", Importance.MEDIUM,
                        "Mqtt topic key, used to fetch topic from json record. processing unit. Topic used to publish to mqtt broker");
        logger.debug("ConfigDef loaded: '{}'", configDef.toEnrichedRst());
        return configDef;
    }

    public static void main(String[] args) {
        logger.info(configuration.toEnrichedRst());
    }

}
