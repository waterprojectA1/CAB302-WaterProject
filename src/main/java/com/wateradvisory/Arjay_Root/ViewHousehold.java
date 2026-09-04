package com.wateradvisory.Arjay_Root;

import com.wateradvisory.database.HouseholdService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import java.io.IOException;
import java.util.List;

import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class ViewHousehold {

    @FXML
    private AnchorPane viewHouseholdPane;

    @FXML
    private AnchorPane noHouseholdPane;

    @FXML
    private AnchorPane houseSettings;

    @FXML
    private Button houseSettingsBtn;

    @FXML
    private Button leaveHouseholdBtn;

    @FXML
    private Text householdText;

    @FXML
    private TextField householdNameField;

    @FXML
    private Text ownerText;

    @FXML
    private Text householdTotalText;

    @FXML
    private TextField joinCodeTextField;

    @FXML
    private VBox memberListBox;

    @FXML
    private Text joinCodeText;

    @FXML
    private VBox editMemberListBox;

    @FXML
    private TextField createHouseholdNameField;

    @FXML
    private TextField createAddressField;

    @FXML
    private void initialize() {

        houseSettings.setVisible(false);
        houseSettings.setManaged(false);

        boolean hasHousehold = HouseholdService.hasHousehold();

        if (hasHousehold) {

            viewHouseholdPane.setVisible(true);
            viewHouseholdPane.setManaged(true);

            noHouseholdPane.setVisible(false);
            noHouseholdPane.setManaged(false);

            String householdName =
                    HouseholdService.getHouseholdName();

            householdText.setText(householdName);

            String owner =
                    HouseholdService.getHouseholdOwner();

            ownerText.setText("Owner: " + owner);

            String joinCode =
                    HouseholdService.getJoinCode();

            joinCodeText.setText("Join Code: " + joinCode);

            loadHouseholdMembers();

            // Check if user is an owner or normal user
            // hide certain buttons for both roles
            boolean isOwner =
                    HouseholdService.isCurrentUserOwner();

            // show settings to owner
            houseSettingsBtn.setVisible(isOwner);
            houseSettingsBtn.setManaged(isOwner);

            // show leave household to normal user
            leaveHouseholdBtn.setVisible(!isOwner);
            leaveHouseholdBtn.setManaged(!isOwner);

        } else {

            viewHouseholdPane.setVisible(false);
            viewHouseholdPane.setManaged(false);

            noHouseholdPane.setVisible(true);
            noHouseholdPane.setManaged(true);
        }
    }

    @FXML
    private void handleHouseSettings(ActionEvent event) {

        viewHouseholdPane.setVisible(false);
        viewHouseholdPane.setManaged(false);

        houseSettings.setVisible(true);
        houseSettings.setManaged(true);

        loadEditableMembers();
    }

    private void loadEditableMembers() {

        editMemberListBox.getChildren().clear();

        String owner = HouseholdService.getHouseholdOwner();

        for (String username : HouseholdService.getHouseholdMembers()) {

            // Owner should never appear in removal list
            if (username.equals(owner)) {
                continue;
            }

            CheckBox memberCheckBox = new CheckBox(username);

            editMemberListBox.getChildren().add(memberCheckBox);
        }
    }

    @FXML
    private void handleCancelSettings(ActionEvent event) {

        houseSettings.setVisible(false);
        houseSettings.setManaged(false);

        viewHouseholdPane.setVisible(true);
        viewHouseholdPane.setManaged(true);
    }

    @FXML
    private void handleApplyChanges() {

        // 1. Change household name if user entered one
        String newName = householdNameField.getText().trim();

        if (!newName.isEmpty()) {

            boolean renamed =
                    HouseholdService.changeHouseholdName(newName);

            if (renamed) {
                System.out.println("Household name updated.");
            } else {
                System.out.println("Failed to update household name.");
            }
        }

        // 2. Remove checked household members
        for (Node node : editMemberListBox.getChildren()) {

            if (node instanceof CheckBox checkBox) {

                if (checkBox.isSelected()) {

                    String username = checkBox.getText();

                    boolean removed =
                            HouseholdService.removeHouseholdMember(username);

                    if (removed) {
                        System.out.println(
                                "Removed member: " + username
                        );
                    } else {
                        System.out.println(
                                "Failed to remove member: " + username
                        );
                    }
                }
            }
        }

        // 3. Clear name field
        householdNameField.clear();

        // 4. Reload page
        initialize();
    }

    @FXML
    private void handleReturnToMain(ActionEvent event) {
        // return to main interface
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/App_Root-view.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHouseholdMembers() {

        memberListBox.getChildren().clear();

        List<String> members =
                HouseholdService.getHouseholdMembers();

        for (String username : members) {

            Label memberLabel = new Label(username);

            memberListBox.getChildren().add(memberLabel);
        }

        householdTotalText.setText(
                "Total Members: " + members.size()
        );
    }

    @FXML
    private void handleJoinHousehold() {

        String joinCode = joinCodeTextField.getText().trim().toUpperCase();

        if (joinCode.isEmpty()) {
            System.out.println("Please enter a join code.");
            return;
        }

        if (joinCode.length() != 6) {
            System.out.println("Join code must contain 6 characters.");
            return;
        }

        boolean success =
                HouseholdService.joinHousehold(joinCode);

        if (success) {

            System.out.println("Joined household successfully.");

            joinCodeTextField.clear();

            // Reload page state
            initialize();

        } else {
            System.out.println("Invalid join code or failed to join household.");
        }
    }

    @FXML
    private void handleLeaveHousehold() {

        boolean success = HouseholdService.leaveHousehold();

        if (success) {
            System.out.println("Left household successfully.");

            initialize();

        } else {
            System.out.println("Failed to leave household.");
        }
    }

    @FXML
    private void handleCreateHousehold() {

        String householdName = createHouseholdNameField.getText().trim();

        String address = createAddressField.getText().trim();

        if (householdName.isEmpty()) {
            System.out.println("Household name is required.");
            return;
        }

        String householdId = HouseholdService.createHousehold(householdName, address);

        if (householdId == null) {
            System.out.println("Failed to create household.");
            return;
        }

        boolean linked =
                HouseholdService.linkUserToHousehold(householdId);

        if (!linked) {
            System.out.println("Failed to link user to household.");
            return;
        }

        System.out.println("Household created successfully.");

        initialize();
    }

    @FXML
    private void handleDeleteHousehold() {

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmation.setTitle("Delete Household");
        confirmation.setHeaderText("Delete this household?");
        confirmation.setContentText(
                "All members will be removed from this household."
        );

        ButtonType result =
                confirmation.showAndWait().orElse(ButtonType.CANCEL);

        if (result != ButtonType.OK) {
            return;
        }

        boolean success =
                HouseholdService.deleteHousehold();

        if (success) {

            System.out.println("Household deleted successfully.");

            initialize();

        } else {

            System.out.println("Failed to delete household.");
        }
    }

}
