package rescuecore2.messages.components;

import rescuecore2.messages.AbstractMessageComponent;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
   An ChangeSet component to a message.
 */
public class ChangeSetComponent extends AbstractMessageComponent {
    private ChangeSet changes;

    /**
       Construct a ChangeSetComponent with no content.
       @param name The name of the component.
    */
    public ChangeSetComponent(String name) {
        super(name);
        changes = new ChangeSet();
    }

    /**
       Construct a ChangeSetComponent with a specific set of changes.
       @param name The name of the component.
       @param changes The changes in this message component.
    */
    public ChangeSetComponent(String name, ChangeSet changes) {
        super(name);
        this.changes = new ChangeSet(changes);
    }

    /**
       Get the ChangeSet.
       @return The ChangeSet.
    */
    public ChangeSet getChangeSet() {
        return changes;
    }

    /**
       Set the ChangeSet.
       @param newChanges The new ChangeSet.
    */
    public void setChangeSet(ChangeSet newChanges) {
        this.changes = new ChangeSet(newChanges);
    }

    @Override
    public void write(OutputStream out) throws IOException {
        changes.write(out);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        changes.write(out);
    }

    @Override
    public void read(InputStream in) throws IOException {
        changes = new ChangeSet();
        changes.read(in);
    }

    @Override
    public String toString() {
        return getName() + " = " + changes.getChangedEntities().size() + " entities";
    }

    @Override
    public int getBytesLength() {
        int total = 0;
        total += 4; // the size of changeset
        for ( Map.Entry<EntityID, Map<String, Property>> next : changes.getChangeMap().entrySet()) {
            total += 4; // entityid
            total += 4; // entity urn string length
            total += changes.getEntityURN(next.getKey()).length(); // entity urn string
            total += 4; // the count of properties
            for (Property p : next.getValue().values()) {
                total += 4; // urn string length
                total += p.getURN().length(); // urn string
                total += 1; // is defined boolean written as 1 byte
                if (p.isDefined()) {
                    total += 4; // size o fhe property
                    // size of the property write
                    total += p.getBytesLength();
                }
            }
        }
        // add deleted
        total += 4; // the count of deleted
        total += 4 * changes.getDeletedEntities().size();
        return total;
    }
}
