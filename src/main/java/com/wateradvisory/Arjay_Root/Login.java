package com.wateradvisory.Arjay_Root;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ComboBox;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

// Database Imports
import com.wateradvisory.database.AuthService;
import javafx.scene.control.PasswordField;

public class Login {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private ToggleButton showPasswordBtn;

    @FXML
    private ComboBox<String> recentLoginBox;

    private final Preferences preferences =
            Preferences.userNodeForPackage(Login.class);

    @FXML
    private void initialize() {

        visiblePasswordField.textProperty()
                .bindBidirectional(passwordField.textProperty());

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        // Load saved accounts into dropdown
        loadRecentLogins();

        recentLoginBox.setOnAction(event -> {

            String selectedEmail =
                    recentLoginBox.getValue();

            if (selectedEmail == null) {
                return;
            }

            String savedPassword =
                    preferences.get(
                            "password_" + selectedEmail,
                            ""
                    );

            emailField.setText(selectedEmail);
            passwordField.setText(savedPassword);
        });
    }

    @FXML
    private void handleShowPassword(ActionEvent event) {

        boolean showPassword = showPasswordBtn.isSelected();

        visiblePasswordField.setVisible(showPassword);
        visiblePasswordField.setManaged(showPassword);

        passwordField.setVisible(!showPassword);
        passwordField.setManaged(!showPassword);

        if (showPassword) {
            showPasswordBtn.setText("Hide Password");
        } else {
            showPasswordBtn.setText("Show Password");
        }
    }

    @FXML
    private void handleChatViewNavigation(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Charlie_FXML/ChatView.fxml"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        double width = stage.getWidth();
        double height = stage.getHeight();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);

        stage.show();
    }

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            System.out.println("Please enter your email and password.");
            return;
        }

        boolean successful = AuthService.login(email, password);

        if (successful) {
            System.out.println("Login successful!");

            // Save successful login locally for prototype testing
            saveRecentLogin(email, password);

            String nextPage;

            if (AuthService.isSetupComplete()) {
                nextPage = "/App_Root-view.fxml";
            } else {
                nextPage = "/Arjay_FXML/postregister.fxml";
            }

            Parent root = FXMLLoader.load(
                    getClass().getResource(nextPage)
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            double width = stage.getWidth();
            double height = stage.getHeight();

            stage.setScene(new Scene(root));
            stage.setWidth(width);
            stage.setHeight(height);
            stage.show();

        } else {
            System.out.println("Incorrect email or password.");
        }
    }

    @FXML
    private void handleSignUp(ActionEvent event) throws IOException {

        Parent root = FXMLLoader.load(
                getClass().getResource("/Arjay_FXML/register.fxml")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        double width = stage.getWidth();
        double height = stage.getHeight();

        stage.setScene(new Scene(root));
        stage.setWidth(width);
        stage.setHeight(height);

        stage.show();
    }

    private void saveRecentLogin(String email, String password) {

        String savedLogins =
                preferences.get("recentLogins", "");

        List<String> logins =
                new java.util.ArrayList<>();

        if (!savedLogins.isEmpty()) {
            logins.addAll(
                    Arrays.asList(savedLogins.split("\\|"))
            );
        }

        // Prevent duplicate emails
        logins.remove(email);

        // Most recent account goes first
        logins.add(0, email);

        // Keep maximum 5 accounts
        if (logins.size() > 5) {

            String removedEmail =
                    logins.get(logins.size() - 1);

            preferences.remove(
                    "password_" + removedEmail
            );

            logins.remove(logins.size() - 1);
        }

        preferences.put(
                "recentLogins",
                String.join("|", logins)
        );

        // Prototype only: save password locally
        preferences.put(
                "password_" + email,
                password
        );
    }

    private void loadRecentLogins() {

        String savedLogins =
                preferences.get("recentLogins", "");

        if (savedLogins.isEmpty()) {
            return;
        }

        List<String> logins =
                Arrays.asList(savedLogins.split("\\|"));

        recentLoginBox.getItems().setAll(logins);
    }
}
