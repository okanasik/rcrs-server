package rescuecore2.standard.entities;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
   The FireStation object.
 */
public class FireStation extends Building {
    /**
       Construct a FireStation object with entirely undefined values.
       @param id The ID of this entity.
     */
    public FireStation(EntityID id) {
        super(id, StandardEntityType.FIRE_STATION);
    }

    @Override
    protected Entity copyImpl() {
        return new FireStation(getID());
    }
}