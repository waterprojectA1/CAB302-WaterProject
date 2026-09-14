package com.wateradvisory.Charlie_Root;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.safetensors.DType;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

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
 *
 * <p><b>Pre-warm from Home.</b> {@link #ensureModelLoading()} lets more than one
 * screen (the Home dashboard, {@code ChatController}) trigger the model load
 * without racing each other: whichever screen calls it first actually starts the
 * background {@link Task}; the in-flight {@code Task} itself is stored here, so a
 * second caller (e.g. the user navigating to chat before Home's pre-warm
 * finishes) observes and attaches to that same load instead of starting a
 * second one. Once the load finishes, {@link #getModel()} returns the cached
 * result exactly as before.</p>
 */
public final class ChatSession {

    private static ChatSession instance;

    // Point this at whichever model folder jlama list showed you -- e.g. the
    // quantized one used by TipPhraser. Kept here (not just in ChatController)
    // so ensureModelLoading() is the one place that knows how to load the model.
    private static final String MODEL_DIR = "./models/Qwen_Qwen2.5-1.5B-Instruct-JQ4";

    private AbstractModel model;
    private boolean welcomed;
    private final List<ChatMessage> history = new ArrayList<>();

    /** Non-null while a load is in flight; cleared (not nulled out of paranoia -- just left) once it settles. */
    private Task<AbstractModel> loadTask;

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

    /**
     * Starts loading the shared model on a background thread if, and only if, no
     * load has started yet and none has completed. Safe to call from multiple
     * screens (Home's pre-warm, {@code ChatController}'s load-on-demand
     * fallback): every caller gets back the SAME {@link Task}, whether it just
     * started it or one already in flight, so they can all attach completion
     * handlers to it via {@link Task#setOnSucceeded}/{@link Task#setOnFailed}
     * (JavaFX fires those on the FX thread and supports multiple handlers via
     * {@code addEventHandler}, so neither caller has to poll).
     *
     * @return the in-flight or already-finished load {@code Task}, or
     *         {@code null} if the model is already loaded (check
     *         {@link #getModel()} first in that case -- no Task is created).
     */
    public synchronized Task<AbstractModel> ensureModelLoading() {
        if (model != null) {
            return null;
        }
        if (loadTask != null) {
            return loadTask;
        }
        Task<AbstractModel> task = new Task<>() {
            @Override
            protected AbstractModel call() throws Exception {
                return ModelSupport.loadModel(new File(MODEL_DIR), DType.F32, DType.I8);
            }
        };
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, e -> setModel(task.getValue()));
        loadTask = task;
        new Thread(task).start();
        return task;
    }

    /**
     * Registers {@code onSuccess}/{@code onFailure} against whatever
     * {@link #ensureModelLoading()} returns, using {@code addEventHandler} (not
     * {@code setOnSucceeded}) so this never clobbers a handler another caller
     * already attached to the same in-flight Task.
     */
    public synchronized void ensureModelLoading(
            EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailure) {
        Task<AbstractModel> task = ensureModelLoading();
        if (task == null) {
            return;
        }
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, onSuccess);
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, onFailure);
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
