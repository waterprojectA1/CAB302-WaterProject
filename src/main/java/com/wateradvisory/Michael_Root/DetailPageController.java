package com.wateradvisory.Michael_Root;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

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

    @FXML
    private void handleReturnTable(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Michael_FXML/TableDisplayPage.fxml"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

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
