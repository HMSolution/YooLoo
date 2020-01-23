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
import client.StichErfolg;
import client.YoolooClient;
import common.YoolooKartenspiel.Kartenfarbe;

public class YoolooSpieler implements Serializable {

	private static final long serialVersionUID = 376078630788146549L;
	private String name;
	private Kartenfarbe spielfarbe;
	private int clientHandlerId = -1;
	private int punkte;
	private YoolooKarte[] aktuelleSortierung;
	private ArrayList<Integer> gespielteNummern = new ArrayList<Integer>();

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
		JsonService jsonService = new JsonService(YoolooClient.historiePfad);
		List<Spielzug> spielzuege = jsonService.LiesDatei();
		if(spielzuege == null || spielzuege.size() < 1)
			return;
		YoolooKarte[] neueSortierung = new YoolooKarte[aktuelleSortierung.length];
		List<YoolooKarte> karten = new ArrayList<YoolooKarte>();
		for(YoolooKarte karte : aktuelleSortierung)
		{
			karten.add(karte);
		}
		
		for(int i = 10; i >= 1; i--)
		{
			final int kartenWert =i;
			YoolooKarte karte = karten.stream().filter(x-> x.getWert() == kartenWert).collect(Collectors.toList()).get(0);
			List<Spielzug> relevanteSpielzuege = spielzuege.stream().filter(x -> x.getGesetzteKarte() == kartenWert).collect(Collectors.toList());
			List<StichErfolg> ausgewerteteStiche = WerteSticheAus(kartenWert, relevanteSpielzuege);
			int bestePosition = ErmittelBestePosition(ausgewerteteStiche);
			
			SetzteKarte(karte, bestePosition, neueSortierung);
		}
		
		aktuelleSortierung = neueSortierung;
	}
	
	// Cheat Testzweck
	public void sortierungAufZehnFestlegen() {
		YoolooKarte[] neueSortierung = new YoolooKarte[this.aktuelleSortierung.length];
		for (int i = 0; i < neueSortierung.length; i++) {
			neueSortierung[i] = new YoolooKarte(Kartenfarbe.Blau,10);
			}
			// System.out.println(i+ ". neuerIndex: "+neuerIndex);
		
		aktuelleSortierung = neueSortierung;
	}

	private void SetzteKarte(YoolooKarte karte, int bestePosition, YoolooKarte[] neueSortierung) {
		int kartenWert = karte.getWert();
		if(neueSortierung[bestePosition] == null)
		{
			neueSortierung[bestePosition] = karte;
			return;
		}
		
		if(kartenWert < bestePosition)
		{
			for(int y = bestePosition-1; y >=0 ; y--)
			{
				if(neueSortierung[y] == null)
				{
					neueSortierung[y] = karte;
					return;
				}
			}
		}
		
		if(kartenWert > bestePosition)
		{
			for(int y = bestePosition+1; y < YoolooKartenspiel.maxKartenWert ; y++)
			{

				if(neueSortierung[y] == null)
				{
					neueSortierung[y] = karte;
					return;
				}
			}
		}
		
		for(int y = 0; y < YoolooKartenspiel.maxKartenWert; y++) {
			
			if(neueSortierung[y] == null)
			{
				neueSortierung[y] = karte;
				return;
			}
		}
		
	}

	private int ErmittelBestePosition(List<StichErfolg> ausgewerteteStiche) {
		StichErfolg besterStich = null;
		
		for(StichErfolg se : ausgewerteteStiche)
		{
			if(besterStich == null)
				besterStich = se;
			
			if(besterStich.getWertungInProzent() == null)
				besterStich = se;
			
			
			if(se.getWertungInProzent() != null && besterStich.getWertungInProzent() < se.getWertungInProzent())
				besterStich = se;
		}
		
		if(besterStich.getWertungInProzent() >= 50)
			return besterStich.getStichNummer();
		
		List<StichErfolg> ungespielteStiche = ausgewerteteStiche.stream().filter(x -> x.getWertungInProzent() == null).collect(Collectors.toList());
		
		if(besterStich.getWertungInProzent() < 50 && 
				besterStich.getWertungInProzent() >= 30)
		{
			if(ungespielteStiche.size() < 1)
				return besterStich.getStichNummer();
		}
		
		if(ungespielteStiche.size() > 0)
		{
			StichErfolg besterUngespielteStich = null;
			for(StichErfolg se : ungespielteStiche)
			{
				if(besterUngespielteStich == null)
					besterUngespielteStich = se;
						
				if(besterUngespielteStich.getAnzahlGespielterStiche() >= se.getAnzahlGespielterStiche())
					besterUngespielteStich = se;
			}
			return besterUngespielteStich.getStichNummer();
		}

		return besterStich.getStichNummer();

	}

	private List<StichErfolg> WerteSticheAus(int kartenWert, List<Spielzug> relevanteSpielzuege) {
		List<StichErfolg> stichErfolge = new ArrayList<>();
		int startIndex = 0;
		
		if(kartenWert < 4) {
			startIndex = 0;
		}else {
			startIndex = kartenWert-3;
		}

		for(int i = startIndex; i < YoolooKartenspiel.maxKartenWert
				 					&& i < kartenWert+3  ; i++)
		{
			final int stichNummer = i;
			List<Spielzug> spielzuegeInStich = relevanteSpielzuege.stream().filter(x -> x.getStichNummer() == stichNummer).collect(Collectors.toList());
			
			if(spielzuegeInStich.size() < 1)
			{
				stichErfolge.add(new StichErfolg(stichNummer, null, 0));
				continue;
			}
			
			int gewonneneSpielzuege = (int) spielzuegeInStich.stream().filter(x -> x.getGewonnen() == true).collect(Collectors.toList()).size();
			int anzahlSpielzuege = spielzuegeInStich.size();
			int prozentual = (gewonneneSpielzuege / anzahlSpielzuege) * 100;
			stichErfolge.add(new StichErfolg(stichNummer, prozentual, anzahlSpielzuege));
		}
		
		return stichErfolge;
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
	
	public void gespielteKarteHinzufuegen(YoolooKarte karte)
	{
		gespielteNummern.add(karte.getWert());
	}
	
	public ArrayList<Integer> GetGespielteKarten()
	{
		return gespielteNummern;
	}

}
