// Summary: Controls the user profile page, including displaying
// account information, water usage totals, and returning to the main application.
package com.wateradvisory.Arjay_Root;

// Project services used to retrieve account details and water usage information.
import com.wateradvisory.database.AuthService;
import com.wateradvisory.database.WaterRecordService;
import com.wateradvisory.Charlie_Root.NavShell;

// Java and JavaFX imports used for storing summary data,
// displaying text, handling events, and navigating between pages.
import java.util.Map;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class Profile {

    @FXML
    private Text usernameText;

    @FXML
    private Text emailText;

    @FXML
    private Label profileAvatarLabel;

    @FXML
    private Text dailyTotalText;

    @FXML
    private Text weeklyTotalText;

    @FXML
    private Text allTimeTotalText;

    // Loads the user's account details and water usage totals when the profile page opens.
    @FXML
    private void initialize() {

        String username = AuthService.getUsername();
        String email = AuthService.getUserEmail();

        if (username != null && !username.isBlank()) {
            profileAvatarLabel.setText(username.substring(0, 1).toUpperCase());
        }

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

    // Returns the user from the profile page to the main application screen.
    @FXML
    private void handleReturnToMain(ActionEvent event) {
        NavShell.go(event, NavShell.Route.HOME);
    }

    // Jumps from the profile page straight to the household screen.
    @FXML
    private void handleViewHousehold(ActionEvent event) {
        NavShell.go(event, NavShell.Route.HOUSEHOLD);
    }

    // Signs the user out. Logout now lives only on this page (removed from
    // Home and the nav drawer), so this is the app's one remaining entry point.
    @FXML
    private void handleLogout(ActionEvent event) {
        NavShell.logout(event);
    }
}
