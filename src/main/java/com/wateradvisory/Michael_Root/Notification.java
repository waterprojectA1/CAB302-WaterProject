package com.wateradvisory.Michael_Root;

import java.time.LocalDate;

public class Notification {
    WaterData waterObject;
    LocalDate dateCreated;
    int id;
    double zScore;

    Notification(WaterData waterObject, int id, double zScore){
        this.waterObject = waterObject;
        this.dateCreated = LocalDate.now();
        this.id = id;
        this.zScore = zScore;
    }

    public double getzScore() {
        return zScore;
    }

    public int getId() {
        return id;
    }

    public LocalDate getDateCreated() {
        return dateCreated;
    }

    public WaterData getWaterObject() {
        return waterObject;
    }
}
