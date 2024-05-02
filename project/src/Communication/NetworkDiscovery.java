package Communication;

import Color.Color;
import Entities.Room;
import Entities.User;
import Message.*;
import java.io.*;
import java.net.*;
import java.util.*;

import static Message.MessageType.*;

public class NetworkDiscovery {
    private final User user;
    private ServerSocket serverSocket;
    public NetworkDiscovery(User user) {
        this.user = user;
    }

    private void handleIncomingConnection(Socket socket) throws IOException {
            User handlingUser = null;
            try {
                while (true) {
                    if(handlingUser != null && !handlingUser.checkHeartbeat()) {
                        System.out.println(Color.RED + "Peer " + handlingUser.getUsername() + " is not responding, removing from peers" + Color.RESET);
                        user.removePeer(handlingUser.getUserId());
                        break;
                    }
                    Message responseMessage = getMessage(socket);
                    if(responseMessage != null) {
                        switch (responseMessage.getType()) {
                            case DISCOVERY:
                            case RESPONSE_DISCOVERY:
                            case PEER:
                                String peerUsername = responseMessage.getSender();
                                UUID peerId = responseMessage.getSenderId();
                                int peerPort = ((ConnectMessage) responseMessage).getPort();
                                String peerAddress = ((ConnectMessage) responseMessage).getIp();
                                if(!userExists(peerId) && !responseMessage.getType().equals(PEER)) {
                                    if (responseMessage.getType().equals(DISCOVERY)) {
                                        sendResponseDiscoveryMessage(socket);
                                    }

                                    handlingUser = user.addPeer(peerUsername, peerId, peerPort, socket);
                                    for (User peerUser : user.listPeers()) {
                                        if (!peerUser.getUserId().equals(peerId) &&  !responseMessage.getType().equals(PEER)) {

                                            //sendMessageToUser(peerUser.getUserId(), "PEER " + peerUsername + " " + peerId + " " + peerPort + " " + peerAddress);
                                            sendPeerMessage(peerUser.getUserId(), new ConnectMessage(0, peerPort, peerAddress, peerUsername, peerId));
                                        }
                                    }
                                }else if(responseMessage.getType().equals(PEER)){
                                    user.startConnection(peerAddress, peerPort);
                                }
                                break;
                            case HEARTBEAT:
                                if (handlingUser != null) {
                                    handlingUser.updateHeartbeat();
                                }
                                break;
                            case ROOM_INIT:
                                Room room = ((RoomInitMessage) responseMessage).getRoom();
                                user.addRoom(room);
                                break;
                            case ROOM_MESSAGE:
                                //System.out.println("Arrivato"); qua arriva
                                RoomMessage roomMessage = (RoomMessage) responseMessage;
                                user.addMessageToRoom(roomMessage);
                                break;
                            default:
                                System.out.println(Color.RED + "Received unknown message type" + Color.RESET);
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }



    private boolean userExists(UUID peerId) {
        for (User user : this.user.listPeers()) {
            if (user.getUserId().equals(peerId)) {
                return true;
            }
        }
        return false;
    }

    public void sendMessageToUser(UUID userId, String message) throws IOException {
        for (User user : this.user.listPeers()) {
            if (user.getUserId().equals(userId)) {
                PrintWriter out = new PrintWriter(user.getListeningSocket().getOutputStream(), true);
                out.println(message);
                return;
            }
        }
        System.out.println("User not found" );
    }
    private void sendPeerMessage(UUID userId, ConnectMessage message) {
        for (User user : this.user.listPeers()) {
            if (user.getUserId().equals(userId)) {
                message.setType(PEER);
                try {
                    sendMessage(user.getListeningSocket(), message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        System.out.println("User not found" );
    }
    private void sendDiscoveryMessage(Socket socket) throws IOException {
        ConnectMessage discoveryMessage = new ConnectMessage( 0, serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());//TODO: sequence number
        discoveryMessage.setType(DISCOVERY);
        sendMessage(socket, discoveryMessage);
    }
    private void sendResponseDiscoveryMessage(Socket socket) throws IOException {
        ConnectMessage responseDiscoveryMessage = new ConnectMessage(0, serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());//TODO: sequence number
        responseDiscoveryMessage.setType(RESPONSE_DISCOVERY);
        sendMessage(socket, responseDiscoveryMessage);
    }
    public void sendRoom(Room room, Socket socket) throws IOException {
        RoomInitMessage roomMessage = new RoomInitMessage(0, user.getUsername(), user.getUserId(), room); // TODO: sequence number
        sendMessage(socket, roomMessage);
    }
    public void sendRoomMessage(RoomMessage message) throws IOException {
        for (UUID userId : user.getRoom().getParticipants()){
            if(!userId.equals(user.getUserId())){
                sendMessage(user.findPeerByUUID(userId).getListeningSocket(), message);
            }
        }
    }
    private Message getMessage(Socket socket)  {
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(socket.getInputStream());
            return (Message) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private void sendMessage(Socket socket, Message message) throws IOException{
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(message);
        out.flush();
    }

    public void connectToPeer(String ipPeer, int portPeer) {
        int MAX_RETRIES = 5;
        for(int i = 0; i < MAX_RETRIES; i++) {
            try {
                Socket socket = new Socket(ipPeer, portPeer);
                sendDiscoveryMessage(socket);
                new Thread(() -> {
                    try {
                        handleIncomingConnection(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;
            } catch (IOException e) {
                System.out.println(Color.RED + "Connection crashed, retrying discovery..." + Color.RESET);
                try {
                    int RETRY_WAIT_TIME = 1000;
                    Thread.sleep(RETRY_WAIT_TIME); // introduce delay before next retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                if (i == MAX_RETRIES - 1) {
                    System.out.println(Color.RED + "Failed to reconnect after " + MAX_RETRIES + " attempts, closing client." + Color.RESET);
                    System.exit(1);
                }
            }
        }
    }

    public void startListening() {
        try {
            serverSocket = new ServerSocket(user.getPort());
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Ignore loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                // Get the addresses for this interface
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // Print only IPv4 addresses (IPv6 can be handled similarly if needed)
                    if (addr instanceof Inet4Address) {
                        System.out.println(Color.GREEN + "Your IP is: " + addr.getHostAddress() +
                                " Listening for incoming connections on port " + user.getPort() + Color.RESET);
                    }
                }
            }
            //System.out.println(Color.GREEN + "Your IP is: " + InetAddress.getLocalHost().getHostAddress() + " Listening for incoming connections on port " + user.getPort() + Color.RESET);
            // Start a new thread that handles incoming connections
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        new Thread(() -> {
                            try {
                                handleIncomingConnection(socket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        } catch (IOException e) {
            System.out.println(Color.RED + "Failed to start listening on port " + user.getPort() +Color.RESET);
            System.exit(1);
        }
    }

    public void startHeartbeat(Socket peerSocket) {
        new Thread(() -> {
            while (true) {
                try {

                    sendMessage(peerSocket, new HeartbeatMessage(user.getUsername(), 0, user.getUserId())); // TODO: sequence number
                    int HEARTBEAT_INTERVAL = 1000;
                    Thread.sleep(HEARTBEAT_INTERVAL);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}