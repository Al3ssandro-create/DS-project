package Entities;
import java.io.Serializable;
import java.util.UUID;

public class UserTuple implements Serializable {
    private final UUID id;
    private final String username;

    public UserTuple(UUID id, String username) {
        this.id = id;
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}