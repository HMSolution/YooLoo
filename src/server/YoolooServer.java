// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add history of change 

package server;

import java.io.IOException; 
import java.util.logging.*;

import Logger.LoggerWrapper;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.YoolooKartenspiel;

public class YoolooServer {

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
	private boolean serverAktiv = true;

	// private ArrayList<Thread> spielerThreads;
	private ArrayList<YoolooClientHandler> clientHandlerList;

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
			clientHandlerList = new ArrayList<YoolooClientHandler>();
			System.out.println("Server gestartet - warte auf Spieler");
			loggerWrapper.logInfoString("[SERVER-LOG]: Server wartet auf Spieler");
			
			while (serverAktiv) {
				loggerWrapper.logInfoString("[SERVER-LOG]: Server aktiv");

				Socket client = null;

				// Neue Spieler registrieren
				try {
					loggerWrapper.logInfoString("[SERVER-LOG]: Spieler wird registriert");

					client = serverSocket.accept();
					YoolooClientHandler clientHandler = new YoolooClientHandler(this, client);
					clientHandlerList.add(clientHandler);
					System.out.println("[YoolooServer] Anzahl verbundene Spieler: " + clientHandlerList.size());
					loggerWrapper.logInfoString("[SERVER-LOG]:Spieler hat sich verbunden ");
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

					// Starte pro Client einen ClientHandlerTread
					for (int i = 0; i < clientHandlerList.size(); i++) {
						loggerWrapper.logInfoString("[SERVER-LOG]: pro Client wird ein ClientHandlerTread startet");
						YoolooClientHandler ch = clientHandlerList.get(i);
						ch.setHandlerID(i);
						ch.joinSession(yoolooSession);
						spielerPool.execute(ch); // Start der ClientHandlerThread - Aufruf der Methode run()
					}

					// nuechste Runde eroeffnen
					clientHandlerList = new ArrayList<YoolooClientHandler>();
					loggerWrapper.logInfoString("[SERVER-LOG]: naechste Runde wird eroeffnet");
				}
			}
		} catch (IOException e1) {
			System.out.println("ServerSocket nicht gebunden");
			loggerWrapper.logWarningString("[SERVER-LOG]: ServerSocket wurde nicht gebunden");
			serverAktiv = false;
			e1.printStackTrace();
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
}
