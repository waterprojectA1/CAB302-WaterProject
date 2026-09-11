package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.TopicFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TopicFilterOnTopicTest {

    @Test
    void clearlyUnrelatedMessageIsNotOnTopic() {
        assertFalse(TopicFilter.isLikelyOnTopic("what's the capital of France"));
    }

    @Test
    void clearlyWaterRelatedMessageIsOnTopic() {
        assertTrue(TopicFilter.isLikelyOnTopic("how can I reduce my shower usage"));
    }
}
