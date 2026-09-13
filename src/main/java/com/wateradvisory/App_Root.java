package com.wateradvisory;

import java.util.Map;

import com.wateradvisory.Charlie_Root.NavShell;
import com.wateradvisory.database.AuthService;
import com.wateradvisory.database.HouseholdService;
import com.wateradvisory.database.WaterRecordService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.text.Text;

public class App_Root {

    @FXML
    private Text usernameText;

    @FXML
    private Text householdText;

    @FXML
    private Text waterTotalText;

    @FXML
    private Label todayValueText;

    @FXML
    private Label weekValueText;

    @FXML
    private void initialize() {

        String username = AuthService.getUsername();

        usernameText.setText("Welcome back, " + username);
        householdText.setText(householdSubtitle());

        double waterTotal = WaterRecordService.getUserTotalWater();

        waterTotalText.setText(waterTotal + " L");

        Map<String, Double> summary =
                WaterRecordService.getUserWaterSummary();

        todayValueText.setText(summary.getOrDefault("today", 0.0) + " L");
        weekValueText.setText(summary.getOrDefault("week", 0.0) + " L");
    }

    // "{Household name} · {N} members", or a no-household placeholder.
    private String householdSubtitle() {

        if (!HouseholdService.hasHousehold()) {
            return "No household yet";
        }

        String householdName = HouseholdService.getHouseholdName();
        int memberCount = HouseholdService.getHouseholdMembers().size();

        return householdName + " · " + memberCount
                + (memberCount == 1 ? " member" : " members");
    }

    @FXML
    private void handleRecordWater(ActionEvent event) {
        NavShell.go(event, NavShell.Route.RECORD_WATER);
    }

    @FXML
    private void handleMyData(ActionEvent event) {
        NavShell.go(event, NavShell.Route.DAILY);
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