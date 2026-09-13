// Summary: Controls the Record Water feature, including selecting activities,
// calculating water usage, managing the daily activity list, and saving daily records.
package com.wateradvisory.Arjay_Root;

// Project services and models used for water calculations and daily water records.
import com.wateradvisory.database.WaterConsumptionService;
import com.wateradvisory.water.WaterActivityEntry;
import com.wateradvisory.database.WaterRecordService;
import com.wateradvisory.Charlie_Root.NavShell;

// JavaFX imports used for events, FXML controls, page navigation, and interface elements.
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;

// Java collections used to store the user's current daily water activities.
import java.util.ArrayList;
import java.util.List;

public class RecordWaterController {

    @FXML
    private AnchorPane mainRecord;

    @FXML
    private AnchorPane activityRecord;

    @FXML
    private MenuButton selectActivity;

    @FXML
    private MenuButton activityDuration;

    @FXML
    private MenuButton activityAmount;

    @FXML
    private Text activityDurationText;

    @FXML
    private Text activityTotalText;

    private String selectedActivity = "";

    private int selectedActivityDuration = 0;
    private int selectedActivityAmount = 0;

    private double pendingWaterTotal = 0;
    private double currentActivityTotal = 0;

    private final List<WaterActivityEntry> pendingActivities = new ArrayList<>();

    @FXML
    private Text pendingTotalText;

    @FXML
    private VBox activityListBox;

    // Sets up the Record Water page and loads today's saved water activities.
    @FXML
    private void initialize() {

        mainRecord.setVisible(true);
        mainRecord.setManaged(true);

        activityRecord.setVisible(false);
        activityRecord.setManaged(false);

        setupActivityMenu();

        // Load today's already-saved activities from Supabase
        pendingActivities.clear();
        pendingActivities.addAll(
                WaterRecordService.getTodayActivities()
        );

        // Display them in the scroll box
        refreshActivityList();

        // Calculate today's current total
        recalculatePendingTotal();
    }

    // Returns the user from the Record Water page to the main application screen.
    @FXML
    private void handleReturnToMain(ActionEvent event) {
        NavShell.go(event, NavShell.Route.HOME);
    }

    // Opens the activity recording form and resets the activity selections.
    @FXML
    private void handleActivityBtn(ActionEvent event) {

        selectedActivity = "";

        resetActivityInputs();

        selectActivity.setText("Choose Activity");

        activityDuration.getItems().clear();
        activityAmount.getItems().clear();

        activityDuration.setDisable(true);
        activityAmount.setDisable(true);

        mainRecord.setVisible(false);
        mainRecord.setManaged(false);

        activityRecord.setVisible(true);
        activityRecord.setManaged(true);

    }

    // Returns from the activity form to the main Record Water screen.
    @FXML
    private void handleReturnToRecordMain(ActionEvent event) {

        activityRecord.setVisible(false);
        activityRecord.setManaged(false);

        mainRecord.setVisible(true);
        mainRecord.setManaged(true);
    }

    // Creates the list of available water-use activities for the activity menu.
    private void setupActivityMenu() {

        String[] activities = {
                "Shower",
                "Dishes",
                "Floor Cleaning",
                "Laundry",
                "Car Wash",
                "Window Cleaning",
                "Bathtub"
        };

        for (String activity : activities) {

            MenuItem item = new MenuItem(activity);

            item.setOnAction(event -> {

                selectedActivity = activity;
                selectActivity.setText(activity);

                resetActivityInputs();
                setupInputsForActivity();
            });

            selectActivity.getItems().add(item);
        }
    }

    // Sets the available duration and amount options for the selected activity.
    private void setupInputsForActivity() {

        activityDuration.getItems().clear();
        activityAmount.getItems().clear();

        int[] durations;
        int maxAmount;

        switch (selectedActivity) {

            case "Shower":
                durations = new int[]{2, 5, 10, 15, 20};
                maxAmount = 5;
                break;

            case "Dishes":
                durations = new int[]{5, 10, 15, 20, 30};
                maxAmount = 5;
                break;

            case "Floor Cleaning":
                durations = new int[]{5, 10, 15, 20, 30};
                maxAmount = 5;
                break;

            case "Laundry":
                durations = new int[]{15, 30, 45, 60};
                maxAmount = 5;
                break;

            case "Car Wash":
                durations = new int[]{5, 10, 15, 20, 30};
                maxAmount = 3;
                break;

            case "Window Cleaning":
                durations = new int[]{5, 10, 15, 20, 30};
                maxAmount = 5;
                break;

            case "Bathtub":
                durations = new int[]{10, 15, 20, 30, 45};
                maxAmount = 3;
                break;

            default:
                return;
        }

        activityDuration.setDisable(false);
        activityAmount.setDisable(false);

        for (int minutes : durations) {

            MenuItem item =
                    new MenuItem(minutes + " minutes");

            item.setOnAction(event -> {

                selectedActivityDuration = minutes;

                activityDuration.setText(
                        minutes + " minutes"
                );

                updateActivityTotal();
            });

            activityDuration.getItems().add(item);
        }

        for (int amount = 1; amount <= maxAmount; amount++) {

            int selectedAmount = amount;

            MenuItem item = new MenuItem(
                    amount == 1
                            ? "1 time"
                            : amount + " times"
            );

            item.setOnAction(event -> {

                selectedActivityAmount = selectedAmount;

                activityAmount.setText(
                        selectedAmount == 1
                                ? "1 time"
                                : selectedAmount + " times"
                );

                updateActivityTotal();
            });

            activityAmount.getItems().add(item);
        }
    }

