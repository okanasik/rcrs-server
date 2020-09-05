package kernel;

import rescuecore2.config.Config;
import rescuecore2.worldmodel.Entity;

import java.util.Collection;

public class ControlledEntityInfo {
    Entity entity;
    Collection<? extends Entity> visibleSet;
    Config config;

    public ControlledEntityInfo(Entity entity, Collection<? extends Entity> visibleSet, Config config) {
        this.entity = entity;
        this.visibleSet = visibleSet;
        this.config = config;
    }

    @Override
    public String toString() {
        return entity.toString();
    }
}
