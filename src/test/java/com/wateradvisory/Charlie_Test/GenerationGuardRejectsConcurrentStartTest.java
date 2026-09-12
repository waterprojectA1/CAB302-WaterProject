package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.GenerationGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerationGuardRejectsConcurrentStartTest {

    @Test
    void secondTryStartFailsWhileFirstGenerationIsStillInFlight() {
        GenerationGuard guard = new GenerationGuard();

        boolean firstStarted = guard.tryStart();
        boolean secondStarted = guard.tryStart();

        assertTrue(firstStarted,
                "The first tryStart() call, with no generation in flight, must succeed");
        assertFalse(secondStarted,
                "A second tryStart() call, while the first generation has not finished, must be "
                        + "rejected -- this is what should stop inputField's Enter key from starting a "
                        + "second concurrent model.generate() call while sendButton is disabled");
    }
}
