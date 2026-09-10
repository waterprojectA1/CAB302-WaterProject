package com.wateradvisory.database;

public class WaterConsumptionService {

    // Shower Litre per minute modifiers
    private static final double SHOWER_LITRES_PER_MINUTE = 9.0;

    // Temporary estimated rates.
    // We can replace these later with researched values.
    private static final double DISHES_LITRES_PER_MINUTE = 6.0;
    private static final double FLOOR_CLEANING_LITRES_PER_MINUTE = 3.0;
    private static final double LAUNDRY_LITRES_PER_MINUTE = 4.0;
    private static final double CAR_WASH_LITRES_PER_MINUTE = 8.0;
    private static final double WINDOW_CLEANING_LITRES_PER_MINUTE = 2.0;
    private static final double BATHTUB_LITRES_PER_MINUTE = 10.0;

    public static double calculateShower(
            int durationMinutes,
            int amount
    ) {

        return durationMinutes
                * amount
                * SHOWER_LITRES_PER_MINUTE;
    }

    public static double calculateActivity(
            String activity,
            int durationMinutes,
            int amount
    ) {

        double rate;

        switch (activity) {

            case "Shower":
                rate = SHOWER_LITRES_PER_MINUTE;
                break;

            case "Dishes":
                rate = DISHES_LITRES_PER_MINUTE;
                break;

            case "Floor Cleaning":
                rate = FLOOR_CLEANING_LITRES_PER_MINUTE;
                break;

            case "Laundry":
                rate = LAUNDRY_LITRES_PER_MINUTE;
                break;

            case "Car Wash":
                rate = CAR_WASH_LITRES_PER_MINUTE;
                break;

            case "Window Cleaning":
                rate = WINDOW_CLEANING_LITRES_PER_MINUTE;
                break;

            case "Bathtub":
                rate = BATHTUB_LITRES_PER_MINUTE;
                break;

            default:
                return 0;
        }

        return durationMinutes * amount * rate;
    }


}