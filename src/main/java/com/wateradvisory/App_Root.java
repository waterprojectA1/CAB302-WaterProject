package com.wateradvisory;

import com.wateradvisory.database.AuthService;
import com.wateradvisory.database.HouseholdService;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Text;

import java.io.IOException;

public class App_Root {

    @FXML
    private Text usernameText;

    @FXML
    private Text householdText;

    @FXML
    private void initialize() {

        String username = AuthService.getUsername();
        String householdName = HouseholdService.getHouseholdName();

        usernameText.setText("Welcome: " + username);
        householdText.setText(householdName);
    }

    @FXML
    private void handleRecordWater(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Arjay_FXML/recordwater.fxml")
            );

            Parent root = loader.load();

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

    @FXML
    private void handleViewHousehold(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/Arjay_FXML/householdview.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        // Jainya & Michael section
        System.out.println("Dashboard clicked");
    }

    @FXML
    private void handleChatBot(ActionEvent event) {
        // Charlie
        System.out.println("Chatbot clicked");
    }

    @FXML
    private void handleLeaderboard(ActionEvent event) {
        // Steve
        System.out.println("Leaderboard clicked");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Arjay
        AuthService.logout();

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Arjay_FXML/login.fxml")
            );

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}