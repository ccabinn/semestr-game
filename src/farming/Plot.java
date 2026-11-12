package farming;

public class Plot {

    private final int index;
    private Crop crop;
    private int growthDaysCurrent = 0;
    private boolean wateredToday = false;
    private boolean wateredYesterday = false;
    private boolean isWithered = false;

    public Plot(int index) {
        this.index = index;
    }

    public boolean isEmpty() {
        return crop == null;
    }

    public boolean plant(Crop crop) {
        if (!isEmpty()) return false;
        this.crop = crop;
        this.growthDaysCurrent = 0;
        this.wateredToday = false;
        this.wateredYesterday = false;
        this.isWithered = false;
        return true;
    }

    public void water() {
        if (!isEmpty() && !isWithered) {
            this.wateredToday = true;
        }
    }

    public void advanceDay(boolean isDrought, boolean isFlooded) {
        if (isEmpty() || isWithered) return;

        // pri zaplave se automaticky zalije
        if (isFlooded) {
            wateredToday = true;
        }

        // pri suchu je potreba zalevat kazdy den vse
        boolean requiresCare = isDrought || crop.getWateringFrequency() == WateringFrequency.EVERY_DAY || !wateredYesterday;

        if (requiresCare && !wateredToday) {
            isWithered = true; // pokud nebylo zalito, uschne
            return;
        }

        growthDaysCurrent++;
        wateredYesterday = wateredToday;
        wateredToday = false;
    }

    // vraci true pokud je pripraveno pro sklizeni
    public boolean isMature() {
        return !isEmpty() && !isWithered && growthDaysCurrent >= crop.getGrowthDays();
    }

    public boolean requiresWateringToday(boolean isDrought, boolean isFlooded) {
        if (isEmpty() || isWithered || wateredToday) {
            return false;
        }

        // pri zaplave je zalito
        if (isFlooded) {
            return false;
        }

        // sucho nebo denni zalevani potrebuje kazdy den
        if (isDrought || crop.getWateringFrequency() == WateringFrequency.EVERY_DAY) {
            return true;
        }
        // pri zalevani obden je voda potreba pokud vcera nebylo zalito
        return !wateredYesterday;
    }

    public boolean isReadyToHarvest() {
        return isMature();
    }

    public int getDaysUntilHarvest() {
        if (crop == null || isWithered) return 0;
        return Math.max(0, crop.getGrowthDays() - growthDaysCurrent);
    }

    public boolean isWatered() {
        return wateredToday;
    }

    public Crop harvest() {
        if (!isMature()) return null;
        Crop harvested = crop;
        clear();
        return harvested;
    }

    public void clear() {
        this.crop = null;
        this.growthDaysCurrent = 0;
        this.wateredToday = false;
        this.wateredYesterday = false;
        this.isWithered = false;
    }

    public int getIndex() { return index; }

    public Crop getCrop() { return crop; }

    public int getGrowthDaysCurrent() { return growthDaysCurrent; }

    public boolean isWateredToday() { return wateredToday; }

    public boolean isWithered() { return isWithered; }
}