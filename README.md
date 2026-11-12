# Farma na konci světa

Autoři: Matěj Cabejšek, Gabriela Brůžková, Samvel Martirosjan, Josef Langmaier

Desktopová tahová survival a farmářská hra v Javě zasazená do post-apokalyptického prostředí. Hráč spravuje vlastní farmu, pěstuje plodiny, obchoduje na trzích, plní úkoly pro přeživší postavy a brání své pole před nočními nájezdy zombíků a extrémním počasím.

Aplikace je postavena na knihovně **Java Swing** a podporuje dva režimy ovládání, mezi kterými lze kdykoli přepínat za běhu hry:
* **GUI režim (Tlačítka & Dialogy)** – interaktivní ovládání přes tlačítka a modální okna (obchod, výběr semínek, interakce s NPC).
* **CLI režim (Konzole)** – klasické textové zadávání příkazů včetně vývojářských příkazů (DevTools) pro testování.

---

## Použité technologie a závislosti

* **Jazyk:** Java (JDK 25 / zdrojový kód kompatibilní s Java 21+)
* **Build systém:** **Apache Maven** (`pom.xml`) – řízení závislostí, sestavení, spouštění testů i hry
* **Grafické rozhraní:** Java Swing (`javax.swing`, `java.awt` – `BorderLayout`, `CardLayout`, `JDialog`)
* **Serializace a načítání dat:** **Google Gson 2.10.1** (závislost v `pom.xml`, Maven ji stáhne automaticky)
  * Slouží v balíčku `loader` k deserializaci všech herních entit (plodin, předmětů, mapy a postav s úkoly) z JSON souborů v `src/data/`.
  * V balíčku `save` slouží k ukládání a načítání rozehrané hry do JSON souborů ve složce `saves/`.
* **Testování:** **JUnit 5** (závislost v `pom.xml`, testy ve složce `test/`)

---

## Sestavení a spuštění

Projekt se sestavuje nástrojem **Maven** (v IntelliJ IDEA: pravý klik na `pom.xml` → *Add as Maven Project*).

| Akce | Příkaz |
| :--- | :--- |
| Sestavení projektu | `mvn compile` |
| Spuštění všech unit testů | `mvn test` |
| Spuštění hry | `mvn compile exec:java` |

Zdrojové kódy jsou ve složce `src/`, testy ve složce `test/` (nastaveno v `pom.xml`).

---

## Herní mechaniky

### 1. Farmaření a pěstování (`farming`)
* Hráč má k dispozici **6 záhonků** na poli (`FieldManager`, `Plot`). Na začátku hry má v inventáři 2× semínko obilí.
* Hra obsahuje **16 druhů plodin** (`crops.json`):
  * **8 druhů zeleniny** (*obilí, hlávkový salát, cibule, špenát, mrkev, brambory, dýně, česnek*) – vyžadují zálivku **obden** (`EVERY_OTHER_DAY`).
  * **8 druhů ovoce** (*jahody, maliny, borůvky, jablka, hrušky, meruňky, broskve, meloun*) – vyžadují zálivku **každý den** (`EVERY_DAY`). Nákup a sázení ovoce se odemkne až po zasazení všech 8 druhů zeleniny.
* Zalévání spotřebovává energii (**5 EN/záhonek**, během sucha **10 EN/záhonek**). Zakoupením až **4 kropítek** lze spotřebu snížit až na 1 EN/záhonek.
* Nezalité plodiny při přechodu do dalšího dne **uschnou**. Uschlé plodiny se odstraní příkazem `sklid`, který zároveň sklidí zralou úrodu.

### 2. Energie, spánek a jídlo
* Maximální energie je **100 EN**. Spánek v Domě doplní **+50 EN** a posune hru do dalšího dne.
* Energii lze doplnit také **jídlem** z inventáře (příkaz `jez`):

| Jídlo | Kde koupit | Cena | Energie |
| :--- | :--- | :--- | :--- |
| Chléb | Místní i Městský trh | 8 Kč | +15 EN |
| Sýr | Místní i Městský trh | 14 Kč | +25 EN |
| Uzené maso | Místní trh | 22 Kč | +40 EN |
| Polévka | Městský trh | 30 Kč | +55 EN |
| Koláč | Městský trh | 40 Kč | +75 EN |
| Guláš | Městský trh | 55 Kč | +100 EN |

