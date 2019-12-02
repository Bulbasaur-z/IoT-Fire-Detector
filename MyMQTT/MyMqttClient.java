package MyMQTT;

import LogInfo.AppLog;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MyMqttClient {
    private MqttClient mqttClient = null;
    private MemoryPersistence memoryPersistence = new MemoryPersistence();
    private MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    private Logger logger = AppLog.getLogger();

    public MyMqttClient(String clientId, String BrokerIp, String protocol) {
        try {
            if (protocol.equals("TCP")) {
                TCPConfig();
                mqttClient = new MqttClient("tcp://" + BrokerIp, clientId, memoryPersistence);
            } else if (protocol.equals("SSL")) {
                SSLConfig();
                mqttClient = new MqttClient("ssl://" + BrokerIp, clientId, memoryPersistence);
            }
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Can't create MqttClient object", e);
        }
    }

    private void TCPConfig() {
        //初始化MqttClient
        //使用内存持久性作为客户端断开连接时清除所有的状态
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(30);
        mqttConnectOptions.setKeepAliveInterval(30);
    }

    private void SSLConfig() {
        TCPConfig();
        try {
            mqttConnectOptions.setSocketFactory(SSLUtil.getSocketFactory());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Can't initial ssl connection", e);
        }
    }

    public void Connect() {
        if (mqttClient != null) {
            if (!mqttClient.isConnected()) {
                //创建回调函数对象
                MqttRecieveCallback mqttRecieveCallback = new MqttRecieveCallback();
                mqttClient.setCallback(mqttRecieveCallback);
                //创建连接
                try {
                    this.mqttClient.connect(mqttConnectOptions);
                } catch (MqttException e) {
                    logger.log(Level.SEVERE, "Fail to connect MQTT Broker!", e);
                }
            }
        } else {
            logger.log(Level.SEVERE, "wrong Protocol, can't create Mqtt client!");
        }
    }


    //关闭连接
    public void closeConnect() {
        // 关闭储存方式
        if (null != this.memoryPersistence) {
            try {
                this.memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                e.printStackTrace();
            }
        }
        //关闭连接
        if (null != mqttClient) {
            if (mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //发布消息
    public void publishMessage(String pubTopic, String message, int qos) {
        if (null != mqttClient && mqttClient.isConnected()) {
            String msg = "publish message: " + pubTopic;
            msg += "--" + message;
            msg += "--id: " + mqttClient.getClientId();
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());

            MqttTopic topic = mqttClient.getTopic(pubTopic);

            if (null != topic) {
                try {
                    MqttDeliveryToken publish = topic.publish(mqttMessage);
                    if (!publish.isComplete()) {
                        msg += "--Publish message successful!";
                    } else {
                        msg += "--Failed to Publish message！";
                    }
                    logger.info(msg);
                } catch (MqttException e) {
                    msg += "--Failed to Publish message！";
                    logger.log(Level.SEVERE, msg, e);
                }
            }
        } else {
            logger.log(Level.SEVERE, "MQTT client didn't connect");
        }
    }

    //重新连接
    public void reConnect() {
        if (null != mqttClient) {
            if (!mqttClient.isConnected()) {
                if (null != mqttConnectOptions) {
                    try {
                        mqttClient.connect(this.mqttConnectOptions);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("mqttConnetcOption is null");
                }
            } else {
                System.out.println("mqttClient is null or connect");
            }
        } else {
            this.Connect();
        }
    }

    //订阅主题
    public void subTopic(String topic) {
        if (null != mqttClient && mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(topic, 1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("mqttClient is error");
        }
    }

    //清空主题
    public void cleanTopic(String topic) {
        if (null != mqttClient && !mqttClient.isConnected()) {
            try {
                mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("mqttClient is error");
        }
    }
}
