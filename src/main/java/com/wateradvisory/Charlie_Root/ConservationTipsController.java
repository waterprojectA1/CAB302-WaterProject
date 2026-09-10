package com.wateradvisory.Charlie_Root;

import java.util.List;
import java.util.Locale;

import com.wateradvisory.Michael_Root.WaterDataList;
import com.wateradvisory.database.WaterRecordService;
import com.wateradvisory.water.WaterActivityEntry;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;

public class ConservationTipsController {

    @FXML private Arc scoreArc;
    @FXML private Label scoreValueLabel;
    @FXML private Label scoreSubtitleLabel;
    @FXML private Label seasonalTipLabel;
    @FXML private VBox tipsContainer;
    @FXML private HBox resourcesContainer;

    /**
     * Brisbane water usage charge in dollars per litre (~$3.57 per kilolitre).
     * Single source of truth for every tip's cost estimate -- change the tariff here.
     */
    public static final double WATER_RATE_PER_LITRE = 0.00357;

    /** Whose data to display. TODO: read from UserSession once the login flow is wired through. */
    private static final int CURRENT_USER_ID = 1;

    /** Drives seasonal-tip hemisphere detection. TODO: read from the household profile when available. */
    private static final String USER_REGION = "Brisbane, AU";

    /** Cap on personalised tip cards shown, so the page stays scannable (ranked, highest impact first). */
    private static final int MAX_TIPS_SHOWN = 4;

    private static final String SCORE_HELP_TITLE = "How is your score calculated?";
    private static final String SCORE_HELP_BODY =
          "Your Conservation Score starts at 50 and updates each day based on how your water "
        + "usage compares to the previous day.\n\n"
        + "If you use less water than yesterday, your score increases (up to +10 points). If you "
        + "use more, it decreases (up to -10 points). The score is always kept between 0 and 100.\n\n"
        + "Your estimated savings are calculated using your current usage compared to recommended "
        + "benchmarks, multiplied by your local water rate ($3.57 per kilolitre).";

    @FXML
    private void onOpenChat(ActionEvent event) {
        SceneNavigator.goTo(event, "/Charlie_FXML/ChatView.fxml");
    }