### 3. Mapa, lokace a doprava (`map`)
* Svět tvoří **7 propojených lokací** (`locations.json`):
  * **Farma:** `Dům (home)` $\leftrightarrow$ `Pole (field)`
  * **Okolí:** `Místní trh (local_market)` (dostupný na **Kole**)
  * **Město (vyžaduje Ojeté Auto):** `Město (city)` $\leftrightarrow$ `Autobazar (autobazar)`, `Army shop (army_shop)`
  * **Městský trh (`city_market`):** dostupný z Města pouze s **Dodávkou**
* **Úrovně dopravy (`TransportType`):** `Pěšky (FOOT)` $\rightarrow$ `Kolo (BIKE)` $\rightarrow$ `Ojeté Auto (CAR)` $\rightarrow$ `Dodávka (VAN)`.
* Energie za cestu se odečítá **při odjezdu z Domu a pokrývá cestu tam i zpět** – návrat domů je zdarma, takže hráč nikdy neuvízne mimo domov:

| Cesta | Kolo | Auto | Dodávka |
| :--- | :--- | :--- | :--- |
| Dům → Místní trh | 40 EN | 20 EN | 10 EN |
| Dům → Město | nelze | 40 EN | 20 EN |
| Město → Městský trh | nelze | nelze | 10 EN |
| Dům ↔ Pole, Město ↔ Autobazar / Army shop, všechny návraty | 0 EN | 0 EN | 0 EN |

* **Motorový tuning** (odměna od Mechanika Romana) snižuje každou cestu autem nebo dodávkou o 5 EN.

### 4. Ekonomika a obchodování (`inventory`, `gui.ShopDialog`)
* **Místní trh:** Semínka, základní jídlo, hlídací pes (60 Kč), krmení pro psy (30 Kč), **Ojeté Auto** (150 Kč). Výkup plodin za **100 %** základní ceny.
* **Městský trh:** Semínka, vydatná jídla, krmení pro psy, vylepšení farmy (**Kropítko**, **Hlubší studna**). Výkup plodin za **200 % (2×)** základní ceny.
* **Autobazar:** Prodej **Dodávky** (300 Kč, po splnění úkolu Mechanika Romana se slevou za 220 Kč).
* **Army shop:** Dřevěný plot, puška, brokovnice; po úkolu Seržanta Krále odemčení **Elektrické ohrady** a **Automatické věže (Turret)** + 20 % sleva na zbraně. Do té doby jsou v nabídce označeny jako `[ZAMČENO]`.
* Zakoupené vozidlo se neukládá do inventáře, ale rovnou se stane hráčovým dopravním prostředkem.

### 5. Počasí a noční nájezdy zombíků (`weather`, `zombies`)
* **Počasí (`WeatherManager`):** Prvních 5 dní je slunečno (`NORMAL`). Od 6. dne každý den roste pravděpodobnost pohromy; po pohromě následují 3 dny klidu:
  * **Sucho (`DROUGHT`):** Zdvojnásobuje spotřebu energie na zalévání a vyžaduje každodenní zálivku všech plodin.
  * **Záplava (`FLOOD`):** Zkratuje elektrické ohrady a automatické věže během nočního útoku.
* **Noční nájezdy (`ZombieManager`):** Od **10. dne** útočí vlny zombíků, jejichž počet i síla rostou s každým dnem (*Chodec*, od 15. dne *Běžec*, od 25. dne *Obrněný Zombie*).
* **Obrana farmy (`DefenseStatus`):** Síla každého obranného prvku se načítá z `items.json` (hodnota `value`):

| Prvek | Kde koupit | Cena | Síla | Poznámka |
| :--- | :--- | :--- | :--- | :--- |
| Hlídací pes | Místní trh | 60 Kč | 25 | Brání jen když je nakrmený |
| Dřevěný plot | Army shop | 80 Kč | 20 | Pohlcuje útok, odolá záplavám |
| Elektrická ohrada | Army shop | 180 Kč | 45 | Po úkolu Seržanta; vyřadí ji záplava |
| Automatická věž | Army shop | 350 Kč | 80 | Po úkolu Seržanta; vyřadí ji záplava |
| Puška | Army shop | 120 Kč | 35 | |
| Brokovnice | Army shop | 240 Kč | 70 | |

