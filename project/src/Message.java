import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private UUID messageId;
    private User sender;
    private String content;
    private int sequenceNumber; // New field for sequence number
    private Room room; //if not NULL is a message of room initialization (maybe I'll add a flag to see it)

    public Message(User sender, String content, int sequenceNumber) {
        this.messageId = UUID.randomUUID();
        this.sender = sender;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
    }

    public Message(Room room){
        this.messageId = UUID.randomUUID();
        this.room = room;
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

    public Room getRoom(){
        return room;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
