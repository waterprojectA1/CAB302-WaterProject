package com.wateradvisory.Charlie_Test;

import com.github.tjake.jlama.model.functions.Generator;
import com.github.tjake.jlama.safetensors.Config;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;
import com.github.tjake.jlama.safetensors.prompt.PromptSupport;
import com.github.tjake.jlama.safetensors.tokenizer.Tokenizer;
import com.wateradvisory.Charlie_Root.TipPhraser;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TipPhraserFallbackTest {

    /** A Generator stub whose generate() always throws, simulating a model failure. */
    private static class ThrowingGenerator implements Generator {
        @Override
        public Response generate(UUID session, PromptContext promptContext, float temperature,
                                  int ntokens, BiConsumer<String, Float> onToken) {
            throw new RuntimeException("simulated model failure");
        }

        @Override
        public float[] embed(String input, PoolingType poolingType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Config getConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tokenizer getTokenizer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PromptSupport> promptSupport() {
            return Optional.empty();
        }

        @Override
        public void close() {
        }
    }

    @Test
    void rephraseReturnsOriginalSentenceUnchangedWhenModelThrows() {
        TipPhraser phraser = new TipPhraser(new ThrowingGenerator());

        String original = "Your showers average 12 minutes, above the 8-minute recommendation.";

        String result = phraser.rephrase(original);

        assertEquals(original, result,
                "rephrase() should return the original sentence byte-for-byte when generation throws");
    }
}
