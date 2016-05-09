package keyczar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.keyczar.KeyMetadata;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

/**
 * Created by shardul on 08-05-2016.
 */
public class KeyczarJsonReader implements KeyczarReader {
    private final JsonObject parsed;

    public KeyczarJsonReader(String json) {
        JsonParser parser = new JsonParser();
        parsed = parser.parse(json).getAsJsonObject();
    }

    @Override
    public String getKey(int version) throws KeyczarException {
        return parsed.get(Integer.toString(version)).getAsString();
    }

    @Override
    public String getKey() throws KeyczarException {
        KeyMetadata metadata = KeyMetadata.read(getMetadata());

        return getKey(metadata.getPrimaryVersion().getVersionNumber());
    }

    @Override
    public String getMetadata() throws KeyczarException {
        return parsed.get("meta").getAsString();
    }
}
