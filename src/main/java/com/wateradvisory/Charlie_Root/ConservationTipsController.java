package com.wateradvisory.Charlie_Root;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

public class ConservationTipsController {

    @FXML private Arc scoreArc;
    @FXML private Label scoreValueLabel;
    @FXML private Label scoreSubtitleLabel;
    @FXML private Label seasonalTipLabel;
    @FXML private VBox tipsContainer;
    @FXML private HBox resourcesContainer;

    @FXML
    private void onOpenChat(ActionEvent event) {
        SceneNavigator.goTo(event, "/Charlie_FXML/ChatView.fxml");
    }

    /**
     * Runs automatically once the FXML has finished loading.
     * Wire up your real data source here (e.g. call your usage-analysis
     * logic, then TipPhraser, then populate the screen).
     */
    @FXML
    public void initialize() {
        // Placeholder data. NEED TO REPLACE WITH COMPUTED VALUES
        setConservationScore(78, "Above average for your household size");
        setSeasonalTip("Seasonal tip: summer irrigation is driving 30% of your usage this month.");

        addTip("Your showers average 11 minutes, above the 8-minute recommendation.", "high", "Save ~35 L/wk");
        addTip("Tap usage spikes between 7-8am, consistent with a slow leak pattern.", "medium", "Check fixtures");
        addTip("Outdoor watering is timed well against evaporation for your area.", "low", "Keep it up");

        addResource("Leak checklist", "A 5-minute self-audit for common fixtures.", leakChecklistIcon());
        addResource("Rebate finder", "Local rebates for water-efficient fixtures.", rebateFinderIcon());
    }

    /** score out of 100 -- drives both the number and the ring's fill amount */
    public void setConservationScore(int score, String subtitle) {
        scoreValueLabel.setText(String.valueOf(score));
        scoreSubtitleLabel.setText(subtitle);
        double fraction = Math.max(0, Math.min(100, score)) / 100.0;
        scoreArc.setLength(-360.0 * fraction);
    }

    public void setSeasonalTip(String text) {
        seasonalTipLabel.setText(text);
    }

    /**
     * Adds one tip card to the tips section.
     * impactLevel must be "high", "medium", or "low" -- controls the tag style.
     */
    public void addTip(String sentence, String impactLevel, String savingsLabel) {
        Label sentenceLabel = new Label(sentence);
        sentenceLabel.setWrapText(true);
        sentenceLabel.getStyleClass().add("tip-text");

        Label impactTag = new Label(capitalize(impactLevel) + " impact");
        impactTag.getStyleClass().addAll("tag", impactTagStyleFor(impactLevel));

        VBox textColumn = new VBox(6, sentenceLabel, impactTag);
        textColumn.setFillWidth(true);

        Label savingsTag = new Label(savingsLabel);
        savingsTag.getStyleClass().addAll("tag", "tag-accent2");

        HBox card = new HBox(12, textColumn, savingsTag);
        card.getStyleClass().add("card");
        card.setStyle(card.getStyle() + "; -fx-alignment: CENTER_LEFT;");
        javafx.scene.layout.HBox.setHgrow(textColumn, javafx.scene.layout.Priority.ALWAYS);

        tipsContainer.getChildren().add(card);
    }

    /** "high" -> outline, "medium" -> accent, "low"/anything else -> neutral */
    private String impactTagStyleFor(String impactLevel) {
        return switch (impactLevel.toLowerCase()) {
            case "high" -> "tag-outline";
            case "medium" -> "tag-accent";
            default -> "tag-neutral";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    /** Adds one resource card (guide, regulation, or product link). */
    public void addResource(String title, String body) {
        addResource(title, body, null);
    }

    /** Adds one resource card with a leading icon (guide, regulation, or product link). */
    public void addResource(String title, String body, Node icon) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("resource-title");

        Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("resource-body");
        bodyLabel.setWrapText(true);

        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        if (icon != null) {
            card.getChildren().add(icon);
        }
        card.getChildren().addAll(titleLabel, bodyLabel);
        card.setPrefWidth(150);
        card.setMinWidth(150);

        resourcesContainer.getChildren().add(card);
    }

    private Node leakChecklistIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M4 19.5A2.5 2.5 0 0 1 6.5 17H20 M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z");
        path.getStyleClass().add("resource-icon");
        return path;
    }

    private Node rebateFinderIcon() {
        SVGPath pin = new SVGPath();
        pin.setContent("M12 22s8-4.5 8-11.8A8 8 0 0 0 4 10.2C4 17.5 12 22 12 22z");
        pin.getStyleClass().add("resource-icon");

        Circle dot = new Circle(12, 10, 3);
        dot.getStyleClass().add("resource-icon");

        return new Group(pin, dot);
    }

    /** Clears all tip cards -- useful if you refresh with new data. */
    public void clearTips() {
        tipsContainer.getChildren().clear();
    }
}
