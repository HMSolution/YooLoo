// History of Change
// vernr    |date  | who | lineno | what
//  V0.106  |200107| cic |    -   | add history of change

package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;

import client.JsonService;
import client.Spielzug;
import common.YoolooKartenspiel.Kartenfarbe;

public class YoolooSpieler implements Serializable {

	private static final long serialVersionUID = 376078630788146549L;
	private String name;
	private Kartenfarbe spielfarbe;
	private int clientHandlerId = -1;
	private int punkte;
	private JsonService jsonService = new JsonService("Spielhistorie.json");
	private YoolooKarte[] aktuelleSortierung;

	public YoolooSpieler(String name, int maxKartenWert) {
		this.name = name;
		this.punkte = 0;
		this.spielfarbe = null;
		this.aktuelleSortierung = new YoolooKarte[maxKartenWert];
	}

	// Letzte genutzte Sortierung (Antwort des Servers auf den Login) wird als Basis genommen.
	// Danach wird die Sortierung nach folgenden Kreterien abgeändert:
	// 1. Erfolg einer Karte in einem Stich
	// 2. Kartenwertigkeit / Stich Punkte Verhältnis 
	public void sortierungFestlegen() {
		List<Spielzug> spielzuege = jsonService.LiesDatei();
		if(spielzuege == null || spielzuege.size() < 1)
			return;
		YoolooKarte[] neueSortierung = aktuelleSortierung;
		
		for(YoolooKarte karte : neueSortierung)
		{
			List<Spielzug> relevanteSpielzuege = spielzuege.stream().filter(x -> x.getGesetzteKarte() == karte.getWert()).collect(Collectors.toList());
			//List<StichErfolg> ausgewerteteStiche = WerteSticheAus(relevanteSpielzuege);
		}
		
	}

	public int erhaeltPunkte(int neuePunkte) {
		System.out.print(name + " hat " + punkte + " P - erhaelt " + neuePunkte + " P - neue Summe: ");
		this.punkte = this.punkte + neuePunkte;
		System.out.println(this.punkte);
		return this.punkte;
	}

	@Override
	public String toString() {
		return "YoolooSpieler [name=" + name + ", spielfarbe=" + spielfarbe + ", puntke=" + punkte
				+ ", altuelleSortierung=" + Arrays.toString(aktuelleSortierung) + "]";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Kartenfarbe getSpielfarbe() {
		return spielfarbe;
	}

	public void setSpielfarbe(Kartenfarbe spielfarbe) {
		this.spielfarbe = spielfarbe;
	}

	public int getClientHandlerId() {
		return clientHandlerId;
	}

	public void setClientHandlerId(int clientHandlerId) {
		this.clientHandlerId = clientHandlerId;
	}

	public int getPunkte() {
		return punkte;
	}

	public void setPunkte(int puntke) {
		this.punkte = puntke;
	}

	public YoolooKarte[] getAktuelleSortierung() {
		return aktuelleSortierung;
	}

	public void setAktuelleSortierung(YoolooKarte[] aktuelleSortierung) {
		this.aktuelleSortierung = aktuelleSortierung;
	}

	public void stichAuswerten(YoolooStich stich) {
		System.out.println(stich.toString());

	}

}
