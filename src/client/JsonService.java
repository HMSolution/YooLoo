package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;

public class JsonService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2308046064104874003L;
	private String filePath;
	public JsonService(String filePath)
	{
		this.filePath = filePath;
	}
	
	public List<Spielzug> LiesDatei() {
		File file = new File(filePath);
		if(!file.exists())
		{
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Gson gson = new Gson();
		
        StringBuilder contentBuilder = new StringBuilder();
        
        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) 
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        
        String json = contentBuilder.toString();
        
        Spielzug[] spielzuege = gson.fromJson(json, Spielzug[].class);
        ArrayList<Spielzug> spielzuegeList = new ArrayList<Spielzug>();
        
        if(spielzuege == null || spielzuege.length < 1)
        	return spielzuegeList;
        
        for(Spielzug zug : spielzuege)
        {
        	spielzuegeList.add(zug);
        }
        
        return spielzuegeList;
	}
}
