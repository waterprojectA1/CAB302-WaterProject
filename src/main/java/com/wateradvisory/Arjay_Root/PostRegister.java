package com.wateradvisory.Arjay_Root;

import com.wateradvisory.database.AuthService;
import com.wateradvisory.database.HouseholdService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.TextField;

import java.io.IOException;

public class PostRegister {

    // 1st Screen asks user
    @FXML
    private AnchorPane postRegister1;

    // 2nd Screen IF User selects Yes
    @FXML
    private AnchorPane postRegister2;

    @FXML
    private void initialize() {
        postRegister1.setVisible(true);
        postRegister1.setManaged(true);
        postRegister2.setVisible(false);
        postRegister2.setManaged(false);
    }

    @FXML
    private void handleYesHousehold() {
        postRegister1.setVisible(false);
        postRegister1.setManaged(false);
        postRegister2.setVisible(true);
        postRegister2.setManaged(true);
    }

    @FXML
    private void handleReturnHousehold() {
        initialize();
    }

    @FXML
    private void handleNoHousehold(ActionEvent event) {
        // move user straight to main screen interface
        try {
            AuthService.markSetupComplete();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/App_Root-view.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Household Creation Section

    @FXML
    private TextField householdNameField;

    @FXML
    private TextField addressField;

    @FXML
    private void handleCreateHousehold(ActionEvent event) {

        String householdName = householdNameField.getText().trim();
        String address = addressField.getText().trim();

        if (householdName.isEmpty()) {
            System.out.println("Household name is required.");
            return;
        }

        String householdId =
                HouseholdService.createHousehold(
                        householdName,
                        address
                );

        if (householdId == null) {
            System.out.println("Failed to create household.");
            return;
        }

        System.out.println(
                "Household created successfully: " + householdId
        );

        boolean linked =
                HouseholdService.linkUserToHousehold(householdId);

        if (!linked) {
            System.out.println("Failed to link user to household.");
            return;
        }

        System.out.println("User successfully linked to household.");

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/App_Root-view.fxml")
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
}