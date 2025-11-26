package seaBattle.server;

import seaBattle.protocol.cmd.CommandThread;
import seaBattle.server.ServerMain;

import java.util.Scanner;

class ServerStopThread extends CommandThread {

	static final String CMD_QUIT = "q";
	static final String CMD_QUIT_LONG = "quit";

	private final Scanner fin;

	public ServerStopThread() {
		fin = new Scanner(System.in);
		ServerMain.setStopFlag(false);
		putHandler(CMD_QUIT, CMD_QUIT_LONG, errorCode -> onCmdQuit());
		this.setDaemon(true);
		log("Admin console ready. Commands: users | sessions | challenges | info | quit");
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}

			if (!fin.hasNextLine())
				continue;
			String cmd = fin.nextLine().trim().toLowerCase();
			if (cmd.isEmpty())
				continue;

			switch (cmd) {
				case "q":
				case "quit":
					if (onCmdQuit())
						return;
					break;
				case "users":
					ServerMain.printUsers();
					break;
				case "sessions":
					ServerMain.printSessions();
					break;
				case "challenges":
					ServerMain.printChallenges();
					break;
				case "info":
					ServerMain.printServerInfo();
					break;
				default:
					log("Unknown command: " + cmd);
			}
		}
	}

	public boolean onCmdQuit() {
		log("Stopping server...");
		fin.close();
		ServerMain.setStopFlag(true);
		return true;
	}

	private void log(String msg) {
		System.out.printf("[%tT] [ADMIN] %s%n", System.currentTimeMillis(), msg);
	}
}