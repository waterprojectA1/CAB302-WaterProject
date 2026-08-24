package com.wateradvisory.Charlie_Root;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.model.functions.Generator;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;

/**
 * Wraps a local Jlama model to lightly rephrase already-computed tip
 * sentences. This class NEVER lets the model invent numbers -- your
 * calculation logic must fill in every figure before calling rephrase().
 *
 * NOTE: Jlama's API has changed across versions -- check the current
 * README/examples at https://github.com/tjake/Jlama for the exact
 * method signatures on the version you depend on, and adjust the
 * generate() call below if it differs.
 */
public class TipPhraser {

    private static final String SYSTEM_PROMPT =
        "You lightly rephrase short water-conservation tips for a dashboard. "
      + "Keep it natural, concise, and one sentence. "
      + "Do not change, invent, recalculate, or drop any numbers, units, or "
      + "percentages that appear in the input -- copy them exactly as given.";

    private final AbstractModel model;

    /**
     * @param modelDir path to the locally downloaded + quantized model
     *                 (e.g. "./models/Qwen_Qwen2.5-1.5B-Instruct-JQ4")
     */
    public TipPhraser(String modelDir) throws IOException {
        File localModelPath = new File(modelDir);
        if (!localModelPath.exists()) {
            throw new IOException(
                "Model not found at " + modelDir
              + ". Run the jlama download/quantize CLI step first (see README)."
            );
        }
        this.model = ModelSupport.loadModel(localModelPath, DType.F32, DType.I8);
    }

    /**
     * Rephrases a fully-formed, factually-correct sentence.
     * Falls back to returning the original sentence unchanged if
     * generation fails for any reason -- callers should still wrap
     * this in their own try/catch and fall back to the static
     * template bank if TipPhraser itself can't be constructed.
     */
    public String rephrase(String factualSentence) {
        try {
            PromptContext ctx;

            if (model.promptSupport().isPresent()) {
                ctx = model.promptSupport().get().builder()
                    .addSystemMessage(SYSTEM_PROMPT)
                    .addUserMessage(factualSentence)
                    .build();
            } else {
                // Model has no chat template -- fall back to a plain prompt
                ctx = PromptContext.of(SYSTEM_PROMPT + "\n\n" + factualSentence);
            }

            Generator.Response response = model.generate(
                UUID.randomUUID(),
                ctx,
                0.6f,   // temperature -- lower keeps it closer to the input
                150,    // max tokens -- short output only
                (token, time) -> { /* no streaming needed here */ }
            );

            return response.responseText.trim();
        } catch (Exception e) {
            // Fail safe: return the original, already-correct sentence
            return factualSentence;
        }
    }
}
