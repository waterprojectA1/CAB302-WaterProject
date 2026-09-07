package com.wateradvisory.Charlie_Root;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.model.functions.Generator;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Simple free-form chat scene for testing the model directly --
 * this is a debugging/testing aid, not the same as TipPhraser
 * (which is tightly constrained to rephrasing pre-computed sentences).
 * Keep this scene separate from your graded "conservation opportunities"
 * feature unless you specifically want live chat as part of the app.
 */
public class ChatController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox conversationContainer;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private VBox headerBox;
    @FXML private Pane waveHolder;
    @FXML private SVGPath waveDivider;

    private AbstractModel model;
    private HBox loadingRow;
    private HBox typingRow;

    /** Live "how wide may a bubble be right now" value. Rebuilt from scrollPane.widthProperty() in {@link #initialize()}. */
    private DoubleBinding bubbleWidth;
    private final List<Animation> typingAnimations = new ArrayList<>();
    private final DoubleProperty wavePhase = new SimpleDoubleProperty(0);

    // Point this at whichever model folder jlama list showed you --
    // e.g. the quantized one used by TipPhraser. Models stayed put,
    // so this relative path is unaffected by the folder restructuring.
    private static final String MODEL_DIR = "./models/Qwen_Qwen2.5-1.5B-Instruct-JQ4";

    // Chat bubbles are sized as a fraction of the CURRENT chat-pane width (like a
    // real messaging app), through a live JavaFX binding -- never a one-time pixel
    // value -- so they keep re-flowing on every window resize, shrinking as well
    // as growing.
    private static final double BUBBLE_WIDTH_FRACTION = 0.78;
    private static final double BUBBLE_WIDTH_FLOOR = 150;      // first-paint / degenerate-size guard only
    private static final double CHAT_HORIZONTAL_PADDING = 44;  // .chat-container padding + vertical scrollbar allowance

    // Messages longer than this start collapsed, with a "more" toggle that expands
    // the bubble in place. Parameterised so it is easy to tune.
    private static final int MAX_COLLAPSED_CHARS = 500;
    private static final int COLLAPSE_WORD_LOOKBACK = 80;      // how far back to hunt for a word boundary when truncating

    // Wavelength/height stay fixed in pixels; the path is re-sampled out to headerBox's actual
    // width on every resize, so the wave always reaches edge to edge instead of being stretched
    // there with a scaleX transform. WAVE_FALLBACK_WIDTH only covers the first paint, before the
    // very first layout pass has given headerBox a real width.
    private static final double WAVE_FALLBACK_WIDTH = 420;
    private static final double WAVE_PERIOD = 210;
    private static final double WAVE_BASELINE = 14;
    private static final double WAVE_AMPLITUDE = 12;
    private static final double WAVE_STEP = 6;

    @FXML
    private void onBack(ActionEvent event) {
        SceneNavigator.goTo(event, "/Charlie_FXML/ConservationTipsView.fxml");
    }

    @FXML
    public void initialize() {
        // The wave divider's SVGPath is redrawn wider every time the header grows. A Shape is not
        // resizable, so its geometric width doubles as its minWidth; if that width could propagate
        // into headerBox's minWidth it would ratchet the whole window's minimum width upward and the
        // window (and every bubble inside the ScrollPane) would never shrink back. The <Pane> wrapper
        // in the FXML already stops that propagation; this explicit override is a second guard so
        // headerBox.minWidth() never consults its computed value at all.
        headerBox.setMinWidth(0);

        startWaveAnimation();

        // --- Live width model for the whole message area ------------------------
        // Let the conversation column collapse to ANY width (min = 0). Combined
        // with the ScrollPane's fitToWidth="true", that pins the VBox width to
        // exactly the current viewport width -- it can never stay stuck at a
        // previously larger size, so every row's alignment (CENTER / CENTER_LEFT /
        // CENTER_RIGHT) is recomputed against the real current width on shrink as
        // well as on grow.
        conversationContainer.setMinWidth(0);

        // One shared binding, derived straight from the ScrollPane's own width
        // property, which the layout pass updates on EVERY resize. Every bubble and
        // system message binds its maxWidth to this, so they all re-flow together.
        // Nothing here is a captured .getWidth() snapshot, so nothing can freeze.
        bubbleWidth = Bindings.max(
            BUBBLE_WIDTH_FLOOR,
            scrollPane.widthProperty()
                .subtract(CHAT_HORIZONTAL_PADDING)
                .multiply(BUBBLE_WIDTH_FRACTION));

        loadingRow = addSystemMessage("Loading Ripple, please wait...");

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                model = ModelSupport.loadModel(new File(MODEL_DIR), DType.F32, DType.I8);
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            conversationContainer.getChildren().remove(loadingRow);
            addSystemMessage("Ripple loaded. Ask it something.");
            addAiBubble("Hi! I'm Ripple. Ask me about your water usage, leak signs, or ways to cut back this season.");
            sendButton.setDisable(false);
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
            cause.printStackTrace();
            conversationContainer.getChildren().remove(loadingRow);
            addSystemMessage("Failed to load Ripple: "
                + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        });

        sendButton.setDisable(true);
        new Thread(loadTask).start();
    }

    @FXML
    private void onSend() {
        String userMessage = inputField.getText().trim();
        if (userMessage.isEmpty() || model == null) {
            return;
        }

        inputField.clear();
        sendButton.setDisable(true);
        addUserBubble(userMessage);
        addTypingIndicator();

        Task<String> generateTask = new Task<>() {
            @Override
            protected String call() {
                PromptContext ctx;
                if (model.promptSupport().isPresent()) {
                    ctx = model.promptSupport().get().builder()
                        .addUserMessage(userMessage)
                        .build();
                } else {
                    ctx = PromptContext.of(userMessage);
                }

                Generator.Response response = model.generate(
                    UUID.randomUUID(),
                    ctx,
                    0.7f,
                    300,
                    (token, time) -> { /* could stream tokens here later */ }
                );

                return response.responseText.trim();
            }
        };

        generateTask.setOnSucceeded(e -> {
            removeTypingIndicator();
            addAiBubble(generateTask.getValue());
            sendButton.setDisable(false);
        });

        generateTask.setOnFailed(e -> {
            removeTypingIndicator();
            addSystemMessage("Error: " + generateTask.getException().getMessage());
            sendButton.setDisable(false);
        });

        new Thread(generateTask).start();
    }

    /**
     * Drifts the header's wave divider sideways forever, looping seamlessly. The shape is
     * regenerated each frame from a sine function rather than reusing the static bezier path,
     * because a sine wave is exactly periodic -- shifting its phase by a full 2*PI lands back
     * on the identical shape, so the loop has no visible seam or jump.
     */
    private void startWaveAnimation() {
        if (waveDivider == null || waveHolder == null) {
            return;
        }

        // Keep an over-wide path (possible for a single frame mid-shrink, before redrawWave runs)
        // from painting outside the holder.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(waveHolder.widthProperty());
        clip.heightProperty().bind(waveHolder.heightProperty());
        waveHolder.setClip(clip);

        wavePhase.addListener((obs, oldValue, newValue) -> redrawWave());
        // Drive the redraw off the holder's width, not headerBox's -- the holder is the node that
        // actually tracks the header width now, and it is free to shrink.
        waveHolder.widthProperty().addListener((obs, oldValue, newValue) -> redrawWave());
        redrawWave();

        Timeline waveTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(wavePhase, 0, Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(6), new KeyValue(wavePhase, 2 * Math.PI, Interpolator.LINEAR))
        );
        waveTimeline.setCycleCount(Animation.INDEFINITE);
        waveTimeline.play();
    }

    /** Redraws the wave divider at the holder's current actual width and the animation's current phase. */
    private void redrawWave() {
        double width = waveHolder.getWidth() > 0 ? waveHolder.getWidth() : WAVE_FALLBACK_WIDTH;
        waveDivider.setContent(buildWavePath(width, wavePhase.get()));
    }

    /** Builds the wave divider's SVG path spanning the given width, as a polyline sampling a sine curve. */
    private String buildWavePath(double width, double phase) {
        StringBuilder path = new StringBuilder();
        path.append(String.format(Locale.ROOT, "M0,%.2f", waveY(0, phase)));
        for (double x = WAVE_STEP; x <= width; x += WAVE_STEP) {
            path.append(String.format(Locale.ROOT, " L%.2f,%.2f", x, waveY(x, phase)));
        }
        path.append(String.format(Locale.ROOT, " L%.2f,28 L0,28 Z", width));
        return path.toString();
    }

    private double waveY(double x, double phase) {
        return WAVE_BASELINE + WAVE_AMPLITUDE * Math.sin((2 * Math.PI * x / WAVE_PERIOD) + phase);
    }

    private void addUserBubble(String text) {
        addBubble(text, "bubble-user", Pos.CENTER_RIGHT);
    }

    private void addAiBubble(String text) {
        addBubble(text, "bubble-ai", Pos.CENTER_LEFT);
    }

    /**
     * Builds one chat bubble and adds it as a full-width row.
     *
     * <ul>
     *   <li>The bubble's maxWidth is BOUND (not assigned once) to {@link #bubbleWidth},
     *       so it keeps re-flowing on every window resize -- narrower as well as wider.</li>
     *   <li>The label wraps inside that width, so a long message never widens the window;
     *       it grows downward and the ScrollPane handles vertical scrolling.</li>
     *   <li>The row itself is left unconstrained (maxWidth = MAX_VALUE) so the VBox stretches
     *       it edge to edge and {@code side} alignment lands the bubble against the correct
     *       edge / dead centre, measured against the live width.</li>
     *   <li>Messages over {@link #MAX_COLLAPSED_CHARS} chars start collapsed with a "more"
     *       toggle that expands the bubble in place.</li>
     * </ul>
     */
    private void addBubble(String text, String bubbleStyleClass, Pos side) {
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add(bubbleStyleClass);
        bubble.setMinWidth(0);
        bubble.maxWidthProperty().bind(bubbleWidth);

        Label body = new Label();
        body.setWrapText(true);
        body.setMinWidth(0);
        bubble.getChildren().add(body);

        if (text.length() > MAX_COLLAPSED_CHARS) {
            String collapsed = collapse(text);
            body.setText(collapsed);

            Hyperlink toggle = new Hyperlink("more ›");
            toggle.getStyleClass().add("bubble-more");
            toggle.setFocusTraversable(false);

            HBox toggleRow = new HBox(toggle);
            toggleRow.setAlignment(Pos.CENTER_RIGHT);
            bubble.getChildren().add(toggleRow);

            final boolean[] expanded = {false};
            toggle.setOnAction(e -> {
                expanded[0] = !expanded[0];
                body.setText(expanded[0] ? text : collapsed);
                toggle.setText(expanded[0] ? "less ‹" : "more ›");
                toggle.setVisited(false);
            });
        } else {
            body.setText(text);
        }

        HBox row = new HBox(bubble);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(side);
        addRow(row);
    }

    /** Cuts {@code text} down to ~{@link #MAX_COLLAPSED_CHARS} chars, backing up to a word boundary, plus an ellipsis. */
    private static String collapse(String text) {
        int cut = MAX_COLLAPSED_CHARS;
        int lastSpace = text.lastIndexOf(' ', cut);
        if (lastSpace >= MAX_COLLAPSED_CHARS - COLLAPSE_WORD_LOOKBACK) {
            cut = lastSpace;
        }
        return text.substring(0, cut).stripTrailing() + "…";
    }

    /** Shows an animated "typing" bubble (three dots bouncing in a wave) while Ripple is generating a reply (like a typical messaging platform). */
    private void addTypingIndicator() {
        Circle dot1 = new Circle(4);
        Circle dot2 = new Circle(4);
        Circle dot3 = new Circle(4);
        dot1.getStyleClass().add("typing-dot");
        dot2.getStyleClass().add("typing-dot");
        dot3.getStyleClass().add("typing-dot");

        HBox dots = new HBox(6, dot1, dot2, dot3);
        dots.setAlignment(Pos.CENTER_LEFT);

        HBox bubble = new HBox(dots);
        bubble.getStyleClass().add("bubble-typing");

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);

        animateDotWave(dot1, 0);
        animateDotWave(dot2, 120);
        animateDotWave(dot3, 240);

        typingRow = row;
        addRow(row);
    }

    /** Bounces one typing dot up and down indefinitely; staggered delays across the three dots create the wave. */
    private void animateDotWave(Circle dot, int delayMs) {
        TranslateTransition bounce = new TranslateTransition(Duration.millis(420), dot);
        bounce.setByY(-5);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(Animation.INDEFINITE);
        bounce.setInterpolator(Interpolator.EASE_BOTH);
        bounce.setDelay(Duration.millis(delayMs));
        bounce.play();
        typingAnimations.add(bounce);
    }

    private void removeTypingIndicator() {
        if (typingRow == null) {
            return;
        }
        for (Animation animation : typingAnimations) {
            animation.stop();
        }
        typingAnimations.clear();
        conversationContainer.getChildren().remove(typingRow);
        typingRow = null;
    }

    private HBox addSystemMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("bubble-system");
        label.setWrapText(true);
        label.setMinWidth(0);
        label.maxWidthProperty().bind(bubbleWidth);
        HBox row = new HBox(label);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.CENTER);
        addRow(row);
        return row;
    }

    private void addRow(HBox row) {
        conversationContainer.getChildren().add(row);
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
