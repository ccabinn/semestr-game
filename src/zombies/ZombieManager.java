package zombies;

import java.util.List;

public class ZombieManager {

    public NightRaidResult resolveNightRaid(int day, DefenseStatus defense, boolean isFlooded) {
        List<Zombie> wave = ZombieWaveFactory.createWaveForDay(day);
        StringBuilder log = new StringBuilder();

        if (wave.isEmpty()) {
            return new NightRaidResult(false, true, 0, 0, "Dnes v noci byl klid, žádní zombíci se neobjevili.");
        }

        log.append("=== NOČNÍ NÁJEZD ZOMBIES (Den ").append(day).append(") ===\n");
        log.append("Kolem pole se shromáždilo ").append(wave.size()).append(" zombíků!\n");

        if (isFlooded && (defense.getElectricFences() > 0 || defense.getTurrets() > 0)) {
            log.append("UPOZORNĚNÍ: Záplava zkratovala elektrická obranná zařízení (ohrada a věže jsou nefunkční)!\n");
        }

        if (defense.getGuardDogs() > 0 && defense.getDogFoodDaysLeft() == 0) {
            log.append("UPOZORNĚNÍ: Hlídací psi mají hlad (došlo krmení) a nebrání pole!\n");
        }

        // Výpočet poškození obrany (při záplavě věže nevystřelí)
        int turretDmg = isFlooded ? 0 : defense.getTurretDamage();
        int attackPool = turretDmg
                + defense.getPlayerWeaponDamage()
                + defense.getEffectiveDogDamage();

        int totalKills = 0;
        for (Zombie zombie : wave) {
            if (attackPool <= 0) break;

            int damageToApply = Math.min(attackPool, zombie.getHealth());
            zombie.takeDamage(damageToApply);
            attackPool -= damageToApply;

            if (!zombie.isAlive()) {
                totalKills++;
                log.append("Obrana zlikvidovala: ").append(zombie.getName()).append("\n");
            }
        }

        int incomingDamage = 0;
        int survivors = 0;
        for (Zombie zombie : wave) {
            if (zombie.isAlive()) {
                incomingDamage += zombie.getAttackPower();
                survivors++;
            }
        }

        // vypocet absorpce plotu
        int electricFenceAbs = isFlooded ? 0 : (defense.getElectricFences() * 35);
        int fenceAbsorption = (defense.getWoodenFences() * 15) + electricFenceAbs;

        int absorbedByFences = Math.min(incomingDamage, fenceAbsorption);
        int breachDamage = incomingDamage - absorbedByFences;

        defense.advanceDay();

        if (survivors == 0) {
            log.append("Všichni zombíci byli zneškodněni dříve, než se dostali k plotu.\n");
            log.append("Pole je v naprostém bezpečí!");
            return new NightRaidResult(true, true, totalKills, 0, log.toString());
        }

        log.append("Přežilo ").append(survivors).append(" zombíků s celkovou silou útoku ").append(incomingDamage).append(".\n");
        log.append("Ploty a zátarasy pohltily ").append(absorbedByFences).append(" bodů poškození.\n");

        if (breachDamage > 0) {
            log.append("POZOR: Zombíci prorazili zábrany a vtrhli na pole! Způsobili škodu na úrodě: ")
                    .append(breachDamage).append(".\n");
            log.append("Kvůli probíhajícímu boji se hráč nemohl vyspat doma a neobnovil energii spánkem!");
            return new NightRaidResult(true, false, totalKills, breachDamage, log.toString());
        } else {
            log.append("Ploty nápor zadržely. Žádná škoda na plodinách nevznikla.");
            return new NightRaidResult(true, true, totalKills, 0, log.toString());
        }
    }
}
