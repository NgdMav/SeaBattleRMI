package seaBattle.server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

import seaBattle.protocol.Protocol;


public class ServerMain {

    private static SeaBattleServiceImpl serviceImpl;

	public static void main(String[] args) {
        try {
            serviceImpl = new SeaBattleServiceImpl();
            String name = System.getProperty("servername", "SeaBattleService");

            LocateRegistry.createRegistry(Protocol.PORT);

            Naming.rebind("SeaBattleService", serviceImpl);

            ServerMain.log("SERVER",name + " is open and ready for customers.");

            ServerStopThread serverStopThread = new ServerStopThread();
            serverStopThread.start();
        }
        catch (Exception e) {
            System.err.println("SERVER ERROR: " + e);
            System.exit(1);
        }
	}

	public static void log(String tag, String msg) {
		System.out.printf("[%tT] [%s] %s%n", System.currentTimeMillis(), tag, msg);
	}

    public static void setStopFlag(boolean b) {
        serviceImpl.setStopFlag(b);
    }

    public static void printUsers() {
        serviceImpl.printUsers();
    }

    public static void printSessions() {
        serviceImpl.printSessions();
    }

    public static void printChallenges() {
        serviceImpl.printChallenges();
    }

    public static void printServerInfo() {
        serviceImpl.printServerInfo();
    }
}