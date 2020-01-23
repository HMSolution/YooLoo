package extensions;

import java.io.Serializable;

public class Spielzug implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5367473574648662387L;

	private int stichNummer;
	private int gesetzteKarte;
	private boolean gewonnen;
	
	public Spielzug(int stichNummer, int kartenWert, boolean stichGewonnen) {
		this.stichNummer = stichNummer;
		this.gesetzteKarte = kartenWert;
		this.gewonnen = stichGewonnen;
		
	}

	public int getGesetzteKarte() {
		return gesetzteKarte;
	}
	
	public int getStichNummer() {
		return stichNummer;
	}

	public boolean getGewonnen() {
		return gewonnen;
	}
}
