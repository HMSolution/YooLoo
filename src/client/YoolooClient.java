// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add  start_Client() SERVERMESSAGE_CHANGE_STATE 

package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import javax.swing.plaf.basic.BasicScrollPaneUI.HSBChangeListener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.google.gson.Gson;
import common.LoginMessage;
import common.YoolooKarte;
import common.YoolooKartenspiel;
import common.YoolooSpieler;
import common.YoolooStich;
import extensions.JsonService;
import extensions.Spielzug;
import messages.ClientMessage;
import messages.ClientMessage.ClientMessageType;
import messages.ServerMessage;
import messages.ServerMessage.ServerMessageType;

public class YoolooClient {

	private String serverHostname = "localhost";
	private int serverPort = 44137;
	private Socket serverSocket = null;
	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;

	private ClientState clientState = ClientState.CLIENTSTATE_NULL;

	private String spielername = "";
	private LoginMessage newLogin = null;
	private YoolooSpieler meinSpieler;
	private YoolooStich[] spielVerlauf = null;
	
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";

	public boolean restart = false;

	private List<Spielzug> spielHistorie = new ArrayList<>();
	public final static String historiePfad = "Spielhistorie.json";
	private JsonService jsonService = new JsonService(historiePfad);
	private Gson gson = new Gson();
	
	public YoolooClient() {
		super();
	}

	public YoolooClient(String serverHostname, int serverPort) {
		super();
		this.serverPort = serverPort;
		clientState = ClientState.CLIENTSTATE_NULL;
	}

	/**
	 * Client arbeitet statusorientiert als Kommandoempfuenger in einer Schleife.
	 * Diese terminiert wenn das Spiel oder die Verbindung beendet wird.
	 */
	public void startClient() {
		
		try {
			// Lese Namen aus stdin //
			boolean nameNotLongEnough = false;
			System.out.println("Bitte gebe zunächst deinen Namen an:");
			Scanner temporary = new Scanner(System.in);
			
			while(this.spielername.length() < 4){
				if(nameNotLongEnough) System.out.println("Dein Name muss mindestens 4 Zeichen lang sein.");
				System.out.print(">> "); 
				this.spielername = temporary.nextLine();
				nameNotLongEnough = true;	
			}

			System.out.println("Bitte gebe die Server IP/Hostname ein:");
			System.out.print(">> ");
			this.serverHostname = temporary.nextLine();


			System.out.println("Logge als " + this.spielername + " ein");

			temporary.close();
			//////////////////////////

			clientState = ClientState.CLIENTSTATE_CONNECT;
			verbindeZumServer();

			while (clientState != ClientState.CLIENTSTATE_DISCONNECTED && ois != null && oos != null) {
				// 1. Schritt Kommado empfangen
				ServerMessage kommandoMessage = empfangeKommando();
				System.out.println("[id-x]ClientStatus: " + clientState + "] " + kommandoMessage.toString());
				if(kommandoMessage.getServerMessageType() == ServerMessage.ServerMessageType.SERVERMESSAGE_ALREADY_LOGGED_IN){
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

						newLogin = new LoginMessage(spielername);
						
						oos.writeObject(newLogin);
						empfangeSpieler();

					break;
				case SERVERMESSAGE_SORT_CARD_SET:
					// sortieren Karten
					meinSpieler.sortierungFestlegen();
					ausgabeKartenSet();
					// ggfs. Spielverlauf löschen
					spielVerlauf = new YoolooStich[YoolooKartenspiel.maxKartenWert];
					ClientMessage message = new ClientMessage(ClientMessageType.ClientMessage_OK,
							"Kartensortierung ist erfolgt!");
					oos.writeObject(message);
					break;
				case SERVERMESSAGE_SEND_CARD:
					try {
						spieleStich(kommandoMessage.getParamInt());	
					}
					catch(ClassCastException e)
					{
						break;
					}					
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
				case SERVERMESSAGE_PLAYER_CHEAT:
					System.out.println(ANSI_RED + "[ALERT] => Du bist beim Cheaten erwischt worden. Deswegen wirst du aus der Sitzung ausgeschlossen! [ALERT]" + ANSI_RESET);
					break;	
				default:
					break;
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
		YoolooStich iStich = new YoolooStich();
		try {
			iStich = empfangeStich();
		}
		catch(Exception e) {
		      System.out.println(e.getStackTrace());
		}
		spielVerlauf[stichNummer] = iStich;
		System.out.println("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState
				+ "] : Empfange Stich " + iStich);
		
		boolean stichGewonnen = false;
		if (iStich.getSpielerNummer() == meinSpieler.getClientHandlerId()) {
			System.out.print(
					"[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState + "] : Gewonnen - ");
			meinSpieler.erhaeltPunkte(iStich.getStichNummer() + 1);
			stichGewonnen = true;
		}
		
		YoolooKarte meineKarte =  Arrays.stream(iStich.getStich()).filter(k -> k.getFarbe() == meinSpieler.getSpielfarbe()).findFirst().get();
		Spielzug spielzug = new Spielzug(iStich.getStichNummer(), meineKarte.getWert(), stichGewonnen);
		SpielzugAbspeichern(spielzug);
	}

	private void SpielzugAbspeichern(Spielzug spielzug) {
		List<Spielzug> historie = jsonService.LiesDatei();
		if(historie == null)
			return;
		
		historie.add(spielzug);
		String json = gson.toJson(historie);
        try (FileWriter file = new FileWriter(historiePfad, false)) {
        	file.write(json);
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
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
			e.printStackTrace();
		}
	}

	private YoolooStich empfangeStich() throws ClassNotFoundException, IOException {
		Object messageReceived = ois.readObject();
		try {			
			return (YoolooStich) messageReceived; 
		} catch (Exception e) {
			if(e instanceof ClassCastException)
			{
				System.out.println(ANSI_RED + "[ALERT] => Das Spiel wurde vom Server abgebrochen. [ALERT]" + ANSI_RESET);
				try {			
					ServerMessage abortionMessage = (ServerMessage) messageReceived;
					if(abortionMessage.getServerMessageType() == ServerMessageType.SERVERMESSAGE_PLAYER_CHEAT ||
					   abortionMessage.getServerMessageType() == ServerMessageType.SERVERMESSAGE_NOTIFY_CHEAT)
					{
						if(abortionMessage.getServerMessageType() == ServerMessageType.SERVERMESSAGE_NOTIFY_CHEAT)
						{
						      System.out.println(ANSI_RED + "[ALERT] => Leider hat jemand geschummelt, der Spieler wurde aus der Sitzung ausgeschlossen."
									  + " Die Sitzung wird zurückgesetzt. [ALERT]" + ANSI_RESET);
							  restart = true;
							  System.exit(0);
						}else
						{
							System.out.println("[id-" + meinSpieler.getClientHandlerId() + "]ClientStatus: " + clientState + "] : " + ANSI_RED + "[ALERT] => Du bist beim Cheaten erwischt worden. Deswegen wirst du aus der Sitzung ausgeschlossen! [ALERT]" + ANSI_RESET);
							restart = false;
							System.exit(0);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}else
			{
				e.printStackTrace();
			}
		}	
		return null;
	}

	private String empfangeErgebnis() {
		try {
			return (String) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private LoginMessage eingabeSpielerDatenFuerLogin() {
		// TODO spielername, GameMode und ggfs mehr ermitteln
		return null;
	}
	public String getDisplayName() {
		return this.spielername;
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

}
