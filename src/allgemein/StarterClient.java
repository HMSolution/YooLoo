// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add history of change

package allgemein;

import client.YoolooClient;
import client.YoolooClient.ClientMode;

public class StarterClient {
	public static void main(String[] args) {

		// Starte Client
		String hostname = "localhost";
//		String hostname = "10.101.251.247";
		int port = 44137;
		ClientMode clientMode = ClientMode.CLIENTMODE_PLAYER;
		if(args.length >= 1 && args[0].equals("spectate")) {
			clientMode = ClientMode.CLIENTMODE_SPECTATOR;
			port += 1;
		}
		
		YoolooClient client = new YoolooClient(clientMode, hostname, port);
		client.startClient();
	}
}
