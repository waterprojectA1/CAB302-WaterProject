package com.wateradvisory.Steve_Test;

import com.wateradvisory.Steve_Root.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LeaderboardEntryTest {

    @Test
    void pointsCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> {new LeaderboardEntry("userID1", "name1", -1);}
        );

    }
    @Test
    void anonymousLeaderboardUserEntry(){
        LeaderboardEntry anonymousEntry =
                new LeaderboardEntry("userID2", "name1", 1, true);

        assertEquals("Anonymous", anonymousEntry.getDisplayName());

    }
}
