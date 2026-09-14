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
    private Text todayTotalText;

    @FXML
    private Text weekTotalText;

    @FXML
    private void initialize() {

        String username = AuthService.getUsername();

        usernameText.setText("Welcome back, " + username);

        if (HouseholdService.hasHousehold()) {
            String householdName = HouseholdService.getHouseholdName();
            int memberCount = HouseholdService.getHouseholdMembers().size();
            householdText.setText(householdName + "  \u00B7  " + memberCount
                    + (memberCount == 1 ? " member" : " members"));
        } else {
            householdText.setText("No household yet");
        }

        double waterTotal = WaterRecordService.getUserTotalWater();

        waterTotalText.setText("" + waterTotal + " L");

        Map<String, Double> summary =
                WaterRecordService.getUserWaterSummary();

        todayTotalText.setText(summary.getOrDefault("today", 0.0) + " L");
        weekTotalText.setText(summary.getOrDefault("week", 0.0) + " L");
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

}