* **Hlídací psi** brání farmu jen tehdy, když jsou nakrmení. Příkaz `nakrm_psy` (na Poli) spotřebuje 1× krmení a psi jsou sytí **5 dní**. Plně nasycené psy nelze krmit znovu, aby se krmení neplýtvalo.
* Výpis nočního nájezdu ukazuje, kolik poškození způsobili psi, věže a zbraně a které zombíky obrana zlikvidovala.
* **Prolomení obrany:** Pokud zombíci obranu prorazí, hráč se v noci nevyspí (nezíská +50 EN) a zombíci **zničí veškerou úrodu na poli**.

### 6. Postavy (NPC), úkoly a cíl hry (`assets`)
Ve světě se nachází **7 NPC postav** (`npcs.json`), které zadávají úkoly za peníze, předměty nebo trvalé herní efekty (`QuestEffect`). Hru lze vyhrát **dvěma způsoby**:
1. **Únik do bezpečí (`KONEC_UNIK`):** Splnění úkolu *Jízdenka do bezpečí* pro **Pašeráka Viktora** ve Městě (1 000 Kč + zásoby jídla).
2. **Vývoj vakcíny (`KONEC_LEK`):** Dokončení třífázové úkolové linie *Projekt Vakcína I–III* pro **Doktorku Elenu** na Městském trhu (vyvrcholí vypěstováním geneticky čistého melounu).

| Postava | Lokace | Úkol | Odměna |
| :--- | :--- | :--- | :--- |
| Kutil Bohouš | Místní trh | 1× Polévka nebo Koláč | Kropítko zdarma |
| Mechanik Roman | Autobazar | 2× Dýně | Dodávka za 220 Kč + motorový tuning |
| Seržant Král | Army shop | 3× Mrkev, 3× Špenát, 2× Chléb | Elektrická ohrada a věže + sleva 20 % na zbraně |
| Pašerák Viktor | Město | 1 000 Kč, 3× Uzené maso, 3× Guláš, 5× Brambory | **Vítězství – Únik** |
| Doktorka Elena | Městský trh | I: 2× Cibule, 2× Česnek → II: 2× Jahody, 2× Jablka → III: 1× Meloun | 50 Kč → 100 Kč → **Vítězství – Lék** |
| Trhovec Jarda, Městský obchodník | trhy | – | (bez úkolu) |

### 7. Prohra (Game Over)
* Hra končí prohrou, pokud hráč po skončení noci **zkrachuje**: nemá peníze ani na nejlevnější semínko, má prázdný inventář a na poli mu nic neroste (typicky poté, co zombíci zničí úrodu).
* Po konci hry (výhře i prohře) lze už jen načíst uloženou hru (`nacti`) nebo hru ukončit (`konec`).

### 8. Ukládání a načítání hry (`save`)
* Rozehranou hru lze kdykoli uložit (`uloz`) a později načíst (`nacti`), i po vypnutí aplikace.
* Ukládá se kompletní stav: den, lokalita, energie, peníze, vozidlo, inventář, stav všech záhonků, kropítka, počasí, krmení psů a stav všech úkolů NPC.
* Hry se ukládají jako čitelné JSON soubory do složky `saves/` (např. `saves/hra.json`).
* Ukládání je oddělené rozhraním `GameStorage` – aktuální implementace `JsonGameStorage` používá Gson, jiný formát (CSV, databáze) lze doplnit bez změny herní logiky.
* Před načtením se soubor **validuje** (verze, lokalita, vozidlo, počasí, předměty, plodiny, počet záhonků). Poškozený nebo ručně upravený soubor hru nepřepíše, hráč dostane popis chyby.

---

## Struktura projektu

