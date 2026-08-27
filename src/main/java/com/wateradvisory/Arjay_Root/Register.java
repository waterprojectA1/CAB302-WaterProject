package com.wateradvisory.Arjay_Root;

import com.wateradvisory.database.AuthService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class Register {

    @FXML
    private TextField emailField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;


    @FXML
    private void handleRegister(ActionEvent event) throws IOException {

        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (email.isEmpty() ||
                username.isEmpty() ||
                password.isEmpty() ||
                confirmPassword.isEmpty()) {

            showMessage(
                    "Registration Error",
                    "Please complete all fields."
            );

            return;
        }

        if (!password.equals(confirmPassword)) {

            showMessage(
                    "Registration Error",
                    "Passwords do not match."
            );

            return;
        }

        if (password.length() < 8) {

            showMessage(
                    "Registration Error",
                    "Password must contain at least 8 characters."
            );

            return;
        }

        boolean successful =
                AuthService.register(
                        email,
                        username,
                        password
                );

        if (successful) {

            showMessage(
                    "Success",
                    "Account created successfully."
            );

            openLoginPage(event);

        } else {

            showMessage(
                    "Registration Error",
                    "Account creation failed."
            );
        }
    }


    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        openLoginPage(event);
    }


    private void openLoginPage(ActionEvent event) throws IOException {

        Parent root = FXMLLoader.load(
                getClass().getResource(
                        "/Arjay_FXML/login.fxml"
                )
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
    }


    private void showMessage(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}