package farming;

public class Crop {

    private final String id;
    private final String seedItemId;
    private final String cropItemId;
    private final CropCategory category;
    private final int growthDays;
    private final WateringFrequency wateringFrequency;

    public Crop(String id, String seedItemId, String cropItemId, CropCategory category, int growthDays, WateringFrequency wateringFrequency) {
        this.id = id;
        this.seedItemId = seedItemId;
        this.cropItemId = cropItemId;
        this.category = category;
        this.growthDays = growthDays;
        this.wateringFrequency = wateringFrequency;
    }

    public String getId() { return id; }

    public String getSeedItemId() { return seedItemId; }

    public String getCropItemId() { return cropItemId; }

    public CropCategory getCategory() { return category; }

    public int getGrowthDays() { return growthDays; }

    public WateringFrequency getWateringFrequency() { return wateringFrequency; }
}