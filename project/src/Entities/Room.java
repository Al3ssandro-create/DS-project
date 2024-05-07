package Entities;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;
import Message.*;
public class Room implements Serializable{
    private final UUID roomId;
    private final String name;
    private final Set<UUID> participants;
    private List<RoomMessage> messages;
    private Map<UUID, Integer> userSequenceNumbers;
    
    public Room(String name, Set<UUID> participants) {
        this.roomId = UUID.randomUUID();
        this.name = name;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>();
        this.userSequenceNumbers = new HashMap<>();        
    }
    public Room(String name, Set<UUID> participants, UUID roomId, List<RoomMessage> messages){
        this.roomId = roomId;
        this.name = name;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>(messages);
        this.userSequenceNumbers = new HashMap<>(); //TODO:???

    }

    public void addMessage(RoomMessage message) {
        messages.add(message);
        UUID sender = message.getSenderId();
        int sequenceNumber = message.getSequenceNumber();
        userSequenceNumbers.put(sender, sequenceNumber);
    }

    public String getRoomName() {
        return name;
    }
    public String getName() {
        return name;
    }
    public Set<UUID> getParticipants() {
        return participants;
    }
    public List<RoomMessage> getMessages() {
        return messages;
    }
    public Map<UUID, Integer> getUserSequenceNumbers() {
        return userSequenceNumbers;
    }
    public UUID getRoomId() {
        return roomId;
    }
}
