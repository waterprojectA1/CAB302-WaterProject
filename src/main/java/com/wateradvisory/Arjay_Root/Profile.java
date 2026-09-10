package com.wateradvisory.Arjay_Root;

import com.wateradvisory.database.AuthService;
import com.wateradvisory.database.WaterRecordService;
import java.util.Map;
import javafx.scene.text.Text;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Profile {

    @FXML
    private Text usernameText;

    @FXML
    private Text emailText;

    @FXML
    private Text dailyTotalText;

    @FXML
    private Text weeklyTotalText;

    @FXML
    private Text allTimeTotalText;

    @FXML
    private void initialize() {

        String username = AuthService.getUsername();
        String email = AuthService.getUserEmail();

        usernameText.setText(
                "Username: " + username
        );

        emailText.setText(
                "Email: " + email
        );

        Map<String, Double> summary =
                WaterRecordService.getUserWaterSummary();

        double daily =
                summary.getOrDefault("today", 0.0);

        double weekly =
                summary.getOrDefault("week", 0.0);

        double allTime =
                summary.getOrDefault("allTime", 0.0);

        dailyTotalText.setText(
                "Daily Total: " + daily + " L"
        );

        weeklyTotalText.setText(
                "Weekly Total: " + weekly + " L"
        );

        allTimeTotalText.setText(
                "All Time Total: " + allTime + " L"
        );
    }

    @FXML
    private void handleReturnToMain(ActionEvent event) {

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/App_Root-view.fxml")
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));

            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
