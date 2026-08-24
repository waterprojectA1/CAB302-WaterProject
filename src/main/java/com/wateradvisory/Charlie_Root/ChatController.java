package com.wateradvisory.Charlie_Root;

import java.io.File;
import java.util.UUID;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.model.functions.Generator;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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

    private AbstractModel model;
    private HBox loadingRow;

    // Point this at whichever model folder jlama list showed you --
    // e.g. the quantized one used by TipPhraser. Models stayed put,
    // so this relative path is unaffected by the folder restructuring.
    private static final String MODEL_DIR = "./models/Qwen_Qwen2.5-1.5B-Instruct-JQ4";

    @FXML
    private void onBack(ActionEvent event) {
        SceneNavigator.goTo(event, "/Charlie_FXML/ConservationTipsView.fxml");
    }

    @FXML
    public void initialize() {
        loadingRow = addSystemMessage("Loading model, please wait...");

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                model = ModelSupport.loadModel(new File(MODEL_DIR), DType.F32, DType.I8);
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            conversationContainer.getChildren().remove(loadingRow);
            addSystemMessage("Rippl loaded. Ask it something.");
            addAiBubble("Hi! I'm Rippl. Ask me about your water usage, leak signs, or ways to cut back this season.");
            sendButton.setDisable(false);
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
            cause.printStackTrace();
            conversationContainer.getChildren().remove(loadingRow);
            addSystemMessage("Failed to load model: "
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
            addAiBubble(generateTask.getValue());
            sendButton.setDisable(false);
        });

        generateTask.setOnFailed(e -> {
            addSystemMessage("Error: " + generateTask.getException().getMessage());
            sendButton.setDisable(false);
        });

        new Thread(generateTask).start();
    }

    private void addUserBubble(String text) {
        Label label = new Label(text);
        HBox bubble = new HBox(label);
        bubble.getStyleClass().add("bubble-user");
        bubble.setMaxWidth(300);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);

        addRow(row);
    }

    private void addAiBubble(String text) {
        Label label = new Label(text);
        HBox bubble = new HBox(label);
        bubble.getStyleClass().add("bubble-ai");
        bubble.setMaxWidth(300);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);

        addRow(row);
    }

    private HBox addSystemMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("bubble-system");
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER);
        addRow(row);
        return row;
    }

    private void addRow(HBox row) {
        conversationContainer.getChildren().add(row);
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
