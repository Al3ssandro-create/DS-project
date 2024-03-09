import java.io.*;
import java.net.*;
import java.util.UUID;

public class NetworkDiscovery {
    private User user;
    private static final String SUBNET = "10.1.10.";
    public NetworkDiscovery(User user) {
        this.user = user;
    }
    private ServerSocket serverSocket;
    public void discoverPeers() {
        final int MAX_RETRIES = 5; // Maximum number of retries
        final int RETRY_WAIT_TIME = 5000; // Wait time between retries in milliseconds

        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                // Create a server socket that listens for incoming connections on a specific port
                serverSocket = new ServerSocket(user.getPort());

                // Start a new thread that handles incoming connections
                new Thread(() -> {
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
                }).start();

                // Send a discovery message to all other ports
                /** for (int port = 1024; port <= 49151; port++) {
                   if (port == user.getPort()) {
                        continue;
                    }

                    try {
                        Socket socket = new Socket("localhost", port);
                        sendDiscoveryMessage(socket);
                        socket.close();
                    } catch (IOException e) {
                        // Ignore exceptions, as they are expected when a connection attempt fails
                    }
                }
                 **/
                try {
                    InetAddress subnetAddress = InetAddress.getByName(SUBNET);
                    for (int j = 1; j < 255; j++) {
                        InetAddress address = InetAddress.getByName(SUBNET + j);
                        if (!address.equals(subnetAddress) && address.isReachable(100)) {
                            // Send discovery message to potential peer
                            Socket socket = new Socket(address, 8888);
                            sendDiscoveryMessage(socket);
                            socket.close();
                        }
                    }
                } catch (IOException e) {
                    // Ignore exceptions, as they are expected when a connection attempt fails
                }

                // If the code reaches this point without throwing an exception, break the loop
                break;
            } catch (IOException e) {
                // Handle exception and restart the method
                System.out.println("Connection crashed, retrying discovery...");

                // If this was the last retry, close the client
                if (i == MAX_RETRIES - 1) {
                    System.out.println("Failed to reconnect after " + MAX_RETRIES + " attempts, closing client.");
                    System.exit(1);
                }

                // Wait before retrying
                try {
                    Thread.sleep(RETRY_WAIT_TIME);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void handleIncomingConnection(Socket socket) throws IOException {
        new Thread(() -> {
            try {
                while (true) {
                    String responseMessage = getResponseMessage(socket);

                    if (responseMessage != null && (responseMessage.startsWith("DISCOVERY") || responseMessage.startsWith("RESPONSE_DISCOVERY"))) {
                        String[] parts = responseMessage.split(" ");
                        String peerUsername = parts[1];
                        UUID peerId = UUID.fromString(parts[2]);
                        int peerPort = Integer.parseInt(parts[3]);
                        String peerAddress = parts[4];


                        Socket peerSocket = new Socket(peerAddress, peerPort);
                        User peer = new User(peerUsername, peerId, peerPort, peerSocket, socket );
                        user.addPeer(peer);


                        if(responseMessage.startsWith("DISCOVERY")) {
                            PrintWriter out = new PrintWriter(peer.getSendingSocket().getOutputStream(), true);
                            String responseBackMessage = "RESPONSE_DISCOVERY " + this.user.getUsername() + " " + this.user.getUserId().toString() + " " + serverSocket.getLocalPort() + " " + serverSocket.getInetAddress().getHostAddress();
                            out.println(responseBackMessage);
                        }
                    }
                }
            } catch (IOException e) {
                this.user.removePeer(socket);
            }
        }).start();
    }
    public void sendMessageToUser(String username, String message) throws IOException {
        for (User user : this.user.listPeers()) {
            if (user.getUsername().equals(username)) {
                PrintWriter out = new PrintWriter(user.getSendingSocket().getOutputStream(), true);
                out.println(message);
                return;
            }
        }
        System.out.println("User not found: " + username);
    }

    private void sendDiscoveryMessage(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String discoveryMessage = "DISCOVERY " + this.user.getUsername() + " " + this.user.getUserId().toString() + " " + serverSocket.getLocalPort() + " " + serverSocket.getInetAddress().getHostAddress();
        out.println(discoveryMessage);
    }

    private String getResponseMessage(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return in.readLine();
    }
}