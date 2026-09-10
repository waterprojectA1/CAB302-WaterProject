package com.wateradvisory.Charlie_Root;

/**
 * One entry in the in-memory chat transcript held by {@link ChatSession}.
 *
 * <p>Pure data -- who said it and what they said. ChatController renders each of
 * these to a bubble, and re-renders the whole list every time the chat screen is
 * rebuilt (e.g. after the user navigates Back and returns), so the conversation
 * survives navigation within an app session.</p>
 */
public record ChatMessage(Sender sender, String text) {
}
