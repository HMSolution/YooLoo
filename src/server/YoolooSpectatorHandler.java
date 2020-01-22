package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import client.YoolooClient.ClientState;
import common.YoolooKarte;
import common.YoolooStich;
import messages.ClientMessage;
import messages.ServerMessage;
import messages.ServerMessage.ServerMessageResult;
import messages.ServerMessage.ServerMessageType;
import utils.socketutils;

public class YoolooSpectatorHandler implements Runnable {

    private Socket clientConnection;
    private YoolooServer parentServer;
    private YoolooSession session;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public YoolooSpectatorHandler(YoolooServer yoolooServer, Socket client) {
        this.clientConnection = client;
        this.parentServer = yoolooServer;
        try {
            this.oos = new ObjectOutputStream(client.getOutputStream());
            this.ois = new ObjectInputStream(client.getInputStream());
        } catch (IOException e) {e.printStackTrace();}
    }

    public void joinSession(YoolooSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        try{
            // Bereite den Client auf die Runde vor
            System.out.println("Sending PREPARE_EVENT_LOOP");
            System.out.println("Socket State: " + (this.clientConnection.isClosed() ? "closed" : "open"));
            this.oos.writeObject(new ServerMessage(
                                    ServerMessageType.SERVERMESSAGE_PREPARE_EVENT_LOOP, 
                                    ClientState.CLIENTSTATE_NULL,
                                    ServerMessageResult.SERVER_MESSAGE_RESULT_OK)
                                );
                        
            
                                      
            while(true){
                try{
                    System.out.println("Socket State: " + (this.clientConnection.isClosed() ? "closed" : "open"));
                }catch(Exception e){}

                YoolooStich[] stiche = this.session.getStiche();
                boolean passed = true;
                for(int i = 0; i < stiche.length; i++){
                    if(!(stiche[i] instanceof YoolooStich)) {
                        passed = false;
                    }
                }
                if(passed) {
                    // Sobald die Runde vorbei ist schickt er die Stiche als Antwort
                    // kein "wahrer" Zuschauermodus, aber er ist funktional
                    //socketutils.sendSerialized(this.clientConnection, stiche);
                    oos.writeObject(stiche);
                    oos.flush();

                    // Warte auf Antwort bevor die Verbindung geschlossen wird
                    //ClientMessage bufferMessage = (ClientMessage) ois.readObject();
                    break;
                };
            }

        }catch(Exception e){
            System.out.println("[.] Error im SpectatorHandler: ");
            e.printStackTrace();
        }finally {
            System.out.println("Closing Spectator Connection...");
            try{
                this.clientConnection.close();
            }catch(Exception e){}

        }
    }


}
