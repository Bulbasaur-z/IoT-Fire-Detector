package MyMQTT;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MyMqttReceieveMessage {

    private static int Qos = 1;
    private String BrokerIp = null;
    private String clientId = null;
    private MemoryPersistence memoryPersistence = null;
    private MqttClient mqttClient = null;
    private MqttConnectOptions mqttConnectOptions = null;

    public MyMqttReceieveMessage(String BrokerIp, String clientId){
        this.BrokerIp = "tcp://" + BrokerIp;
        this.clientId = clientId;
        this.init();
    }
    public  void init(){
        this.mqttConnectOptions = new MqttConnectOptions();
        this.memoryPersistence  = new MemoryPersistence();
        try{
            mqttClient = new MqttClient(this.BrokerIp, this.clientId, this.memoryPersistence);
        }catch (MqttException e){
            e.printStackTrace();
        }
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(30);
        mqttConnectOptions.setKeepAliveInterval(45);
        if(null != mqttClient && !mqttClient.isConnected()){
            mqttClient.setCallback(new MqttRecieveCallback());
            try {
                mqttClient.connect();
            }catch (MqttException e){
                e.printStackTrace();
            }
        }
    }

    public  void receive(String topic){
        int[] Qoss = {MyMqttReceieveMessage.Qos};
        String[] topics = {topic};
        if(null != this.mqttClient && this.mqttClient.isConnected()){
            try{
                mqttClient.subscribe(topics, Qoss);
            }catch (MqttException e){
                e.printStackTrace();
            }
        }else{
            this.init();
            receive(topic);
        }

    }
}
