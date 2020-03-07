package rescuecore2.messages.components;

import rescuecore2.messages.AbstractMessageComponent;
import rescuecore2.messages.Command;
import rescuecore2.messages.Message;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static rescuecore2.misc.EncodingTools.readInt32;
import static rescuecore2.misc.EncodingTools.readMessage;
import static rescuecore2.misc.EncodingTools.writeInt32;
import static rescuecore2.misc.EncodingTools.writeMessage;

/**
   A message component made up of a list of agent commands.
 */
public class CommandListComponent extends AbstractMessageComponent {
    private List<Command> commands;

    /**
       Construct a CommandListComponent with no content.
       @param name The name of the component.
    */
    public CommandListComponent(String name) {
        super(name);
        commands = new ArrayList<Command>();
    }

    /**
       Construct a CommandListComponent with a specific list of agent commands.
       @param name The name of the component.
       @param commands The agent commands in this message component.
    */
    public CommandListComponent(String name, Collection<? extends Command> commands) {
        super(name);
        this.commands = new ArrayList<Command>(commands);
    }

    /**
       Get the agent commands that make up this message component.
       @return The agent commands in this component.
    */
    public List<Command> getCommands() {
        return commands;
    }

    /**
       Set the commands that make up this message component.
       @param commands The commands in this component.
    */
    public void setCommands(Collection<? extends Command> commands) {
        this.commands = new ArrayList<Command>(commands);
    }

    @Override
    public void write(OutputStream out) throws IOException {
        writeInt32(commands.size(), out);
        for (Command next : commands) {
            writeMessage(next, out);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        writeInt32(commands.size(), out);
        for (Command next : commands) {
            writeMessage(next, out);
        }
    }

    @Override
    public void read(InputStream in) throws IOException {
        commands.clear();
        int size = readInt32(in);
        for (int i = 0; i < size; ++i) {
            Message m = readMessage(in);
            if (m instanceof Command) {
                commands.add((Command)m);
            }
            else {
                throw new IOException("Command list stream contained a non-command message: " + m + " (" + m.getClass().getName() + ")");
            }
        }
    }

    @Override
    public String toString() {
        return commands.size() + " commands";
    }

    @Override
    public int getBytesLength() {
        int total = 4; // the size of the commands
        for (Command c : commands) {
            total += 4; // urn string length
            total += c.getURN().length(); // urn string
            total += 4; // message size
            total += c.getBytesLength();
        }
        return total;
    }
}