    // Resets the selected duration, amount, and calculated activity total.
    private void resetActivityInputs() {

        selectedActivityDuration = 0;
        selectedActivityAmount = 0;
        currentActivityTotal = 0;

        activityDuration.setText("Duration");
        activityAmount.setText("Select Amount");

        activityTotalText.setText(
                "Water Consumption Total: N/A"
        );
    }

    // Calculates and displays the water usage for the currently selected activity.
    private void updateActivityTotal() {

        if (selectedActivity.isEmpty()
                || selectedActivityDuration == 0
                || selectedActivityAmount == 0) {

            activityTotalText.setText(
                    "Water Consumption Total: N/A"
            );

            currentActivityTotal = 0;
            return;
        }

        currentActivityTotal =
                WaterConsumptionService.calculateActivity(
                        selectedActivity,
                        selectedActivityDuration,
                        selectedActivityAmount
                );

        activityTotalText.setText(
                "Water Consumption Total: "
                        + currentActivityTotal
                        + " L"
        );
    }

    // Refreshes the daily activity list and adds a remove option to each activity.
    private void refreshActivityList() {

        activityListBox.getChildren().clear();

        for (WaterActivityEntry entry : pendingActivities) {

            String text =
                    entry.getActivity()
                            + " | "
                            + entry.getDuration()
                            + " min × "
                            + entry.getAmount()
                            + " | "
                            + entry.getLitres()
                            + " L";

            Label activityLabel = new Label(text);

            Button removeButton = new Button("Remove");

            removeButton.setOnAction(event -> {
                pendingActivities.remove(entry);

                recalculatePendingTotal();
                refreshActivityList();
            });

            // row set up

            Region spacer = new Region();

            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(15);

            row.getStyleClass().add("list-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 20, 5, 10));

            row.getChildren().addAll(
                    activityLabel,
                    spacer,
                    removeButton
            );

            activityListBox.getChildren().add(row);
        }
    }

    // Adds the completed activity to the user's current daily activity list.
    @FXML
    private void handleSubmitActivity() {

        if (selectedActivity.isEmpty()) {
            System.out.println("Please select an activity.");
            return;
        }

        if (currentActivityTotal <= 0) {
            System.out.println("Please select duration and amount.");
            return;
        }

        WaterActivityEntry activityEntry =
                new WaterActivityEntry(
                        selectedActivity,
                        selectedActivityDuration,
                        selectedActivityAmount,
                        currentActivityTotal
                );

        pendingActivities.add(activityEntry);

        refreshActivityList();
        recalculatePendingTotal();

        System.out.println(
                "Added activity: " + selectedActivity
        );

        System.out.println(
                "Pending activities: " + pendingActivities.size()
        );

        // Reset activity inputs
        selectedActivity = "";
        selectedActivityDuration = 0;
        selectedActivityAmount = 0;
        currentActivityTotal = 0;

        selectActivity.setText("Choose Activity");
        activityDuration.setText("Duration");
        activityAmount.setText("Select Amount");

        activityTotalText.setText(
                "Water Consumption Total: N/A"
        );

        activityRecord.setVisible(false);
        activityRecord.setManaged(false);

        mainRecord.setVisible(true);
        mainRecord.setManaged(true);
    }

    // Recalculates the total water usage from all activities currently in the daily list.
    private void recalculatePendingTotal() {

        pendingWaterTotal = 0;

        for (WaterActivityEntry entry : pendingActivities) {
            pendingWaterTotal += entry.getLitres();
        }

        updatePendingTotalText();
    }

    // Saves the user's current daily water activities and displays the save result.
    @FXML
    private void handleSubmitWaterConsumption() {

        boolean success =
                WaterRecordService.saveDailyWaterSubmission(
                        pendingActivities
                );

        if (success) {

            System.out.println(
                    "Today's water activities saved successfully."
            );

            recalculatePendingTotal();
            refreshActivityList();

            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION
            );

            alert.setTitle("Water Usage Saved");
            alert.setHeaderText("Daily Water Usage Updated");

            alert.setContentText(
                    "Your water usage report for today has been successfully saved."
            );

            alert.showAndWait();

        } else {

            System.out.println(
                    "Failed to save today's water activities."
            );

            Alert alert = new Alert(
                    Alert.AlertType.ERROR
            );

            alert.setTitle("Save Failed");
            alert.setHeaderText("Unable to Save Water Usage");

            alert.setContentText(
                    "Your daily water usage report could not be saved. Please try again."
            );

            alert.showAndWait();
        }
    }

    // Updates the displayed total water consumption for the current day.
    private void updatePendingTotalText() {

        pendingTotalText.setText(
                "Current Water Consumption: "
                        + pendingWaterTotal
                        + " L"
        );
    }


}
