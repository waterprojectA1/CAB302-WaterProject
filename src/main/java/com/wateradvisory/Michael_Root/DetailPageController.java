package com.wateradvisory.Michael_Root;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DetailPageController {

    @FXML
    private Label sDateLabel;
    @FXML
    private Label eDateLabel;
    @FXML
    private Label usageLabel;
    @FXML
    private Label zScoreLabel;
    @FXML
    private Label costLabel;
    @FXML
    private Label timeSpanLabel;

    @FXML
    private Label reportLabel;

    public void setWaterData(WaterData aqua){
        sDateLabel.setText("Start Date: " + aqua.getDate1());
        eDateLabel.setText("End Date: " + aqua.getDate2());
        timeSpanLabel.setText("Interval " + aqua.getTimespan());
        usageLabel.setText("Water Consumption: " + aqua.getWaterUsage());
        zScoreLabel.setText("Z-Score: " + aqua.getUsageRating());
        costLabel.setText("Cost of water: $" + aqua.getWatercost());

        reportLabel.setText("Put generated Report here");

    }

}
