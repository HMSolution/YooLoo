package client;

import java.io.Serializable;

public class Spielzug implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5367473574648662387L;

	private int punkte;
	private int gesetzteKarte;
	private boolean gewonnen;
	
	public Spielzug(int stichNummer, int kartenWert, boolean stichGewonnen) {
		this.punkte = stichNummer;
		this.gesetzteKarte = kartenWert;
		this.gewonnen = stichGewonnen;
		
	}

	public int getGesetzteKarte() {
		return gesetzteKarte;
	}
	
	public void setGesetzteKarte(int gesetzteKarte) {
		this.gesetzteKarte = gesetzteKarte;
	}
	
	public int getPunkte() {
		return punkte;
	}
	
	public void setPunkte(int punkte) {
		this.punkte = punkte;
	}

	public boolean getGewonnen() {
		return gewonnen;
	}

	public void setGewonnen(boolean gewonnen) {
		this.gewonnen = gewonnen;
	}
}
