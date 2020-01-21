// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add history of change 

package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.YoolooKartenspiel;
import messages.ServerMessage;
import client.YoolooClient;
import common.YoolooKarte;
import utils.socketutils;

public class YoolooServer {

	// Server Standardwerte koennen ueber zweite Konstruktor modifiziert werden!
	private int port = 44137;
	private int spielerProRunde = 8; // min 1, max Anzahl definierte Farben in Enum YoolooKartenSpiel.KartenFarbe)
	private GameMode serverGameMode = GameMode.GAMEMODE_SINGLE_GAME;

	public GameMode getServerGameMode() {
		return serverGameMode;
	}

	public void setServerGameMode(GameMode serverGameMode) {
		this.serverGameMode = serverGameMode;
	}

	private ServerSocket serverSocket = null;
	private boolean serverAktiv = true;
	private LinkedHashMap<String, ArrayList<YoolooKarte>> cardMap;

	// private LinkedHashMap<Thread> spielerThreads;
	private LinkedHashMap<String, YoolooClientHandler> clientHandlerList;

	private ExecutorService spielerPool;

	/**
	 * Serverseitig durch ClientHandler angebotenen SpielModi. Bedeutung der
	 * einzelnen Codes siehe Inlinekommentare.
	 * 
	 * Derzeit nur Modus Play Single Game genutzt
	 */
	public enum GameMode {
		GAMEMODE_NULL, // Spielmodus noch nicht definiert
		GAMEMODE_SINGLE_GAME, // Spielmodus: einfaches Spiel
		GAMEMODE_PLAY_ROUND_GAME, // noch nicht genutzt: Spielmodus: Eine Runde von Spielen
		GAMEMODE_PLAY_LIGA, // noch nicht genutzt: Spielmodus: Jeder gegen jeden
		GAMEMODE_PLAY_POKAL, // noch nicht genutzt: Spielmodus: KO System
		GAMEMODE_PLAY_POKAL_LL // noch nicht genutzt: Spielmodus: KO System mit Lucky Looser
	};

	public YoolooServer(int port, int spielerProRunde, GameMode gameMode) {
		this.port = port;
		this.spielerProRunde = spielerProRunde;
		this.serverGameMode = gameMode;
	}

	public void startServer() {
		try {
			// Init
			serverSocket = new ServerSocket(port);
			spielerPool = Executors.newCachedThreadPool();
			clientHandlerList = new LinkedHashMap<String, YoolooClientHandler>();
			System.out.println("Server gestartet - warte auf Spieler");

			while (serverAktiv) {
				Socket client = null;

				// Neue Spieler registrieren
				try {
					client = serverSocket.accept();
					String IP = ((InetSocketAddress) client.getRemoteSocketAddress()).getAddress().toString();
					
					if(!this.clientHandlerList.containsKey(IP)){
						YoolooClientHandler clientHandler = new YoolooClientHandler(this, client);
						clientHandlerList.put(IP, clientHandler);
						System.out.println("[YoolooServer] Anzahl verbundene Spieler: " + clientHandlerList.size());
					}else{
						System.out.println("[YoolooServer] " + IP + " ist bereits angemeldet, lehne Verbindung ab.");
						ServerMessage loginErr = new ServerMessage(
													ServerMessage.ServerMessageType.SERVERMESSAGE_ALREADY_LOGGED_IN, 
													YoolooClient.ClientState.CLIENTSTATE_DISCONNECTED,
													ServerMessage.ServerMessageResult.SERVER_MESSAGE_RESULT_NOT_OK	
												);
						socketutils.sendSerialized(client, loginErr);
						client.close();
					}


				} catch (IOException e) {
					System.out.println("Client Verbindung gescheitert");
					e.printStackTrace();
				}

				// Neue Session starten wenn ausreichend Spieler verbunden sind!
				if (clientHandlerList.size() >= Math.min(spielerProRunde,
						YoolooKartenspiel.Kartenfarbe.values().length)) {
					// Init Session
					YoolooSession yoolooSession = new YoolooSession(clientHandlerList.size(), serverGameMode);

					// Starte pro Client einen ClientHandlerThread
					int i = 0;
					for (Entry<String, YoolooClientHandler> ent : this.clientHandlerList.entrySet()) {
						YoolooClientHandler ch = ent.getValue();
						ch.setHandlerID(i);
						ch.joinSession(yoolooSession);
						spielerPool.execute(ch); // Start der ClientHandlerThread - Aufruf der Methode run()
						i++;
					}

					// nuechste Runde eroeffnen
					clientHandlerList = new LinkedHashMap<String, YoolooClientHandler>();
				}
			}
		} catch (IOException e1) {
			System.out.println("ServerSocket nicht gebunden");
			serverAktiv = false;
			e1.printStackTrace();
		}

	}

	// TODO Dummy zur Serverterminierung noch nicht funktional
	public void shutDownServer(int code) {
		if (code == 543210) {
			this.serverAktiv = false;
			System.out.println("Server wird beendet");
			spielerPool.shutdown();
		} else {
			System.out.println("Servercode falsch");
		}
	}
	public void saveCardOrder(ArrayList<YoolooKarte> order, String clientName) {
		System.out.println("[~] Speichere Kartenabfolge für " + clientName);
		cardMap.put(clientName, order);
	}
}
