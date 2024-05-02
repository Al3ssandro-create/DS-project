package Message;

import Entities.User;

import java.util.UUID;

public class RoomMessage extends Message {
    private final UUID roomId;
    private final String content;
    public RoomMessage(String content, int sequenceNumber, String sender, UUID senderId, UUID roomId){
        super(sender, senderId, sequenceNumber);
        setType(MessageType.ROOM_MESSAGE);
        this.content = content;
        this.roomId = roomId;
    }
    public String getContent() {
        return content;
    }
    public UUID getRoomId() {
        return roomId;
    }
}
