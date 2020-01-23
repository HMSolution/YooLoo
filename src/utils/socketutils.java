package utils;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;

public class socketutils {

    public static void sendSerialized(Socket s, Serializable object) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            oos.writeObject(object);
            oos.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        return;
    }
}


