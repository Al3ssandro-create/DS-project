package Entities;

import Color.Color;
import Message.RoomMessage;
import Vector.VectorClock;

import java.io.Serializable;
import java.util.*;
public class Room implements Serializable{
    private final UUID roomId;
    private final String name;
    private final Set<UUID> participants;
    private List<RoomMessage> messages;
    private VectorClock vectorClock;
    private Set<RoomMessage> messageQueue = new HashSet<>();
    
    public Room(String name, Set<UUID> participants) {
        this.roomId = UUID.randomUUID();
        this.name = name;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>();
        this.vectorClock = new VectorClock(participants);       
    }

    public Room(String name, Set<UUID> participants, UUID roomId, List<RoomMessage> messages, VectorClock vectorClock){
        this.roomId = roomId;
        this.name = name;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>(messages);
        this.vectorClock = vectorClock;
    }

    /**
     * Increase the clock of the user
     * @param user user to increase the clock
     */
    public void incrementClock(UUID user){
        vectorClock.incrementUser(user);
    }

    /**
     * Add a message to the room
     * @param message message to add
     */
    public void addOwnMessage(RoomMessage message){
        messages.add(message);
        checkQueue();
    }

    /**
     * Add a message to the room
     * @param message message to add
     */
    public void addMessage(RoomMessage message) {       
        
        boolean valid = vectorClockCheck(message);
        if(valid){
            messages.add(message);
            vectorClock.update(message.getVectorClock());
        }else{
            messageQueue.add(message);
        }
        checkQueue();
        for(RoomMessage msg: messageQueue){
            System.out.println(Color.RED + msg.getSender() + ": " + msg.getContent() + Color.RESET);
        }
    }

    /**
     * This function checks whether the vector clock of an incoming message is consistent with the vector
     * clock that the user receiving the message has in the room.
     * If it's not consistent, the message is placed in a queue of waiting messages.
     * If it's consistent, it's added to the list of messages in the room and checks if waiting messages
     * are now consistent.
     * @param message received
     * @return whether the received message is valid (consistent) or not
     */
    private boolean vectorClockCheck(RoomMessage message){
        Map<UUID, Integer> receivedVector = message.getVectorClock().getVector();
        Map<UUID, Integer> thisVector = vectorClock.getVector();
        boolean valid = true;
        for(UUID peer: receivedVector.keySet()){
            if((!message.getSenderId().equals(peer)) && (receivedVector.get(peer) > thisVector.get(peer))){
                valid = false;
            }
            else if((message.getSenderId().equals(peer)) && (receivedVector.get(peer) != (thisVector.get(peer) + 1))){
                valid = false;
            }
                
        }
        return valid;

    }

    /**
     * This function checks if there are messages in the queue that can be inserted into the room
     * after the arrival of other messages
     * If a message is inserted, the function is called recursively to check if other messages can be inserted
     *
     * @return void
     */
    private void checkQueue(){
        boolean update = false;
        for(RoomMessage message: messageQueue){
            if(vectorClockCheck(message)){
                messages.add(message);
                vectorClock.update(message.getVectorClock());
                messageQueue.remove(message);
                update = true;
                break;
            }
        }
        if(update)checkQueue();
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

    public UUID getRoomId() {
        return roomId;
    }

    public VectorClock getVectorClock(){
        return vectorClock.copy();
    }

    public Set<RoomMessage> getMessageQueue(){ return messageQueue;}

    /**
     * This function checks if a user is in the room
     * @param userId user to check
     * @return boolean
     */
    public boolean contains(UUID userId){
        return participants.contains(userId);
    }
}
