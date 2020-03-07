package rescuecore2.messages.components;

import rescuecore2.messages.AbstractMessageComponent;
import rescuecore2.worldmodel.Entity;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static rescuecore2.misc.EncodingTools.readEntity;
import static rescuecore2.misc.EncodingTools.readInt32;
import static rescuecore2.misc.EncodingTools.writeEntity;
import static rescuecore2.misc.EncodingTools.writeInt32;

/**
   An EntityList component to a message.
 */
public class EntityListComponent extends AbstractMessageComponent {
    private List<Entity> entities;

    /**
       Construct an EntityListComponent with no content.
       @param name The name of the component.
    */
    public EntityListComponent(String name) {
        super(name);
        entities = new ArrayList<Entity>();
    }

    /**
       Construct an EntityListComponent with a specific list of entities.
       @param name The name of the component.
       @param entities The entities in this message component.
    */
    public EntityListComponent(String name, Collection<? extends Entity> entities) {
        super(name);
        this.entities = new ArrayList<Entity>(entities);
    }

    /**
       Get the entities that make up this message component.
       @return The entities in this component.
    */
    public List<Entity> getEntities() {
        return entities;
    }

    /**
       Set the entities that make up this message component.
       @param entities The entities in this component.
    */
    public void setEntities(Collection<? extends Entity> entities) {
        this.entities = new ArrayList<Entity>(entities);
    }

    @Override
    public void write(OutputStream out) throws IOException {
        writeInt32(entities.size(), out);
        for (Entity next : entities) {
            writeEntity(next, out);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        writeInt32(entities.size(), out);
        for (Entity next : entities) {
            writeEntity(next, out);
        }
    }

    @Override
    public void read(InputStream in) throws IOException {
        entities.clear();
        int size = readInt32(in);
        for (int i = 0; i < size; ++i) {
            Entity e = readEntity(in);
            if (e != null) {
                entities.add(e);
            }
        }
    }

    @Override
    public String toString() {
        return getName() + " = " + entities.size() + " entities";
    }

    @Override
    public int getBytesLength() {
        int total = 4; // the size of the entity list
        for (Entity e : entities) {
            total += 4; // entity urn string length
            total += e.getURN().length();
            total += 4; // entityid integer
            total += 4; // size of the entity content
            total += e.getBytesLength();
        }
        return total;
    }
}
