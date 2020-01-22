package utils;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class socketutils {

    public static void sendSerialized(Socket s, Serializable object) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            oos.writeObject(object);
            oos.close();
        }catch(IOException e){
            System.out.println("[!] Konnte Daten nicht versenden!");
            e.printStackTrace();
        }
        return;
    }

    public static <T> T receive(Socket s){
        try {
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            T o = (T) ois.readObject();
            ois.close();
            return o;
        }catch(ClassCastException e){
            System.out.println("[!] Konnte Nachricht nicht in den angegebenen generischen Typen casten.");
        }catch(Exception e){
            System.out.println("[!] Es ist ein fehler beim empfangen von Daten aufgetreten!");
            e.printStackTrace();
        }
        return null;
    }

}


