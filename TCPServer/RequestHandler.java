package TCPServer;

import DBoperation.DBEntry;
import LogInfo.AppLog;
import MyMQTT.MyMqttClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author yucong li
 * @version 1.0.1
 * json {'username':,   'password':,   'type': not null,    'device_id': int,   'data': }
 */
public class RequestHandler {
    private ThreadPoolExecutor service = null;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private Logger logger = AppLog.getLogger();

    public RequestHandler() {
        service = new ThreadPoolExecutor(5, 20,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(512),
                new ThreadFactoryBuilder().setNameFormat("demo-pool-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    /**
     * Asynchronous multithreading for task
     *
     * @param RawData String from socket('utf-8')
     */
    private void TaskSubmit(SocketChannel sc, String RawData) {
        JsonObject data = (JsonObject) JsonParser.parseString(RawData);
        String type = data.get("type").getAsString();
        switch (type) {
            case "FireWarning":
                try {
                    sc.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "can't close socketchannel", e);
                } finally {
                    FireWarning fireWarning = new FireWarning(data);
                    this.service.execute(fireWarning);
                }
                break;
            case "register":
                RegisterRequest registerRequest = new RegisterRequest(sc, data);
                this.service.execute(registerRequest);
                break;
            case "login":
                LoginRequest loginRequest = new LoginRequest(sc, data);
                this.service.execute(loginRequest);
                break;
            case "addDevice":
                AddDevice addDevice = new AddDevice(sc, data);
                this.service.execute(addDevice);
                break;
            case "closeRing":
                try {
                    sc.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "can't close socketchannel", e);
                } finally {
                    CloseRing closeRing = new CloseRing(data);
                    this.service.execute(closeRing);
                }
                break;
            default:
                String msg = "Invalid type" + " Data: " + data;
                logger.info(msg);
        }
    }

    public void handleRead(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        int numBytesRead;
        try {
            numBytesRead = sc.read(buffer);
            if (numBytesRead > 0) {
                buffer.flip();
                byte[] buff = new byte[numBytesRead];
                buffer.get(buff, 0, numBytesRead);
                String RawData = new String(buff, 0, numBytesRead, StandardCharsets.UTF_8);
                buffer.clear();
                key.cancel();
                this.TaskSubmit(sc, RawData);
            } else {
                buffer.clear();
                key.cancel();
                sc.close();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Exception happened during processing of request from" + sc, ex);
            key.cancel();
        }

    }

    public void handleAccept(SelectionKey key) {
        try {
            //接收到连接请求时
            if (key.isAcceptable()) {
                SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
                sc.configureBlocking(false);
                sc.register(key.selector(), SelectionKey.OP_READ);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Fails to accept", ex);
        }
    }

    public void CloseRequestProcess() {
        try {
            this.service.shutdown();
            if (!this.service.awaitTermination(1, TimeUnit.SECONDS)) {
                System.out.println("Time out, terminate thread pool!");
                this.service.shutdownNow();
            }
            System.out.println("Thread pool closed");
        } catch (InterruptedException e) {
            this.service.shutdownNow();
            e.printStackTrace();
        }
    }
}


/**
 *
 */
class FireWarning implements Runnable {
    private String deviceID;


    public FireWarning(@NotNull JsonObject data) {
        this.deviceID = data.get("deviceID").getAsString();
    }


    @Override
    public void run() {
        System.out.println(this.deviceID + " FireWaring!");
        DBEntry dbEntry = new DBEntry("IoTFireDetector",
                "127.0.0.1",
                //"iotee542.cccfsroldvdd.us-west-1.rds.amazonaws.com",
                "3306", "root",
                "Qianr0912");
        List<Map> users = dbEntry.ReadData("relation_table", "device_ID", deviceID);
        String locate = (String) dbEntry.ReadData("sensor_info", "device_ID", deviceID)
                .get(0).get("locate");
        //格式转换
        JsonObject sendData = new JsonObject();
        sendData.addProperty("deviceID", deviceID);
        sendData.addProperty("locate", locate);
        MyMqttClient PubClient = new MyMqttClient("MasterServer",
                "54.193.9.150:1883", "TCP");
        PubClient.Connect();
        for (Map user : users) {
            String topic = (String) user.get("username");
            PubClient.publishMessage(topic, sendData.toString(), 1);
        }
        //close mqtt connect
        PubClient.closeConnect();
    }

}

class RegisterRequest implements Runnable {
    private SocketChannel sc;
    private String username;
    private String password;
    private Logger logger = AppLog.getLogger();

    public RegisterRequest(SocketChannel sc, JsonObject data) {
        this.username = data.get("username").getAsString();
        this.password = data.get("password").getAsString();
        this.sc = sc;
    }

    @Override
    public void run() {
        DBEntry dbEntry = new DBEntry("IoTFireDetector",
                "iotee542.cccfsroldvdd.us-west-1.rds.amazonaws.com",
                "3306", "root",
                "Qianr0912");
        List<Map> user_info = dbEntry.ReadData("user_Info", "username", username);
        ByteBuffer buf;
        if (user_info == null) {
            buf = ByteBuffer.wrap("approved".getBytes(StandardCharsets.UTF_8));
            if (dbEntry.InsertData("user_Info", new String[]{"username", "password"},
                    new String[]{username, password})) {
                logger.info("Register successfully From: " + this.username);
            } else {
                logger.warning("Fails to write into database");
            }
        } else {
            buf = ByteBuffer.wrap("wrong".getBytes(StandardCharsets.UTF_8));
            logger.info("Reject register From: " + this.username);
        }
        try {
            sc.write(buf);
            sc.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error on send or close socket", e);
        }
    }
}

class LoginRequest implements Runnable {
    private String username;
    private String password;
    private SocketChannel sc;
    private Logger logger = AppLog.getLogger();

    public LoginRequest(SocketChannel sc, JsonObject data) {
        this.username = data.get("username").getAsString();
        this.password = data.get("password").getAsString();
        this.sc = sc;
    }

    @Override
    public void run() {
        DBEntry dbEntry = new DBEntry("IoTFireDetector",
                "iotee542.cccfsroldvdd.us-west-1.rds.amazonaws.com",
                "3306", "root", "Qianr0912");
        List<Map> user_info = dbEntry.ReadData("user_Info", "username", username);
        ByteBuffer buffer;
        if (user_info != null && user_info.get(0).get("password").equals(this.password)) {
            buffer = ByteBuffer.wrap("Approved".getBytes(StandardCharsets.UTF_8));
            String msg = "Login: " + this.username;
            logger.info(msg);
        } else {
            buffer = ByteBuffer.wrap("wrong".getBytes(StandardCharsets.UTF_8));
            String msg = "Login Reject from: " + this.username;
            logger.info(msg);
        }
        try {
            sc.write(buffer);
            sc.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error on send or close socket", e);
        }
    }
}


class CloseRing implements Runnable {
    private String deviceID;

    public CloseRing(JsonObject data) {
        this.deviceID = data.get("deviceID").getAsString();
    }

    @Override
    public void run() {
        MyMqttClient myMqttClient = new MyMqttClient("MasterServer",
                "a2a1zfem06d51g-ats.iot.us-west-1.amazonaws.com:8883", "SSL");
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "CloseRing");
        jsonObject.addProperty("deviceID", deviceID);
        myMqttClient.Connect();
        myMqttClient.publishMessage("iot/cmd", jsonObject.toString(), 1);
        myMqttClient.closeConnect();
    }
}

class AddDevice implements Runnable {
    private String username;
    private String deviceID;
    private String locate;
    private Logger logger = AppLog.getLogger();
    private SocketChannel sc;

    public AddDevice(SocketChannel sc, JsonObject data) {
        this.username = data.get("username").getAsString();
        this.deviceID = data.get("deviceID").getAsString();
        this.locate = data.get("locate").getAsString();
        this.sc = sc;
    }

    @Override
    public void run() {
        DBEntry dbEntry = new DBEntry("IoTFireDetector",
                "iotee542.cccfsroldvdd.us-west-1.rds.amazonaws.com",
                "3306", "root", "Qianr0912");
        ByteBuffer buf;
        if (dbEntry.InsertData("relation_table", new String[]{"username", "device_ID"},
                new String[]{username, deviceID}) &&
                dbEntry.InsertData("sensor_info", new String[]{"device_ID", "locate"},
                        new String[]{deviceID, locate})) {
            logger.info("Add device successfully From: " + this.username);
            buf = ByteBuffer.wrap("OK".getBytes(StandardCharsets.UTF_8));
        } else {
            logger.warning("Fails to write into database");
            buf = ByteBuffer.wrap("wrong".getBytes(StandardCharsets.UTF_8));
        }
        try {
            sc.write(buf);
            sc.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error on send or close socket", e);
        }
    }

}