# Farma na konci světa

Autoři: Matěj Cabejšek, Gabriela Brůžková, Samuel Martirosjan, Josef Langmaier

Desktopová tahová survival a farmářská hra v Javě zasazená do post-apokalyptického prostředí. Hráč spravuje vlastní farmu, pěstuje plodiny, obchoduje na trzích, plní úkoly pro přeživší postavy a brání své pole před nočními nájezdy zombíků a extrémním počasím.

Aplikace je postavena na knihovně **Java Swing** a podporuje dva režimy ovládání, mezi kterými lze kdykoli přepínat za běhu hry:
* **GUI režim (Tlačítka & Dialogy)** – interaktivní ovládání přes tlačítka a modální okna (obchod, výběr semínek, interakce s NPC).
* **CLI režim (Konzole)** – klasické textové zadávání příkazů včetně vývojářských příkazů (DevTools) pro testování.

---

## Použité technologie a závislosti

* **Jazyk:** Java (JDK 25 / kompatibilní s Java SE 17+)
* **Grafické rozhraní:** Java Swing (`javax.swing`, `java.awt` – `BorderLayout`, `CardLayout`, `JDialog`)
* **Serializace a načítání dat:** **Google Gson (`gson-2.10.1.jar`)**
  * Knihovna Gson je uložena ve složce `lib/gson-2.10.1.jar` a slouží v balíčku `loader` k deserializaci všech herních entit (plodin, předmětů, mapy a postav s úkoly) z JSON souborů v `src/data/`.

---

## Herní mechaniky

### 1. Farmaření a pěstování (`farming`)
* Hráč má k dispozici **6 záhonků** na poli (`FieldManager`, `Plot`).
* Hra obsahuje **16 druhů plodin** (`crops.json`):
  * **8 druhů zeleniny** (*obilí, hlávkový salát, cibule, špenát, mrkev, brambory, dýně, česnek*) – vyžadují zálivku **obden** (`EVERY_OTHER_DAY`).
  * **8 druhů ovoce** (*jahody, maliny, borůvky, jablka, hrušky, meruňky, broskve, meloun*) – vyžadují zálivku **každý den** (`EVERY_DAY`). Nákup a sázení ovoce se odemkne až po zasazení všech 8 druhů zeleniny.
* Zalévání spotřebovává energii (**5 EN/záhonek**, během sucha **10 EN/záhonek**). Zakoupením až **4 kropítek** lze spotřebu snížit až na 1 EN/záhonek. Nezalité plodiny při přechodu do dalšího dne uschnou.

### 2. Mapa, lokace a doprava (`map`)
* Svět tvoří **7 propojených lokací** (`locations.json`):
  * **Farma:** `Dům (home)` $\leftrightarrow$ `Pole (field)`
  * **Okolí:** `Místní trh (local_market)` (dostupný na **Kole**)
  * **Město (vyžaduje Ojeté Auto):** `Město (city)` $\leftrightarrow$ `Autobazar (autobazar)`, `Army shop (army_shop)`, `Městský trh (city_market)`
* **Úrovně dopravy (`TransportType`):** `Pěšky (FOOT)` $\rightarrow$ `Kolo (BIKE)` $\rightarrow$ `Ojeté Auto (CAR)` $\rightarrow$ `Dodávka (VAN)`. Lepší vozidla a **motorový tuning** snižují energetickou náročnost cestování.

### 3. Ekonomika a obchodování (`inventory`, `gui.ShopDialog`)
* **Místní trh:** Základní semínka, potraviny, hlídací pes, krmení, **Ojeté Auto** (150 Kč). Výkup plodin za **100 %** základní ceny.
* **Městský trh:** Všechna semínka, vydatná jídla, vylepšení farmy (**Kropítko**, **Hlubší studna**). Výkup plodin za **200 % (2×)** základní ceny.
* **Autobazar:** Prodej **Dodávky** (300 Kč, po splnění úkolu Mechanika Romana se slevou za 220 Kč).
* **Army shop:** Dřevěný plot, puška, brokovnice; po úkolu Seržanta Krále odemčení **Elektrické ohrady** a **Automatické věže (Turret)** + 20 % sleva na zbraně.

