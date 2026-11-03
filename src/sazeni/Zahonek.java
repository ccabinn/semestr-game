package sazeni;

public class Zahonek {
    private Plodina zasazenaPlodina;
    private int dnyRustu;
    private boolean dnesZalito;
    private boolean vceraZalito;
    private boolean uschlo;

    public Zahonek() {
        vyprazdni();
    }

    public void zasad(Plodina plodina) {
        this.zasazenaPlodina = plodina;
        this.dnyRustu = 0;
        this.dnesZalito = true; // Při zasazení považujeme za zalité
        this.vceraZalito = true;
        this.uschlo = false;
    }

    public void vyprazdni() {
        this.zasazenaPlodina = null;
        this.dnyRustu = 0;
        this.dnesZalito = false;
        this.vceraZalito = false;
        this.uschlo = false;
    }

    public boolean jePrazdny() {
        return zasazenaPlodina == null;
    }

    public Plodina getPlodina() { return zasazenaPlodina; }

    public void setDnesZalito(boolean stav) { this.dnesZalito = stav; }
    public boolean isDnesZalito() { return dnesZalito; }

    public void posunDen() {
        if (jePrazdny() || uschlo) return;
        vceraZalito = dnesZalito;
        dnesZalito = false;
        dnyRustu++;
    }

    public boolean jePripravenoKSklizni() {
        return !jePrazdny() && !uschlo && dnyRustu >= zasazenaPlodina.getDobaRastuDny();
    }

    public void znicPlodinu() {
        this.uschlo = true;
    }

    // Zjišťuje, zda daný záhonek dnes vyžaduje obstarání
    public boolean vyzadujePeci(boolean jeSucho) {
        if (jePrazdny() || uschlo || jePripravenoKSklizni()) return false;

        // Během sucha se i zelenina musí zalévat každý den[cite: 1]
        if (jeSucho) return true;

        // Ovoce vyžaduje péči každý den[cite: 1]
        if (zasazenaPlodina.getTyp() == Plodina.Typ.OVOCE) return true;

        // Zelenina se v základu zalévá obden (pokud nebyla zalita včera, musí se dnes)[cite: 1]
        return !vceraZalito;
    }
}