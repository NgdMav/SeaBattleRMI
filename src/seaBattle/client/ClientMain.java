	package seaBattle.client;

	import java.io.IOException;
	import java.io.ObjectInput;
	import java.io.ObjectInputStream;
	import java.io.ObjectOutput;
	import java.io.ObjectOutputStream;
	import java.net.InetAddress;
	import java.net.Socket;
	import java.util.ArrayList;
	import java.util.Arrays;
	import java.util.List;
	import java.util.Random;
	import java.util.Scanner;
	import java.util.TreeMap;
	import javax.crypto.spec.ChaCha20ParameterSpec;
	import javax.naming.event.ObjectChangeListener;

	import seaBattle.gameLogic.Ship;
	import seaBattle.protocol.Protocol;
	import seaBattle.protocol.messages.Message;
	import seaBattle.protocol.messages.messages.MessageChallenge;
	import seaBattle.protocol.messages.messages.MessageConnect;
	import seaBattle.protocol.messages.messages.MessageDisconnect;
	import seaBattle.protocol.messages.messages.MessageIgnore;
	import seaBattle.protocol.messages.messages.MessagePing;
	import seaBattle.protocol.messages.messages.MessageUser;
	import seaBattle.protocol.messages.messagesRequest.MessageChallengeRequest;
	import seaBattle.protocol.messages.messagesRequest.MessageGameStart;
	import seaBattle.protocol.messages.messagesRequest.MessageMove;
	import seaBattle.protocol.messages.messagesRequest.MessageOpponentReady;
	import seaBattle.protocol.messages.messagesRequest.MessagePlaceShips;
	import seaBattle.protocol.messages.messagesRequest.MessageReadyToPlay;
	import seaBattle.protocol.messages.messagesResponse.MessageChallengeFinal;
	import seaBattle.protocol.messages.messagesResponse.MessageChallengeResponse;
	import seaBattle.protocol.messages.messagesResult.MessageChallengeResult;
	import seaBattle.protocol.messages.messagesResult.MessageConnectResult;
	import seaBattle.protocol.messages.messagesResult.MessageError;
	import seaBattle.protocol.messages.messagesResult.MessageGameOver;
	import seaBattle.protocol.messages.messagesResult.MessageMoveResult;
	import seaBattle.protocol.messages.messagesResult.MessagePlaceShipsResult;
	import seaBattle.protocol.messages.messagesResult.MessageUserResult;

	@SuppressWarnings("deprecation")
	public class ClientMain 
	{
		public static void main(String[] args)  
		{
			if (args.length < 2 || args.length > 3) 
			{
				System.err.println(	"Invalid number of arguments\n" + "Use: nickname fullname [host]" );
				waitKeyToStop();
				return;
			}
			try ( Socket sock = ( args.length == 2 ? 
					new Socket( InetAddress.getLocalHost(), Protocol.PORT):
					new Socket( args[2], Protocol.PORT ) )) 
			{ 		
				System.err.println("initialized");
				session(sock, args[0], args[1]);
			} 
			catch ( Exception e) 
			{
				System.err.println(e);
			} 
			finally 
			{
				System.err.println("bye...");
			}
		}

		static void waitKeyToStop() 
		{
			System.err.println("Press a key to stop...");
			try 
			{
				System.in.read();
			} 
			catch (IOException e) 
			{
			}
		}

			static class ChallengeFromPlayer
			{
				Long challengeID;
				String playerNickName;

				public ChallengeFromPlayer(Long challengeID, String playerNickName) {
					this.challengeID = challengeID;
					this.playerNickName = playerNickName;
				}	

				public Long getChallengeID()
				{
					return challengeID;
				}

				public String getPlayerNickName()
				{
					return playerNickName;
				}
			}

			static class Session {
			boolean connected = false;
			String userNickName = null;
			String userFullName = null;
			String userOponnentNick = null;
			List<ChallengeFromPlayer> challenges = null;
			Long currentGameSessionID = null;
			boolean shipsReady = false;
			boolean gameStarted = false;
			boolean playerReady = false;
			boolean yourTurn = false;
			String notProcessedSymbols = new String("0123456789YN");
			Session(String userNickName, String userFullName) 
			{
				this.userNickName = userNickName;
				this.userFullName = userFullName;
				this.challenges = new ArrayList<ChallengeFromPlayer>();
			}
		}

		static class ListenerThread extends Thread
		{
			ObjectInputStream ois;
			Session ses;
			private volatile boolean running = true;
			public ListenerThread(ObjectInputStream ois, Session ses)
			{
				this.ois = ois;
				this.ses = ses;
				setDaemon(true);
			}
			public void stopListening() {
				running = false;
				this.interrupt();
			}
			@Override
			public void run()
			{
				while(running)
				{
					try
					{
						Message msg = (Message) ois.readObject();
						if (msg == null) {
							System.out.println("Received null message, continuing...");
							continue;
						}
						switch (msg.getID()) 
						{
							case Protocol.CMD_ERROR:
								System.out.println(((MessageError) msg).getMessage());
								break;
							case Protocol.CMD_PONG:
								System.out.println("pong");
								break;
							case Protocol.CMD_USER:
							{
								System.out.print("active users: ");
								String[] users = ((MessageUserResult) msg).getNics();
								for(String user : users)
								{
									if(!ses.userNickName.equals(user))
										System.out.print(user + "; ");
								} 
								System.out.println();
								break;
							}
							case Protocol.CMD_CHALLENGE_REQUEST:
							{
								ChallengeFromPlayer cfp = new ChallengeFromPlayer(((MessageChallengeRequest) msg).getChallengeId(), ((MessageChallengeRequest) msg).getFrom());
								ses.challenges.add(cfp);
								System.out.println("You've received challenge request " + ((MessageChallengeRequest) msg).getChallengeId() + " from " + ((MessageChallengeRequest) msg).getFrom());
								System.out.println("To answer the challenge enter 'answer'");
								break;
							}
							case Protocol.CMD_CHALLENGE:
							{
								System.out.println(((MessageChallengeResult) msg).getMessage());
								break;
							}
							case Protocol.CMD_GAME_STARTS:
							{
								String opponent = ((MessageGameStart) msg).getOppNic();
								System.out.println("Game started with " + opponent);
								ses.userOponnentNick = opponent;
								ses.currentGameSessionID = ((MessageGameStart) msg).getSessionId();
								ses.gameStarted = false;
								ses.shipsReady = false;
								ses.playerReady = false;
								System.out.println("To place ships enter <place>");
								break;
							}
							case Protocol.CMD_CHALLENGE_SUCCESFULLY_SEND:
							{
								System.out.println("Challenge succesfully send");
								break;
							}
							case Protocol.CMD_PLACE_SHIPS_RESULT:
							{
								boolean good = ((MessagePlaceShipsResult) msg).getGood();
								if(good)
								{
									ses.shipsReady = true;
									System.out.println("Ships succesfully placed. Enter <ready> if you are ready and wait for your opponent");
								}
								else
								{
									ses.shipsReady = false;
									System.out.println("Ships aren't placed succesfully. Please retry by entering <place>");
								}
								break;
							}
							case Protocol.CMD_OPPONENT_READY:
							{
								System.out.println("Your opponent " + ((MessageOpponentReady) msg).getFrom() + " is ready");
								break;
							}
							case Protocol.CMD_READY:
							{
								String firstPlayer = ((MessageReadyToPlay) msg).getFrom();
								if (firstPlayer.equals(ses.userNickName)) {
									System.out.println("Game started! Your turn first.");
									ses.yourTurn = true;
								} else {
									System.out.println("Game started! Opponent's turn first.");
									ses.yourTurn = false;
								}
								ses.gameStarted = true;
								System.out.println("To move print <move>");
								break;
							}
							case Protocol.CMD_MOVE:
							{
								int x = ((MessageMoveResult) msg).getX();
								int y = ((MessageMoveResult) msg).getY();
								boolean hitted = ((MessageMoveResult) msg).getHitted();
								boolean sunked = ((MessageMoveResult) msg).getSunked();
								int[][] enemyField = ((MessageMoveResult) msg).getEnemyField();
								boolean enemy = ((MessageMoveResult) msg).getEnemy();
								if(hitted)
								{
									Random random = new Random();
									int randomNumber = random.nextInt(3);
									if(randomNumber == 0)
									{
										System.out.println("Popal: " + x + " " + y);
									}
									if(randomNumber == 1)
									{
										System.out.println("Tochno v cel: " + x + " " + y);
									}
									if(randomNumber == 2)
									{
										System.out.println("Da ty snayper: " + x + " " + y);
									}
									if(sunked)
									{
										System.out.println("Potopil nedruga");
									}
								}
								else
								{
									System.out.println("Blya mymo: " + x + " " + y);
								}
								if(enemy)
								{
									System.out.println("Pole nedruga: ");
								}
								else
								{
									System.out.println("Matushka zemlya: ");
								}
								printField(enemyField);
								if(enemy)
								{
									if(hitted)
									{
										System.out.println("Your Turn. To move print <move>");
									}
									else
									{
										System.out.println("Opponent's turn");
									}
								}
								else
								{
									if(hitted)
									{
										System.out.println("Opponent's turn");
									}
									else
									{
										System.out.println("Your Turn. To move print <move>");
									}
								}
								break;	
							}
							case Protocol.CMD_GAMEOVER:
							{
								System.out.println("Game over");
								break;
							}
							default:
								assert(false);
								break;
						}
					}
					catch(IOException | ClassNotFoundException e)
					{
						if (running) {
							System.err.println("Connection error: " + e.getMessage());
						}
					}
				}
			}
		}
		static void session(Socket s, String userNickName, String userFullName) 
		{
			try ( Scanner in = new Scanner(System.in);
				ObjectInputStream is = new ObjectInputStream(s.getInputStream());
				ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream())) 
				{
					Session ses = new Session(userNickName, userFullName);
					if ( openSession( ses, is, os, in )) 
					{ 
						ListenerThread lt = new ListenerThread(is, ses);
						lt.start();
						try 
						{
							while (true) 
							{
								Message msg = getCommand(ses, in, is, os);

								os.writeObject(msg);
								if(msg.getID() == Protocol.CMD_DISCONNECT)
								{
									lt.stopListening();
									break;
								}	
							}			
						} 
						finally 
						{
							lt.stopListening();
							closeSession(ses, os);
						}
					}
			} catch (Exception e) {
				System.err.println(e);
			}
		}

		
		static boolean openSession(Session ses, ObjectInputStream is, ObjectOutputStream os, Scanner in) 
				throws IOException, ClassNotFoundException 
			{
			os.writeObject(new MessageConnect(ses.userNickName, ses.userFullName));
			MessageConnectResult msg = (MessageConnectResult) is.readObject();
			if (msg.Error() == false) 
			{
				System.err.println("connected");
				ses.connected = true;
				return true;
			}
			System.err.println("Unable to connect: "+ msg.getMessage());
			System.err.println("Press <Enter> to continue...");
			if(in.hasNextLine())
				in.nextLine();
			return false;
		}
		
		static void closeSession(Session ses, ObjectOutputStream os) throws IOException 
		{
			if ( ses.connected ) 
			{
				ses.connected = false;
				os.writeObject(new MessageDisconnect());
			}
		}

		static Message getCommand(Session ses, Scanner in, ObjectInputStream is, ObjectOutputStream os) {	
		try
		{
			while (true) 
			{
				if (in.hasNextLine()== false)
					break;
				String str = in.nextLine();
				if(ses.notProcessedSymbols.contains(str))
				{
					return new MessageIgnore();
				}
				byte cmd = translateCmd(str);
				if (cmd == -1) {
					System.out.println("Disconnecting...");
					return new MessageDisconnect();
				}
				switch ( cmd ) 
				{
					case Protocol.CMD_PING:
					{
						return new MessagePing();
					}
					case Protocol.CMD_USER:
					{
						return new MessageUser();
					}
					case Protocol.CMD_CHALLENGE:
					{
						os.writeObject(new MessageUser());
						Thread.currentThread().sleep(500);
						System.out.print("Enter opponent's nickname: ");
						String opponentNickName = in.nextLine();
						return new MessageChallenge(ses.userNickName, opponentNickName);
					}
					case Protocol.CMD_CHALLENGE_RESPONSE:
					{
						System.out.println("Challenges list");
						for(ChallengeFromPlayer cfp : ses.challenges)
						{
							System.out.println(cfp.getPlayerNickName() + ": " + cfp.getChallengeID());
						}
						System.out.println("Enter challenge ID which you want to response + Y/N: ");
						System.out.print("Enter challenge ID: ");
						try {
							Long chID = Long.parseLong(in.nextLine().trim());
							boolean challengeExists = false;
							for(ChallengeFromPlayer cfp : ses.challenges) {
								if (cfp.getChallengeID().equals(chID)) {
									challengeExists = true;
								}
							}
							
							if (!challengeExists) {
								System.out.println("Challenge with ID " + chID + " not found");
								continue;
							}
							
							System.out.print("Accept challenge? (Y/N): ");
							String answer = in.nextLine().trim().toUpperCase();
							if(answer.equals("Y"))
							{
								return new MessageChallengeResponse(chID, true);
							}
							if(answer.equals("N"))
							{
								return new MessageChallengeResponse(chID, false);
							}
							System.out.println("Wrong answer");
							break;
						}
						catch(Exception e)
						{
							System.err.println(e);
						}
						break;
					}
					case Protocol.CMD_SHIP_PLACE:
					{
						if(ses.gameStarted)
						{
							System.out.println("You can't place ships during the game");
							break;
						}
						System.out.print("Do you want to randomise your ships placement(Y/N): ");
						String answer = in.nextLine();
						if(answer.equals("Y"))
						{
							List<Ship> ships = randomShips();
							System.out.println("Ships created");
							int field[][] = new int[12][12];
							for(Ship ship : ships)
							{
								placeOnField(field, ship);
							}
							System.out.println("Matushka zemlya:");
							printField(field);
							return new MessagePlaceShips(ses.userNickName, ses.currentGameSessionID, ships);
						}
						else
						{
							boolean done = false;
							while(!done)
							{
								System.out.println("Place your ships. To recreate ships' placement type <-1>");
								int[][] field = new int[12][12];
								List<Ship> result = new ArrayList<>();
								int[] sizes = { 1, 1, 1, 1, 2, 2, 2, 3, 3, 4 };
								while(sizes.length > 0)
								{
									int x;
									int y;
									int len;
									boolean vert;
									System.out.println("New ship:");
									System.out.print("Enter x of ship's start: ");
									x = in.nextInt();
									if(retry(x))
									{
										continue;
									}
									System.out.print("Enter y of ship's start: ");
									y = in.nextInt();
									if(retry(y))
									{
										continue;
									}
									System.out.print("Enter length of the ship: ");
									len = in.nextInt();
									if(retry(len))
									{
										continue;
									}
									System.out.print("Enter orientation of the ship(0 - horizontal, 1 - vertical): ");
									int or = in.nextInt();
									if(retry(or))
									{
										continue;
									}
									if(or == 0)
									{
										vert = false;
									}
									else
									{
										vert = true;
									}
									if(len < 0 || len > 4)
									{
										System.out.println("Incorrect ship size");
										System.out.println("Current field's state:");
										printField(field);
										continue;
									}
									if(Arrays.binarySearch(sizes, len) < 0)
									{
										System.out.println("Ships with such size are already placed");
										System.out.println("Current field's state:");
										printField(field);
										continue;
									}
									try
									{
										Ship s = new Ship(x, y, len, vert);
										if(canPlace(field, s))
										{
											placeOnField(field, s);
											result.add(s);
											sizes = removeElement(sizes, Arrays.binarySearch(sizes, len));
										}
										else
										{
											System.out.println("Can't place ship");
										}
									}
									catch(Exception e)
									{
										System.out.println(e);
									}
									System.out.println("Current field's state:");
									printField(field);
								}
								done = true;
								System.out.println("Ships created");
								System.out.println("Matushka zemlya:");
								printField(field);
								return new MessagePlaceShips(ses.userNickName, ses.currentGameSessionID, result);
							}
						}
					}
					case Protocol.CMD_READY:
					{
						System.out.println("You are ready!");
						if(!ses.playerReady)
						{
							ses.playerReady = true;
							return new MessageReadyToPlay(ses.userNickName, ses.currentGameSessionID);
						}
						break;
					}
					case Protocol.CMD_MOVE:
						{
							System.out.print("Enter x: ");
							int x;
							x = in.nextInt();
							int y;
							System.out.print("Enter y: ");
							y = in.nextInt();
							return new MessageMove(ses.userNickName, ses.currentGameSessionID, x, y);
						}
					default: 
					{
						System.err.println("Unknown command!");
						break;
					}
				}
			}
		}
		catch(Exception e)
		{}
		return new MessageDisconnect();
		}

		public static int[] removeElement(int[] array, int index) {
			if (index < 0 || index >= array.length) {
				return array.clone();
			}
			
			int[] newArray = new int[array.length - 1];
			System.arraycopy(array, 0, newArray, 0, index);
			System.arraycopy(array, index + 1, newArray, index, array.length - index - 1);
			return newArray;
		}

		static boolean retry(int n)
		{
			if (n == -1)
			{
				return true;
			}
			else
			{
				return false;
			}
		}

		static void printField(int[][] field)
		{
			System.out.print("   ");
			for(int x = 1; x < 11; ++x)
			{
				System.out.print(x + " ");
			}
			System.out.println();
			for(int y = 1; y < 11; ++y)
			{
				if(y == 10)
					System.out.print(y + " ");
				else
					System.out.print(y + "  ");

				for(int x = 1; x < 11; ++x)
				{
					int cell = field[y][x];
					switch(cell)
					{
						case 0: System.out.print(". "); break;
						case 1: System.out.print("S "); break;
						case 2: System.out.print("M "); break;
						case 3: System.out.print("H "); break;
						case 4: System.out.print("S "); break;
						default: System.out.print("? "); break;
					}
				}
				System.out.println();
			}
		}


		static List<Ship> randomShips() {
			int[][] field = new int[12][12]; // Поле 12x12 (индексы 0-11), игровое поле 1-10
			List<Ship> result = new ArrayList<>();

			int[] sizes = { 4, 3, 3, 2, 2, 2, 1, 1, 1, 1 };

			for (int len : sizes) {
			boolean placed = false;

			for (int attempt = 0; attempt < 1000 && !placed; attempt++) {
				boolean vert = Math.random() < 0.5;

				int maxX = vert ? 10 : 10 - len + 1;
				int maxY = vert ? 10 - len + 1 : 10;

				if (maxX < 1 || maxY < 1) {
				continue;
				}

				int x = 1 + (int) (Math.random() * maxX);
				int y = 1 + (int) (Math.random() * maxY);

				try {
				Ship s = new Ship(x, y, len, vert);

				if (!canPlace(field, s))
					continue;

				placeOnField(field, s);
				result.add(s);
				placed = true;

				} catch (Exception ignore) {

				}
			}

			if (!placed) {
				throw new RuntimeException("Unable to place random ships of length " + len);
			}
			}

			return result;
		}

		static boolean canPlace(int[][] field, Ship s) {
			int x = s.getX();
			int y = s.getY();
			int len = s.getLength();
			boolean vert = s.getOrientation() == Ship.Orientation.vertical;

			if (vert) {
			if (y + len - 1 > 10)
				return false;
			} else {
			if (x + len - 1 > 10)
				return false;
		}
		
			for (int i = 0; i < len; i++) {
			int cx = x + (vert ? 0 : i);
			int cy = y + (vert ? i : 0);

			if (field[cy][cx] != 0) {
				return false;
			}

			// Проверяем соседние клетки
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
				int nx = cx + dx;
				int ny = cy + dy;

				// Проверяем только клетки в пределах игрового поля (1-10)
				if (nx >= 1 && nx <= 10 && ny >= 1 && ny <= 10) {
					if (field[ny][nx] != 0) {
					return false;
					}
				}
				}
			}
			}
			return true;
		}

		static void placeOnField(int[][] field, Ship s) {
			int x = s.getX();
			int y = s.getY();
			int len = s.getLength();
			boolean vert = s.getOrientation() == Ship.Orientation.vertical;

			for (int i = 0; i < len; i++) {
			int cx = x + (vert ? 0 : i);
			int cy = y + (vert ? i : 0);

			// Убеждаемся, что координаты в пределах игрового поля
			if (cx >= 1 && cx <= 10 && cy >= 1 && cy <= 10) {
				field[cy][cx] = 1;
			}
			}
		}


		static TreeMap<String,Byte> commands = new TreeMap<String,Byte>();
		static 
		{
			commands.put("ping", Byte.valueOf(Protocol.CMD_PING));
			commands.put("users", Byte.valueOf(Protocol.CMD_USER));
			commands.put("challenge", Byte.valueOf(Protocol.CMD_CHALLENGE));
			commands.put("answer", Byte.valueOf(Protocol.CMD_CHALLENGE_RESPONSE));
			commands.put("q", Byte.valueOf((byte) -1));
			commands.put("place", Byte.valueOf(Protocol.CMD_SHIP_PLACE));
			commands.put("ready", Byte.valueOf(Protocol.CMD_READY));
			commands.put("move", Byte.valueOf(Protocol.CMD_MOVE));
			commands.put("field", Byte.valueOf(Protocol.CMD_MOVE));
		}
		
		static byte translateCmd(String str) 
		{
			str = str.trim();
			Byte r = commands.get(str);
			return (r == null ? 0 : r.byteValue());
		}
		
		static void printPrompt() 
		{
			System.out.println();
			System.out.print("ping >");
			System.out.flush();
		}
	}
