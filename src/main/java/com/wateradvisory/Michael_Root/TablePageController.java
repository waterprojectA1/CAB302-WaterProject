package com.wateradvisory.Michael_Root;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;


public class TablePageController {
    @FXML
    private TableView<WaterData> tableView;

    @FXML
    private TableColumn<WaterData, LocalDate> sDateColumn;

    @FXML
    private TableColumn<WaterData, LocalDate> eDateColumn;

    @FXML
    private TableColumn<WaterData, Double> usageColumn;

    @FXML
    private TableColumn<WaterData, Double> zScoreColumn;

    @FXML
    private TableColumn<WaterData, Double> userDiffColumn;

    @FXML
    private TableColumn<WaterData, Double> globalDiffColumn;

    @FXML
    private TableColumn<WaterData, Integer> userIDColumn;

    @FXML
    private RadioButton dailyRadio;

    @FXML
    private RadioButton weeklyRadio;

    @FXML
    private RadioButton monthlyRadio;

    @FXML
    private void handleReturnToMain(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/App_Root-view.fxml"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }
    @FXML
    private void handleGoToNotification(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Michael_FXML/NotificationPage.fxml"));

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


    private WaterDataList model = new WaterDataList();

    private int loggedUser = 1;

    private FilteredList<WaterData> filteredDailyWater;
    private FilteredList<WaterData> filteredWeeklyWater;
    private FilteredList<WaterData> filteredMonthlyWater;

    public void initialize(){


        filteredDailyWater = new FilteredList<>(model.getDailyWater(), water -> true);
        filteredWeeklyWater = new FilteredList<>(model.getWeeklyWater(), water -> true);
        filteredMonthlyWater = new FilteredList<>(model.getMonthlyWater(), water -> true);
        filteredDailyWater.setPredicate(water -> water.getUserID() == loggedUser);

        setupTable();
        setupRadioButtons();

        tableView.setItems(filteredDailyWater);

        tableView.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                WaterData selectedPerson =
                        tableView.getSelectionModel()
                                .getSelectedItem();

                if (selectedPerson != null) {
                    openPersonDetails(selectedPerson);
                }
            }
        });
    }

    public void setModel(WaterDataList model) {


    }

    private void setupTable() {

        sDateColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getDate1()
                )
        );

        eDateColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getDate2()
                )
        );
        usageColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getWaterUsage()
                )
        );
        zScoreColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        model.getZScore(cellData.getValue())
                )
        );
        userDiffColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        model.getUserDiff(cellData.getValue())
                )
        );
        globalDiffColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        model.getGlobalDiff(cellData.getValue())
                )
        );

        userIDColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getUserID()
                )
        );

        tableView.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

    }
    private void setupRadioButtons() {

        ToggleGroup group = new ToggleGroup();

        dailyRadio.setToggleGroup(group);
        weeklyRadio.setToggleGroup(group);
        monthlyRadio.setToggleGroup(group);

        dailyRadio.setSelected(true);

        group.selectedToggleProperty().addListener(
                (observable, oldToggle, newToggle) -> {

                    if (newToggle == dailyRadio) {

                        filteredDailyWater.setPredicate(water -> water.getUserID() == loggedUser);
                        tableView.setItems(filteredDailyWater);


                    } else if (newToggle == weeklyRadio) {


                        filteredWeeklyWater.setPredicate(water -> water.getUserID() == loggedUser);
                        tableView.setItems(filteredWeeklyWater);

                    } else if (newToggle == monthlyRadio) {

                        filteredMonthlyWater.setPredicate(water -> water.getUserID() == loggedUser);
                        tableView.setItems(filteredMonthlyWater);
                    }
                }
        );
    }


    private void openPersonDetails(WaterData aqua) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/Michael_FXML/DetailDataPage.fxml"
                    )
            );

            Parent root = loader.load();

            DetailPageController controller =
                    loader.getController();

            controller.setWaterData(aqua);

            Stage stage = (Stage) tableView.getScene().getWindow();

            stage.setScene(
                    new Scene(root, 500, 400)
            );

            stage.setTitle("Details");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}