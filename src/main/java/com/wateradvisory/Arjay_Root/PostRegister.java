package com.wateradvisory.Arjay_Root;

import com.wateradvisory.database.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class PostRegister {

    @FXML
    private AnchorPane householdDetailsPane;

    @FXML
    private void initialize() {
        householdDetailsPane.setVisible(false);
        householdDetailsPane.setManaged(false);
    }

    @FXML
    private void handleYesHousehold() {
        householdDetailsPane.setVisible(true);
        householdDetailsPane.setManaged(true);
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

    @FXML
    private MenuButton peopleCountMenu;

    @FXML
    private void handlePeopleCount(ActionEvent event) {
        MenuItem selectedItem = (MenuItem) event.getSource();
        peopleCountMenu.setText(selectedItem.getText());
    }

    @FXML
    private void handleCreateHousehold() {
        System.out.println("Create Household Clicked");
    }
}