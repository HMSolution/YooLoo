package extensions;

public class StichErfolg {

	private int stichNummer;
	private Integer wertungInProzent;
	private int anzahlGespielterStiche;
	public StichErfolg(int stichNummer, Integer wertung, int anzahlGespielterStiche) {
		this.stichNummer = stichNummer;
		this.wertungInProzent = wertung;
		this.anzahlGespielterStiche = anzahlGespielterStiche;
	}
	
	public Integer getWertungInProzent() {
		return wertungInProzent;
	}
	
	public int getAnzahlGespielterStiche() {
		return anzahlGespielterStiche;
	}
	public void setWertungInProzent(Integer wertungInProzent) {
		this.wertungInProzent = wertungInProzent;
	}
	public int getStichNummer() {
		return stichNummer;
	}
	public void setStichNummer(int stichNummer) {
		this.stichNummer = stichNummer;
	}
}
