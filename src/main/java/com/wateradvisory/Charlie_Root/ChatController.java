package com.wateradvisory.Charlie_Root;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.model.functions.Generator;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import com.vladsch.flexmark.util.data.MutableDataSet;

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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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

    // Real chat-template "system" turn (see onSend). Qwen renders this into its own
    // <|im_start|>system ... <|im_end|> block, so it is a genuine system instruction,
    // NOT prepended user text. It tells the model to answer as "Rippl" and, crucially,
    // to never open with a self-description -- that identity sentence
    // ("You are Qwen, created by Alibaba Cloud...") was the model echoing its default
    // system prompt into its own output. stripLeadingPreamble() is the defensive
    // second line of defence if the model ignores this.
    private static final String SYSTEM_PROMPT =
          "You are Rippl, a friendly water conservation assistant built into the Water Advisory app. "
        + "Answer the user's question directly and helpfully. "
        + "Never introduce yourself, describe your origins, or mention Alibaba, Qwen, or any other model name. "
        + "Never wrap your response in quotation marks. "
        + "Keep answers concise but complete.";

    // Was 300 -- too low, responses were cut off mid-sentence. 1024 gives a structured
    // answer (header + a short list + a closing line) room to finish. isLikelyTruncated()
    // still guards the case where even this runs out.
    private static final int MAX_RESPONSE_TOKENS = 1024;

    // Shared flexmark parser. Instances are immutable + thread-safe once built, and parsing
    // happens on the FX thread anyway (in generateTask.setOnSucceeded -> addAiBubble), so one
    // static instance is fine. Plain CommonMark config -- no extensions needed for the
    // bold / italic / list / heading subset the model produces.
    private static final Parser MARKDOWN_PARSER = Parser.builder(new MutableDataSet()).build();

    // Base body size for AI-bubble Text runs, matched to ".bubble-ai .label" in app.css so
    // TextFlow spans and plain Labels line up.
    private static final double BODY_FONT_SIZE = 13.5;

    // Leading "identity preamble" the model sometimes emits before the real answer.
    // Deliberately NARROW: it only fires on "You are / I am / I'm ..." sentences that
    // ALSO name a model/vendor or call themselves an assistant, so a genuine answer
    // that happens to start with "You are using 60 L per shower." is left alone.
    private static final Pattern IDENTITY_PREAMBLE = Pattern.compile(
          "^\\s*[\"'“‘]?\\s*(?:you are|i am|i'm)\\s+[^.!?\\n]*?"
        + "(?:qwen|alibaba|openai|anthropic|a language model|an ai language model|"
        + "a large language model|an ai assistant|a helpful assistant|"
        + "created by|developed by|trained by|built by)"
        + "[^.!?\\n]*[.!?]\\s*",
        Pattern.CASE_INSENSITIVE);

    // Generic "As an AI / As a language model ..." disclaimer opener.
    private static final Pattern AI_DISCLAIMER = Pattern.compile(
        "^\\s*as an?\\s+(?:ai|language model|large language model)[^.!?\\n]*[.!?]\\s*",
        Pattern.CASE_INSENSITIVE);

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
                        .addSystemMessage(SYSTEM_PROMPT)
                        .addUserMessage(userMessage)
                        .build();
                } else {
                    // No chat template -- fold the system instruction into the plain prompt.
                    ctx = PromptContext.of(SYSTEM_PROMPT + "\n\n" + userMessage);
                }

                Generator.Response response = model.generate(
                    UUID.randomUUID(),
                    ctx,
                    0.7f,
                    MAX_RESPONSE_TOKENS,
                    (token, time) -> { /* could stream tokens here later */ }
                );

                // Order matters (see task): preamble strip -> quote strip -> truncation check.
                // Format detection / structured rendering then happens in addAiBubble().
                return postProcess(response.responseText, response.finishReason);
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

    /**
     * Renders one AI reply as formatted content inside a {@code .bubble-ai} container.
     *
     * <p>The model emits Markdown ({@code **bold**}, {@code *italic*}, {@code "- "} / {@code "1. "}
     * lists, {@code "## "} headings). Rather than pattern-matching that with regex -- which cannot
     * cope with inline mixed formatting such as "a sentence with one <b>bold</b> word" -- the text
     * is parsed to a real Markdown AST by flexmark, and this method walks that AST once, mapping
     * each node to a JavaFX node:</p>
     *
     * <ul>
     *   <li><b>Heading</b> ({@code ## ...}), or a short {@code "caption:"} paragraph that is
     *       followed by more content -&gt; a {@link Label} with the {@code score-title} style class.</li>
     *   <li><b>Bullet / ordered list</b> -&gt; a {@link VBox} of rows; each row is a marker
     *       {@link Label} ({@code list-marker}, coloured {@code -color-accent2}) beside the item's
     *       own rendered content. The raw {@code -} / {@code *} / {@code 1.} characters never appear.</li>
     *   <li><b>Paragraph of only plain text</b> -&gt; one wrapped {@link Label} in {@code -color-text}.</li>
     *   <li><b>Paragraph with inline formatting</b> -&gt; a {@link TextFlow} whose children are
     *       individually-styled {@link Text} runs -- a single Label cannot mix styles across spans.</li>
     * </ul>
     *
     * <p>Identity-preamble / quote stripping (see {@link #postProcess}) has already run on
     * {@code markdown} before it reaches here.</p>
     */
    private void addAiBubble(String markdown) {
        String md = (markdown == null) ? "" : markdown;

        VBox bubble = new VBox(6);
        bubble.getStyleClass().add("bubble-ai");
        bubble.setMinWidth(0);
        bubble.setFillWidth(true);
        bubble.maxWidthProperty().bind(bubbleWidth);

        Node document = MARKDOWN_PARSER.parse(md);
        appendBlockNodes(bubble.getChildren(), document);

        // Degenerate input (blank, or only unsupported node types) -- fall back to raw text so
        // the bubble is never empty.
        if (bubble.getChildren().isEmpty()) {
            bubble.getChildren().add(plainLabel(md.strip()));
        }

        HBox row = new HBox(bubble);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.CENTER_LEFT);
        addRow(row);
    }

    /** Walks the block-level children of {@code parent}, appending one JavaFX node per block. */
    private void appendBlockNodes(List<javafx.scene.Node> out, Node parent) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNext()) {
            javafx.scene.Node fx = renderBlock(child);
            if (fx != null) {
                out.add(fx);
            }
        }
    }

    /** Maps one block-level flexmark node to a JavaFX node (or {@code null} to skip it). */
    private javafx.scene.Node renderBlock(Node node) {
        if (node instanceof Heading heading) {
            return headerLabel(collectText(heading));
        }
        if (node instanceof Paragraph paragraph) {
            String plain = collectText(paragraph);
            if (looksLikeCaptionHeader(plain, node.getNext() != null)) {
                return headerLabel(plain);
            }
            return isPlainInline(paragraph) ? plainLabel(plain) : buildTextFlow(paragraph);
        }
        if (node instanceof BulletList bulletList) {
            return buildList(bulletList, false);
        }
        if (node instanceof OrderedList orderedList) {
            return buildList(orderedList, true);
        }
        if (node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock) {
            return codeBlock(node);
        }
        if (node instanceof BlockQuote blockQuote) {
            VBox quoted = new VBox(4);
            quoted.setMinWidth(0);
            quoted.setFillWidth(true);
            quoted.setStyle("-fx-padding: 0 0 0 10; -fx-border-color: -color-divider; "
                + "-fx-border-width: 0 0 0 2;");
            appendBlockNodes(quoted.getChildren(), blockQuote);
            return quoted;
        }
        if (node instanceof ThematicBreak) {
            Region rule = new Region();
            rule.setMinHeight(1);
            rule.setPrefHeight(1);
            rule.setMaxWidth(Double.MAX_VALUE);
            rule.setStyle("-fx-background-color: -color-divider;");
            return rule;
        }
        // Anything else (tables, raw HTML, ...): render its plain text so nothing is silently lost.
        String fallback = collectText(node);
        return fallback.isBlank() ? null : plainLabel(fallback);
    }

    /** Bullet / ordered list -&gt; a VBox of "[marker] [content]" rows. */
    private VBox buildList(Node listNode, boolean ordered) {
        VBox box = new VBox(4);
        box.setMinWidth(0);
        box.setFillWidth(true);

        int number = ordered ? ((OrderedList) listNode).getStartNumber() : 0;
        for (Node item = listNode.getFirstChild(); item != null; item = item.getNext()) {
            if (!(item instanceof ListItem)) {
                continue;
            }
            String markerText = ordered ? (number++ + ".") : "•";

            Label marker = new Label(markerText);
            marker.getStyleClass().add("list-marker");
            marker.setMinWidth(Region.USE_PREF_SIZE);   // marker column never wraps or clips

            VBox content = new VBox(4);
            content.setMinWidth(0);
            content.setFillWidth(true);
            HBox.setHgrow(content, Priority.ALWAYS);
            appendBlockNodes(content.getChildren(), item);
            if (content.getChildren().isEmpty()) {
                content.getChildren().add(plainLabel(collectText(item)));
            }

            HBox itemRow = new HBox(6, marker, content);
            itemRow.setMinWidth(0);
            itemRow.setAlignment(Pos.TOP_LEFT);
            box.getChildren().add(itemRow);
        }
        return box;
    }

    /** True when a paragraph holds only literal text / line breaks -- no bold, italic, code or links. */
    private static boolean isPlainInline(Node paragraph) {
        for (Node c = paragraph.getFirstChild(); c != null; c = c.getNext()) {
            if (!(c instanceof com.vladsch.flexmark.ast.Text
                    || c instanceof SoftLineBreak
                    || c instanceof HardLineBreak)) {
                return false;
            }
        }
        return true;
    }

    /** Builds a TextFlow of individually-styled Text runs for a paragraph with inline formatting. */
    private TextFlow buildTextFlow(Node parent) {
        TextFlow flow = new TextFlow();
        flow.setMinWidth(0);
        addInlineRuns(flow.getChildren(), parent, InlineStyle.PLAIN);
        return flow;
    }

    /** Recursively appends styled {@link Text} runs for {@code parent}'s inline children. */
    private void addInlineRuns(List<javafx.scene.Node> out, Node parent, InlineStyle style) {
        for (Node c = parent.getFirstChild(); c != null; c = c.getNext()) {
            if (c instanceof com.vladsch.flexmark.ast.Text) {
                out.add(styledRun(c.getChars().unescape(), style));
            } else if (c instanceof StrongEmphasis) {
                addInlineRuns(out, c, style.withBold());
            } else if (c instanceof Emphasis) {
                addInlineRuns(out, c, style.withItalic());
            } else if (c instanceof Code code) {
                out.add(styledRun(code.getText().toString(), style.withMono()));
            } else if (c instanceof SoftLineBreak) {
                out.add(styledRun(" ", style));
            } else if (c instanceof HardLineBreak) {
                out.add(styledRun("\n", style));
            } else if (c instanceof Link link) {
                if (link.getFirstChild() != null) {
                    addInlineRuns(out, link, style.withLink());
                } else {
                    out.add(styledRun(link.getUrl().toString(), style.withLink()));
                }
            } else {
                // Unknown inline wrapper: descend so its text is still shown.
                addInlineRuns(out, c, style);
            }
        }
    }

    /** One styled text run for a TextFlow. */
    private static Text styledRun(CharSequence content, InlineStyle style) {
        Text run = new Text(content.toString());
        run.setFont(Font.font(
            style.mono() ? "monospace" : "System",
            style.bold() ? FontWeight.BOLD : FontWeight.NORMAL,
            style.italic() ? FontPosture.ITALIC : FontPosture.REGULAR,
            BODY_FONT_SIZE));
        run.setStyle(style.link() ? "-fx-fill: -color-accent2;" : "-fx-fill: -color-text;");
        run.setUnderline(style.link());
        return run;
    }

    /** Immutable bold / italic / mono / link flags carried down the inline walk. */
    private record InlineStyle(boolean bold, boolean italic, boolean mono, boolean link) {
        static final InlineStyle PLAIN = new InlineStyle(false, false, false, false);
        InlineStyle withBold()   { return new InlineStyle(true, italic, mono, link); }
        InlineStyle withItalic() { return new InlineStyle(bold, true, mono, link); }
        InlineStyle withMono()   { return new InlineStyle(bold, italic, true, link); }
        InlineStyle withLink()   { return new InlineStyle(bold, italic, mono, true); }
    }

    private static Label plainLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinWidth(0);
        return label;
    }

    private static Label headerLabel(String text) {
        Label header = new Label(text);
        header.getStyleClass().add("score-title");
        header.setWrapText(true);
        header.setMinWidth(0);
        return header;
    }

    private static Label codeBlock(Node node) {
        String content = (node instanceof FencedCodeBlock fenced)
            ? fenced.getContentChars().toString()
            : (node instanceof IndentedCodeBlock indented)
                ? indented.getContentChars().toString()
                : node.getChars().toString();
        Label label = new Label(content.stripTrailing());
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setStyle("-fx-font-family: 'monospace'; -fx-background-color: -color-accent2-100; "
            + "-fx-padding: 6 8; -fx-background-radius: 8;");
        return label;
    }

    /** Plain text of a subtree (soft breaks -&gt; space, hard breaks -&gt; newline), via flexmark's own visitor. */
    private static String collectText(Node node) {
        return new TextCollectingVisitor().collectAndGetText(node).strip();
    }

    /**
     * The model often writes {@code "Ways to save water:"} on its own line before a list, which
     * flexmark sees as a plain paragraph. Treat such a short, punctuation-free, colon-terminated
     * single line as a section header -- but only when another block actually follows it.
     */
    private static boolean looksLikeCaptionHeader(String text, boolean hasFollowingBlock) {
        if (!hasFollowingBlock) {
            return false;
        }
        String t = text.strip();
        if (!t.endsWith(":") || t.indexOf('\n') >= 0) {
            return false;
        }
        String caption = t.substring(0, t.length() - 1).strip();
        return !caption.isEmpty()
            && caption.length() <= 60
            && caption.chars().noneMatch(ch -> ch == '.' || ch == '!' || ch == '?');
    }

    // ---------------------------------------------------------------------------
    // Post-processing of raw model output. Pipeline order is fixed and matters:
    //   1. trim
    //   2. strip a leaked identity / "as an AI" preamble        (stripLeadingPreamble)
    //   3. strip a single pair of wrapping quotation marks       (stripSurroundingQuotes)
    //   4. flag/mark truncated output                            (isLikelyTruncated)
    // Only AFTER all of the above does the cleaned text go to the flexmark Markdown
    // parser + AST walk in addAiBubble() to become JavaFX nodes.
    // ---------------------------------------------------------------------------

    /** Runs the full clean-up pipeline on one raw model response. */
    static String postProcess(String raw, Generator.FinishReason finishReason) {
        String text = (raw == null) ? "" : raw.strip();
        text = stripLeadingPreamble(text);
        text = stripSurroundingQuotes(text);
        text = text.strip();

        if (isLikelyTruncated(text, finishReason)) {
            // We APPEND A MARKER rather than doing a continuation generation. Jlama's
            // Generator.generate() has no "resume from where you stopped" entry point --
            // every call starts fresh from a PromptContext and the session UUID only scopes
            // KV-cache reuse, not conversational resumption. A DIY continuation (re-send the
            // prompt + the partial answer as an assistant turn, ask it to keep going) would
            // double the latency of an already-slow local 1.5B model AND hit the exact
            // multi-turn pattern that CLAUDE.md gotcha #7 says makes this model garble its
            // output. A subtle, deterministic marker is the safe choice for a testing scene.
            text = text + " … [response cut off]";
        }
        return text;
    }

    /**
     * Removes a leading self-description the model sometimes emits before the real answer,
     * e.g. "You are Qwen, created by Alibaba Cloud. You are a helpful assistant. &lt;answer&gt;".
     * Only known identity / AI-disclaimer sentence shapes are removed (see {@link #IDENTITY_PREAMBLE}
     * / {@link #AI_DISCLAIMER}); an ordinary answer that merely starts with "You are ..." is left
     * intact. Up to 3 stacked preamble sentences are peeled.
     */
    static String stripLeadingPreamble(String text) {
        String result = text;
        for (int i = 0; i < 3; i++) {
            Matcher identity = IDENTITY_PREAMBLE.matcher(result);
            if (identity.find()) {
                result = result.substring(identity.end());
                continue;
            }
            Matcher disclaimer = AI_DISCLAIMER.matcher(result);
            if (disclaimer.find()) {
                result = result.substring(disclaimer.end());
                continue;
            }
            break;
        }
        // A stripped preamble can leave a dangling opening quote ("You are Qwen..." -> ...).
        String trimmed = result.strip();
        if (!trimmed.equals(text.strip()) && !trimmed.isEmpty()) {
            char c = trimmed.charAt(0);
            if (c == '"' || c == '\'' || c == '“' || c == '‘') {
                trimmed = trimmed.substring(1).strip();
            }
        }
        return trimmed;
    }

    /**
     * If the whole response is wrapped in one matching pair of quotation marks
     * (straight or curly, double or single), strips just that outer pair. Runs AFTER
     * {@link #stripLeadingPreamble} so a leaked preamble can't hide the opening quote.
     */
    static String stripSurroundingQuotes(String text) {
        if (text.length() < 2) {
            return text;
        }
        char first = text.charAt(0);
        char last = text.charAt(text.length() - 1);
        boolean matchedPair =
               (first == '"'  && last == '"')
            || (first == '\'' && last == '\'')
            || (first == '“'  && last == '”')
            || (first == '‘'  && last == '’');
        if (matchedPair) {
            return text.substring(1, text.length() - 1).strip();
        }
        // Leftover single quote: e.g. stripLeadingPreamble already ate the opening quote of a
        // fully-wrapped reply, leaving one dangling closing quote (or vice-versa). Only remove it
        // when that quote character appears exactly once in the whole string, so real quoted
        // phrases inside the answer are untouched.
        if ("\"'".indexOf(last) >= 0 && "\"'".indexOf(first) < 0
                && countChar(text, last) == 1) {
            return text.substring(0, text.length() - 1).strip();
        }
        if ("\"'".indexOf(first) >= 0 && "\"'".indexOf(last) < 0
                && countChar(text, first) == 1) {
            return text.substring(1).strip();
        }
        return text;
    }

    private static int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    /**
     * Heuristic "was this reply cut off?" check. True when the model hit the token ceiling
     * ({@link Generator.FinishReason#MAX_TOKENS}), or when the last meaningful character is
     * not sentence-ending punctuation (. ! ? …). A single trailing closing quote/bracket is
     * looked past first so a legitimately-quoted or parenthesised ending isn't mis-flagged.
     */
    static boolean isLikelyTruncated(String text, Generator.FinishReason finishReason) {
        if (text.isEmpty()) {
            return false;
        }
        if (finishReason == Generator.FinishReason.MAX_TOKENS) {
            return true;
        }
        String t = text.stripTrailing();
        char last = t.charAt(t.length() - 1);
        if (last == '"' || last == '\'' || last == '”' || last == '’' || last == ')' || last == ']') {
            t = t.substring(0, t.length() - 1).stripTrailing();
            if (t.isEmpty()) {
                return false;
            }
            last = t.charAt(t.length() - 1);
        }
        return last != '.' && last != '!' && last != '?' && last != '…';
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
