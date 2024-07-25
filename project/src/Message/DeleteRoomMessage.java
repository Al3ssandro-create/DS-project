package Message;

import java.util.UUID;

public class DeleteRoomMessage extends Message {
    private final UUID roomId;
    public DeleteRoomMessage(String senderUsername, UUID senderId, int sequenceNumber, UUID roomId) {
        super(senderUsername, senderId, sequenceNumber);
        this.roomId = roomId;
        setType(MessageType.DELETE_ROOM);
    }
    public UUID getRoomId(){
        return roomId;
    }
}
