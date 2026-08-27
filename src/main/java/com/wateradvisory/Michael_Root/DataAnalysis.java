package com.wateradvisory.Michael_Root;

import java.util.ArrayList;

public class DataAnalysis {
    //determine whether i'm analysing on data creation or called on a specific points
    //should store outliers in a table with needed data for persistence
    //need single water data to analysis
    //need user general water data
    //need a way to pull
    //need global data - static data from that 2010 water usage csv
    //IF user give use of items that use water, attempts to analyse item usage to --
    // determine reason for high usage/outliers -- might need seperate class for that
    //will need to function for dailies, weekly, monthly

    Integer userID; //do you really need a note for this?
    Integer waterID;
    Integer CAPITA = 4080000; //population of melbourne 2010

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public void setWaterID(Integer waterID) {
        this.waterID = waterID;
    }

    ArrayList<Integer> testWater = new ArrayList<Integer>(); //for testing

    private Integer globalPerCapita(ArrayList<Integer> water){
        return null;
    }// the ArrayList will probaly change depending on how i can grab the static data

    private double getStandDev(ArrayList<Integer> water) {
        double sum = 0;
        for (int num : water){
            sum += num;
        }
        double mean = sum / water.size();

        double squareSum = 0;
        for (int num : water){
            squareSum += Math.pow(num - mean, 2);
        }

        return Math.sqrt(squareSum / water.size());
        //math for standard deviation. will change to accommodate database access
    }

    private String getzscore(Integer water){
        int zscore = 0;
        double standeviation = getStandDev(testWater);
        for(int i = 1; i < 4; i++){
            if(water < standeviation * i){
              zscore = i;
              break;
            }
            else{
              zscore = 5;
            }
        }
        String zScorestring = "error";
        if (zscore == 1){
            zScorestring = "normal";
        } else if (zscore == 2) {
            zScorestring = "high";
        } else if (zscore == 3) {
            zScorestring = "extreme";
        }
        else {zScorestring = "outlier";}


        return zScorestring;
    }

    private Integer percDiffGlobal(ArrayList<Integer> water, int userwater){
        double mean = 0;
        for (int num : water){
            mean += num;
        }
        mean = mean / water.size();

        double diff = mean - userwater;
        double percentDiff = (diff / mean) * 100;

        return null;
    }

    private Integer percDiffUser(){
        //will get the percent difference from the user avg
        return null;
    }

}
