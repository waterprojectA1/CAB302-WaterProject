package com.wateradvisory.Steve_Test;

import com.wateradvisory.Steve_Root.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LeaderboardEntryTest {

    @Test
    void pointsCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> {new LeaderboardEntry("userid1", "name1", -1);}
        );

    }
}
