package Vector;

import java.io.Serializable;
import java.util.*;

public class VectorClock implements Serializable{
    private Map<UUID, Integer> vector = new HashMap<>();

    public VectorClock(Set<UUID> users){
        for(UUID user : users){
            vector.put(user, 0);
        }
    }

    public VectorClock(Map<UUID, Integer> vector){
        this.vector = vector;
    }

    public VectorClock copy(){
        Map<UUID, Integer> copy = new HashMap<>();
        for(UUID user : vector.keySet()){
            copy.put(user, vector.get(user));
        }
        return new VectorClock(copy);
    }

    public void updateUser(UUID userId, int val){
        vector.put(userId, val);
    }

    public void incrementUser(UUID userId){
        int val = vector.get(userId) + 1;
        vector.put(userId, val);
    }

    public Map<UUID, Integer> getVector(){
        return this.vector;
    }

    public void update(VectorClock vectorClock){
        Map<UUID, Integer> vc = vectorClock.getVector();
        for(UUID user: vector.keySet()){
            if(vc.get(user) > vector.get(user))
                vector.put(user, vc.get(user));
        }
    }

    @Override
    public String toString(){
        StringBuilder clock = new StringBuilder();
        for(UUID user: vector.keySet())
            clock.append(user).append(": ").append(vector.get(user)).append("\n");
        return clock.toString();
    }
}
