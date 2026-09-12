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

    public static double calculateActivity(
            String activity,
            int durationMinutes,
            int amount
    ) {
        // test 1 & 2 fix solution code
        if (activity == null || durationMinutes <= 0 || amount <= 0) {
            return 0;
        }

        activity = activity.trim().toLowerCase().replaceAll("\\s+", " ");

        // Test 8 fix: only allow valid Shower duration options
        if (activity.equals("shower")
                && durationMinutes != 2
                && durationMinutes != 5
                && durationMinutes != 10
                && durationMinutes != 15
                && durationMinutes != 20) {

            return 0;
        }

        double rate;
        int maxAmount = 5;
        int maxDuration = 30;

        switch (activity) {

            case "shower":
                rate = SHOWER_LITRES_PER_MINUTE;
                maxDuration = 20;
                break;

            case "dishes":
                rate = DISHES_LITRES_PER_MINUTE;
                break;

            case "floor cleaning":
                rate = FLOOR_CLEANING_LITRES_PER_MINUTE;
                break;

            case "laundry":
                rate = LAUNDRY_LITRES_PER_MINUTE;
                maxDuration = 60;
                break;

            case "car wash":
                rate = CAR_WASH_LITRES_PER_MINUTE;
                maxAmount = 3;
                break;

            case "window cleaning":
                rate = WINDOW_CLEANING_LITRES_PER_MINUTE;
                break;

            case "bathtub":
                rate = BATHTUB_LITRES_PER_MINUTE;
                maxAmount = 3;
                maxDuration = 45;
                break;

            default:
                return 0;
        }

        // Check if amount is above limit return 0
        if (amount > maxAmount) {
            return 0;
        }

        // Check if duration is above limit return 0
        if (durationMinutes > maxDuration) {
            return 0;
        }

        return durationMinutes * amount * rate;
    }


}