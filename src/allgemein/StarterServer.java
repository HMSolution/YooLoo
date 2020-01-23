// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add history of change

package allgemein;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import server.YoolooServer;
import server.YoolooServer.GameMode;

public class StarterServer {

	public static void main(String[] args) {
		int listeningPort = 44137;
		int spieleranzahl = 2; // min 1, max Anzahl definierte Farben in Enum YoolooKartenSpiel.KartenFarbe)
		
		// Threadpool für Spectator/Client Sockets
		ExecutorService socketPool = Executors.newCachedThreadPool();;
		
		YoolooServer server = new YoolooServer(listeningPort, listeningPort + 1, spieleranzahl, GameMode.GAMEMODE_SINGLE_GAME);
		
		socketPool.execute(new Thread(() -> {
			server.startSpectatorServer();
		}));
		socketPool.execute(new Thread(() -> {
			server.startServer();
		}));
		

	}

}
