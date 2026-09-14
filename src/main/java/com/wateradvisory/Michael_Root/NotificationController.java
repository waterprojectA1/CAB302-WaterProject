package com.wateradvisory.Michael_Root;

import com.wateradvisory.Charlie_Root.NavShell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;

public class NotificationController {

    @FXML
    private ListView<Notification> notificationList;

    @FXML
    private Label notificationCount;

    @FXML
    private void handleReturnToMain(ActionEvent event) {
        NavShell.go(event, NavShell.Route.HOME);
    }

    @FXML
    private void handleGoToGraph(ActionEvent event) {
        NavShell.go(event, NavShell.Route.DAILY);
    }

    @FXML
    private void handleReturnTable(ActionEvent event) {
        NavShell.go(event, NavShell.Route.DATA_TABLE);
    }

    private ObservableList<Notification> notifications =
            FXCollections.observableArrayList();

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @FXML
    public void initialize() {

        WaterData testdata = new WaterData(1,  755, "02-09-2026", "02-09-2026", "DAILY", "1");


        // Example notifications
        notifications.add(new Notification(
                testdata,
                2,
                3
        ));


        notificationList.setItems(notifications);

        // Tell the ListView how each notification should look
        notificationList.setCellFactory(list -> new ListCell<>() {

            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);

                if (empty || notification == null) {
                    setGraphic(null);
                    setText(null);
                } else {

                    Circle dot = new Circle(4);
                    dot.getStyleClass().add("notif-dot");

                    Label title = new Label(
                            notification.getWaterObject().getWaterUsage() + " L logged"
                    );
                    title.getStyleClass().add("notif-title");

                    Label meta = new Label("Z-score " + notification.getzScore());
                    meta.getStyleClass().add("notif-meta");

                    VBox textBox = new VBox(2, title, meta);
                    HBox.setHgrow(textBox, Priority.ALWAYS);

                    Label date = new Label(
                            notification.getDateCreated().format(formatter)
                    );
                    date.getStyleClass().add("notif-meta");

                    HBox row = new HBox(14, dot, textBox, date);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("notif-row");

                    setGraphic(row);
                }
            }
        });

        updateNotificationCount();
    }

    private void updateNotificationCount() {
        notificationCount.setText(
                notifications.size() + " notifications"
        );
    }
}
