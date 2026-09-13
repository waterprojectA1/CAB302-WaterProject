// Summary: Controls the household page, including viewing household details,
// managing members, joining or creating households, and editing household settings.
package com.wateradvisory.Arjay_Root;

// Project services used for household management and household water usage data.
import com.wateradvisory.Charlie_Root.NavShell;
import com.wateradvisory.database.HouseholdService;
import com.wateradvisory.database.WaterRecordService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

// Java imports used for error handling and storing household data.
import java.util.Map;

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
    private Text householdWaterTotalText;

    // Loads the correct household view and displays the current household information.
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

            // show household water total
            double householdWaterTotal = WaterRecordService.getHouseholdTotalWater();
            householdWaterTotalText.setText("Household Water Total: " + householdWaterTotal + " L");

        } else {

            viewHouseholdPane.setVisible(false);
            viewHouseholdPane.setManaged(false);

            noHouseholdPane.setVisible(true);
            noHouseholdPane.setManaged(true);
        }
    }

    // Opens the household settings screen and loads editable household members.
    @FXML
    private void handleHouseSettings(ActionEvent event) {

        viewHouseholdPane.setVisible(false);
        viewHouseholdPane.setManaged(false);

        houseSettings.setVisible(true);
        houseSettings.setManaged(true);

        loadEditableMembers();
    }

    // Loads household members that the owner is allowed to remove.
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

    // Closes household settings and returns to the household overview.
    @FXML
    private void handleCancelSettings(ActionEvent event) {

        houseSettings.setVisible(false);
        houseSettings.setManaged(false);

        viewHouseholdPane.setVisible(true);
        viewHouseholdPane.setManaged(true);
    }

    // Applies household name changes and removes any selected household members.
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

    // Returns the user from the household page to the main application screen.
    @FXML
    private void handleReturnToMain(ActionEvent event) {
        NavShell.go(event, NavShell.Route.HOME);
    }

    // Loads household members and displays each member's total water usage.
    private void loadHouseholdMembers() {

        memberListBox.getChildren().clear();

        Map<String, Double> memberTotals =
                WaterRecordService.getHouseholdMemberTotals();

        String owner = HouseholdService.getHouseholdOwner();

        for (Map.Entry<String, Double> member
                : memberTotals.entrySet()) {

            String username = member.getKey();
            double totalWater = member.getValue();

            HBox row = new HBox(10);
            row.getStyleClass().add("list-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(username);
            nameLabel.getStyleClass().add("list-row-name");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            nameLabel.setMaxWidth(Double.MAX_VALUE);

            Label totalLabel = new Label(totalWater + " L");
            totalLabel.getStyleClass().add("form-label");

            row.getChildren().add(NavShell.avatar(username, 26));
            row.getChildren().add(nameLabel);

            if (username.equals(owner)) {
                Label ownerPill = new Label("Owner");
                ownerPill.getStyleClass().add("role-pill");
                row.getChildren().add(ownerPill);
            }

            row.getChildren().add(totalLabel);

            memberListBox.getChildren().add(row);
        }

        householdTotalText.setText(
                "Total Members: " + memberTotals.size()
        );
    }

    // Validates a join code and attempts to join the selected household.
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

    // Removes the current user from their household and refreshes the page.
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

    // Creates a new household, links the current user to it, and refreshes the page.
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

    // Confirms and deletes the current household when requested by the owner.
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
