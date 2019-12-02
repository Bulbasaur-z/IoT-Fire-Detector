package MyMQTT;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;

//class TestSSL {
////    private  MqttConnectOptions options = null;
//
//    public static void test(){
//        MqttConnectOptions options = new MqttConnectOptions();
//        String serverUrl = "ssl://a2a1zfem06d51g-ats.iot.us-west-1.amazonaws.com:8883";
//        options.setCleanSession(true);
//        options.setConnectionTimeout(10);
//        options.setKeepAliveInterval(10);
//        MqttClient mqttClient = null;
//        try {
//            options.setSocketFactory(SSLUtil.getSocketFactory());
//            mqttClient = new MqttClient(serverUrl, "mainserver", null);
//            MqttRecieveCallback mqttRecieveCallback = new MqttRecieveCallback();
//            mqttClient.setCallback(mqttRecieveCallback);
//            mqttClient.connect(options);
//            MqttTopic topic = mqttClient.getTopic("iot/cmd");
//            MqttMessage mqttMessage = new MqttMessage();
//            mqttMessage.setQos(1);
//            mqttMessage.setPayload("test From Main Server".getBytes());
//            MqttDeliveryToken publish = topic.publish(mqttMessage);
//            if(!publish.isComplete()){
//                System.out.println("Complete");
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//}

class SSLUtil {
    static SSLSocketFactory getSocketFactory() throws Exception {
        final String caCrtFile = "mqttCA/Amazon_Root_CA_1.pem";
        final String crtFile = "mqttCA/690ce7c017-certificate.pem.crt";
        final String keyFile = "mqttCA/690ce7c017-private.pem.key";
        Security.addProvider(new BouncyCastleProvider());
        //load CA certificate
        PEMReader reader = new PEMReader(new FileReader(caCrtFile));
        X509Certificate caCert = (X509Certificate) reader.readObject();
        reader.close();

        //load client certificate
        reader = new PEMReader(new FileReader(crtFile));
        X509Certificate cert = (X509Certificate) reader.readObject();
        reader.close();

        //load client private key
        reader = new PEMReader(new FileReader(keyFile));
        KeyPair keyPair = (KeyPair) reader.readObject();
        reader.close();

        //CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(caKs);

        //client key and certificates are sent to server
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", keyPair.getPrivate(), "".toCharArray(),
                new java.security.cert.Certificate[]{cert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "".toCharArray());

        //finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return context.getSocketFactory();
    }
}