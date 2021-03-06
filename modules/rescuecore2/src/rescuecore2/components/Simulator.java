package rescuecore2.components;

import kernel.EntityIDGenerator;
import rescuecore2.config.Config;
import rescuecore2.connection.Connection;
import rescuecore2.messages.control.KSCommands;
import rescuecore2.messages.control.KSUpdate;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;

import java.util.Collection;

/**
   Sub-interface for Simulator components.
 */
public interface Simulator extends Component {
    /**
       Notification that this simulator has been connected to the kernel.
       @param c The connection to the kernel.
       @param simulatorID The ID of this simulator.
       @param entities The set of Entities the kernel sent to this simulator on connection.
       @param config The Config the kernel send to this simulator on connection.
     */
    void postConnect(Connection c, int simulatorID, Collection<Entity> entities, Config config);
    int getID();

    void setEntityIDGenerator(EntityIDGenerator idGenerator);
    void handleUpdate(KSUpdate update);
    void processCommands(KSCommands commands, ChangeSet change);
}
