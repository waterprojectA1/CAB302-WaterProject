package com.wateradvisory.Jainya_Root.Controllers;

import com.wateradvisory.database.WaterRecordService;
import com.wateradvisory.water.WaterUsageEntry;
import com.wateradvisory.water.WaterUsageStats;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DailyController {

    @FXML private BarChart<String, Number> dailyChart;
    @FXML private CategoryAxis xAxis;
    @FXML private Label rangeLabel;
    @FXML private Label avgValue;
    @FXML private Label highValue;
    @FXML private Label lowValue;
    @FXML private DatePicker startPicker;
    @FXML private DatePicker endPicker;
    @FXML private Label errorLabel;

    private static final DateTimeFormatter AXIS_FMT = DateTimeFormatter.ofPattern("dd MMM yy");
    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private LocalDate rangeStart;
    private LocalDate rangeEnd;

    @FXML
    public void initialize() {
        rangeEnd = LocalDate.now();
        rangeStart = rangeEnd.minusDays(29); // default: last 30 days
        startPicker.setValue(rangeStart);
        endPicker.setValue(rangeEnd);
        xAxis.setTickLabelRotation(90);
        render();
    }

    @FXML
    private void onApplyRange() {
        errorLabel.setText("");
        LocalDate s = startPicker.getValue();
        LocalDate e = endPicker.getValue();

        if (s == null || e == null) {
            errorLabel.setText("Please choose both a start and end date.");
            return;
        }
        if (s.isAfter(e)) {
            errorLabel.setText("Start date must be before end date.");
            return;
        }
        LocalDate today = LocalDate.now();
        if (s.isAfter(today) || e.isAfter(today)) {
            errorLabel.setText("Dates cannot be in the future.");
            return;
        }

        rangeStart = s;
        rangeEnd = e;
        render();
    }

    private void render() {
        List<WaterUsageEntry> entries = WaterRecordService.getDailyUsageEntries(rangeStart, rangeEnd);
        WaterUsageStats.Stats stats = WaterUsageStats.computeStats(entries);

        avgValue.setText(stats.average != null ? stats.average + " L" : "No data");
        highValue.setText(stats.highest != null ? stats.highest.getLitres() + " L" : "No data");
        lowValue.setText(stats.lowest != null ? stats.lowest.getLitres() + " L" : "No data");

        highValue.setTooltip(stats.highest != null ? new Tooltip(stats.highest.getDate().format(FULL_FMT)) : null);
        lowValue.setTooltip(stats.lowest != null ? new Tooltip(stats.lowest.getDate().format(FULL_FMT)) : null);

        rangeLabel.setText(rangeStart.format(FULL_FMT) + "  \u2013  " + rangeEnd.format(FULL_FMT)
                + "   (" + entries.size() + " day" + (entries.size() == 1 ? "" : "s") + " recorded)");

        dailyChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (WaterUsageEntry entry : entries) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getDate().format(AXIS_FMT), entry.getLitres());
            series.getData().add(data);
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, new Tooltip(entry.getDate().format(FULL_FMT) + ":  " + entry.getLitres() + " L"));
                }
            });
        }
        dailyChart.getData().add(series);
    }
}