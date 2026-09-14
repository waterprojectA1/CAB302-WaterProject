package com.wateradvisory;

import java.util.List;
import java.util.Map;

import com.wateradvisory.Charlie_Root.ChatSession;
import com.wateradvisory.Charlie_Root.NavShell;
import com.wateradvisory.Steve_Root.LeaderboardEntry;
import com.wateradvisory.Steve_Root.LeaderboardService;
import com.wateradvisory.database.AuthService;
import com.wateradvisory.database.HouseholdService;
import com.wateradvisory.database.UserSession;
import com.wateradvisory.database.WaterRecordService;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class App_Root {

    /** Same subtle fade duration ChatController uses for its "scroll to latest" button. */
    private static final Duration PREWARM_FADE = Duration.millis(180);

    @FXML
    private Text usernameText;

    @FXML
    private Text householdText;

    @FXML
    private HBox chatbotPrewarmBar;

    @FXML
    private Text waterTotalText;

    @FXML
    private Text todayTotalText;

    @FXML
    private Text weekTotalText;

    @FXML
    private VBox myDataCard;

    @FXML
    private VBox tipsCard;

    @FXML
    private VBox leaderboardCard;

    @FXML
    private Label leaderboardCardSubtitle;

    @FXML
    private VBox householdCard;

    @FXML
    private VBox waterRecordsCard;

    @FXML
    private VBox profileCard;

    @FXML
    private void initialize() {

        String username = AuthService.getUsername();

        usernameText.setText("Welcome back, " + username);

        if (HouseholdService.hasHousehold()) {
            String householdName = HouseholdService.getHouseholdName();
            int memberCount = HouseholdService.getHouseholdMembers().size();
            householdText.setText(householdName + "  \u00B7  " + memberCount
                    + (memberCount == 1 ? " member" : " members"));
        } else {
            householdText.setText("No household yet");
        }

        double waterTotal = WaterRecordService.getUserTotalWater();

        waterTotalText.setText("" + waterTotal + " L");

        Map<String, Double> summary =
                WaterRecordService.getUserWaterSummary();

        todayTotalText.setText(summary.getOrDefault("today", 0.0) + " L");
        weekTotalText.setText(summary.getOrDefault("week", 0.0) + " L");

        leaderboardCardSubtitle.setText(currentUserRankSubtitle());

        myDataCard.setOnMouseClicked(e -> NavShell.go(myDataCard, NavShell.Route.DAILY));
        tipsCard.setOnMouseClicked(e -> NavShell.go(tipsCard, NavShell.Route.TIPS));
        leaderboardCard.setOnMouseClicked(e -> NavShell.go(leaderboardCard, NavShell.Route.LEADERBOARD));
        householdCard.setOnMouseClicked(e -> NavShell.go(householdCard, NavShell.Route.HOUSEHOLD));
        waterRecordsCard.setOnMouseClicked(e -> NavShell.go(waterRecordsCard, NavShell.Route.DATA_TABLE));
        profileCard.setOnMouseClicked(e -> NavShell.go(profileCard, NavShell.Route.PROFILE));

        prewarmChatbot();
    }

    /**
     * Kicks off the Ripple chatbot's model load in the background as soon as Home
     * appears, so a later visit to the chat page finds it already loaded (or
     * loading) instead of starting cold. Purely a nicety -- Home stays fully
     * usable throughout; only a small bottom-docked "Preparing chatbot..." pill
     * (spinner + label) reflects the in-flight load, and it fades out on its own
     * once the model is ready.
     *
     * <p>Uses {@link ChatSession#ensureModelLoading()} rather than any bespoke
     * loading logic, so this can never race {@code ChatController}'s own
     * load-on-demand fallback: whichever screen calls it first actually starts
     * the load, and the other one just observes the same {@link javafx.concurrent.Task}.</p>
     */
    private void prewarmChatbot() {
        ChatSession session = ChatSession.getInstance();
        if (session.getModel() != null) {
            return;  // already loaded (e.g. a previous Home/Chat visit this run) -- never shown
        }

        chatbotPrewarmBar.setManaged(true);
        chatbotPrewarmBar.setVisible(true);
        chatbotPrewarmBar.setOpacity(1.0);

        session.ensureModelLoading(
            e -> fadeOutPrewarmBar(),
            e -> fadeOutPrewarmBar());
    }

    /**
     * Both success and failure fade the bar out the same way -- either way, the
     * load attempt is over. Reuses the same subtle 180ms fade
     * {@code ChatController}'s "scroll to latest" button uses; only once the fade
     * finishes is the bar fully removed from layout ({@code setVisible(false)} +
     * {@code setManaged(false)}), so it never leaves reserved empty space behind.
     */
    private void fadeOutPrewarmBar() {
        FadeTransition fade = new FadeTransition(PREWARM_FADE, chatbotPrewarmBar);
        fade.setFromValue(chatbotPrewarmBar.getOpacity());
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            chatbotPrewarmBar.setVisible(false);
            chatbotPrewarmBar.setManaged(false);
        });
        fade.play();
    }

    /**
     * "Nth \u00B7 P pts this season" for the current user, built from real
     * LeaderboardService data (already sorted by points descending). Never
     * fabricated -- falls back to "Not yet available" if there's no session,
     * no household, or the user isn't found in their own household's list.
     */
    private String currentUserRankSubtitle() {
        String userId = UserSession.getUserId();
        if (userId == null) {
            return "Not yet available";
        }

        List<LeaderboardEntry> entries = LeaderboardService.getHouseholdLeaderboard();
        for (int i = 0; i < entries.size(); i++) {
            if (userId.equals(entries.get(i).getUserId())) {
                return ordinal(i + 1) + " \u00B7 " + entries.get(i).getPoints()
                        + " pts this season";
            }
        }
        return "Not yet available";
    }

    private static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) {
            return n + "th";
        }
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }

    @FXML
    private void handleRecordWater(ActionEvent event) {
        NavShell.go(event, NavShell.Route.RECORD_WATER);
    }

}