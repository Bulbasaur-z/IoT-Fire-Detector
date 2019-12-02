package TCPServer;


import LogInfo.AppLog;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MasterApp {
    private int Port;
    private String IP;
    private Thread listenThread = null;
    private RequestHandler requestHandler = null;
    private Logger logger = AppLog.getLogger();
    public MasterApp(String IP, int Port) {
        this.Port = Port;
        this.IP = IP;
    }

    public void ServerRun() throws IllegalArgumentException{
            requestHandler = new RequestHandler();
            logger.info("TCP Processor thread pool open");
            PortListen portListen = new PortListen(this.IP, this.Port, requestHandler);
            listenThread = new Thread(portListen);
            listenThread.start();
    }

    public void ServerStop(){
        listenThread.interrupt();
        try {
            listenThread.join();
            requestHandler.CloseRequestProcess();
            Thread.sleep(3000);
            System.out.println("close main service");
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}


class PortListen implements Runnable {
    private Selector selector;
    private Logger logger = AppLog.getLogger();
    private RequestHandler requestHandler;
    private String msg = "...Start Port Listening";
    public PortListen(String IP, int Port, RequestHandler requestHandler) throws IllegalArgumentException {
        try {
            selector = Selector.open();
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(IP, Port));
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            msg += Port + "...";
            this.requestHandler = requestHandler;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Server fails to open! Invalid IP address or Port", ex);
            throw new IllegalArgumentException("Invalid IP address or Port");
        }
    }

    @Override
    public void run() {
        logger.info(msg );
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (selector.select(1000) == 0) {
                    continue;
                }
                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
                while (keyIter.hasNext()) {
                    SelectionKey key = keyIter.next();
                    if (key.isAcceptable()) {
                        this.requestHandler.handleAccept(key);
                    }
                    if (key.isReadable()) {
                        this.requestHandler.handleRead(key);
                    }
                    keyIter.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Close listen thread");
    }
}