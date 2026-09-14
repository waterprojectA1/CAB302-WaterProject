package com.wateradvisory.Steve_Root;

import com.wateradvisory.database.UserSession;
import com.wateradvisory.Charlie_Root.NavShell;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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
    private Label profileSymbolLabel;

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
                if (!entry.getUsername().isEmpty()) {
                    profileSymbolLabel.setText(entry.getUsername().substring(0, 1).toUpperCase());
                }
            }
        }
    }

    private HBox createLeaderboardRow(int rank, LeaderboardEntry entry, String currentUserId) {

        boolean isCurrentUser = entry.getUserId().equals(currentUserId);
        boolean isTopRank = rank == 1;

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(12);
        row.getStyleClass().add(isTopRank ? "leaderboard-row-top" : "leaderboard-row");

        if (isCurrentUser && !isTopRank) {
            row.getStyleClass().add("current-user-row");
        }

        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.getStyleClass().add("rank-label");
        rankLabel.setMinWidth(20);

        String initials = entry.getUsername().isEmpty()
                ? "?" : entry.getUsername().substring(0, 1).toUpperCase();
        Label avatar = new Label(initials);
        avatar.getStyleClass().addAll("avatar-circle", "avatar-tint", "avatar-md");

        Label usernameLabel = new Label(entry.getUsername());
        usernameLabel.getStyleClass().add("username-label");

        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);

        Label pointsLabel = new Label(entry.getPoints() + " PTS");
        pointsLabel.getStyleClass().add("points-label");

        row.getChildren().add(rankLabel);
        row.getChildren().add(avatar);
        row.getChildren().add(usernameLabel);

        if (isCurrentUser) {
            Label youPill = new Label("You");
            youPill.getStyleClass().add("pill-you");
            row.getChildren().add(youPill);
        }

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
        NavShell.go(event, NavShell.Route.HOME);
    }
}
