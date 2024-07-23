package Vector;

import java.io.Serializable;
import java.util.*;

//questa classe ha un mappa a cui associa ad ogni uiserID il suo clock scalare
//creando così il clock vettoriale
public class VectorClock implements Serializable{
    private Map<UUID, Integer> vector = new HashMap<>();

    public VectorClock(Set<UUID> users){
        for(UUID user : users){
            vector.put(user, 0);
        }
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
        String clock = "";
        for(UUID user: vector.keySet())
            clock += user + ": " + vector.get(user) + "\n";
        return clock;
    }
}
