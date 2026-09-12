package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ChatDataContextBuilder;
import com.wateradvisory.Michael_Root.WaterDataList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatDataContextBuilderReturnsNoDataBlockForUnknownUserTest {

    @Test
    void dataRelatedQuestionForUserWithNoRecordsAnywhereReturnsNoDataBlockNotFabricatedNumbers() {
        // No real Supabase records, and a user id (999) that matches none of the seeded
        // WaterDataList fallback records either -- genuinely zero data anywhere.
        ChatDataContextBuilder builder = new ChatDataContextBuilder(List.of(), new WaterDataList());

        String context = builder.buildContext("why did my score drop this week?", 999);

        assertTrue(context != null && context.toLowerCase().contains("none is available"),
                "With zero real records and zero matching seeded records, the builder must tell "
                        + "the model it has no data rather than fabricating a score/usage figure -- got: "
                        + context);
    }
}