```text
semestr-game/
├── pom.xml                      # Maven: závislosti (Gson, JUnit 5), sestavení a spouštění
├── saves/                       # Uložené hry (vznikne při prvním uložení, není v gitu)
├── test/                        # Unit testy (JUnit 5)
│   ├── assets/NpcManagerTest.java
│   ├── farming/FieldManagerTest.java
│   ├── farming/PlotTest.java
│   ├── game/DogFeedingTest.java
│   ├── game/GameOverTest.java
│   ├── game/SaveLoadTest.java
│   ├── inventory/InventoryTest.java
│   ├── inventory/ShopManagerTest.java
│   ├── map/MovementTest.java
│   ├── save/JsonGameStorageTest.java
│   ├── zombies/DefenseStatusTest.java
│   └── zombies/ZombieManagerTest.java
└── src/
    ├── Main.java                # Vstupní bod aplikace (spuštění GUI)
    ├── assets/                  # NPC systém, úkoly, požadavky a herní efekty
    │   ├── GameContext.java     # Rozhraní mezi NPC/úkoly a hrou
    │   ├── Npc.java
    │   ├── NpcManager.java
    │   ├── Quest.java
    │   ├── QuestEffect.java
    │   ├── QuestState.java
    │   └── Requirement.java
    ├── data/                    # Konfigurační JSON soubory hry
    │   ├── crops.json           # Definice plodin, doby růstu a frekvence zálivky
    │   ├── items.json           # Katalog předmětů, cen, síly a dostupnosti v lokacích
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
    ├── save/                    # Ukládání a načítání rozehrané hry
    │   ├── GameState.java       # Snímek stavu hry (record)
    │   ├── GameStorage.java     # Rozhraní úložiště
    │   └── JsonGameStorage.java # Implementace ukládání do JSON (Gson)
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

---

## Standardní herní příkazy

Příkazy se zadávají v CLI režimu (nebo do textového pole v GUI), malými písmeny a bez diakritiky.

### 1. Pohyb a lokace
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `jdi` | `go`, `move` | `<id_lokace>` | Přesun do sousední lokace. | `jdi field`, `jdi local_market` |

*Dostupné lokace:* `home` (Dům), `field` (Pole), `local_market` (Místní trh), `city` (Město), `autobazar` (Autobazar), `army_shop` (Army shop), `city_market` (Městský trh).

---

### 2. Správa farmy a pole
Příkazy pro farmaření fungují na lokalitě `field` (Pole).

| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `pole` | `zahony`, `fieldstatus` | *žádný* | Zobrazí stav všech 6 záhonků (plodina, fáze růstu, stav zálivky). | `pole` |
| `zasad` | `plant` | `<id_seminka>` | Zasadí vybrané semínko na první volný záhonek. | `zasad seminko_obili` |
| `obstarej`| `zali`, `water` | *žádný* | Zalije všechny zasazené záhonky (spotřebovává EN). | `obstarej` |
| `sklid` | `harvest` | *žádný* | Sklidí zralou úrodu do inventáře a vytrhá uschlé plodiny. | `sklid` |
| `nakrm_psy` | *žádný* | *žádný* | Spotřebuje 1× krmení pro psy a nakrmí všechny hlídací psy na 5 dní. | `nakrm_psy` |

---

### 3. Obchod a inventář
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `inventar` | `inv` | *žádný* | Vypíše obsah batohu seřazený podle abecedy a jeho celkovou hodnotu. | `inventar` |
| `obchod` | `nabidka` | *žádný* | Zobrazí zboží dostupné ke koupi v aktuální lokalitě (včetně slev a zamčeného zboží). | `obchod` |
| `kup` | `buy` | `<id_itemu> [počet]` | Nakoupí předmět z obchodu. Pokud počet chybí, nakoupí 1 ks. | `kup kropitko`, `kup seminko_mrkev 5` |
| `prodej` | `sell` | `<id_itemu> [počet]` | Prodá předmět na trhu (plodiny za 100 % na Místním trhu, 200 % na Městském trhu). | `prodej mrkev 10` |

---

### 4. Postavy (NPC) a úkoly
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `mluv` | `talk` | `<id_npc>` | Zahájí rozhovor s NPC postavou v aktuální lokalitě a přijme její úkol. | `mluv bohous`, `mluv elena` |
| `ukol` | *žádný* | `<id_npc>` | Pokusí se odevzdat splněný úkol vybrané postavě. | `ukol bohous` |

*Přehled NPC postav:* `bohous` (Kutil Bohouš), `roman` (Mechanik Roman), `kral` (Seržant Král), `jarda` (Trhovec Jarda), `obchodnik` (Městský obchodník), `viktor` (Pašerák Viktor), `elena` (Doktorka Elena).

---

### 5. Odpočinek a stav
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `stav` | `status` | *žádný* | Zobrazí aktuální den, počasí, EN, peníze, vozidlo a obranu farmy. | `stav` |
| `spi` | `sleep` | *žádný* | Ukončí den (lze použít pouze v `home`). Obnoví +50 EN a vyhodnotí noční události. | `spi` |
| `jez` | `eat` | `<id_jidla>` | Sní jídlo z inventáře a doplní energii (max. 100 EN). | `jez chleb`, `jez gulas` |
| `napoveda` | `help`, `pomoc` | *žádný* | Vypíše zkrácenou nápovědu příkazů přímo do konzole. | `napoveda` |
| `konec` | `exit`, `quit` | *žádný* | Ukončí aplikaci. | `konec` |

---

### 6. Ukládání a načítání hry
| Příkaz | Aliasy | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- | :--- |
| `uloz` | `save` | `[název]` | Uloží rozehranou hru. Bez názvu se uloží jako `hra`. Název smí obsahovat malá písmena, čísla, `_` a `-`. | `uloz`, `uloz pokus1` |
| `nacti` | `load` | `[název]` | Načte uloženou hru. Bez názvu načte `hra`. Funguje i po konci hry. | `nacti`, `nacti pokus1` |
| `ulozene` | `saves` | *žádný* | Vypíše seznam všech uložených her. | `ulozene` |

---

## Vývojářské příkazy (DevTools)

Tyto příkazy slouží pro rychlé testování mechanik, odlaďování úkolů a simulaci situací.

| Příkaz | Parametry | Popis | Příklad |
| :--- | :--- | :--- | :--- |
| `devhelp` | *žádný* | Vypíše přehled všech vývojářských příkazů. | `devhelp` |
| `tp` | `<id_lokace>` | Okamžitý teleport do libovolné lokace bez omezení cestování. | `tp city_market` |
| `give` | `<id_itemu> [počet]` | Přidá vybraný předmět přímo do inventáře. | `give puska 1`, `give chleb 5` |
| `money` | `<částka>` | Přidá zadanou částku peněz (akceptuje i záporné hodnoty). | `money 1000` |
| `energy` | `<částka>` | Přidá nebo doplní zadané množství energie. | `energy 100` |
| `setday` | `<den>` | Nastaví Herní Den na konkrétní číslo. | `setday 10` |
| `setweather` | `<slunecno\|sucho\|zaplava>` | Změní aktuální stav počasí. | `setweather sucho` |
| `setvehicle` | `<bike\|car\|van>` | Nastaví úroveň dopravního prostředku hráče. | `setvehicle van` |
| `growall` | `dozraj` | Okamžitě zrychlí růst všech plodin na záhoncích do finální fáze. | `growall` |
| `raid` | *žádný* | Okamžitě vyvolá noční útok zombíků pro otestování obrany. | `raid` |
| `items` | *žádný* | Zobrazí kompletní seznam všech ID předmětů načtených v katalogu. | `items` |

---

## Testy

Unit testy (JUnit 5) se spouští příkazem `mvn test` nebo v IntelliJ IDEA (pravý klik na složku `test` → *Run 'All Tests'*). Projekt obsahuje **67 testů ve 12 testovacích třídách**.

| Testovací třída | Co ověřuje |
| :--- | :--- |
| `InventoryTest` | Přidávání a odebírání předmětů, neexistující předměty. |
| `ShopManagerTest` | Nákup a prodej, nedostatek peněz, výkup 2× na Městském trhu, zamčené zboží a slevy z úkolů, nákup vozidel. |
| `NpcManagerTest` | Aktivace a odevzdání úkolu, kontrola požadavků, odměny, požadavky typu „nebo“, uložení stavů úkolů. |
| `FieldManagerTest` | Sázení, odemčení ovoce, spotřeba energie při zalévání, sucho, kropítka. |
| `PlotTest` | Růst plodin, zalévání obden / každý den, usychání, sklizeň. |
| `MovementTest` | Cestování mezi lokalitami, požadované vozidlo, spotřeba energie, motorový tuning. |
| `DefenseStatusTest` | Síla obrany z `items.json`, krmení psů, vliv záplav. |
| `ZombieManagerTest` | Nájezdy až od 10. dne, nakrmení psi zabíjejí zombíky, silná obrana zastaví útok. |
| `DogFeedingTest` | Příkaz `nakrm_psy`: spotřeba krmení, zákaz krmení sytých psů, krmení jen na Poli. |
| `GameOverTest` | Zničení úrody při prolomení obrany, prohra bankrotem, načtení hry po prohře. |
| `JsonGameStorageTest` | Uložení a načtení souboru, chybějící a poškozený soubor, neplatný název. |
| `SaveLoadTest` | Uložení celé hry a její obnovení v nové instanci, odmítnutí neplatného uložení. |
