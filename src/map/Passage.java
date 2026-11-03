package map;

public class Passage {
    private final Location destination;
    private final TransportType requiredTransport;
    private final int bikeEnergy;
    private final int carEnergy;
    private final int vanEnergy;

    public Passage(Location destination, TransportType requiredTransport, int bikeEnergy, int carEnergy, int vanEnergy) {
        this.destination = destination;
        this.requiredTransport = requiredTransport;
        this.bikeEnergy = bikeEnergy;
        this.carEnergy = carEnergy;
        this.vanEnergy = vanEnergy;
    }

    public Location getDestination() {
        return destination;
    }

    public TransportType getRequiredTransport() {
        return requiredTransport;
    }

    public int getEnergyCost(TransportType currentTransport, boolean hasEngineTuning) {
        int baseCost = switch (currentTransport) {
            case BIKE -> bikeEnergy;
            case CAR -> carEnergy;
            case VAN -> vanEnergy;
            default -> 0;
        };

        if (hasEngineTuning && (currentTransport == TransportType.CAR || currentTransport == TransportType.VAN)) {
            baseCost = Math.max(0, baseCost - 5);
        }

        return baseCost;
    }
}