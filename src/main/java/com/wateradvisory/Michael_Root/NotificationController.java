package com.wateradvisory.Michael_Root;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class NotificationController {

    @FXML
    private ListView<Notification> notificationList;

    @FXML
    private Label notificationCount;

    @FXML
    private void handleReturnToMain(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/App_Root-view.fxml"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleGoToGraph(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Jainya_FXML/MainView.fxml"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleReturnTable(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Michael_FXML/TableDisplayPage.fxml"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
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

                    Label waterUsage = new Label(String.valueOf(notification.getWaterObject().getWaterUsage()));
                    waterUsage.setStyle(
                            "-fx-font-size: 16px;" +
                                    "-fx-font-weight: bold;"
                    );

                    Label zScore = new Label(String.valueOf(notification.getzScore()));

                    Label date = new Label(
                            notification.getDateCreated().format(formatter)
                    );

                    date.setStyle("-fx-text-fill: grey;");

                    VBox box = new VBox(
                            5,
                            waterUsage,
                            zScore,
                            date
                    );

                    box.setPadding(
                            new javafx.geometry.Insets(10)
                    );

                    setGraphic(box);
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
