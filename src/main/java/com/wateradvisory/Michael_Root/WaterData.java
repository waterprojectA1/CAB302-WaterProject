package com.wateradvisory.Michael_Root;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class WaterData {


    private int id;
    private int userID;
    private double waterUsage;
    private LocalDate startDate;
    private LocalDate endDate;
    private String timeSpan;
    private String usageRating;
    private double watercost = 0.008;

    public WaterData(int id, double waterUsage, String startDate, String endDate, String timeSpan, int userID){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        this.id = id;
        this.waterUsage = waterUsage;
        this.startDate = LocalDate.parse(startDate, formatter);
        this.endDate = LocalDate.parse(endDate, formatter);
        this.timeSpan = timeSpan;
        this.userID = userID;
    }

    public void setUsageRating(String usageRating){
        this.usageRating = usageRating;
    }
    public String getUsageRating(){
        return usageRating;
    }

    public double getWaterUsage(){
        return waterUsage;
    }

    public LocalDate getDate1(){
        return startDate;
    }
    public LocalDate getDate2(){ return endDate;}

    public String getTimespan(){
        return timeSpan;
    }

    public int getUserID(){
        return userID;
    }

    public double getWatercost(){
        return watercost * waterUsage;
    }


}