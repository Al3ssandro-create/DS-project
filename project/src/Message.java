import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Message implements Serializable {
    private UUID messageId;
    private User sender;
    private String content;
    private int sequenceNumber; // New field for sequence number
    private String roomName;
    private Set<String> users;
    private MsgType type; //enum to see what type of message is sent

    public Message(User sender, String content, int sequenceNumber) {
        this.messageId = UUID.randomUUID();
        this.sender = sender;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
    }

    public Message(Room room){
        this.messageId = UUID.randomUUID();
        this.roomName = room.getName();
        this.users = new HashSet<>();
        this.roomName = room.getName();
        for(User user: room.getParticipants()){
            users.add(user.getUsername());
        }
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
    public UUID getMessageId() {
        return messageId;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getRoomName(){
        return roomName;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public MsgType getType(){
        return type;
    }

    public Set<String> getUsers(){
        return users;
    }
}
