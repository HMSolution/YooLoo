// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add  start_Client() SERVERMESSAGE_CHANGE_STATE 

package client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import common.LoginMessage;
import common.YoolooKarte;
import common.YoolooKartenspiel;
import common.YoolooSpieler;
import common.YoolooStich;
import messages.ClientMessage;
import messages.ClientMessage.ClientMessageType;
import messages.ServerMessage.ServerMessageType;
import messages.ServerMessage;
import server.YoolooServer.GameMode;
import utils.socketutils;

public class YoolooClient {

	private String serverHostname = "localhost";
	private int serverPort = 44137;
	private Socket serverSocket = null;
	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;

	private ClientState clientState = ClientState.CLIENTSTATE_NULL;
	private ClientMode clientMode;

	private String spielerName = "";
	private LoginMessage newLogin = null;
	private YoolooSpieler meinSpieler;
	private YoolooStich[] spielVerlauf = null;

	public YoolooClient() {
		super();
	}

	public YoolooClient(ClientMode clientMode, String serverHostname, int serverPort) {
		super();
		this.serverPort = serverPort;
		this.clientMode = clientMode;
		clientState = ClientState.CLIENTSTATE_NULL;
	}

	/**
	 * Client arbeitet statusorientiert als Kommandoempfuenger in einer Schleife.
	 * Diese terminiert wenn das Spiel oder die Verbindung beendet wird.
	 */
	public void startClient() {

		try {
			if (this.clientMode == ClientMode.CLIENTMODE_SPECTATOR)
				System.out.println("Starte Client im Zuschauermodus - funktioniert nicht mit älteren Servern!");
			// Lese Namen aus stdin //
			
			boolean failedOnce = false;
			System.out.println("Bitte gebe zunächst deinen Namen an:"); 
			Scanner temporary = new Scanner(System.in);

			while(this.spielerName.length() < 4){ 
				if(failedOnce)
					System.out.println("Dein Name muss mindestens 4 Zeichen lang sein.");
				System.out.print(">> "); 
				this.spielerName = temporary.nextLine(); 
				failedOnce= true;
			}

			System.out.println("Bitte gebe die Server IP/Hostname ein:");
			System.out.print(">> "); this.serverHostname = temporary.nextLine();

			
			System.out.println("Logge als " + this.spielerName + " ein");
			
			temporary.close();
			
			//////////////////////////

			clientState = ClientState.CLIENTSTATE_CONNECT;
			verbindeZumServer();

			while (clientState != ClientState.CLIENTSTATE_DISCONNECTED && ois != null && oos != null) {
				// 1. Schritt Kommado empfangen
				ServerMessage kommandoMessage = empfangeKommando();
				System.out.println("[id-x]ClientStatus: " + clientState + "] " + kommandoMessage.toString());
				if (kommandoMessage
						.getServerMessageType() == ServerMessage.ServerMessageType.SERVERMESSAGE_ALREADY_LOGGED_IN) {
					System.out.println("Du bist bereits eingeloggt!");
					System.exit(0);
				}
				// 2. Schritt ClientState ggfs aktualisieren (fuer alle neuen Kommandos)
				ClientState newClientState = kommandoMessage.getNextClientState();
				if (newClientState != null) {
					clientState = newClientState;
				}
				// 3. Schritt Kommandospezifisch reagieren
				switch (kommandoMessage.getServerMessageType()) {
				case SERVERMESSAGE_SENDLOGIN:
					// Server fordert Useridentifikation an

					newLogin = new LoginMessage(spielerName);
					System.out.println("Sende LoginMessage an Server");
					oos.writeObject(newLogin);
					empfangeSpieler();
					/*
					 * System.out.println("[id-x]ClientStatus: " + clientState +
					 * "] : LoginMessage fuer  " + spielerName +
					 * " an server gesendet warte auf Spielerdaten"); empfangeSpieler(); //
					 * ausgabeKartenSet();
					 */
					break;
				case SERVERMESSAGE_PREPARE_EVENT_LOOP:
					System.out.println("[*] Warte auf Spielzusammenfassung");

					// Lese Spielzusammenfassung vom Server
					ArrayList<YoolooStich> stiche = new ArrayList<>();
					try {stiche = (ArrayList<YoolooStich>) ois.readObject();} catch (ClassNotFoundException e) {e.printStackTrace();}
					// Da der Server kein RESULT_SET sendet, erstellen wir unser eigenes, der String ist die Farbe während die Punkte im Integer gespeichert sind
					LinkedHashMap<String, Integer> Punkte = new LinkedHashMap<String, Integer>();

					int bonus = 0;
					for(int i = 0; i < stiche.size(); i++) {
						System.out.println("Stich #" + (stiche.get(i).getStichNummer()+1));
						for(YoolooKarte karte : stiche.get(i).getStich()){
							System.out.println("[~] " + karte.getFarbe() + " spielt " + karte.getWert());
						}
						if(stiche.get(i).getSpielerNummer() == -1){
							System.out.println("Alle Spieler haben die selbe Karte gespielt, die Punkte gehen als Bonuspunkte in die nächste Runde mit über");
							bonus = i + 1;
						}else {
							// Farbe des gewinners ermitteln
							String COLOR_NAME = YoolooKartenspiel.Kartenfarbe.values()[stiche.get(i).getSpielerNummer()].toString();
							System.out.println(COLOR_NAME + " hat den Stich gewonnen");
							// colorPoints sind die Punkte die der Spieler bereits hat, wir updaten also Spieler Punkte + Punkte diese Runde + Bonus
							int colorPoints = Punkte.containsKey(COLOR_NAME) ? Punkte.get(COLOR_NAME) : 0;
							Punkte.put(COLOR_NAME, ((colorPoints + (i + 1)) + bonus));
							bonus = 0;
						}
						System.out.println("------------------");
					}
					
					// Ermittle Spieler mit den meisten Punkten
					Entry<String, Integer> Winner = null;
					for(Entry<String, Integer> curPlayer : Punkte.entrySet()) {
						if(Winner == null || curPlayer.getValue() > Winner.getValue()) Winner = curPlayer;
					}
					
					System.out.println(Winner.getKey() + " hat das Spiel mit " + Winner.getValue() + " Punkten gewonnen!");

					System.exit(0);

				case SERVERMESSAGE_SORT_CARD_SET:
					// sortieren Karten
					meinSpieler.sortierungFestlegen();
					ausgabeKartenSet();
					// ggfs. Spielverlauf lรถschen
					spielVerlauf = new YoolooStich[YoolooKartenspiel.maxKartenWert];
					ClientMessage message = new ClientMessage(ClientMessageType.ClientMessage_OK,
							"Kartensortierung ist erfolgt!");
					oos.writeObject(message);
					break;
				case SERVERMESSAGE_SEND_CARD:
					spieleStich(kommandoMessage.getParamInt());
					break;
				case SERVERMESSAGE_RESULT_SET:
					System.out.println("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
							+ "] : Ergebnis ausgeben ");
					String ergebnis = empfangeErgebnis();
					System.out.println(ergebnis.toString());
					break;
					           // basic version: wechsel zu ClientState Disconnected thread beenden
				case SERVERMESSAGE_CHANGE_STATE:
				break ;
				
				default:
					break;
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("startClient (main event loop) exception");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Verbindung zum Server aufbauen, wenn Server nicht antwortet nach ein Sekunde
	 * nochmals versuchen
	 *
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	// TODO Abbruch nach x Minuten einrichten
	private void verbindeZumServer() throws UnknownHostException, IOException {
		while (serverSocket == null) {
			try {
				serverSocket = new Socket(serverHostname, serverPort);
			} catch (ConnectException e) {
				System.out.println("Server antwortet nicht - ggfs. neu starten");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
			}
		}
		System.out.println("[Client] Serversocket eingerichtet: " + serverSocket.toString());
		// Kommunikationskanuele einrichten
		ois = new ObjectInputStream(serverSocket.getInputStream());
		oos = new ObjectOutputStream(serverSocket.getOutputStream());
	}

	private void spieleStich(int stichNummer) throws IOException {
		System.out.println("-> Spiele Karte " + stichNummer);
		spieleKarteAus(stichNummer);
		System.out.println("-> Karte ausgespielt, empfange Stich");
		YoolooStich iStich = empfangeStich();
		spielVerlauf[stichNummer] = iStich;
		System.out.println("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
				+ "] : Empfange Stich " + iStich);
		if (iStich.getSpielerNummer() == meinSpieler.getClientHandlerId()) {
			System.out.print(
					"[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState + "] : Gewonnen - ");
			meinSpieler.erhaeltPunkte(iStich.getStichNummer() + 1);
		}

	}

	private void spieleKarteAus(int i) throws IOException {
		oos.writeObject(meinSpieler.getAktuelleSortierung()[i]);
	}

	// Methoden fuer Datenempfang vom Server / ClientHandler
	private ServerMessage empfangeKommando() {
		ServerMessage kommando = null;
		boolean failed = false;
		try {
			kommando = (ServerMessage) ois.readObject();
		} catch (ClassNotFoundException e) {
			failed = true;
			e.printStackTrace();
		}catch(EOFException e){
			System.out.println("!!!!!!!!!!! EOFException triggered in 'empfangeKommando' !!!!!!!!!!!!!!!!!!");
		} catch (IOException e) {
			failed = true;
			e.printStackTrace();
		}
		if (failed)
			kommando = null;
		return kommando;
	}

	private void empfangeSpieler() {
		try {
			meinSpieler = (YoolooSpieler) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("empfangespieler exception");
			e.printStackTrace();
		}
	}

	private YoolooStich empfangeStich() {
		try {
			return (YoolooStich) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("empfangestich exception");

			e.printStackTrace();
		}
		return null;
	}

	private String empfangeErgebnis() {
		try {
			return (String) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("empfangeErgebnis exception");

			e.printStackTrace();
		}
		return null;
	}

	private LoginMessage eingabeSpielerDatenFuerLogin() {
		// TODO Spielername, GameMode und ggfs mehr ermitteln
		return null;
	}
	public String getDisplayName() {
		return this.spielerName;
	}

	public void ausgabeKartenSet() {
		// Ausgabe Kartenset
		System.out.println("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
				+ "] : Uebermittelte Kartensortierung beim Login ");
		for (int i = 0; i < meinSpieler.getAktuelleSortierung().length; i++) {
			System.out.println("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
					+ "] : Karte " + (i + 1) + ":" + meinSpieler.getAktuelleSortierung()[i]);
		}

	}

	public enum ClientState {
		CLIENTSTATE_NULL, // Status nicht definiert
		CLIENTSTATE_CONNECT, // Verbindung zum Server wird aufgebaut
		CLIENTSTATE_LOGIN, // Anmeldung am Client Informationen des Users sammeln
		CLIENTSTATE_RECEIVE_CARDS, // Anmeldung am Server
		CLIENTSTATE_SORT_CARDS, // Anmeldung am Server
		CLIENTSTATE_REGISTER, // t.b.d.
		CLIENTSTATE_PLAY_SINGLE_GAME, // Spielmodus einfaches Spiel
		CLIENTSTATE_DISCONNECT, // Verbindung soll getrennt werden
		CLIENTSTATE_DISCONNECTED // Vebindung wurde getrennt
	};
	
	public enum ClientMode {
		CLIENTMODE_PLAYER, 
		CLIENTMODE_SPECTATOR
	};

}
