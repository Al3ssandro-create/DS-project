package Entities;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import Color.Color;
import Communication.NetworkDiscovery;
import Message.*;

public class User {
    private final UUID userId;
    private Set<User> peers;
    private final String username;
    public Map<UUID, Room> rooms;
    private Room actualRoom = null;
    private int port;
    private Socket listeningSocket;
    private NetworkDiscovery networkDiscovery;
    private Instant lastHeartbeat;

    public User(String username, int port) {
        this.userId = UUID.randomUUID();
        this.username = username;
        this.rooms = new HashMap<>();
        this.port = port;
        this.peers = new HashSet<>();
        networkDiscovery = new NetworkDiscovery(this);
        new Thread(() -> {
            networkDiscovery.startListening();
        }).start();
        this.lastHeartbeat = Instant.now();
    }
    public User(String username,UUID userId, int port, Socket listeningSocket) {
        this.userId = userId;
        this.username = username;
        this.rooms = new HashMap<>();
        this.port = port;
        this.peers = new HashSet<>();
        this.listeningSocket = listeningSocket;
        this.lastHeartbeat = Instant.now();
    }

    public void createRoom(String name){
        System.out.println(Color.BLUE + "Select the partecipants of the room:\n" + Color.RESET);
        Set<UUID> selected = new HashSet<>(selectPeers());
        System.out.println(Color.BLUE + "Room created with the following partecipants:\n" + Color.RESET);
        for(UUID userId:selected)
            System.out.println(findPeerByUUID(userId).getUsername());
        selected.add(this.userId); //add the creator of the room (this user
        Room room = new Room(name, new HashSet<>(selected));
        rooms.put(room.getRoomId(), room);
        for(UUID userId:selected) {
            if(userId != this.userId) {
                try {
                    networkDiscovery.sendRoom(room, findPeerByUUID(userId).getListeningSocket());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public User findPeerByUUID(UUID uuid) {
        return this.peers.stream()
                .filter(peer -> peer.getUserId().equals(uuid))
                .findFirst()
                .orElse(null);
    }
    //this method is called from the method that reads the received messages if !message.getRoom().isNull()
    public void addRoom(Room receivedRoom){
        Set<UUID> partecipant = new HashSet<>();
        for(User user: peers){
            if(receivedRoom.getParticipants().contains(user.getUserId()))
                partecipant.add(user.getUserId());
        }
        Room room = new Room(receivedRoom.getName(), receivedRoom.getParticipants(), receivedRoom.getRoomId(), receivedRoom.getMessages());
        rooms.put(room.getRoomId(), room);
    }

    public boolean checkHeartbeat() {
        return Instant.now().minusSeconds(5).isBefore(this.lastHeartbeat); // 10 seconds// timeout
    }
    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
    }
    public void listRooms(){
        for (Room room : rooms.values()) {
            System.out.println(room.getName() + ": " + room.getRoomId());
        }
    }

    public void viewChat(String roomName){
        Room room = findRoom(roomName);
        if(room != null){
            if(room.getMessages().isEmpty()){
                System.out.println("No messages in this room");
                return;
            }
            System.out.println(Color.GREEN + "_______________________ " + room.getRoomName() + " _______________________" + Color.RESET);
            for (RoomMessage message : room.getMessages()) {
                System.out.println(message.getSender() + ": " + message.getContent());
            }
        } else {
            System.out.println("Room not found");
        }
    }

    public void removePeer(UUID userId) {
        User disconnectedUser = this.peers.stream()
                .filter(user -> user.getUserId() == userId)
                .findFirst()
                .orElse(null);
        if (disconnectedUser != null) {
            System.out.println("\n" + Color.RESET + disconnectedUser.getUsername() + Color.RED + " DISCONNECT FROM THE NETWORK" + Color.RESET);
            this.peers.remove(disconnectedUser);
        }
    }

    public void startConnection(String ipPeer, int portPeer) {
        System.out.println("\n" + Color.GREEN + "Connecting to peer..." + Color.RESET);
        networkDiscovery.connectToPeer(ipPeer, portPeer);
    }

    public User addPeer(String peerUsername, UUID peerId, int peerPort, Socket socket) {
        boolean loop;
        int i = 0;
        do {
            i++;
            loop = false;
            for (User user : this.peers) {
                if (user.getUsername().equals(peerUsername)) {
                    peerUsername = peerUsername + " (" + i + ")";
                    loop = true;
                }
            }
        }while (loop) ;
        User peer = new User(peerUsername, peerId, peerPort, socket);
        System.out.println("\n" + Color.RESET + peer.getUsername() + Color.GREEN + " CONNECT TO THE NETWORK" + Color.RESET);
        this.peers.add(peer);
        networkDiscovery.startHeartbeat(socket);
        return peer;
    }

    /*public void receiveMessages(){
        new Thread(()->{
            try{
                while(true){
                    ObjectInputStream in = new ObjectInputStream(listeningSocket.getInputStream());
                    Message msg = (Message) in.readObject();
                    if(msg.getType() == MsgType.ROOM)
                        addRoom(msg);
                    // else handling of messages of chat, maybe will do it directly in the room
                }
            }catch(IOException e){
                e.printStackTrace();
            }catch(ClassNotFoundException e){
                e.printStackTrace();
            }
        }).start();
    }*/

    private Set<UUID> selectPeers(){
        Scanner scan = new Scanner(System.in);
        Set<UUID> selected = new HashSet<>();
        for(User peer:peers)
            System.out.println(Color.GREEN + peer.getUsername() + "\n" + Color.RESET);
        System.out.println(Color.BLUE + "Write the username(s) of the client(s) to add separated by a comma.\n" + Color.RESET);
        String users = scan.nextLine();
        while(users.isEmpty()){
            System.out.println(Color.BLUE + "The usernames list cannot be empty.\n" + Color.RESET);
            users = scan.nextLine();
        }
        Set<String> cleanUsers = new HashSet<>(spliceString(users));
        for(User peer:peers){
            if(cleanUsers.contains(peer.getUsername()))
                selected.add(peer.getUserId());
        }
        return selected;
    }

    private Set<String> spliceString(String fullString){
        String[] wordsArray = fullString.split(",");
        Set<String> spliced = new HashSet<>();
        for(String word:wordsArray)
            spliced.add(word.trim());
        return spliced;
    }

    public UUID getUserId() {
        return userId;
    }
    public String getUsername() {
        return username;
    }
    public Room getRoom() {
        return actualRoom;
    }
    public void setRoom(Room room){
        actualRoom = room;
    }
    public Set<User> listPeers() {
        return peers;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public Socket getListeningSocket() {
        return listeningSocket;
    }
    public void addMessageToRoom(RoomMessage message){;
        if( rooms.get(message.getRoomId()) != null){
            rooms.get(message.getRoomId()).addMessage(message);
        }
    }
    public void addMessageToRoomAndSend(String message) {
        Room room = getRoom();
        RoomMessage preparedMessage = new RoomMessage( message, 0, this.getUsername(), this.getUserId(), room.getRoomId()); //TODO: sequence number
        room.addMessage(preparedMessage);
        try {
            networkDiscovery.sendRoomMessage(preparedMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Room findRoom(String roomToEnter) {
        Room result = null;
        for(Room room: rooms.values()){
            if(room.getName().equals(roomToEnter)){
                result = room;
            }
        }
        return result;
    }
}