package com.sintef.asam.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.common.record.Record;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MqttConverter {

    /*
    The desired mqtt topic we want to publish to is encoded as a field in JSON format,
    received as a byte array from a Kafka record.
    */
    private Record record;
    private String mqttTopic;
    private MqttMessage mqttMessage;
    private int qos;
    private String mqttTopicKey;
    private static final Logger logger = LoggerFactory.getLogger(MqttConverter.class);

    public MqttConverter(Record record, int mqttQos, String mqttTopicKey) {
        this.record = record;
        this.qos = mqttQos;
        this.mqttTopicKey = mqttTopicKey;
        this.mqttMessage = new MqttMessage();
    }

    private void convert() {
        try {
            JSONObject payloadAsJSON = JSON.parseObject(record.toString());
            mqttTopic = payloadAsJSON.getString(mqttTopicKey);
            mqttMessage.setPayload(record.value().array());
            mqttMessage.setQos(qos);
        } catch (JSONException e) {
            logger.error("Could not convert the following record to JSON: '{}'", record.toString(), e);
        }
    }

    private MqttMessage getMqttMessage() {
        return mqttMessage;
    }

    private String getMqttTopic() {
        return mqttTopic;
    }
}
