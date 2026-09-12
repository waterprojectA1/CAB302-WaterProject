package com.wateradvisory.Michael_Test;

import com.wateradvisory.Michael_Root.WaterData;
import com.wateradvisory.Michael_Root.WaterDataList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WaterDataListTest {
    private WaterDataList waterDataList;

    @BeforeEach
    void setUp() {
        waterDataList = new WaterDataList();
        // Replace the automatically loaded data with test data.
        ObservableList<WaterData> daily = FXCollections.observableArrayList(
                new WaterData(1, 100, "01-09-2026", "01-09-2026", "DAILY", "1"),
                new WaterData(2, 200, "02-09-2026", "02-09-2026", "DAILY", "1"),
                new WaterData(3, 500, "03-09-2026", "03-09-2026", "DAILY", "1"),
                new WaterData(5, 300, "03-09-2026", "03-09-2026", "DAILY", "1"),
                new WaterData(6, 30, "03-09-2026", "03-09-2026", "DAILY", "2"),
                new WaterData(8, 30, "03-09-2026", "03-09-2026", "DAILY", "2"),
                new WaterData(7, 30, "03-09-2026", "03-09-2026", "DAILY", "2"),
                new WaterData(4, 30, "03-09-2026", "03-09-2026", "DAILY", "2") );

        ObservableList<WaterData> weekly = FXCollections.observableArrayList(
                new WaterData(1, 1000, "01-09-2026", "07-09-2026", "WEEKLY", "1"),
                new WaterData(2, 2000, "08-09-2026", "14-09-2026", "WEEKLY", "1"),
                new WaterData(3, 3000, "15-09-2026", "21-09-2026", "WEEKLY", "1"),
                new WaterData(5, 5000, "15-09-2026", "21-09-2026", "WEEKLY", "1"),
                new WaterData(6, 300, "15-09-2026", "21-09-2026", "WEEKLY", "2"),
                new WaterData(8, 300, "15-09-2026", "21-09-2026", "WEEKLY", "2"),
                new WaterData(7, 300, "15-09-2026", "21-09-2026", "WEEKLY", "2"),
                new WaterData(4, 300, "15-09-2026", "21-09-2026", "WEEKLY", "2") );

        ObservableList<WaterData> monthly = FXCollections.observableArrayList(

                new WaterData(1, 10000, "01-09-2026", "30-09-2026", "MONTHLY", "1"),
                new WaterData(2, 20000, "01-10-2026", "31-10-2026", "MONTHLY", "1"),
                new WaterData(3, 30000, "01-11-2026", "30-11-2026", "MONTHLY", "1"),
                new WaterData(5, 50000, "01-11-2026", "30-11-2026", "MONTHLY", "1"),
                new WaterData(6, 3000, "01-11-2026", "30-11-2026", "MONTHLY", "2"),
                new WaterData(8, 3000, "01-11-2026", "30-11-2026", "MONTHLY", "2"),
                new WaterData(7, 3000, "01-11-2026", "30-11-2026", "MONTHLY", "2"),
                new WaterData(3, 3000, "01-11-2026", "30-11-2026", "MONTHLY", "2") );

        waterDataList.setDailyWater(daily);
        waterDataList.setWeeklyWater(weekly);
        waterDataList.setMonthlyWater(monthly); }
    //GETTER / SETTER TESTS //
    @Test
    void testGetDailyWater() {
        assertEquals(4, waterDataList.getDailyWater().size()); }

    @Test
    void testGetWeeklyWater() { assertEquals(4, waterDataList.getWeeklyWater().size()); }

    @Test
    void testGetMonthlyWater() { assertEquals(4, waterDataList.getMonthlyWater().size()); }


    //ADD TESTS //
    @Test
    void testAddDailyWater() {
        WaterData newData = new WaterData( 4, 400, "04-09-2026", "04-09-2026", "DAILY", "1" );
        int originalSize = waterDataList.getDailyWater().size();
        waterDataList.addDailyWater(newData);
        assertEquals(originalSize + 1, waterDataList.getDailyWater().size());
        assertTrue(waterDataList.getDailyWater().contains(newData)); }

    @Test
    void testAddWeeklyWater() {
        WaterData newData = new WaterData( 4, 4000, "22-09-2026", "28-09-2026", "WEEKLY", "1" );
        int originalSize = waterDataList.getWeeklyWater().size();
        waterDataList.addWeeklyWater(newData);
        assertEquals(originalSize + 1, waterDataList.getWeeklyWater().size());
        assertTrue(waterDataList.getWeeklyWater().contains(newData)); }

    @Test
    void testAddMonthlyWater() {
        WaterData newData = new WaterData( 4, 40000, "01-12-2026", "31-12-2026", "MONTHLY", "1" );
        int originalSize = waterDataList.getMonthlyWater().size();
        waterDataList.addMonthlyWater(newData);
        assertEquals(originalSize + 1, waterDataList.getMonthlyWater().size());
        assertTrue(waterDataList.getMonthlyWater().contains(newData)); }

    // DAILY MEAN//

    @Test
    void testGetDailyMean() {
        // Values: 100 + 200 + 500 + 300 + 30 + 30 + 30 + 30  = 1220 1220 / 8 = 152.5
         double result = waterDataList.getDailyMean();
         assertEquals(152.5, result, 0.001); }

    // WEEKLY MEAN//

    @Test
    void testGetWeeklyMean() {
        // Values: 1000 + 2000 + 3000 + 5000 + 300 + 300 + 300 + 300 = 12200 12200 / 8 = 1525
        double result = waterDataList.getWeeklyMean();
        assertEquals(1525, result, 0.001); }

    //MONTHLY MEAN//
    @Test
    void testGetMonthlyMean() {
        // Values: 10000 + 20000 + 30000 + 50000 + 3000 + 3000 + 3000 + 3000 = 122000 122000 / 8 = 15250
        double result = waterDataList.getMonthlyMean();
        assertEquals(15250.0, result, 0.001); }

    //USER DAILY MEAN//

    @Test
    void testGetUserDailyMean() {
        //User 1:  100 + 200 + 500 + 300 = 1100 1100 / 4 = 275
        double result = waterDataList.getUserDailyMean();
        assertEquals(275.0, result, 0.001); }
    //USER WEEKLY MEAN//

    @Test
    void testGetUserWeeklyMean() {
        //User 1: 1000 + 2000 + 3000 + 5000 = 11000 11000 / 4 = 2750
        double result = waterDataList.getUserWeeklyMean();
        assertEquals(2750.0, result, 0.001); }

    // USER MONTHLY MEAN //

    @Test
    void testGetUserMonthlyMean() {
        // User 1: 10000 + 20000 + 30000 + 50000 = 110000 110000 / 4 = 27500
        double result = waterDataList.getUserMonthlyMean();
        assertEquals(27500.0, result, 0.001); }

    // GLOBAL DIFFERENCE//

    @Test
    void testGetGlobalDiffDaily() {
        WaterData water = waterDataList.getDailyWater().get(2);
        /* Global mean = 152.5 Usage = 500
        ((500 - 152.5) / 152.5) 100 = 227.86885%
        Method rounds to 2 decimal places. Expected = 227.87 */
        double result = waterDataList.getGlobalDiff(water);
        assertEquals(227.87, result, 0.001); }

    @Test
    void testGetGlobalDiffWeekly() { WaterData water = waterDataList.getWeeklyWater().get(2);
        /*Global mean = 1525 Usage = 3000
        ((3000 - 1525) / 1525) 100 = 96.72131%
        Expected = 96.72 */
         double result = waterDataList.getGlobalDiff(water);
         assertEquals(96.72, result, 0.001); }

    @Test void testGetGlobalDiffMonthly() {
        WaterData water = waterDataList.getMonthlyWater().get(2);
        /*Global mean = 15250 Usage = 30000
        ((30000 - 15250) / 15250) 100 = 96.72131%
        Expected = 96.72 */
        double result = waterDataList.getGlobalDiff(water);
        assertEquals(96.72, result, 0.001); }

    // USER DIFFERENCE//

    @Test
    void testGetUserDiffDaily() {
        /*User mean = 275 Usage = 500
        ((500 - 275) / 275) 100 = 81.81818%
        Expected = 81.82 */
        WaterData water = waterDataList.getDailyWater().get(2);
        double result = waterDataList.getUserDiff(water);
        assertEquals(81.82, result, 0.001); }

    @Test
    void testGetUserDiffWeekly() {
        /*User mean = 2750 Usage = 3000
        ((3000 - 2750) / 2750) 100 = 9.0909%
        Expected = 9.09 */
        WaterData water = waterDataList.getWeeklyWater().get(2);
        double result = waterDataList.getUserDiff(water);
        assertEquals(9.09, result, 0.001); }

    @Test void testGetUserDiffMonthly() {
        /*User mean = 27500 Usage = 30000
        ((30000 - 27500) / 27500) 100 = 9.0909%
        Expected = 9.09 */
        WaterData water = waterDataList.getMonthlyWater().get(2);
        double result = waterDataList.getUserDiff(water);
        assertEquals(9.09, result, 0.001); }

    //STANDARD DEVIATION //

    @Test
    void testGetDailyStandardDeviation() {
        //Expected = 161.0706267
        double result = waterDataList.getDailyStandardDeviation();
        assertEquals(161.0706267, result, 0.001); }

    @Test void testGetWeeklyStandardDeviation() {
        //Expected = 1610.706267
        double result = waterDataList.getWeeklyStandardDeviation();
        assertEquals(1610.706267, result, 0.001); }

    @Test void testGetMonthlyStandardDeviation() {
        //Expected = 16107.06267
        double result = waterDataList.getMonthlyStandardDeviation();
        assertEquals(16107.06267, result, 0.001); }

    //Z-SCORE //
    @Test void testGetZScoreDaily() {
        WaterData water = waterDataList.getDailyWater().get(2);
        /*Usage = 500 Mean = 152.5
        Standard deviation = 161.070637
        Z = (500 - 152.5) / 161.070637 = 2.157...
        Rounded to 2 decimal places = 2.16 */
        double result = waterDataList.getZScore(water);
        assertEquals(2.16, result, 0.001); }

    @Test void testGetZScoreSetsExtremeRating() {
        WaterData water = waterDataList.getDailyWater().get(2);
        waterDataList.getZScore(water);
        assertEquals("Extreme", water.getUsageRating()); }

    @Test void testGetZScoreSetsNormalRating() {
        WaterData water = waterDataList.getDailyWater().get(3);
        waterDataList.getZScore(water);
        assertEquals("Normal", water.getUsageRating()); }

    // USER FILTERING //

    @Test void testUserDailyMeanIgnoresOtherUsers() {
        waterDataList.getDailyWater().add( new WaterData( 4, 1000, "04-09-2026", "04-09-2026", "DAILY", "2" ) );
        /* User 1: 100, 200, 500, 300
        User 2: 30, 30, 30, 30
        User 1 mean: (100 + 200 + 500 + 300) / 4  = 275 */
        double result = waterDataList.getUserDailyMean();
        assertEquals(275.0, result, 0.001); }

    //SETTER TESTS //
    @Test void testSetDailyWater() {
        ObservableList<WaterData> newList = FXCollections.observableArrayList();
        waterDataList.setDailyWater(newList);
        assertSame(newList, waterDataList.getDailyWater());}

    @Test void testSetWeeklyWater() {
        ObservableList<WaterData> newList = FXCollections.observableArrayList();
        waterDataList.setWeeklyWater(newList);
        assertSame(newList, waterDataList.getWeeklyWater()); }

    @Test void testSetMonthlyWater() {
        ObservableList<WaterData> newList = FXCollections.observableArrayList();
        waterDataList.setMonthlyWater(newList);
        assertSame(newList, waterDataList.getMonthlyWater());
    }

}

