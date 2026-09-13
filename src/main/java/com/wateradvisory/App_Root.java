package com.wateradvisory;

import java.util.Map;

import com.wateradvisory.Charlie_Root.NavShell;
import com.wateradvisory.database.AuthService;
import com.wateradvisory.database.HouseholdService;
import com.wateradvisory.database.WaterRecordService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class App_Root {

    @FXML
    private Text usernameText;

    @FXML
    private Text householdText;

    @FXML
    private Text waterTotalText;

    @FXML
    private void initialize() {

        String username = AuthService.getUsername();
        String householdName = HouseholdService.getHouseholdName();

        usernameText.setText("Welcome: " + username);
        householdText.setText(householdName);

        double waterTotal = WaterRecordService.getUserTotalWater();

        waterTotalText.setText("Total Water Recorded: " + waterTotal + " L");

        Map<String, Double> summary =
                WaterRecordService.getUserWaterSummary();

        System.out.println(summary);
    }

    @FXML
    private void handleRecordWater(ActionEvent event) {
        NavShell.go(event, NavShell.Route.RECORD_WATER);
    }

    @FXML
    private void handleViewHousehold(ActionEvent event) {
        NavShell.go(event, NavShell.Route.HOUSEHOLD);
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        NavShell.go(event, NavShell.Route.PROFILE);
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        NavShell.go(event, NavShell.Route.DATA_TABLE);
    }

    @FXML
    private void handleConservationTips(ActionEvent event) {
        NavShell.go(event, NavShell.Route.TIPS);
        System.out.println("The user wants some conservation tips!");
    }

    @FXML
    private void handleLeaderboard(ActionEvent event) {
        NavShell.go(event, NavShell.Route.LEADERBOARD);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        NavShell.logout(event);
    }
}