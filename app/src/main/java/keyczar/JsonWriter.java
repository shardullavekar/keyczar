package keyczar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.keyczar.GenericKeyczar;
import org.keyczar.KeyVersion;
import org.keyczar.KeyczarFileReader;
import org.keyczar.exceptions.KeyczarException;

import java.lang.reflect.Type;

/**
 * Created by shardul on 08-05-2016.
 */
public class JsonWriter {
    public static final class KeyczarSerializer implements
            JsonSerializer<GenericKeyczar> {
        // Everything must be final so this is thread-safe
        private final String password;

        public KeyczarSerializer(String password) {
            this.password = password;
        }

        @Override
        public JsonElement serialize(GenericKeyczar keyczar, Type typeOfKey,
                                     JsonSerializationContext context) {
            // key.toString() returns the serialized metadata
            // this is a bit gross: JSON embedded in JSON, but it makes it
            // easier to integrate
            JsonObject obj = new JsonObject();

            // hack to set encrypted = true if needed
            String metadataString = keyczar.toString();
            if (password != null && !keyczar.getMetadata().isEncrypted()) {
                String out = metadataString.replaceFirst("\"encrypted\":false", "\"encrypted\":true");
                assert !out.equals(metadataString);
                metadataString = out;
            }
            obj.addProperty("meta", metadataString);

            for (KeyVersion version : keyczar.getVersions()) {
                String keyData = keyczar.getKey(version).toString();

                if (password != null) {
                    try {
                        keyData = KeyczarPBEReader.encryptKey(keyData, password);
                    } catch (KeyczarException e) {
                        throw new RuntimeException(e);
                    }
                }

                obj.addProperty(Integer.toString(version.getVersionNumber()),
                        keyData);
            }

            return obj;
        }
    }

    private static final Gson gson;

    static {
        // gson itself is thread-safe. Our KeyczarSerializer must be as well.
        KeyczarSerializer serializer = new KeyczarSerializer(null);
        gson = new GsonBuilder().registerTypeAdapter(
                GenericKeyczar.class, serializer).create();
    }

    /** Writes input Keyczar key as a JSON string to output. */
    public static void write(GenericKeyczar input, Appendable output) {
        gson.toJson(input, output);
    }

    /** Returns input Keyczar key as a JSON string. */
    public static String toString(GenericKeyczar input) {
        return gson.toJson(input);
    }

    public static void writeEncrypted(GenericKeyczar input, String password, Appendable output) {
        KeyczarSerializer serializer = new KeyczarSerializer(password);

        Gson gson = new GsonBuilder().registerTypeAdapter(
                GenericKeyczar.class, serializer).create();
        gson.toJson(input, output);
    }

    public static void main(String[] arguments) throws KeyczarException {
        if (arguments.length != 1) {
            System.err.println("JsonWriter (input key path)");
            System.err.println("  Reads a key and writes to stdout as JSON");
            System.exit(1);
        }

        GenericKeyczar keyczar = new GenericKeyczar(new KeyczarFileReader(arguments[0]));
        write(keyczar, System.out);
    }
}
