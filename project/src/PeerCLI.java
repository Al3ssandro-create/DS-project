import Color.Color;

import java.io.IOException;
import java.util.*;

public class PeerCLI {
    private int PORT;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.print(Color.BLUE + "Enter your username: " + Color.RESET);
        String username = scanner.nextLine();
        if (username.isEmpty()) {
            System.out.println(Color.RED + "Username cannot be empty" + Color.RESET);
            return;
        }
        System.out.print(Color.BLUE + "Enter your port (must be between 1024 and 49151): " + Color.RESET);
        int PORT = scanner.nextInt();
        if(PORT < 1024 || PORT > 49151){
            System.out.println(Color.RED + "Port must be between 1024 and 49151" + Color.RESET);
            return;
        }
        User user = new User(username, PORT);
        user.startPeerDiscovery();

        while (true) {
            System.out.println(Color.BLUE + "1) Create a new Room\n2) See the list of all your rooms\n3) See the chat in a particular room\n4) List all peers\n5) Exit" + Color.RESET);
            System.out.print(Color.BLUE + "Enter your choice: " + Color.RESET);
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    System.out.print(Color.BLUE + "Creating room name: " + Color.RESET);
                    String roomName = scanner.nextLine();
                    if (roomName.isEmpty()) {
                        System.out.println(Color.RED + "Room name cannot be empty" + Color.RESET);
                        continue;
                    }
                    user.createRoom(roomName);
                    break;
                case 2:
                    System.out.println(Color.GREEN);
                    user.listRooms();
                    System.out.println(Color.RESET);
                    break;
                case 3:
                    System.out.print(Color.BLUE + "Enter room name: " + Color.RESET);
                    String roomToEnter = scanner.nextLine();
                    System.out.println(Color.GREEN);
                    user.setRoom(user.rooms.get(roomToEnter));
                    user.viewChat(roomToEnter);
                    System.out.println(Color.RESET);
                    while (true) {
                        System.out.println(Color.BLUE + "1) Add a message to the room chat\n2) Exit the room" + Color.RESET);
                        System.out.print(Color.BLUE + "Enter your choice: " + Color.RESET);
                        int roomChoice = scanner.nextInt();
                        scanner.nextLine();
                        switch (roomChoice) {
                            case 1:
                                System.out.print(Color.BLUE + "Enter your message: " + Color.RESET);
                                String message = scanner.nextLine();
                                Room room = user.getRoom();
                                room.addMessage(new Message(user, message, room.getUserSequenceNumbers().get(user) + 1));
                                user.viewChat(roomToEnter);
                                break;
                            case 2:
                                System.out.println(Color.BLUE + "Exiting room..." + Color.RESET);
                                user.setRoom(null);
                                break;
                            default:
                                System.out.println(Color.RED + "Invalid choice. Please enter a number between 1 and 2." + Color.RESET);
                                break;
                        }
                        if (roomChoice == 2) break;
                    }
                    break;
                case 4:
                    System.out.println(Color.GREEN);
                    for(User peer: user.listPeers()){
                        System.out.println(Color.GREEN + peer.getUsername() + " on port: "+ peer.getPort() +"\n" + Color.RESET);
                    }
                    System.out.println(Color.RESET);
                    break;
                case 5:
                    System.out.println(Color.BLUE + "Exiting..." + Color.RESET);
                    return;
                default:
                    System.out.println(Color.RED + "Invalid choice. Please enter a number between 1 and 5." + Color.RESET);
                    break;
            }
        }
    }
}