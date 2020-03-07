package rescuecore2.messages.components;

import rescuecore2.config.Config;
import rescuecore2.messages.AbstractMessageComponent;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import static rescuecore2.misc.EncodingTools.readInt32;
import static rescuecore2.misc.EncodingTools.readString;
import static rescuecore2.misc.EncodingTools.writeInt32;
import static rescuecore2.misc.EncodingTools.writeString;

/**
   A Config component to a message.
 */
public class ConfigComponent extends AbstractMessageComponent {
    private Config config;

    /**
       Construct a ConfigComponent with no content.
       @param name The name of the component.
     */
    public ConfigComponent(String name) {
        super(name);
        config = new Config();
    }

    /**
       Construct a ConfigComponent with a specific config as content.
       @param name The name of the component.
       @param data The content.
     */
    public ConfigComponent(String name, Config data) {
        super(name);
        this.config = data;
    }

    /**
       Get the content of this message component.
       @return The content of the component.
     */
    public Config getConfig() {
        return config;
    }

    /**
       Set the content of this message component.
       @param config The new content.
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        Set<String> keys = config.getAllKeys();
        writeInt32(keys.size(), out);
        for (String key : keys) {
            writeString(key, out);
            writeString(config.getValue(key), out);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Set<String> keys = config.getAllKeys();
        writeInt32(keys.size(), out);
        for (String key : keys) {
            writeString(key, out);
            writeString(config.getValue(key), out);
        }
    }

    @Override
    public void read(InputStream in) throws IOException {
        int count = readInt32(in);
        config = new Config();
        for (int i = 0; i < count; ++i) {
            String key = readString(in);
            String value = readString(in);
            config.setValue(key, value);
        }
    }

    @Override
    public String toString() {
        return getName() + " (" + config.getAllKeys().size() + " entries)";
    }

    @Override
    public int getBytesLength() {
        int total = 4; // the count of the keys
        for (String key : config.getAllKeys()) {
            total += 4; // the length of the key string
            total += key.length(); // bytes of the string
            total += 4; // the length of the value string
            total += config.getValue(key).length();
        }
        return total;
    }
}