    /**
     * Runs automatically once the FXML has finished loading. Wires the screen to the
     * in-memory usage model: real conservation score, per-tip savings estimates, and a
     * seasonal tip that rotates once per day.
     */
    @FXML
    public void initialize() {
        WaterDataList data = new WaterDataList();

        // 1. Conservation score -- real calculation (daily granularity for now).
        ConservationScoreCalculator scoreCalculator = new ConservationScoreCalculator();
        int score = scoreCalculator.calculateDailyScore(
            CURRENT_USER_ID, ConservationScoreCalculator.STARTING_SCORE, data);
        setConservationScore(score, subtitleForScore(score));

        // 3. Seasonal tip -- deterministic, rotates once per calendar day (never random).
        SeasonalTipProvider seasonalTips = new SeasonalTipProvider(USER_REGION);
        setSeasonalTip("Seasonal tip: " + seasonalTips.getTipForToday(USER_REGION));

        // 2. Personalised tips -- generated from the user's REAL recorded data:
        //    logged activities from Supabase (shower length, laundry frequency,
        //    category share) plus aggregate weekly trend / outlier signals from
        //    WaterDataList. No time-of-day tips: the schema has no per-activity
        //    timestamp yet (see PersonalizedTipGenerator / CLAUDE.md gotcha).
        List<WaterActivityEntry> activities = WaterRecordService.getUserActivities(CURRENT_USER_ID);
        PersonalizedTipGenerator tipGenerator = new PersonalizedTipGenerator(WATER_RATE_PER_LITRE);
        List<PersonalizedTipGenerator.TipCandidate> tips =
            tipGenerator.generate(CURRENT_USER_ID, activities, data);

        if (tips.isEmpty()) {
            // Brand-new user with nothing logged yet -- never leave the section blank.
            addFallbackTip("Start logging your water usage to get personalised tips as your history builds up.");
        } else {
            tips.stream().limit(MAX_TIPS_SHOWN).forEach(tip ->
                addTip(tip.sentence(), tip.impact().name().toLowerCase(Locale.ROOT),
                    tip.litresSavedPerWeek(), tip.costSavedPerWeek()));
        }

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

    /** Subtitle text for each score band. */
    private static String subtitleForScore(int score) {
        if (score >= 80) return "Excellent -- well above average conservation";
        if (score >= 60) return "Good -- above average for your household size";
        if (score >= 40) return "Average -- some room to improve";
        return "Needs improvement -- see tips below";
    }

    public void setSeasonalTip(String text) {
        seasonalTipLabel.setText(text);
    }

    /**
     * Adds one tip card to the tips section.
     * impactLevel must be "high", "medium", or "low" -- controls the tag style.
     * The weekly litres/cost saving is shown as small muted text under the impact tag,
     * but only when there is actually something to save (a positive-reinforcement tip
     * passes 0 and gets no savings line).
     */
    public void addTip(String sentence, String impactLevel,
                       double litresSavedPerWeek, double costSavedPerWeek) {
        Label sentenceLabel = new Label(sentence);
        sentenceLabel.setWrapText(true);
        sentenceLabel.getStyleClass().add("tip-text");

        Label impactTag = new Label(capitalize(impactLevel) + " impact");
        impactTag.getStyleClass().addAll("tag", impactTagStyleFor(impactLevel));

        VBox textColumn = new VBox(6, sentenceLabel, impactTag);
        textColumn.setFillWidth(true);

        if (litresSavedPerWeek > 0) {
            Label savingsLabel = new Label(formatSavings(litresSavedPerWeek, costSavedPerWeek));
            savingsLabel.getStyleClass().add("text-muted");
            savingsLabel.setStyle("-fx-font-size: 11px;");
            savingsLabel.setWrapText(true);
            textColumn.getChildren().add(savingsLabel);
        }

        HBox card = new HBox(12, textColumn);
        card.getStyleClass().add("card");
        card.setStyle(card.getStyle() + "; -fx-alignment: CENTER_LEFT;");
        HBox.setHgrow(textColumn, Priority.ALWAYS);

        tipsContainer.getChildren().add(card);
    }

    /**
     * Single plain card shown when {@link PersonalizedTipGenerator} produced nothing
     * (e.g. a brand-new user with no logged activity and no usage history) -- so the
     * personalised tips section is never just an unexplained blank.
     */
    private void addFallbackTip(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.getStyleClass().add("tip-text");

        HBox card = new HBox(label);
        card.getStyleClass().add("card");
        card.setStyle(card.getStyle() + "; -fx-alignment: CENTER_LEFT;");
        HBox.setHgrow(label, Priority.ALWAYS);

        tipsContainer.getChildren().add(card);
    }

    /** e.g. "Save ~35 L/wk &#183; ~$0.12/wk" -- litres to the nearest whole, cost to two decimals. */
    private static String formatSavings(double litresSavedPerWeek, double costSavedPerWeek) {
        return String.format(Locale.ROOT, "Save ~%d L/wk · ~$%.2f/wk",
            Math.round(litresSavedPerWeek), costSavedPerWeek);
    }

    /**
     * Opens the "how is your score calculated" help dialog. Styled to the app design
     * system via app-extra.css (the {@code .score-help-dialog} rules repaint the default
     * JavaFX chrome in {@code -color-bg} / {@code -color-text}).
     */
    @FXML
    private void onShowScoreHelp(ActionEvent event) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(SCORE_HELP_TITLE);
        dialog.initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(getClass().getResource("/Charlie_FXML/app.css").toExternalForm());
        pane.getStyleClass().addAll("root", "score-help-dialog");

        Label title = new Label(SCORE_HELP_TITLE);
        title.getStyleClass().add("score-title");
        title.setWrapText(true);

        Label body = new Label(SCORE_HELP_BODY);
        body.getStyleClass().add("tip-text");
        body.setWrapText(true);

        VBox content = new VBox(10, title, body);
        content.setPrefWidth(380);
        content.setMaxWidth(380);
        pane.setContent(content);

        ButtonType gotIt = new ButtonType("Got it", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().add(gotIt);
        Node gotItButton = pane.lookupButton(gotIt);
        gotItButton.getStyleClass().add("btn");

        dialog.showAndWait();
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
