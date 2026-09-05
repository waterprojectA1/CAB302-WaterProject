package com.wateradvisory.Arjay_Root;
import com.wateradvisory.database.WaterConsumptionService;
import com.wateradvisory.water.WaterActivityEntry;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.io.IOException;

public class RecordWaterController {

    @FXML
    private AnchorPane mainRecord;

    @FXML
    private AnchorPane showerRecord;

    @FXML
    private MenuButton showerDuration;

    @FXML
    private MenuButton showerAmount;

    private int selectedShowerDuration = 0;
    private int selectedShowerAmount = 0;

    private double pendingWaterTotal = 0;
    private double currentShowerTotal = 0;

    private final List<WaterActivityEntry> pendingActivities = new ArrayList<>();

    @FXML
    private Text showerTotalText;

    @FXML
    private Text pendingTotalText;

    @FXML
    private VBox activityListBox;

    @FXML
    private void initialize() {

        mainRecord.setVisible(true);
        mainRecord.setManaged(true);

        showerRecord.setVisible(false);
        showerRecord.setManaged(false);

        setupShowerMenus();
        updatePendingTotalText();
    }

    @FXML
    private void handleReturnToMain(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/App_Root-view.fxml"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleShowerBtn(ActionEvent event) {

        mainRecord.setVisible(false);
        mainRecord.setManaged(false);

        showerRecord.setVisible(true);
        showerRecord.setManaged(true);
    }

    @FXML
    private void handleReturnToRecordMain(ActionEvent event) {

        showerRecord.setVisible(false);
        showerRecord.setManaged(false);

        mainRecord.setVisible(true);
        mainRecord.setManaged(true);
    }

    private void setupShowerMenus() {

        // Shower Duration
        int[] durations = {2, 5, 10, 15, 20};

        for (int minutes : durations) {

            MenuItem item = new MenuItem(minutes + " minutes");

            item.setOnAction(event -> {
                selectedShowerDuration = minutes;
                showerDuration.setText(minutes + " minutes");

                updateShowerTotal();
            });

            // Add duration
            showerDuration.getItems().add(item);
        }

        // Shower Amount
        for (int amount = 1; amount <= 5; amount++) {

            int selectedAmount = amount;

            MenuItem item = new MenuItem(amount == 1 ? "1 time" : amount + " times");

            item.setOnAction(event -> {

                selectedShowerAmount = selectedAmount;
                showerAmount.setText(selectedAmount == 1 ? "1 time" : selectedAmount + " times");

                updateShowerTotal();
            });

            // Add amount
            showerAmount.getItems().add(item);
        }
    }

    private void updateShowerTotal() {

        if (selectedShowerDuration == 0
                || selectedShowerAmount == 0) {

            showerTotalText.setText(
                    "Water Consumption Total: N/A"
            );

            currentShowerTotal = 0;
            return;
        }

        currentShowerTotal =
                WaterConsumptionService.calculateShower(
                        selectedShowerDuration,
                        selectedShowerAmount
                );

        showerTotalText.setText(
                "Water Consumption Total: "
                        + currentShowerTotal
                        + " L"
        );
    }

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

    @FXML
    private void handleSubmitShower() {

        if (currentShowerTotal <= 0) {
            System.out.println("Please select duration and amount.");
            return;
        }

        WaterActivityEntry showerEntry =
                new WaterActivityEntry(
                        "Shower",
                        selectedShowerDuration,
                        selectedShowerAmount,
                        currentShowerTotal
                );

        pendingActivities.add(showerEntry);

        refreshActivityList();
        recalculatePendingTotal();

        recalculatePendingTotal();

        System.out.println(
                "Pending activities: " + pendingActivities.size()
        );

        selectedShowerDuration = 0;
        selectedShowerAmount = 0;
        currentShowerTotal = 0;

        showerDuration.setText("Duration");
        showerAmount.setText("Select Amount");
        showerTotalText.setText(
                "Water Consumption Total: N/A"
        );

        showerRecord.setVisible(false);
        showerRecord.setManaged(false);

        mainRecord.setVisible(true);
        mainRecord.setManaged(true);
    }

    private void recalculatePendingTotal() {

        pendingWaterTotal = 0;

        for (WaterActivityEntry entry : pendingActivities) {
            pendingWaterTotal += entry.getLitres();
        }

        updatePendingTotalText();
    }

    @FXML
    private void handleSubmitWater(ActionEvent event) {
        System.out.println("submit water clicked");
    }

    private void updatePendingTotalText() {

        pendingTotalText.setText(
                "Current Water Consumption: "
                        + pendingWaterTotal
                        + " L"
        );
    }


}
