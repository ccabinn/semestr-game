package sazeni;

public class Plodina {
    public enum Typ {
        ZELENINA, // Obstarává se obden (v základu)
        OVOCE     // Obstarává se každý den
    }

    private String id;
    private String nazev;
    private Typ typ;
    private int dobaRastuDny; // Za jak dlouho lze sklidit

    public Plodina(String id, String nazev, Typ typ, int dobaRastuDny) {
        this.id = id;
        this.nazev = nazev;
        this.typ = typ;
        this.dobaRastuDny = dobaRastuDny;
    }

    public Typ getTyp() { return typ; }
    public int getDobaRastuDny() { return dobaRastuDny; }
    public String getNazev() { return nazev; }
}