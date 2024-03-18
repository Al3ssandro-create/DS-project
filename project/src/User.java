import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import Color.Color;
public class User implements Serializable {
    private final UUID userId;
    private Set<User> peers;
    private final String username;
    public Map<String, Room> rooms;
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
        Room room = new Room(name, new HashSet<>(List.of(this)));
        rooms.put(room.getRoomName(), room);
    }
    public boolean checkHeartbeat() {
        return Instant.now().minusSeconds(5).isBefore(this.lastHeartbeat); // 10 seconds// timeout
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
        Room room = rooms.get(roomName);
        if(room != null){
            if(room.getMessages().isEmpty()){
                System.out.println("No messages in this room");
                return;
            }
            System.out.println(Color.GREEN + "_______________________ " + room.getRoomName() + " _______________________" + Color.RESET);
            for (Message message : room.getMessages()) {
                System.out.println(message.getSender().getUsername() + ": " + message.getContent());
            }
        } else {
            System.out.println("Room not found");
        }
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
}