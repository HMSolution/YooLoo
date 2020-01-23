// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add history of change 

package server;

import java.io.IOException; 
import java.util.logging.*;

import Logger.LoggerWrapper;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import client.YoolooClient.ClientState;
import common.YoolooKartenspiel;
import messages.ServerMessage;
import messages.ServerMessage.ServerMessageType;
import utils.Socketutils;
import client.YoolooClient;


public class YoolooServer {

	public boolean restart = false;

	// Server Standardwerte koennen ueber zweite Konstruktor modifiziert werden!
	private int port = 44137;
	private int spielerProRunde = 8; // min 1, max Anzahl definierte Farben in Enum YoolooKartenSpiel.KartenFarbe)
	private GameMode serverGameMode = GameMode.GAMEMODE_SINGLE_GAME;
	private LoggerWrapper loggerWrapper = new LoggerWrapper("Server");
	
	public GameMode getServerGameMode() {
		return serverGameMode;
	}
	
	public void setServerGameMode(GameMode serverGameMode) {
		this.serverGameMode = serverGameMode;
	}

	private ServerSocket serverSocket = null;
	public boolean serverAktiv = true;
	Socketutils socketUtils = new Socketutils();
	private LinkedHashMap<String, ArrayList<Integer>> cardMap = new LinkedHashMap<>();

	
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
			loggerWrapper.logInfoString("[SERVER-LOG]: Server wird gestartet");

			serverSocket = new ServerSocket(port);
			spielerPool = Executors.newCachedThreadPool();
			clientHandlerList = new LinkedHashMap<String, YoolooClientHandler>();
			System.out.println("Server gestartet - warte auf Spieler");
			loggerWrapper.logInfoString("[SERVER-LOG]: Server wartet auf Spieler");
			
			while (serverAktiv) {
				loggerWrapper.logInfoString("[SERVER-LOG]: Server aktiv");

				Socket client = null;

				// Neue Spieler registrieren
				try {
					loggerWrapper.logInfoString("[SERVER-LOG]: Spieler wird registriert");

					client = serverSocket.accept();
					String IP = ((InetSocketAddress) client.getRemoteSocketAddress()).getAddress().toString();
					
					if(!this.clientHandlerList.containsKey(IP)){
						YoolooClientHandler clientHandler = new YoolooClientHandler(this, client);
						clientHandlerList.put(IP, clientHandler);
						System.out.println("[YoolooServer] Anzahl verbundene Spieler: " + clientHandlerList.size());
						loggerWrapper.logInfoString("[SERVER-LOG]:Spieler hat sich verbunden ");
					}else{
						System.out.println("[YoolooServer] " + IP + " ist bereits angemeldet, lehne Verbindung ab.");
						ServerMessage loginErr = new ServerMessage(
													ServerMessage.ServerMessageType.SERVERMESSAGE_ALREADY_LOGGED_IN, 
													YoolooClient.ClientState.CLIENTSTATE_DISCONNECTED,
													ServerMessage.ServerMessageResult.SERVER_MESSAGE_RESULT_NOT_OK	
												);
						Socketutils.sendSerialized(client, loginErr);
						client.close();
					}


				} catch (IOException e) {
					System.out.println("Client Verbindung gescheitert");
					loggerWrapper.logWarningString("[SERVER-LOG]: Client Verbindung gescheitert  ");
					e.printStackTrace();
				}

				
				// Neue Session starten wenn ausreichend Spieler verbunden sind!
				if (clientHandlerList.size() >= Math.min(spielerProRunde,
						YoolooKartenspiel.Kartenfarbe.values().length)) {
					// Init Session
					loggerWrapper.logInfoString("[SERVER-LOG]: Neue Session wird startet");
					YoolooSession yoolooSession = new YoolooSession(clientHandlerList.size(), serverGameMode);

					// Starte pro Client einen ClientHandlerThread
					int i = 0;
					for (Entry<String, YoolooClientHandler> ent : this.clientHandlerList.entrySet()) {
						loggerWrapper.logInfoString("[SERVER-LOG]: pro Client wird ein ClientHandlerThread startet");
						YoolooClientHandler ch = ent.getValue();
						ch.setHandlerID(i);
						ch.joinSession(yoolooSession);
						spielerPool.execute(ch); // Start der ClientHandlerThread - Aufruf der Methode run()
						i++;
					}

					try {
						//Pause, da sonst die clienthandler thread werte nicht durchkommen
						Thread.sleep(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					for(Entry<String, YoolooClientHandler> ent : this.clientHandlerList.entrySet()) {
						YoolooClientHandler handler = ent.getValue();
						if(handler.cheated)
						serverAktiv = false;
						restart = true;
						//prüfen ob in den Handlern gecheatet wurde
						//entsprechend wird ein Restart eingeleitet
						
					}

					// nuechste Runde eroeffnen
					clientHandlerList = new LinkedHashMap<String, YoolooClientHandler>();
					loggerWrapper.logInfoString("[SERVER-LOG]: naechste Runde wird eroeffnet");
				}
			}
			EndSession(543210);
		} catch (IOException e1) {
			System.out.println("ServerSocket nicht gebunden");
			loggerWrapper.logWarningString("[SERVER-LOG]: ServerSocket wurde nicht gebunden");
			serverAktiv = false;
			e1.printStackTrace();
		}
	}
	
	public synchronized void EndSession(int code)
	{
		if(code == 543210)
		{
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sendCheatMessageToAllClients();
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.out.println("SERVERSOCKET KONNTE NICHT GESCHLOSSEN WERDEN");
		}
		} else {
			System.out.println("Servercode falsch");
		}
	}

	public void sendCheatMessageToAllClients()
	{
		ServerMessage notificationForPlayerCheating = new ServerMessage(ServerMessageType.SERVERMESSAGE_NOTIFY_CHEAT,
				ClientState.CLIENTSTATE_DISCONNECT,
				null);

	   for(Entry<String, YoolooClientHandler> ent : this.clientHandlerList.entrySet()) {
		   if(!ent.getValue().cheated)
		   {
				Socketutils.sendSerialized(ent.getValue().getSocket(), notificationForPlayerCheating);
		   }
	   }
	}

	// TODO Dummy zur Serverterminierung noch nicht funktional
	public void shutDownServer(int code) {
		if (code == 543210) {
			this.serverAktiv = false;
			System.out.println("Server wird beendet");
			loggerWrapper.logInfoString("[SERVER-LOG]: Server wird beendet");
			spielerPool.shutdown();
		} else {
			System.out.println("Servercode falsch");
			loggerWrapper.logWarningString("[SERVER-LOG]: Servercode ist falsch");
		}
	}
	public void saveCardOrder(ArrayList<Integer> order, String clientName) {
		System.out.println("[~] Speichere Kartenabfolge für " + clientName);
		cardMap.put(clientName, order);
	}
	public ArrayList<Integer> getCardOrder(String clientName) {
		if(this.cardMap.containsKey(clientName)){
			System.out.println("[>] " + clientName + " ist bekannt, sende letzte Kartenreihenfolge.");
			return this.cardMap.get(clientName);
		}else{
			return new ArrayList<Integer>();
		}
	}
}
