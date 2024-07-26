package Entities;

import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import Color.Color;
import Communication.NetworkDiscovery;
import Message.*;
import Vector.VectorClock;

public class User {
    private UUID userId;
    private Set<User> peers;
    private Set<UserTuple> disconnectedPeers;
    private String username;
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
        this.disconnectedPeers = new HashSet<>();
        networkDiscovery = new NetworkDiscovery(this);
        new Thread(() -> {
            networkDiscovery.startListening();
        }, "startListening_").start();
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
        for(UUID userId : selected) {
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
        if(!rooms.keySet().contains(receivedRoom.getRoomId()) && receivedRoom.getParticipants().contains(this.userId)){
            Set<UUID> partecipant = new HashSet<>();
            for(User user: peers){
            if(receivedRoom.getParticipants().contains(user.getUserId()))
                partecipant.add(user.getUserId());
            }
            Room room = new Room(receivedRoom.getName(), receivedRoom.getParticipants(), receivedRoom.getRoomId(), receivedRoom.getMessages(), receivedRoom.getVectorClock());
            rooms.put(room.getRoomId(), room);
        }
    }

    public void deleteRoomAndForward(Room room){
        if(room != null){
            for(UUID userId : room.getParticipants()) {
                if(userId != this.userId) {
                    User peerToNotice = findPeerByUUID(userId);
                    if(peerToNotice != null) {
                        networkDiscovery.deleteRoom(room.getRoomId(), peerToNotice.getListeningSocket());
                    }else{
                        //TODO:LUI entra qua ma non so perchè, ma allo stesso tempo funziona
                    }
                }
            }
            rooms.remove(room.getRoomId());
        } else {
            System.out.println("Room not found");
        }
    }
    public void deleteRoom(UUID roomId){
        rooms.remove(roomId);
    }
    public boolean checkHeartbeat() {
        return Instant.now().minusSeconds(7).isBefore(this.lastHeartbeat); // 10 seconds// timeout
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
    }

    public void listRooms(){
        for (Room room : rooms.values()) {
            System.out.println(room.getName());
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
            this.disconnectedPeers.add(new UserTuple(disconnectedUser.getUserId(), disconnectedUser.getUsername()));
            this.peers.remove(disconnectedUser);
        }
    }

    public void startConnection(String ipPeer, int portPeer) {
        System.out.println("\n" + Color.GREEN + "Connecting to peer..." + Color.RESET);
        networkDiscovery.connectToPeer(ipPeer, portPeer, this);
    }

    public User addPeer(String peerUsername, UUID peerId, int peerPort, Socket socket) {
        if(this.username.equals(peerUsername)){
            return null;
        }
        for (User user : this.peers) {
            if (user.getUsername().equals(peerUsername)) {
                return null;
            }
        }
        User peer = new User(peerUsername, peerId, peerPort, socket);
        System.out.println("\n" + Color.RESET + peer.getUsername() + Color.GREEN + " CONNECT TO THE NETWORK" + Color.RESET);
        this.peers.add(peer);
        //socketPeers.put(peerUsername, socket);
        networkDiscovery.startHeartbeat(socket);
        return peer;
    }

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
        System.out.println("selected are" + selected);
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

    public Map<UUID, Room> getRooms(){
        return rooms;
    }

    public Map<UUID, Room> commonRooms(UUID peerId){
        Map<UUID, Room> commonRooms = new HashMap<>();
        for(UUID room : rooms.keySet()){
            if(rooms.get(room).contains(peerId));
                commonRooms.put(room, rooms.get(room));
        }
        return commonRooms;
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

    public void addMessageToRoom(RoomMessage message){
        if( rooms.get(message.getRoomId()) != null){
            rooms.get(message.getRoomId()).addMessage(message);
        }
    }

    public void addMessageToRoomAndSend(String message) {
        Room room = getRoom();
        try {
            //ho messo tutto nel try perché incremento del clock e invio del messaggio è un'operazione atomica
            //System.out.println(Color.GREEN + "Vector clock prima dell'incremento" + Color.RESET);
            //System.out.println(room.getVectorClock().toString());
            room.getVectorClock().incrementUser(userId);
            //System.out.println(Color.GREEN + "Vector clock dopo dell'incremento" + Color.RESET);
            //System.out.println(room.getVectorClock().toString());
            room.incrementClock(this.getUserId());
            VectorClock nowVector = room.getVectorClock();
            RoomMessage preparedMessage = new RoomMessage(message, 0, this.getUsername(), this.getUserId(), room.getRoomId(), nowVector); //TODO: sequence number
            room.addOwnMessage(preparedMessage);
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

    public void setUsername(String newUsername) {
        this.username = newUsername;
    }

    public User reconnectPeer(String peerUsernameRe, int peerPortRe, Socket socket) {
        UserTuple peerTuple = findPeerByUsername(peerUsernameRe);
        User peer = null;
        if(peerTuple != null){
            peer = new User(peerTuple.getUsername(), peerTuple.getId(), peerPortRe, socket);
            peer.updateHeartbeat();
            System.out.println("\n" + Color.RESET + peer.getUsername() + Color.GREEN + " RECONNECT TO THE NETWORK" + Color.RESET);
            this.peers.add(peer);
            this.disconnectedPeers.remove(peerTuple);
            networkDiscovery.startHeartbeat(socket);
        }
        return peer;
    }

    public void reconnectPeerWithID(String peerUsername) {
        UserTuple peerTuple = findPeerByUsername(peerUsername);
        if(peerTuple != null){
            this.disconnectedPeers.remove(peerTuple);
        }
    }

    private UserTuple findPeerByUsername(String peerUsername) {
        for(UserTuple peer:disconnectedPeers){
            if(peer.getUsername().equals(peerUsername))
                return peer;
        }
        return null;
    }

    private void setListeningSocket(Socket socket) {
        this.listeningSocket = socket;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean inDisconnected(String peer){
        for(UserTuple user:disconnectedPeers){
            if(user.getUsername().equals(peer))
                return true;
        }
        return false;
    }

    public Set<UserTuple> getDisconnectedUser() {
        return disconnectedPeers;
    }

    public void setDisconnectedUser(Set<UserTuple> disconnectedUser) {
        this.disconnectedPeers = disconnectedUser;
    }
}