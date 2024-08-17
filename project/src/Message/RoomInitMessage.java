package Message;

import Entities.Room;

import java.util.UUID;

public class RoomInitMessage extends Message {
    private final Room room;
    public RoomInitMessage(String sender, UUID senderId, Room room) {
        super(sender, senderId);
        setType(MessageType.ROOM_INIT);
        this.room = room;
    }
    public Room getRoom() {
        return room;
    }

}
