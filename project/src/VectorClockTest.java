
import Vector.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VectorClockTest {
    private VectorClock vectorClock;
    private UUID user1;
    private UUID user2;

    @BeforeEach
    void setUp() {
        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();
        Set<UUID> users = new HashSet<>();
        users.add(user1);
        users.add(user2);
        vectorClock = new VectorClock(users);
    }

    @Test
    void testIncrementUser() {
        vectorClock.incrementUser(user1);
        assertEquals(1, vectorClock.getVector().get(user1).intValue());
        assertEquals(0, vectorClock.getVector().get(user2).intValue());
    }

    @Test
    void testUpdateUser() {
        vectorClock.updateUser(user1, 5);
        assertEquals(5, vectorClock.getVector().get(user1).intValue());
    }

    @Test
    void testUpdate() {
        VectorClock otherClock = vectorClock.copy();
        otherClock.incrementUser(user1);
        vectorClock.update(otherClock);
        assertEquals(1, vectorClock.getVector().get(user1).intValue());
    }

    @Test
    void testCopy() {
        VectorClock copy = vectorClock.copy();
        assertEquals(vectorClock.getVector(), copy.getVector());
    }
}