### 4. Počasí a noční nájezdy zombíků (`weather`, `zombies`)
* **Počasí (`WeatherManager`):** Prvních 5 dní je slunečno (`NORMAL`). Od 6. dne roste pravděpodobnost extrémního počasí:
  * **Sucho (`DROUGHT`):** Zdvojnásobuje spotřebu energie na zalévání a vyžaduje každodenní zálivku všech plodin.
  * **Záplava (`FLOOD`):** Zkratuje elektrické ohrady a automatické věže během nočního útoku.
* **Noční nájezdy (`ZombieManager`):** Od **10. dne** útočí vlny zombíků (*Chodec*, *Běžec*, *Obrněný Zombie*). Obranu zajišťují věže, zbraně hráče, nakrmení hlídací psi a ploty. Při prolomení obrany se hráč v noci nevyspí a neobnoví si +50 EN.

### 5. Postavy (NPC), úkoly a cíl hry (`assets`)
Ve světě se nachází **7 NPC postav** (`npcs.json`), které zadávají úkoly za peníze, předměty nebo trvalé herní efekty (`QuestEffect`). Hru lze vyhrát **dvěma způsoby**:
1. **Únik do bezpečí (`KONEC_UNIK`):** Splnění úkolu *Jízdenka do bezpečí* pro **Pašeráka Viktora** ve Městě (1 000 Kč + zásoby jídla).
2. **Vývoj vakcíny (`KONEC_LEK`):** Dokončení třífázové úkolové linie *Projekt Vakcína I–III* pro **Doktorku Elenu** na Městském trhu (vyvrcholí vypěstováním geneticky čistého melounu).

---

## Struktura projektu

```text
semestr-game/
├── lib/
│   └── gson-2.10.1.jar          # Knihovna Google Gson pro parsování JSON dat
└── src/
    ├── Main.java                # Vstupní bod aplikace (spuštění GUI)
    ├── assets/                  # NPC systém, úkoly, požadavky a herní efekty
    │   ├── GameContext.java
    │   ├── Npc.java
    │   ├── NpcManager.java
    │   ├── Quest.java
    │   ├── QuestEffect.java
    │   ├── QuestState.java
    │   └── Requirement.java
    ├── data/                    # Konfigurační JSON soubory hry
    │   ├── crops.json           # Definice plodin, doby růstu a frekvence zálivky
    │   ├── items.json           # Katalog předmětů, cen a dostupnosti v lokacích
    │   ├── locations.json       # Mapa světa, přechody a energetická náročnost
    │   └── npcs.json            # Postavy, dialogy, úkoly a odměny
    ├── farming/                 # Logika pole, záhonků a růstu plodin
    │   ├── Crop.java
    │   ├── CropCatalog.java
    │   ├── CropCategory.java
    │   ├── FieldManager.java
    │   ├── Plot.java
    │   └── WateringFrequency.java
    ├── game/                    # Hlavní řídicí logika hry a zpracování příkazů
    │   └── GameManager.java
    ├── gui/                     # Grafické uživatelské rozhraní (Java Swing)
    │   ├── GameWindow.java
    │   └── ShopDialog.java
    ├── inventory/               # Inventář hráče, katalog a logika obchodování
    │   ├── Inventory.java
    │   ├── Item.java
    │   ├── ItemCatalog.java
    │   ├── ItemType.java
    │   └── ShopManager.java
    ├── loader/                  # Načítání JSON dat pomocí knihovny Gson
    │   ├── CropLoader.java
    │   ├── ItemLoader.java
    │   ├── MapLoader.java
    │   └── NpcLoader.java
    ├── map/                     # Reprezentace lokací, cest a dopravních prostředků
    │   ├── GamePlan.java
    │   ├── Location.java
    │   ├── Passage.java
    │   └── TransportType.java
    ├── player/                  # Stav hráče (energie, peníze, vozidlo, progres)
    │   └── Player.java
    ├── weather/                 # Dynamické počasí a pohromy (sucho, záplavy)
    │   ├── WeatherManager.java
    │   └── WeatherType.java
    └── zombies/                 # Noční nájezdy zombíků a vyhodnocení obrany
        ├── DefenseStatus.java
        ├── NightRaidResult.java
        ├── Zombie.java
        ├── ZombieManager.java
        └── ZombieWaveFactory.java
```

## Standardní herní příkazy

### 1. Pohyb a lokace
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `jdi` | `go`, `move` | `<id_lokace>` | Přesun do sousední nebo dostupné lokace. | `jdi field`, `jdi local_market` |

