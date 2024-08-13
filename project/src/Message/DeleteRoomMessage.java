package Message;

import java.util.UUID;

public class DeleteRoomMessage extends Message {
    private final UUID roomId;
    public DeleteRoomMessage(String senderUsername, UUID senderId, UUID roomId) {
        super(senderUsername, senderId);
        this.roomId = roomId;
        setType(MessageType.DELETE_ROOM);
    }
    public UUID getRoomId(){
        return roomId;
    }
}
