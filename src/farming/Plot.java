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

    public void advanceDay(boolean isDrought) {
        if (isEmpty() || isWithered) return;

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