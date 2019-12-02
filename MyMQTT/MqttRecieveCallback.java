package MyMQTT;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class MqttRecieveCallback implements MqttCallback{
    @Override
    public  void connectionLost(Throwable cause){

    }
    @Override
    public void messageArrived(String topic, MqttMessage message){
        System.out.println("Client 接受消息主题：" + topic);
        System.out.println("Client 接受消息Qos : " + message.getQos());
        System.out.println("Client 接受消息内容 ： " + new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token){

    }
}
