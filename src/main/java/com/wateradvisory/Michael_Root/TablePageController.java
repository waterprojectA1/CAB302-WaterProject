package com.wateradvisory.Michael_Root;
import com.wateradvisory.Charlie_Root.NavShell;
import com.wateradvisory.database.UserSession;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
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
    private TableColumn<WaterData, String> userIDColumn;

    @FXML
    private ToggleButton dailyRadio;

    @FXML
    private ToggleButton weeklyRadio;

    @FXML
    private ToggleButton monthlyRadio;

    @FXML
    private void handleReturnToMain(ActionEvent event) {
        NavShell.go(event, NavShell.Route.HOME);
    }

    @FXML
    private void handleGoToNotification(ActionEvent event) {
        NavShell.go(event, NavShell.Route.NOTIFICATIONS);
    }

    @FXML
    private void handleGoToGraph(ActionEvent event) {
        NavShell.go(event, NavShell.Route.DAILY);
    }


    private WaterDataList model = new WaterDataList();

    private String loggedUser = UserSession.getUserId();

    private FilteredList<WaterData> filteredDailyWater;
    private FilteredList<WaterData> filteredWeeklyWater;
    private FilteredList<WaterData> filteredMonthlyWater;

    public void initialize(){


        filteredDailyWater = new FilteredList<>(model.getDailyWater(), water -> true);
        filteredWeeklyWater = new FilteredList<>(model.getWeeklyWater(), water -> true);
        filteredMonthlyWater = new FilteredList<>(model.getMonthlyWater(), water -> true);
        filteredDailyWater.setPredicate(water -> water.getUserID() == loggedUser);

        setupTable();
        setupToggleButtons();

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
    private void setupToggleButtons() {

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

            Scene scene = tableView.getScene();
            scene.setRoot(NavShell.wrap(root, NavShell.Route.DETAIL));

            if (scene.getWindow() instanceof Stage stage) {
                stage.sizeToScene();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}