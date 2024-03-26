import Color.Color;

import java.io.IOException;
import java.util.*;

public class PeerCLI {
    private int PORT;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print(Color.BLUE + "Enter your username: " + Color.RESET);
        String username = scanner.nextLine();
        while (username.isEmpty()) {
            System.out.println(Color.RED + "Username cannot be empty, please insert a valid username" + Color.RESET);
            username = scanner.nextLine();
        }
        System.out.print(Color.BLUE + "Enter your port (must be between 1024 and 49151): " + Color.RESET);
        int PORT = scanner.nextInt();
        while(PORT < 1024 || PORT > 49151){
            System.out.println(Color.RED + "Port must be between 1024 and 49151" + Color.RESET);
            PORT = scanner.nextInt();
        }
        scanner.nextLine();
        User user = new User(username, PORT);
        System.out.println(Color.BLUE + "Do you already know someone in the network? (y/n)" + Color.RESET);
        boolean loop = true;
        while(loop) {
            loop = false;
            String answer = scanner.nextLine();
            if (answer.equals("y") || answer.equals("Y") || answer.equals("yes") || answer.equals("Yes")) {
                System.out.println(Color.BLUE + "Enter the ip of a peer to connect to: " + Color.RESET);
                String ipPeer = scanner.nextLine();
                System.out.println(Color.BLUE + "Enter the port of a peer to connect to: " + Color.RESET);
                int portPeer = scanner.nextInt();
                user.startConnection(ipPeer, portPeer);
            }else if(!answer.equals("n") && !answer.equals("N") && !answer.equals("no") && !answer.equals("No")) {
                System.out.println(Color.RED + "Invalid choice. Please enter y or n." + Color.RESET);
                loop = true;
            }
        }
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
                    if(user.listPeers().isEmpty()){
                        System.out.println(Color.RED + "No peers found" + Color.RESET);
                    }
                    for(User peer: user.listPeers()){
                        System.out.println(Color.GREEN + peer.getUsername() + " on port: "+ peer.getPort() +"\n" + Color.RESET);
                    }
                    System.out.println(Color.RESET);
                    break;
                case 5:
                    System.out.println(Color.BLUE + "Exiting..." + Color.RESET);
                    scanner.close();
                    return;
                default:
                    System.out.println(Color.RED + "Invalid choice. Please enter a number between 1 and 5." + Color.RESET);
                    break;
            }
        }
    }
}