package Logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.*; 
import java.util.logging.SimpleFormatter;

public class LoggerWrapper {
	// insert Logger
	static Logger logger;
	Handler file_handler;
	Formatter klartext;
	
	public LoggerWrapper()
	{
		// Logger erzeugen
		logger = Logger.getLogger("Server-Logger");
		
		//File Handler erzeugen
		try {
			file_handler = new FileHandler("ServerLog.txt", true);
			logger.addHandler(file_handler);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Formatter erzeugen
		klartext = new SimpleFormatter();
		file_handler.setFormatter(klartext);
	}
	
	public static void logBasicString(String message)
	{
		logger.log(Level.INFO, message);
	}

}
