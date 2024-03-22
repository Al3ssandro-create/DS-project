import Color.Color;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.UUID;

public class NetworkDiscovery {
    private final User user;
    public NetworkDiscovery(User user) {
        this.user = user;
    }
    private ServerSocket serverSocket;

    private void handleIncomingConnection(Socket socket) throws IOException {
            Socket peerSocket;
            User handlingUser = null;
            try {
                while (true) {
                    if(handlingUser != null && !handlingUser.checkHeartbeat()) {
                        System.out.println(Color.RED + "Peer " + handlingUser.getUsername() + " is not responding, removing from peers" + Color.RESET);
                        user.removePeer(handlingUser.getUserId());
                        break;
                    }
                    String responseMessage = getMessage(socket);
                    if (responseMessage != null && (responseMessage.startsWith("DISCOVERY") || responseMessage.startsWith("RESPONSE_DISCOVERY") || responseMessage.startsWith("PEER"))){
                        String[] parts = responseMessage.split(" ");
                        String peerUsername = parts[1];
                        UUID peerId = UUID.fromString(parts[2]);
                        int peerPort = Integer.parseInt(parts[3]);
                        String peerAddress = parts[4];

                        if(!userExists(peerId) && !responseMessage.startsWith("PEER")) {

                            if (responseMessage.startsWith("DISCOVERY")) {
                                String responseBackMessage = "RESPONSE_DISCOVERY " + this.user.getUsername() + " " + this.user.getUserId().toString() + " " + serverSocket.getLocalPort() + " " + serverSocket.getInetAddress().getHostAddress();
                                sendMessage(socket, responseBackMessage);
                            }

                            handlingUser = user.addPeer(peerUsername, peerId, peerPort, socket);
                            for (User peerUser : user.listPeers()) {
                                if (!peerUser.getUserId().equals(peerId) &&  !responseMessage.startsWith("PEER")) {
                                    sendMessageToUser(peerUser.getUserId(), "PEER " + peerUsername + " " + peerId + " " + peerPort + " " + peerAddress);
                                }
                            }
                        }else if(responseMessage.startsWith("PEER")){
                            user.startConnection(peerAddress, peerPort);
                        }
                    }else if(responseMessage != null && responseMessage.startsWith("HEARTBEAT") && handlingUser != null){
                        handlingUser.updateHeartbeat();
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace();
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

    private void sendDiscoveryMessage(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String discoveryMessage = "DISCOVERY " + this.user.getUsername() + " " + this.user.getUserId().toString() + " " + serverSocket.getLocalPort() + " " + serverSocket.getInetAddress().getHostAddress();
        out.println(discoveryMessage);
    }

    private String getMessage(Socket socket)  {
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private void sendMessage(Socket socket, String message) throws IOException{
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
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
                    PrintWriter out = new PrintWriter(peerSocket.getOutputStream(), true);
                    out.println("HEARTBEAT " + user.getUserId());
                    int HEARTBEAT_INTERVAL = 1000;
                    Thread.sleep(HEARTBEAT_INTERVAL);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}