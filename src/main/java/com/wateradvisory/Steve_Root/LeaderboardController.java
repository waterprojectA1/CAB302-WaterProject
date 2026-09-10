package com.wateradvisory.Steve_Root;

import com.wateradvisory.database.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class LeaderboardController {

    @FXML
    private VBox leaderboardList;

    @FXML
    private Label messageLabel;

    @FXML
    private Label seasonLabel;

    @FXML
    private Label currentPointsLabel;

    @FXML
    private void initialize() {
        seasonLabel.setText(formatSeasonName(LeaderboardService.getCurrentSeason()));
        loadLeaderboard();
    }

    private void loadLeaderboard() {

        leaderboardList.getChildren().clear();

        List<LeaderboardEntry> entries = LeaderboardService.getHouseholdLeaderboard();

        if (entries.isEmpty()) {
            messageLabel.setText("No household leaderboard information was found.");
            messageLabel.setVisible(true);
            currentPointsLabel.setText("0 PTS");
            return;
        }

        messageLabel.setVisible(false);
        String currentUserId = UserSession.getUserId();

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            HBox row = createLeaderboardRow(i + 1, entry, currentUserId);
            leaderboardList.getChildren().add(row);

            if (entry.getUserId().equals(currentUserId)) {
                currentPointsLabel.setText(entry.getPoints() + " PTS");
            }
        }
    }

    private HBox createLeaderboardRow(int rank, LeaderboardEntry entry, String currentUserId) {

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(12);
        row.getStyleClass().add("leaderboard-row");

        if (entry.getUserId().equals(currentUserId)) {
            row.getStyleClass().add("current-user-row");
        }

        Label rankLabel = new Label(rank + ".");
        rankLabel.getStyleClass().add("rank-label");

        String displayName = entry.getUsername();

        if (entry.getUserId().equals(currentUserId)) {
            displayName = displayName + " (You)";
        }

        Label usernameLabel = new Label(displayName);
        usernameLabel.getStyleClass().add("username-label");

        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);

        Label pointsLabel = new Label(entry.getPoints() + " PTS");
        pointsLabel.getStyleClass().add("points-label");

        row.getChildren().add(rankLabel);
        row.getChildren().add(usernameLabel);
        row.getChildren().add(space);
        row.getChildren().add(pointsLabel);

        return row;
    }

    private String formatSeasonName(String season) {

        if (season == null || season.isEmpty()) {
            return "Current season";
        }

        String firstLetter = season.substring(0, 1).toUpperCase();
        return firstLetter + season.substring(1);
    }

    @FXML
    private void handleGoHome(ActionEvent event) {

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/App_Root-view.fxml")
            );

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
