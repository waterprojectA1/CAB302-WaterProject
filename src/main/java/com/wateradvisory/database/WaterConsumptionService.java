package com.wateradvisory.database;

public class WaterConsumptionService {

    // Shower Litre per minute modifiers
    private static final double SHOWER_LITRES_PER_MINUTE = 9.0;

    public static double calculateShower(
            int durationMinutes,
            int amount
    ) {

        return durationMinutes
                * amount
                * SHOWER_LITRES_PER_MINUTE;
    }
}