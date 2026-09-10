package com.wateradvisory.Jainya_Root.Controllers;

import com.wateradvisory.database.WaterRecordService;
import com.wateradvisory.water.WaterUsageEntry;
import com.wateradvisory.water.WaterUsageStats;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.util.*;

public class CompareController {

    @FXML private DatePicker p1Start;
    @FXML private DatePicker p1End;
    @FXML private DatePicker p2Start;
    @FXML private DatePicker p2End;
    @FXML private Label errorLabel;
    @FXML private Label p1Value;
    @FXML private Label p2Value;
    @FXML private Label changeBadge;
    @FXML private Label insightLabel;

    @FXML
    public void initialize() {
        LocalDate p2EndDate = LocalDate.now();
        LocalDate p2StartDate = p2EndDate.minusDays(29);
        LocalDate p1EndDate = p2StartDate.minusDays(1);
        LocalDate p1StartDate = p1EndDate.minusDays(29);

        p1Start.setValue(p1StartDate);
        p1End.setValue(p1EndDate);
        p2Start.setValue(p2StartDate);
        p2End.setValue(p2EndDate);

        onCompare();
    }

    @FXML
    private void onCompare() {
        errorLabel.setText("");
        LocalDate s1 = p1Start.getValue(), e1 = p1End.getValue();
        LocalDate s2 = p2Start.getValue(), e2 = p2End.getValue();
        if (s1 == null || e1 == null || s2 == null || e2 == null) return;

        if (s1.isAfter(e1) || s2.isAfter(e2)) {
            errorLabel.setText("Each period needs a start date before its end date.");
            return;
        }

        List<WaterUsageEntry> ent1 = WaterRecordService.getDailyUsageEntries(s1, e1);
        List<WaterUsageEntry> ent2 = WaterRecordService.getDailyUsageEntries(s2, e2);
        WaterUsageStats.Stats stats1 = WaterUsageStats.computeStats(ent1);
        WaterUsageStats.Stats stats2 = WaterUsageStats.computeStats(ent2);

        p1Value.setText(stats1.average != null ? stats1.average + " L" : "No data");
        p2Value.setText(stats2.average != null ? stats2.average + " L" : "No data");

        if (stats1.average == null || stats2.average == null) {
            changeBadge.setText("");
            changeBadge.getStyleClass().removeAll("up", "down");
            insightLabel.setText("Not enough recorded data in one of the selected periods to compare.");
            return;
        }

        int diff = stats2.average - stats1.average;
        int pctChange = stats1.average == 0 ? 0 : Math.round((diff / (float) stats1.average) * 100);

        String arrow = "\u00b1";
        changeBadge.getStyleClass().removeAll("up", "down");
        if (pctChange < -1) {
            arrow = "\u2193"; // down = using less water = improvement
            changeBadge.getStyleClass().add("down");
        } else if (pctChange > 1) {
            arrow = "\u2191"; // up = using more water
            changeBadge.getStyleClass().add("up");
        }

        changeBadge.setText(arrow + " " + Math.abs(pctChange) + "%");

        Map<WaterUsageStats.Season, Integer> s1Seasons = seasonTotals(ent1);
        Map<WaterUsageStats.Season, Integer> s2Seasons = seasonTotals(ent2);

        WaterUsageStats.Season biggest = null;
        int biggestDelta = 0;
        for (WaterUsageStats.Season season : WaterUsageStats.Season.values()) {
            int delta = s2Seasons.get(season) - s1Seasons.get(season);
            if (Math.abs(delta) > Math.abs(biggestDelta)) {
                biggestDelta = delta;
                biggest = season;
            }
        }

        String direction = pctChange <= -1 ? "improved" : (pctChange >= 1 ? "increased" : "stayed about the same");
        StringBuilder sb = new StringBuilder();
        sb.append("Average daily usage has ").append(direction)
                .append(" by ").append(Math.abs(pctChange)).append("% between the two periods.");
        if (biggest != null && biggestDelta != 0) {
            sb.append(" ").append(capitalize(biggest.name()))
                    .append(" usage contributed the biggest ")
                    .append(biggestDelta > 0 ? "increase" : "decrease")
                    .append(" between periods.");
        }
        insightLabel.setText(sb.toString());
    }

    private Map<WaterUsageStats.Season, Integer> seasonTotals(List<WaterUsageEntry> entries) {
        Map<WaterUsageStats.Season, Integer> totals = new EnumMap<>(WaterUsageStats.Season.class);
        for (WaterUsageStats.Season s : WaterUsageStats.Season.values()) totals.put(s, 0);
        for (WaterUsageEntry e : entries) {
            WaterUsageStats.Season s = WaterUsageStats.seasonForMonth(e.getDate().getMonthValue());
            totals.put(s, totals.get(s) + e.getLitres());
        }
        return totals;
    }

    private String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}