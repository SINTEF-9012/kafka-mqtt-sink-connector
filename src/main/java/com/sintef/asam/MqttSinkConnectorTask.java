package com.sintef.asam;

import com.sintef.asam.util.SSLUtils;
import com.sintef.asam.util.Version;
import netscape.javascript.JSException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLSocketFactory;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


public class MqttSinkConnectorTask extends SinkTask {

    private MqttClient mqttClient;
    private String mqttClientId;
    private String connectorName;
    private String mqttTopicKey;
    private int qos;
    private MqttSinkConnectorConfig connectorConfiguration;
    private static final Logger logger = LogManager.getLogger(MqttSinkConnectorTask.class);
    private SSLSocketFactory sslSocketFactory;

    @Override
    public String version() {
        return Version.getVersion();
    }

    @Override
    public void start(Map<String, String> map) {
        connectorConfiguration = new MqttSinkConnectorConfig(map);
        connectorName = connectorConfiguration.getString("mqtt.connector.kafka.name");
        mqttClientId = connectorConfiguration.getString("mqtt.connector.client.id");
        mqttTopicKey = connectorConfiguration.getString("mqtt.connector.mqttt_topic_key");
        qos = connectorConfiguration.getInt("mqtt.connector.qos");
        logger.info("Starting MqttSinkConnectorTask with connector name: '{}'", connectorName);
        initMqttClient();
    }

    @Override
    public void put(Collection<SinkRecord> collection) {
        try {
            for (Iterator<SinkRecord> sinkRecordIterator = collection.iterator(); sinkRecordIterator.hasNext(); ) {
                SinkRecord sinkRecord = sinkRecordIterator.next();
                logger.debug("Received record: '{}',\n for on connector: '{}'", sinkRecord.value(), connectorName);
                JSONObject jsonSinkRecord;
                String downstreamMqttTopic;
                byte[] downstreamMqttPayload;
                // Try clause to fetch dynamic topic from the Kafka record.
                // "mqttTopicKey" can be used as a static topic for mqtt, and payload can be fetched directly from record.
                try {
                    try {
                        String stringSinkRecord = new String((byte[])sinkRecord.value(), "UTF-8");
                        jsonSinkRecord = new JSONObject(stringSinkRecord);
                        logger.debug("Successfull JSON parsing of record: '{}',\n for connector: '{}'", jsonSinkRecord.toString(), connectorName);
                        jsonSinkRecord.getString(mqttTopicKey);
                        downstreamMqttTopic = jsonSinkRecord.getString(mqttTopicKey);
                        downstreamMqttPayload = jsonSinkRecord.get("cam").toString().getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new JSException(e.getMessage());
                    }
                } catch (JSONException e) {
                    logger.error("Could not convert record to JSON '{}', and extract mqtt topic with key '{}'", sinkRecord, mqttTopicKey);
                    throw new MqttException(e);
                }
                // End of try clause to fetch dynamic topic from the Kafka record.
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(downstreamMqttPayload);
                mqttMessage.setQos(qos);
                if (!mqttClient.isConnected()) mqttClient.connect();
                logger.debug("Publishing message to topic '{}' with payload '{}'", downstreamMqttTopic, downstreamMqttPayload);
                mqttClient.publish(downstreamMqttTopic, mqttMessage);
            }
        } catch (MqttException e) {
            logger.error("ERROR: Not able to create and publish sink record  as mqtt message", e);
        }
    }

    @Override
    public void stop() {

    }

    private void initMqttClient() {
        logger.debug("Starting mqtt client: '{}', for connector: '{}'", mqttClientId, connectorName);
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setServerURIs(new String[] {connectorConfiguration.getString("mqtt.connector.broker.uri")});
        mqttConnectOptions.setConnectionTimeout(connectorConfiguration.getInt("mqtt.connector.connection_timeout"));
        mqttConnectOptions.setKeepAliveInterval(connectorConfiguration.getInt("mqtt.connector.keep_alive"));
        mqttConnectOptions.setCleanSession(connectorConfiguration.getBoolean("mqtt.connector.clean_session"));
        mqttConnectOptions.setKeepAliveInterval(connectorConfiguration.getInt("mqtt.connector.connection_timeout"));
        if (connectorConfiguration.getBoolean("mqtt.connector.ssl") == true) {
            logger.info("SSL TRUE for MqttSinkConnectorTask: '{}, and mqtt client: '{}'.", connectorName, mqttClientId);
            try {
                 String caCrtFilePath = connectorConfiguration.getString("mqtt.connector.ssl.ca");
                 String crtFilePath = connectorConfiguration.getString("mqtt.connector.ssl.crt");
                 String keyFilePath = connectorConfiguration.getString("mqtt.connector.ssl.key");
                 SSLUtils sslUtils = new SSLUtils(caCrtFilePath, crtFilePath, keyFilePath);
                 sslSocketFactory = sslUtils.getMqttSocketFactory();
                 mqttConnectOptions.setSocketFactory(sslSocketFactory);
            } catch (Exception e) {
                logger.error("Not able to create socket factory for mqtt client: '{}', and connector: '{}'", mqttClientId, connectorName);
                logger.error(e);
            }
        } else {
            logger.info("SSL FALSE for MqttSinkConnectorTask: '{}, and mqtt client: '{}'.", connectorName, mqttClientId);
        }

        try {
            mqttClient = new MqttClient(connectorConfiguration.getString("mqtt.connector.broker.uri"), mqttClientId, new MemoryPersistence());
            mqttClient.connect(mqttConnectOptions);
            logger.info("SUCCESSFULL MQTT CONNECTION for MqttSinkConnectorTask: '{}, and mqtt client: '{}'.", connectorName, mqttClientId);
        } catch (MqttException e) {
            logger.error("FAILED MQTT CONNECTION for MqttSinkConnectorTask: '{}, and mqtt client: '{}'.", connectorName, mqttClientId);
            logger.error(e);
        }

    }
}
