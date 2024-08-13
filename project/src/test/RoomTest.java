package test;

import Entities.Room;
import org.junit.Test;
import org.junit.After;

import java.util.HashSet;
import java.util.*;

import static org.junit.Assert.*;
public class RoomTest {

    @Test
    public void room_creation(){
        String name = "name";
        Set<UUID> users = new HashSet<>();
        Room room = new Room(name, users);

        assertEquals(name, room.getRoomName());
        assertEquals(users, room.getParticipants());
    }
}
