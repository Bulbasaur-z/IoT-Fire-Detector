package UserInterface;

import LogInfo.AppLog;
import TCPServer.MasterApp;

import java.util.Scanner;


public class ServerMain {

    public static void main(String[] args) {
        String p = System.getProperty("port");
        AppLog appLog = new AppLog();
        appLog.StartLog();
        int PN = 40002;
        try {
            if (p != null) {
                PN = Integer.parseInt(p);
            }
            MasterApp masterApp = new MasterApp("0.0.0.0", PN);
            masterApp.ServerRun();
            System.out.println("Enter Quit() to exit App");
            Scanner s = new Scanner(System.in);
            while (true) {
                if (s.next().equals("Quit()")) break;
            }
            masterApp.ServerStop();
            appLog.CloseLog();
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage() +"\nProgram Exit!");
        }
    }
}