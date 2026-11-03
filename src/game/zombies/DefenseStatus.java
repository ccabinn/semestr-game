package game.zombies;

public class DefenseStatus {
    private int guardDogs;
    private int dogFoodDaysLeft;
    private int woodenFences;
    private int electricFences;
    private int turrets;
    private boolean hasRifle;
    private boolean hasShotgun;

    public DefenseStatus(int guardDogs, int dogFoodDaysLeft, int woodenFences,
                         int electricFences, int turrets, boolean hasRifle, boolean hasShotgun) {
        this.guardDogs = guardDogs;
        this.dogFoodDaysLeft = dogFoodDaysLeft;
        this.woodenFences = woodenFences;
        this.electricFences = electricFences;
        this.turrets = turrets;
        this.hasRifle = hasRifle;
        this.hasShotgun = hasShotgun;
    }

    public int getEffectiveDogDamage() {
        if (guardDogs > 0 && dogFoodDaysLeft > 0) {
            return guardDogs * 15;
        }
        return 0;
    }

    public int getTurretDamage() {
        return turrets * 30; // Turret z Army shopu udělí 30 damage za noc
    }

    public int getPlayerWeaponDamage() {
        int damage = 0;
        if (hasShotgun) damage += 25;
        if (hasRifle) damage += 15;
        return damage;
    }

    public int getFenceDefenseAbsorption() {
        // Ploty absorbují útok dříve, než projde na pole
        return (woodenFences * 15) + (electricFences * 35);
    }

    public void advanceDay() {
        if (dogFoodDaysLeft > 0) {
            dogFoodDaysLeft--;
        }
    }

    // Gettery a settery pro nákupy a záplavy
    public int getGuardDogs() { return guardDogs; }
    public void addGuardDogs(int count) { this.guardDogs += count; }
    public void feedDogs() { this.dogFoodDaysLeft += 5; }
    public int getDogFoodDaysLeft() { return dogFoodDaysLeft; }

    public int getWoodenFences() { return woodenFences; }
    public void addWoodenFence(int count) { this.woodenFences += count; }

    public int getElectricFences() { return electricFences; }
    public void addElectricFence(int count) { this.electricFences += count; }
    public void destroyElectricFences() { this.electricFences = 0; } // Pro případ záplav

    public int getTurrets() { return turrets; }
    public void addTurret(int count) { this.turrets += count; }
    public void destroyTurrets() { this.turrets = 0; } // Pro případ záplav

    public void setHasRifle(boolean hasRifle) { this.hasRifle = hasRifle; }
    public void setHasShotgun(boolean hasShotgun) { this.hasShotgun = hasShotgun; }
}