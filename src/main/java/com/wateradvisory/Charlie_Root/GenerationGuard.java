package com.wateradvisory.Charlie_Root;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Plain-Java re-entrancy guard for {@code ChatController.onSend()}. Extracted out of the
 * controller so the "is a generation already running" check is unit-testable without a
 * JavaFX/TestFX harness.
 *
 * <p>Bug this fixes: {@code sendButton.setDisable(true)} only disables the button node --
 * {@code inputField}'s own {@code onAction="#onSend"} (see {@code ChatView.fxml}) fires
 * independently of the button's disabled state, so pressing Enter while a generation
 * {@code Task} is still running used to start a second {@code Task}/{@code Thread} calling
 * the same shared {@code AbstractModel} instance's {@code generate()} concurrently with the
 * first -- a real data race on Jlama's internal per-model scratch buffers, not just a UI
 * glitch. {@code onSend()} must call {@link #tryStart()} before doing anything else, only
 * proceed if it returns {@code true}, and call {@link #finish()} in both the success and
 * failure completion handlers.</p>
 */
public final class GenerationGuard {

    private final AtomicBoolean inFlight = new AtomicBoolean(false);

    /** @return {@code true} if no generation was already in flight (and this call started one); {@code false} if one was already running. */
    public boolean tryStart() {
        return inFlight.compareAndSet(false, true);
    }

    /** Marks the in-flight generation as finished, so a later {@link #tryStart()} can succeed again. */
    public void finish() {
        inFlight.set(false);
    }
}
