package test;

import Entities.*;
import Message.*;
import Vector.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User user;
    private User peer;
    private Room room;

    @BeforeEach
    public void setUp() {
        user = new User("TestUser", 8080);
        peer = new User("PeerUser", 9090);
        room = new Room("TestRoom", new HashSet<>(Arrays.asList(user.getUserId(), peer.getUserId())));
    }

    @Test
    public void testCreateRoom() {
        user.getRooms().clear();
        user.getRooms().put(room.getRoomId(), room);
        assertEquals(1, user.getRooms().size());
        assertEquals(room, user.getRooms().get(room.getRoomId()));
    }

    @Test
    public void testAddRoom() {
        user.addRoom(room);
        assertEquals(1, user.getRooms().size());
        assertTrue(user.getRooms().containsKey(room.getRoomId()));
    }

    @Test
    public void testAddRoomNotParticipant() {
        Room roomNotIn = new Room("TestRoom2", new HashSet<>(Collections.singletonList(peer.getUserId())));
        user.addRoom(roomNotIn);
        assertEquals(0, user.getRooms().size());
        assertFalse(user.getRooms().containsKey(roomNotIn.getRoomId()));
    }

    @Test
    public void testDeleteRoom() {
        user.getRooms().put(room.getRoomId(), room);
        user.deleteRoom(room.getRoomId());
        assertFalse(user.getRooms().containsKey(room.getRoomId()));
    }

    @Test
    public void testFindPeerByUUID() {
        user.listPeers().add(peer);
        User foundPeer = user.findPeerByUUID(peer.getUserId());
        assertNotNull(foundPeer);
        assertEquals(peer.getUsername(), foundPeer.getUsername());
    }

    @Test
    public void testRemovePeer() {
        user.listPeers().add(peer);
        user.removePeer(peer.getUserId());
        assertFalse(user.listPeers().contains(peer));
    }

    @Test
    public void testCheckHeartbeat() {
        assertTrue(user.checkHeartbeat());
    }

    @Test
    public void testUpdateHeartbeat() throws InterruptedException {
        user.updateHeartbeat();
        Thread.sleep(8000);  // Simulate a delay longer than the heartbeat threshold
        assertFalse(user.checkHeartbeat());
    }

    @Test
    public void testAddPeer() {
        User newPeer = user.addPeer("NewPeer", UUID.randomUUID(), 7070, new Socket());
        assertNotNull(newPeer);
        assertEquals("NewPeer", newPeer.getUsername());
        assertTrue(user.listPeers().contains(newPeer));
    }

    @Test
    public void testReconnectPeer() {
        user.getDisconnectedUser().add(new UserTuple(peer.getUserId(), peer.getUsername()));

        User reconnectedPeer = user.reconnectPeer(peer.getUsername(), peer.getPort(), peer.getListeningSocket());
        assertNotNull(reconnectedPeer);
        assertEquals(peer.getUsername(), reconnectedPeer.getUsername());
        assertFalse(user.getDisconnectedUser().contains(new UserTuple(peer.getUserId(), peer.getUsername())));
    }

    @Test
    public void testFindRoom() {
        user.getRooms().put(room.getRoomId(), room);
        Room foundRoom = user.findRoom("TestRoom");
        assertNotNull(foundRoom);
        assertEquals(room.getRoomId(), foundRoom.getRoomId());
    }

    @Test
    public void testAddMessageToRoom() {
        user.addRoom(room);
        room.incrementClock(user.getUserId());
        RoomMessage message = new RoomMessage("Hello", user.getUsername(), user.getUserId(), room.getRoomId(), room.getVectorClock());
        user.addMessageToRoom(message);
        assertEquals(1, user.getRooms().get(room.getRoomId()).getMessages().size());
        assertEquals(message, user.getRooms().get(room.getRoomId()).getMessages().get(0));
    }

    @Test
    public void testAddMessageToRoomAndSend_ValidClock() {
        user.addRoom(room);
        room.incrementClock(user.getUserId());
        RoomMessage message = new RoomMessage("Hello", user.getUsername(), user.getUserId(), room.getRoomId(), room.getVectorClock());

        user.addMessageToRoom(message);

        assertTrue(user.getRooms().get(room.getRoomId()).getVectorClock().getVector().containsKey(user.getUserId()));
        assertEquals(1, user.getRooms().get(room.getRoomId()).getMessages().size());
        assertEquals(0, user.getRooms().get(room.getRoomId()).getMessageQueue().size());

        assertEquals(message, user.getRooms().get(room.getRoomId()).getMessages().get(0));
    }

    @Test
    public void testAddMessageToRoomAndSend_InvalidClock() {
        user.addRoom(room);

        Map<UUID, Integer> wrongVector = new HashMap<>(room.getVectorClock().getVector());
        wrongVector.put(user.getUserId(), 99);  // Set an invalid vector clock
        VectorClock invalidClock = new VectorClock(wrongVector);
        RoomMessage message = new RoomMessage("Hello", user.getUsername(), user.getUserId(), room.getRoomId(), invalidClock);

        user.addMessageToRoom(message);

        assertEquals(0, user.getRooms().get(room.getRoomId()).getMessages().size());
        assertEquals(1, user.getRooms().get(room.getRoomId()).getMessageQueue().size());
    }

    @Test
    public void testSetAndGetRoom() {
        user.setRoom(room);
        assertEquals(room, user.getRoom());
    }
}
