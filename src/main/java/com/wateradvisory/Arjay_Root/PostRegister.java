package com.wateradvisory.Arjay_Root;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;

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
    private void handleNoHousehold() {
        householdDetailsPane.setVisible(true);
        householdDetailsPane.setManaged(true);
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