*Dostupné lokace:* `home` (Dům), `field` (Pole), `local_market` (Místní trh), `city` (Město), `autobazar` (Autobazar), `army_shop` (Army shop), `city_market` (Městský trh).

---

### 2. Správa farmy a pole
Příkazy pro farmaření fungují na lokalitě `field` (Pole).

| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `pole` | `zahony`, `fieldstatus` | *žádný* | Zobrazí stav všech 6 záhonků (plodina, fáze růstu, stav zálivky). | `pole` |
| `zasad` | `plant` | `<id_seminka>` | Zasadí vybrané semínko na první volný záhonek. | `zasad seminko_obili` |
| `obstarej`| `zali`, `water` | *žádný* | Zalije všechny zasazené záhonky (spotřebovává EN). | `obstarej` |
| `sklid` | `harvest` | *žádný* | Sklidí všechno zralé ovoce a zeleninu ze záhonků do inventáře. | `sklid` |

---

### 3. Obchod a inventář
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `inventar` | `inv` | *žádný* | Vypíše aktuální obsah hráčova batohu a předměty. | `inventar` |
| `obchod` | `nabidka` | *žádný* | Zobrazí zboží dostupné ke koupi v aktuální lokalitě. | `obchod` |
| `kup` | `buy` | `<id_itemu> [počet]` | Nakoupí předmět z obchodu. Pokud počet chybí, nakoupí 1 ks. | `kup kropitko`, `kup seminko_mrkev 5` |
| `prodej` | `sell` | `<id_itemu> [počet]` | Prodá úrodu na trhu (100% ceny na Místním trhu, 200% na Městském trhu). | `prodej mrkev 10` |

---

### 4. Postavy (NPC) a Úkoly
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `mluv` | `talk` | `<id_npc>` | Zahájí rozhovor s NPC postavou v aktuální lokalitě a přijme její úkol. | `mluv bohous`, `mluv elena` |
| `ukol` | *žádný* | `<id_npc>` | Pokusí se odevzdat splněný úkol vybrané postavě. | `ukol bohous` |

*Přehled NPC postav:* `bohous` (Kutil Bohouš), `roman` (Mechanik Roman), `kral` (Seržant Král), `jarda` (Trhovec Jarda), `obchodnik` (Městský obchodník), `viktor` (Pašerák Viktor), `elena` (Doktorka Elena).

---

### 5. Odpočinek a stav
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `stav` | `status` | *žádný* | Zobrazí aktuální den, pořadí dnu, počasí, EN, peníze a vozidlo. | `stav` |
| `spi` | `sleep` | *žádný* | Ukončí den (lze použít pouze v `home`). Obnoví +50 EN a vyhodnotí noční události. | `spi` |
| `napoveda` | `help`, `pomoc` | *žádný* | Vypíše zkrácenou nápovědu příkazů přímo do konzole. | `napoveda` |

---

## Vývojářské příkazy (DevTools)

Tyto příkazy slouží pro rychlé testování mechanik, odlaďování úkolů a simulaci situací.

| Příkaz | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- |
| `devhelp` | *žádný* | Vypíše přehled všech vývojářských příkazů. | `devhelp` |
| `tp` | `<id_lokace>` | Okamžitý teleport do libovolné lokace bez omezení cestování. | `tp city_market` |
| `give` | `<id_itemu> [počet]` | Přidá vybraný předmět přímo do inventáře. | `give pusska 1`, `give jizdenka 1` |
| `money` | `<částka>` | Přidá zadanou částku peněz (akceptuje i záporné hodnoty). | `money 1000` |
| `energy` | `<částka>` | Přidá nebo doplní zadané množství energie. | `energy 100` |
| `setday` | `<den>` | Nastaví Herní Den na konkrétní číslo. | `setday 10` |
| `setweather` | `<slunecno\|sucho\|zaplava>` | Změní aktuální stav počasí. | `setweather sucho` |
| `setvehicle` | `<bike\|car\|van>` | Nastaví úroveň dopravního prostředku hráče. | `setvehicle van` |
| `growall` | `dozraj` | Okamžitě zrychlí růst všech plodin na záhoncích do finální fáze. | `growall` |
| `raid` | *žádný* | Okamžitě vyvolá noční útok zombíků pro otestování obrany. | `raid` |
| `items` | *žádný* | Zobrazí kompletní seznam všech ID předmětů načtených v katalogu. | `items` |
