
import Entities.*;
import Vector.*;
import Message.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {
    private Room room;
    private UUID user1;
    private UUID user2;
    private UUID user3;

    @BeforeEach
    void setUp() {
        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();
        user3 = UUID.randomUUID();

        Set<UUID> participants = new HashSet<>();
        participants.add(user1);
        participants.add(user2);

        room = new Room("Test Room", participants);
    }

    @Test
    void testGetName() {
        assertEquals("Test Room", room.getName());
    }

    @Test
    void testGetRoomId() {
        assertNotNull(room.getRoomId());
    }

    @Test
    void testGetParticipants() {
        Set<UUID> participants = room.getParticipants();
        assertEquals(2, participants.size());
        assertTrue(participants.contains(user1));
        assertTrue(participants.contains(user2));
    }

    @Test
    void testAddMessage() {
        VectorClock vectorClock = new VectorClock(room.getParticipants());
        vectorClock.incrementUser(user1);

        RoomMessage message = new RoomMessage("Hello", "user1", user1, room.getRoomId(), vectorClock);

        room.addMessage(message);

        List<RoomMessage> messages = room.getMessages();
        assertEquals(1, messages.size());
        assertEquals("Hello", messages.get(0).getContent());
        assertEquals("user1", messages.get(0).getSender());
        assertEquals(user1, messages.get(0).getSenderId());
    }

    @Test
    void testContains() {
        assertTrue(room.contains(user1));
        assertFalse(room.contains(user3));
    }

    @Test
    void testIncrementClock() {
        VectorClock vcBefore = room.getVectorClock().copy();
        room.incrementClock(user1);
        VectorClock vcAfter = room.getVectorClock();
        assertEquals((int) vcAfter.getVector().get(user1), vcBefore.getVector().get(user1) + 1);
    }

    @Test
    void testAddOwnMessage() {
        VectorClock vectorClock = new VectorClock(room.getParticipants());
        vectorClock.incrementUser(user1);

        RoomMessage message = new RoomMessage("Hi there", "user1", user1, room.getRoomId(), vectorClock);

        room.addOwnMessage(message);

        List<RoomMessage> messages = room.getMessages();
        assertEquals(1, messages.size());
        assertEquals("Hi there", messages.get(0).getContent());
    }

    @Test
    void testRoomInitializationWithExistingData() {
        VectorClock vectorClock = new VectorClock(room.getParticipants());
        List<RoomMessage> messages = new ArrayList<>();
        messages.add(new RoomMessage("Test message", "user1", user1, room.getRoomId(), vectorClock));

        Room existingRoom = new Room("Existing Room", room.getParticipants(), room.getRoomId(), messages, vectorClock);

        assertEquals("Existing Room", existingRoom.getName());
        assertEquals(1, existingRoom.getMessages().size());
        assertEquals("Test message", existingRoom.getMessages().get(0).getContent());
    }

    @Test
    void testAddMessageWithValidClock() {
        VectorClock vectorClock = room.getVectorClock().copy();
        vectorClock.incrementUser(user1);

        RoomMessage message = new RoomMessage("Valid Message", "user1", user1, room.getRoomId(), vectorClock);

        room.addMessage(message);

        assertEquals(1, room.getMessages().size());
        assertEquals("Valid Message", room.getMessages().get(0).getContent());
    }

    @Test
    void testAddMessageWithInvalidClock() {
        VectorClock vectorClock = room.getVectorClock().copy();
        vectorClock.incrementUser(user1);
        vectorClock.incrementUser(user1); // Increment twice to simulate invalid state.

        RoomMessage message = new RoomMessage("Invalid Message", "user1", user1, room.getRoomId(), vectorClock);

        room.addMessage(message);

        assertEquals(1, room.getMessageQueue().size());
        assertEquals(0, room.getMessages().size(), "Message with invalid clock should not be added.");
    }

    @Test
    void testAddInvalidMessageAfterCorrectClock(){
        VectorClock vectorClock = room.getVectorClock().copy();
        VectorClock vectorClock1 = room.getVectorClock().copy();
        vectorClock.incrementUser(user1);
        RoomMessage latterMessage = new RoomMessage("Valid Message", "user1", user1, room.getRoomId(), vectorClock);

        vectorClock1.incrementUser(user1);
        vectorClock1.incrementUser(user1);
        RoomMessage earlierMessage = new RoomMessage("Invalid Message", "user1", user1, room.getRoomId(), vectorClock1);

        room.addMessage(earlierMessage);
        assertEquals(1, room.getMessageQueue().size());
        assertEquals(0, room.getMessages().size(), "Message with invalid clock should not be added.");

        room.addMessage(latterMessage);
        assertEquals(2, room.getMessages().size());
        assertEquals(0, room.getMessageQueue().size());
    }
}
