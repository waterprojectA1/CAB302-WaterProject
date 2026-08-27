package com.wateradvisory.Arjay_Root;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Database Imports
import com.wateradvisory.database.AuthService;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class Login {

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
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

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

            String nextPage;

            if (AuthService.isSetupComplete()) {
                nextPage = "/Arjay_FXML/recordwater.fxml";
            } else {
                nextPage = "/Arjay_FXML/postregister.fxml";
            }

            Parent root = FXMLLoader.load(getClass().getResource(nextPage));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

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
}
