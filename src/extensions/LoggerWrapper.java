package extensions;

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
	
	public LoggerWrapper(String name)
	{
		// Logger erzeugen
		logger = Logger.getLogger("Server-Logger-"+name);
		
		//File Handler erzeugen
		try {
			file_handler = new FileHandler("ServerLog-"+name+".txt", true);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Formatter erzeugen
		klartext = new SimpleFormatter();
		file_handler.setFormatter(klartext);
		logger.addHandler(file_handler);
	}
	
	public static void logInfoString(String message)
	{
		logger.log(Level.INFO, message);
	}
	public static void logWarningString(String message)
	{
		logger.log(Level.WARNING, message);
	}
	public static void logFinestString(String message)
	{
		logger.log(Level.FINEST, message);
	}

}
