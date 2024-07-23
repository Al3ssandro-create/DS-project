package Message;

import Entities.User;
import Vector.VectorClock;

import java.util.UUID;

public class RoomMessage extends Message {
    private final UUID roomId;
    private final String content;
    private final VectorClock vectorClock;
    public RoomMessage(String content, int sequenceNumber, String sender, UUID senderId, UUID roomId, VectorClock vc){
        super(sender, senderId, sequenceNumber);
        setType(MessageType.ROOM_MESSAGE);
        this.content = content;
        this.roomId = roomId;
        this.vectorClock = vc;
    }
    public String getContent() {
        return content;
    }
    public UUID getRoomId() {
        return roomId;
    }
    public VectorClock getVectorClock(){
        return vectorClock;
    }
}
