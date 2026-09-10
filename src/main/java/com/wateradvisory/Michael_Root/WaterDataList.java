package com.wateradvisory.Michael_Root;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class WaterDataList {

    private ObservableList<WaterData> dailyWater = FXCollections.observableArrayList();

    private ObservableList<WaterData> weeklyWater = FXCollections.observableArrayList();

    private ObservableList<WaterData> monthlyWater = FXCollections.observableArrayList();

    private int loggedUser = 1;

    public WaterDataList(){
        dailyWater.add(new WaterData(1,  755, "02-09-2026", "02-09-2026", "DAILY", 1));
        dailyWater.add(new WaterData(2,  844, "03-09-2026", "03-09-2026", "DAILY", 1));
        dailyWater.add(new WaterData(3,  734,"04-09-2026", "04-09-2026", "DAILY" , 1));
        dailyWater.add(new WaterData(4,  765, "05-09-2026", "05-09-2026","DAILY" , 1));
        dailyWater.add(new WaterData(5,  688, "06-09-2026", "06-09-2026","DAILY" , 1));
        dailyWater.add(new WaterData(6,  889, "07-09-2026", "07-09-2026", "DAILY", 1));
        dailyWater.add(new WaterData(7,  714, "08-09-2026", "08-09-2026","DAILY", 1));
        dailyWater.add(new WaterData(8,  912, "08-09-2026", "08-09-2026","DAILY", 2));
        weeklyWater.add(new WaterData(9,  5379, "02-09-2026", "08-09-2026","WEEKLY", 1));
        weeklyWater.add(new WaterData(10,  5821, "09-09-2026", "15-09-2026","WEEKLY", 1));
        weeklyWater.add(new WaterData(11,  5058, "16-09-2026", "22-09-2026","WEEKLY", 1));
        weeklyWater.add(new WaterData(12,  5603, "16-09-2026", "22-09-2026","WEEKLY", 2));
        monthlyWater.add(new WaterData(13,  23647, "01-09-2026", "30-09-2026","MONTHLY", 1));
        monthlyWater.add(new WaterData(14,  20745, "01-10-2026", "31-10-2026","MONTHLY", 1));
        monthlyWater.add(new WaterData(15,  27315, "01-11-2026", "30-11-2026","MONTHLY", 1));
        monthlyWater.add(new WaterData(16,  26045, "01-11-2026", "30-11-2026","MONTHLY", 2));
    }


    public ObservableList<WaterData> getDailyWater() {
        return dailyWater;
    }

    public ObservableList<WaterData> getMonthlyWater() {
        return monthlyWater;
    }

    public ObservableList<WaterData> getWeeklyWater() {
        return weeklyWater;
    }

    public void addDailyWater(WaterData aqua) {
        dailyWater.add(aqua);
        if (getZScore(aqua) >= 2){
            // createNotfication(); - to be made
        }
    }
    public void addWeeklyWater(WaterData aqua){
        weeklyWater.add(aqua);
        if (getZScore(aqua) >= 2){
            // createNotfication(); - to be made
        }
    }
    public void addMonthlyWater(WaterData aqua){
        monthlyWater.add(aqua);
        if (getZScore(aqua) >= 2){
            // createNotfication(); - to be made
        }
    }

    public void setDailyWater(ObservableList<WaterData> dailyWater) {
        this.dailyWater = dailyWater;
    }

    public void setMonthlyWater(ObservableList<WaterData> monthlyWater) {
        this.monthlyWater = monthlyWater;
    }

    public void setWeeklyWater(ObservableList<WaterData> weeklyWater) {
        this.weeklyWater = weeklyWater;
    }

    public double getDailyMean(){
        double total = 0;
        for (WaterData aqua : dailyWater){
            total += aqua.getWaterUsage();
        }
        return total / dailyWater.size();
    }

    public double getUserDailyMean(){
        double total = 0;
        int count = 0;
        for (WaterData aqua : dailyWater){
            if(aqua.getUserID() == loggedUser) {
                total += aqua.getWaterUsage();
                count++;
            }
        }
        return total / count;
    }

    public double getWeeklyMean(){
        double total = 0;
        for (WaterData aqua : weeklyWater){
            total += aqua.getWaterUsage();

        }
        return total / weeklyWater.size();
    }

    public double getUserWeeklyMean(){
        double total = 0;
        int count = 0;
        for (WaterData aqua : weeklyWater){
            if(aqua.getUserID() == loggedUser) {
                total += aqua.getWaterUsage();
                count++;
            }
        }
        return total / count;
    }

    public double getMonthlyMean(){
        double total = 0;
        for (WaterData aqua : monthlyWater){
            total += aqua.getWaterUsage();
        }
        return total / monthlyWater.size();
    }

    public double getUserMonthlyMean(){
        double total = 0;
        int count = 0;
        for (WaterData aqua : monthlyWater){
            if(aqua.getUserID() == loggedUser) {
                total += aqua.getWaterUsage();
                count++;
            }
        }
        return total / count;
    }

    public double getGlobalDiff(WaterData water){
        double mean = 0;
        if(water.getTimespan().equals("DAILY")){
            mean = getDailyMean();
        }
        if(water.getTimespan().equals("WEEKLY")){
            mean = getWeeklyMean();
        }
        if(water.getTimespan().equals("MONTHLY")){
            mean = getMonthlyMean();
        }

        double percdiff = (water.getWaterUsage() - mean) / mean * 100;
        double roundedpercdiff = Math.round(percdiff * 100);
        roundedpercdiff = roundedpercdiff / 100;
        return roundedpercdiff;
    }

    public double getUserDiff(WaterData water){
        double mean = 0;
        if(water.getTimespan().equals("DAILY")){
            mean = getUserDailyMean();
        }
        if(water.getTimespan().equals("WEEKLY")){
            mean = getUserWeeklyMean();
        }
        if(water.getTimespan().equals("MONTHLY")){
            mean = getUserMonthlyMean();
        }

        double percdiff = (water.getWaterUsage() - mean) / mean * 100;
        double roundedpercdiff = Math.round(percdiff * 100);
        roundedpercdiff = roundedpercdiff / 100;
        return roundedpercdiff;
    }


    public double getDailyStandardDeviation() {

        double mean = getDailyMean();
        double total = 0;

        for (WaterData aqua : dailyWater) {
            total += Math.pow(aqua.getWaterUsage() - mean, 2);
        }

        return Math.sqrt(total / dailyWater.size());
    }
    double getWeeklyStandardDeviation() {

        double mean = getWeeklyMean();
        double total = 0;

        for (WaterData aqua : weeklyWater) {
            total += Math.pow(aqua.getWaterUsage() - mean, 2);
        }

        return Math.sqrt(total / weeklyWater.size());
    }
    public double getMonthlyStandardDeviation() {

        double mean = getMonthlyMean();
        double total = 0;

        for (WaterData aqua : monthlyWater) {
            total += Math.pow(aqua.getWaterUsage() - mean, 2);
        }

        return Math.sqrt(total / monthlyWater.size());
    }


    public double getZScore(WaterData water) {

        double mean = 0;
        double standardDeviation = 0;
        if(water.getTimespan().equals("DAILY")){
            mean = getDailyMean();
            standardDeviation = getDailyStandardDeviation();
        }
        if(water.getTimespan().equals("WEEKLY")){
            mean = getWeeklyMean();
            standardDeviation = getWeeklyStandardDeviation();
        }
        if(water.getTimespan().equals("MONTHLY")){
            mean = getMonthlyMean();
            standardDeviation = getMonthlyStandardDeviation();
        }

        double zscore = (water.getWaterUsage() - mean) / standardDeviation;
        double zScoreRounded = Math.round(zscore * 100);
        zScoreRounded = zScoreRounded / 100;
        if (zScoreRounded >= 1){
            water.setUsageRating("High");
        } else if (zScoreRounded >= 2) {
            water.setUsageRating("Extreme");

        } else if (zScoreRounded >= 3) {
            water.setUsageRating("Outlier");
        }
        else{
            water.setUsageRating("Normal");
        }
        return zScoreRounded;


    }
}
