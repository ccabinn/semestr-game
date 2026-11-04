package sazeni;

import java.util.ArrayList;
import java.util.List;

public class SpravcePole {
    private List<Zahonek> zahonky;
    private static final int MAX_ZAHONKU = 6; // Pole má 6 záhonků[cite: 1]

    public SpravcePole() {
        zahonky = new ArrayList<>(MAX_ZAHONKU);
        for (int i = 0; i < MAX_ZAHONKU; i++) {
            zahonky.add(new Zahonek());
        }
    }

    // Mechanika: zasad <plodina>
    public boolean zasadPlodinu(Plodina plodina) {
        for (Zahonek z : zahonky) {
            if (z.jePrazdny()) {
                z.zasad(plodina);
                System.out.println("Zasadil jsi " + plodina.getNazev() + " na volný záhonek.");
                return true; // Úspěšně zasazeno na první volný záhonek[cite: 1]
            }
        }
        System.out.println("Všechny záhonky jsou plné!");
        return false;
    }

    // Mechanika: obstarej
    // Bere ohled na kropítka (0 až 4 ks) a počasí (JASNO, SUCHO, ZAPLAVY)[cite: 1]
    public void obstarejPole(Hrac hrac, String pocasi, int pocetKropitek) {
        boolean jeSucho = pocasi.equals("SUCHO");
        boolean jsouZaplavy = pocasi.equals("ZAPLAVY");

        // Během záplav jsou záhonky zavodněné automaticky a zdarma[cite: 1]
        if (jsouZaplavy) {
            for (Zahonek z : zahonky) {
                if (!z.jePrazdny()) z.setDnesZalito(true);
            }
            System.out.println("Jsou záplavy. O záhonky se nemusíš starat, jsou zavodněné automaticky.");
            return;
        }

        int pocetKZaliti = 0;
        for (Zahonek z : zahonky) {
            if (z.vyzadujePeci(jeSucho) && !z.isDnesZalito()) {
                pocetKZaliti++;
            }
        }

        if (pocetKZaliti == 0) {
            System.out.println("Žádný záhonek dnes nepotřebuje obstarat.");
            return;
        }

        // Výpočet energie na jeden záhonek
        // Základ je 5, každé kropítko sníží o 1 (minimum je 1)[cite: 1]
        int cenaZaZahonek = Math.max(1, 5 - Math.min(pocetKropitek, 4));

        // Během sucha bere zalévání 2x více energie[cite: 1]
        if (jeSucho) {
            cenaZaZahonek *= 2;
        }

        int celkovaEnergie = pocetKZaliti * cenaZaZahonek;

        if (hrac.getEnergie() < celkovaEnergie) {
            System.out.println("Nemáš dostatek energie na obstarání pole! Potřebuješ " + celkovaEnergie);
            return;
        }

        // Odečtení energie hráči a označení záhonků jako zalitých
        hrac.uberEnergii(celkovaEnergie);
        for (Zahonek z : zahonky) {
            if (z.vyzadujePeci(jeSucho)) {
                z.setDnesZalito(true);
            }
        }

        System.out.println("Zalil jsi " + pocetKZaliti + " záhonků. Spotřebováno " + celkovaEnergie + " energie.");
    }

    // Denní aktualizace (volá se v noci / při spánku)
    public void vyhodnotKonecDne(String pocasi) {
        boolean jeSucho = pocasi.equals("SUCHO");
        boolean jsouZaplavy = pocasi.equals("ZAPLAVY");

        for (Zahonek z : zahonky) {
            if (!z.jePrazdny()) {
                // Pokud záhonek vyžadoval péči a nebyl zalit (a nebyly záplavy), uschne[cite: 1]
                if (z.vyzadujePeci(jeSucho) && !z.isDnesZalito() && !jsouZaplavy) {
                    z.znicPlodinu();
                    System.out.println("Plodina uschla kvůli nedostatku péče!");
                }
                z.posunDen();
            }
        }
    }
}