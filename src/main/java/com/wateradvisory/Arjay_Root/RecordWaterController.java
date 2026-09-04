package com.wateradvisory.Arjay_Root;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class RecordWaterController {

    @FXML
    private void handleReturnToMain(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/App_Root-view.fxml"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleShowerSubmit(ActionEvent event) {
        System.out.println("shower submit clicked");
    }

    @FXML
    private void handleSubmitWater(ActionEvent event) {
        System.out.println("submit water clicked");
    }


}
