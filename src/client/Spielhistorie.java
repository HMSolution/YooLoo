package client;

import java.io.Serializable;
import java.util.List;

public class Spielhistorie implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1556319813329717822L;
	private List<Spielzug> spielzuege;
	
	public List<Spielzug> getSpielzuege() {
		return spielzuege;
	}
	
	public void setSpielzuege(List<Spielzug> spielzuege) {
		this.spielzuege = spielzuege;
	}
	
	public void addSpielzug(Spielzug spielzug) {
		spielzuege.add(spielzug);
	}
}
