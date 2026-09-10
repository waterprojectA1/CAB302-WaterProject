package com.wateradvisory.Jainya_Root.Controllers;

import com.wateradvisory.database.WaterRecordService;
import com.wateradvisory.water.WaterUsageEntry;
import com.wateradvisory.water.WaterUsageStats;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SeasonalController {

    @FXML private ToggleButton totalToggle;
    @FXML private ToggleButton avgToggle;
    @FXML private BarChart<String, Number> seasonChart;
    @FXML private Label rangeLabel;
    @FXML private DatePicker startPicker;
    @FXML private DatePicker endPicker;
    @FXML private Label errorLabel;

    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private LocalDate rangeStart;
    private LocalDate rangeEnd;
    private boolean showTotal = true;

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        totalToggle.setToggleGroup(group);
        avgToggle.setToggleGroup(group);
        totalToggle.setSelected(true);

        totalToggle.setOnAction(e -> { showTotal = true; render(); });
        avgToggle.setOnAction(e -> { showTotal = false; render(); });

        rangeEnd = LocalDate.now();
        rangeStart = rangeEnd.minusDays(29);
        startPicker.setValue(rangeStart);
        endPicker.setValue(rangeEnd);
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

        Map<WaterUsageStats.Season, List<Integer>> buckets = new LinkedHashMap<>();
        for (WaterUsageStats.Season s : WaterUsageStats.Season.values()) {
            buckets.put(s, new ArrayList<>());
        }
        for (WaterUsageEntry en : entries) {
            buckets.get(WaterUsageStats.seasonForMonth(en.getDate().getMonthValue())).add(en.getLitres());
        }

        seasonChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (Map.Entry<WaterUsageStats.Season, List<Integer>> entry : buckets.entrySet()) {
            List<Integer> vals = entry.getValue();
            int total = vals.stream().mapToInt(Integer::intValue).sum();
            int avg = vals.isEmpty() ? 0 : Math.round((float) total / vals.size());
            int metricVal = showTotal ? total : avg;
            int count = vals.size();
            String label = capitalize(entry.getKey().name());

            XYChart.Data<String, Number> data = new XYChart.Data<>(label, metricVal);
            series.getData().add(data);
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, new Tooltip(label + ":  " + metricVal + " L  (" + count + " days recorded)"));
                }
            });
        }
        seasonChart.getData().add(series);

        rangeLabel.setText(rangeStart.format(FULL_FMT) + "  \u2013  " + rangeEnd.format(FULL_FMT)
                + "   (" + entries.size() + " day" + (entries.size() == 1 ? "" : "s") + " recorded)");
    }

    private String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}