# kafka-mqtt-sink-connector
A connector plugin to use with Kafka's Connect API. It is contains functionality to extract a topic from the Kafka record, and publish to that topic on the mqtt broker, if dynamic publishing is required. The connector can be configured to use SSL in communication with the mqtt broker.

## Setting up a single Zookeeper and Kafka instance
To be able to test the connector, we first need to set up the Kafka infrastructure. For simplicity, we start by configuring single nodes (one Zookeeper and one Kafka).

### Prerequisites
- Java version >8 installed to run Kafka and this source-connector. Check if Java is installed, and which version by running `java -version` in your terminal. We use `openjdk version "11.0.6" 2020-01-14`.
- Linux. We are running this setup on Ubuntu 16.4.
- A mqtt-broker. We use EMQX
- Maven. Check if maven is installed properly with running `mvn -v` in your terminal. We use Maven 3.6.0

### Mqtt Broker - EMQX
- Download EMQX from [https://www.emqx.io/downloads](https://www.emqx.io/downloads)
- Extract the download to your desired destination, here termed _"path-to-emqx"_.
- Run the following command in your terminal to start the EMQX broker:
```
"path-to-emqx"/emqx/bin/emqx start
```
- Check that EMQX is running with the following terminal command:
 ```
"path-to-emqx"/emqx/bin/emqx_ctl status
```

### Download Kafka binaries
Download a binary Kafka release from https://kafka.apache.org/downloads. We work with the compressed download:
>kafka_2.13-2.4.1.tgz
Extract the download to your desired destination, here termed _"path-to-kafka"_.

### Zookeeper
About Zookeeper:
>"Zookeeper is a top-level software developed by Apache that acts as a centralized service and is used to maintain naming and configuration data and to provide flexible and robust synchronization within distributed systems. Zookeeper keeps track of status of the Kafka cluster nodes and it also keeps track of Kafka topics, partitions etc.
Zookeeper it self is allowing multiple clients to perform simultaneous reads and writes and acts as a shared configuration service within the system. The Zookeeper atomic broadcast (ZAB) protocol i s the brains of the whole system, making it possible for Zookeeper to act as an atomic broadcast system and issue orderly updates." [Cloudkarafka](https://www.cloudkarafka.com/blog/2018-07-04-cloudkarafka_what_is_zookeeper.html)

**Start Zookeeper**
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/zookeeper-server-start.sh "path-to-kafka"/kafka_2.13-2.4.1/config/zookeeper.properties
```
P.S. The default properties of zookeeper.properties works well for this tutorial's purpose. It will start Zookeeper on the default port `2181`.

### Kafka Broker
As mentioned, we will only kick up a single instance Kafka Broker. The Kafka Broker will use `"path-to-kafka"/kafka_2.13-2.4.1/config/server.properties`, and it could be worth checking that
```
zookeeper.connect=localhost:2181
```
or set according to your custom configuration in `zookeeper.properties`.

**Start Kafka Broker**
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/kafka-server-start.sh "path-to-kafka"/kafka_2.13-2.4.1/config/server.properties
```

**Create Kafka Topic**
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/kafka-topics --create --bootstrap-server 127.0.0.1:9092 --replication-factor 1 --partitions 1 --topic test
```

### Kafka Connect
The Kafka Connect API is what we utilise as a framework around our connectors, to handle scaling, polling from Kafka, work distribution etc. Kafka Connect can run as _connect-standalone_ or as _connect-distributed_. The _connect-standalone_ is engineered for demo and test purposes, as it cannot provide fallback in a production environment.

**Start Kafka Connect**
Follow the respective steps below to start Kafka Connect in preferred mode.

_Connect in general_
Build this java maven project, but navigating to root `kafka-mqtt-source-connector` in a terminal and typing:
```
mvn install
```
`Copy the kafka-mqtt-source-connector-"version".jar` from your maven target directory to the directory `/usr/share/java/kafka`:

```
sudo mkdir /usr/share/java/kafka
sudo cp ./target/*with-dependencies.jar /usr/share/java/kafka/.
```

**Insecure - using tcp**
__*Connect Standalone*__
1. Uncomment `plugin.path` in `"path-to-kafka"/kafka_2.13-2.4.1/config/connect-standalone.properties`, so that it is set to
```
plugin.path=/usr/share/java,/usr/local/share/kafka/plugins,/usr/local/share/java/
```
2. Copy  the accompanying source connector properties file in this repository, **[sink-connect-mqtt.properties](https://github.com/SINTEF-9012/kafka-mqtt-sink-connector/src/main/resources/sink-connect-mqtt.properties)**, to `"path-to-kafka"/kafka_2.13-2.4.1/config/` (or create a new properties file with the same name in the given directory).
3. Ensure the following configuration in `source-connect-mqtt.properties`:
```
name=mqtt-sink-connector
tasks.max=1
connector.class=com.sintef.asam.MqttSinkConnector
mqtt.connector.broker.uri=tcp://0.0.0.0:1883
mqtt.connector.mqtt_topic_key=topic
topics.regex=test*
```
where `mqtt.connector.mqtt_topic_key` is the key used by the connector to fetch a topic from the Kafka record. When the Kafka record is pushed to the sink connector, the sink connector decodes the record into a JSON. After that a looup is made on the `mqtt.connector.mqtt_topic_key` and the fetched value is the topic used to publish to the mqtt broker.
`topics.regex` is standard property of the SinkConnector we inherit from, and the regex determines which topic(s) the sink connector should subscribe to from Kafka. 

4. Start _Connect Standalone_ with our connector by typing (this may take a minute or two):
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/connect-standalone.sh "path-to-kafka"/kafka_2.13-2.4.1/config/connect-standalone.properties "path-to-kafka"/kafka_2.13-2.4.1/config/sink-connect-mqtt.properties
```

__*Connect Distributed*__
Kafka Connect Distributed does not need properties files to configure connectors. It uses the Kafka Connect REST-interface.
5. Uncomment `plugin.path` in `"path-to-kafka"/kafka_2.13-2.4.1/config/connect-distributed.properties`, so that it is set to
```
plugin.path=/usr/share/java,/usr/local/share/kafka/plugins,/usr/local/share/java/
```
and that `rest.port` so that it is set to
```
rest.port=19005
```
which will help one to avoid some "bind" exceptions. This will be the port for the Connect REST-interface.
6. Start _Connect Distributed_ with by typing (this may take a minute or two):
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/connect-distributed.sh "path-to-kafka"/kafka_2.13-2.4.1/config/connect-distributed.properties
```
7. Start our connector by posting the following command to the Connect REST-interface:
```
curl -s -X POST -H 'Content-Type: application/json' http://127.0.0.1:19005/connectors -d '{"name":"mqtt-sink-connector","config":{"connector.class":"com.sintef.asam.MqttSinkConnector","tasks.max":"1","mqtt.connector.broker.uri":"tcp://localhost:1883", "mqtt.connector.broker.topics.regex":"test*","mqtt.connector.mqtt_topic_key":"topic"}}'
```
8. Inspect the terminal where you started Conncet Distributed, and after the connector seem to have successfully started, check the existence by typing:
```
curl 'Content-Type: application/json' http://127.0.0.1:19005/connectors
```
where the response is an array with connectors by name.
9. Test the connector by starting a mosquitto subscriber - subscribing to the EMQX broker - in a new terminal window:
```
mosquitto_sub -h 127.0.0.1 -p 1883 -t test
```
10. Then publish something to the test topic on the Kafka broker. Start a Kafka console producer in yet a new terminal window:
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/kafka-console-producer --broker-list 127.0.0.1:9092 --topic test 
```
Type the following in the _kafka-console-producer_:
```
{"Hello": "World!", "topic":"test"}
```
and see the message appear in your mosquitto subscriber terminal window.

Note that the sink connector is made to receive JSON formatted messages. The JSON also needs to have a key/value pair where key is equal to the property `mqtt.connector.mqtt_topic_key`.

**Secure - using SSL**  

__Setting up your own certificate authority (for test purposes) and configure MQTT broker to use SSL__
We first need certificates and keys to encrypt our _secure socket layer_ (SSL) communication to and from the broker. To make your own certificate authority, and to create a client certificate and a client key, this provides very thorough and instructive guide: https://deliciousbrains.com/ssl-certificate-authority-for-local-https-development/

Let us assume that you have a `/home/CA.crt`, a `/home/client.crt` and a `/home/client.key`, we configure our EMQX broker by finding the configuration file `"path-to-emqx"/etc/emqx.conf` and setting/uncommenting the following properties:
 ```
listener.ssl.external = 8883
listener.ssl.external.access.1 = allow all
listener.ssl.external.keyfile = /home/client.key
listener.ssl.external.certfile = /home/client.cert
listener.ssl.external.cacertfile = /home/CA.cert
```
Then restart your mqtt broker.

__*Connect Distributed*__
12. Delete the previous made connector using TCP, if one is running, using the following call to the Connect REST-interface:
 ```
curl -X DELETE 'Content-Type: application/json' http://127.0.0.1:19005/connectors/mqtt-sink-connector
```
if your connector was named `mqtt-source-connector`. Check running connectors by name using:
```
curl 'Content-Type: application/json' http://127.0.0.1:19005/connectors
```
13. Copy your CA certificate, client certificate and client key to desired directory. In our test case we have the full paths `/home/ca.crt`, `/home/client.crt` and `/home/client.key`.
14. Start our connector by posting the following command to the Connect REST-interface:
```
curl -s -X POST -H 'Content-Type: application/json' http://127.0.0.1:19005/connectors -d '{"name":"mqtt-sink-connector","config":{"connector.class":"com.sintef.asam.MqttSinkConnector","tasks.max":"1","mqtt.connector.broker.uri":"tcp://localhost:1883", "mqtt.connector.broker.topics.regex":"test*","mqtt.connector.mqtt_topic_key":"topic","mqtt.connector.ssl":true, "mqtt.connector.ssl.ca":"/home/ca.crt/","mqtt.connector.ssl.crt":"/home/client.crt","mqtt.connector.ssl.key":"/home/client.key"}}'
```
15. Test the connector by making a Kafka Consumer subscribing to the topic `test`:
```
mosquitto_sub --url mqtts://127.0.0.1:8883/test --cafile /home/ca.crt --cert /home/client.crt --key /home/client.key --insecure --tls-version tlsv1.2
```
16. Then publish something to the test topic on the Kafka broker. Start a Kafka console producer in yet a new terminal window:
```
"path-to-kafka"/kafka_2.13-2.4.1/bin/kafka-console-producer --broker-list 127.0.0.1:9092 --topic test 
```
Type the following in the _kafka-console-producer_:
```
{"Hello": "World!", "topic":"test"}
```
and see the message appear in your mosquitto subscriber terminal window.

Note that the sink connector is made to receive JSON formatted messages. The JSON also needs to have a key/value pair where key is equal to the property `mqtt.connector.mqtt_topic_key`.
