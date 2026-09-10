package com.wateradvisory.Charlie_Root;

import java.util.ArrayList;
import java.util.List;

import com.github.tjake.jlama.model.AbstractModel;

/**
 * Part 3 -- process-wide singleton that outlives JavaFX scene swaps.
 *
 * <p>{@link SceneNavigator} rebuilds the chat screen from scratch (new
 * {@code FXMLLoader}, new {@link ChatController}) every time the user navigates
 * back to it. Without somewhere outside the controller to keep state, each visit
 * would (a) throw the whole conversation away and (b) reload the ~1.5B-parameter
 * model from disk again -- a multi-second stall. This singleton keeps both alive
 * for the life of the app process:</p>
 *
 * <ul>
 *   <li>the loaded {@link AbstractModel} -- loaded once, on the first visit, then
 *       reused;</li>
 *   <li>the ordered {@link ChatMessage} transcript shown so far --
 *       {@code ChatController.initialize()} replays it into fresh bubbles.</li>
 * </ul>
 *
 * <p>History is in memory only. It is deliberately NOT persisted to disk or
 * Supabase, so it resets when the app exits -- that is the intended scope.</p>
 */
public final class ChatSession {

    private static ChatSession instance;

    private AbstractModel model;
    private boolean welcomed;
    private final List<ChatMessage> history = new ArrayList<>();

    private ChatSession() {
    }

    public static synchronized ChatSession getInstance() {
        if (instance == null) {
            instance = new ChatSession();
        }
        return instance;
    }

    /** The shared model, or {@code null} if it has not been loaded yet this app session. */
    public synchronized AbstractModel getModel() {
        return model;
    }

    /**
     * Stores the model the first time it loads. Later calls are ignored, so a
     * stray second load (only possible if the user navigates away and back while
     * the very first load is still running) can never swap the instance out from
     * under an in-flight request.
     */
    public synchronized void setModel(AbstractModel model) {
        if (this.model == null) {
            this.model = model;
        }
    }

    /** True once the one-time welcome messages have been added to the transcript. */
    public synchronized boolean isWelcomed() {
        return welcomed;
    }

    public synchronized void markWelcomed() {
        this.welcomed = true;
    }

    /**
     * The live transcript. ChatController iterates this to rebuild bubbles and
     * appends to it via {@link #addMessage}. All access is on the JavaFX
     * Application Thread, so no external locking is needed.
     */
    public synchronized List<ChatMessage> getHistory() {
        return history;
    }

    public synchronized void addMessage(Sender sender, String text) {
        history.add(new ChatMessage(sender, text));
    }
}
