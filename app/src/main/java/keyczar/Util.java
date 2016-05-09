package keyczar;

import org.keyczar.Crypter;
import org.keyczar.DefaultKeyType;
import org.keyczar.Encrypter;
import org.keyczar.GenericKeyczar;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyVersion;
import org.keyczar.KeyczarKey;
import org.keyczar.SessionCrypter;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.enums.RsaPadding;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.i18n.Messages;
import org.keyczar.interfaces.KeyczarReader;
import org.keyczar.keyparams.RsaKeyParameters;
import org.keyczar.util.Base64Coder;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by shardul on 08-05-2016.
 */
public class Util {
    private Util() {
    }

    /**
     * Returns a GenericKeyczar containing a new generated key of type.
     */
    public static GenericKeyczar createKey(DefaultKeyType type, KeyPurpose purpose)
            throws KeyczarException {
        return createKey(type, purpose, type.applyDefaultParameters(null).getKeySize());
    }

    /**
     * Returns a GenericKeyczar containing a new generated key of type.
     */
    public static GenericKeyczar createKey(DefaultKeyType type, KeyPurpose purpose, final int size)
            throws KeyczarException {
        KeyMetadata metadata = new KeyMetadata("Key", purpose, type);
        KeyczarReader reader = new MemoryKeyReader(metadata, null);
        GenericKeyczar keyczar = new GenericKeyczar(reader);
        keyczar.addVersion(KeyStatus.PRIMARY, new RsaKeyParameters() {
            @Override
            public RsaPadding getRsaPadding() throws KeyczarException {
                return RsaPadding.OAEP;
            }

            @Override
            public int getKeySize() throws KeyczarException {
                return size;
            }
        });
        return keyczar;
    }

    /**
     * Returns a KeyczarReader containing a new generated key of type.
     */
    public static KeyczarReader generateKeyczarReader(DefaultKeyType type, KeyPurpose purpose)
            throws KeyczarException {
        return generateKeyczarReader(type, purpose, type.applyDefaultParameters(null).getKeySize());
    }

    /**
     * Returns a KeyczarReader containing a new generated key of type.
     */
    public static KeyczarReader generateKeyczarReader(
            DefaultKeyType type, KeyPurpose purpose, int size) throws KeyczarException {
        return readerFromKeyczar(createKey(type, purpose, size));
    }

    /**
     * Writes input to a JSON file at path.
     */
    public static void writeJsonToPath(GenericKeyczar input, String path) {
        try {
            FileOutputStream output = new FileOutputStream(path);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
            JsonWriter.write(input, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Crypter crypterFromJson(String json) throws KeyczarException {
        KeyczarReader key = new KeyczarJsonReader(json);
        return new Crypter(key);
    }

    /**
     * Encrypts plaintext with a new session key, which is encrypted using crypter. The encrypted session
     * key and encrypted message are packed together using lenPrefixPack().
     */
    public static byte[] encryptWithSession(Encrypter crypter, byte[] plaintext) throws KeyczarException {
        // Create a session crypter
        SessionCrypter session = new SessionCrypter(crypter);
        byte[] rawEncrypted = session.encrypt(plaintext);
        byte[][] input = {session.getSessionMaterial(), rawEncrypted};
        return org.keyczar.util.Util.lenPrefixPack(input);
    }

    /**
     * Decrypts message as a session by unpacking the encrypted session key, decrypting it with crypter,
     * then decrypting the message.
     */
    public static byte[] decryptWithSession(Crypter crypter, byte[] ciphertext) throws KeyczarException {
        byte[][] unpacked = org.keyczar.util.Util.lenPrefixUnpack(ciphertext);
        SessionCrypter session = new SessionCrypter(crypter, unpacked[0]);
        return session.decrypt(unpacked[1]);
    }

    public static String encryptWithSession(Encrypter crypter, String plaintext) throws KeyczarException {
        try {
            return Base64Coder.encodeWebSafe(encryptWithSession(crypter, plaintext.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    public static String decryptWithSession(Crypter crypter, String ciphertext) throws KeyczarException {
        try {
            return new String(decryptWithSession(crypter, Base64Coder.decodeWebSafe(ciphertext)), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    // Hack to get access to KeyczarPrivateKey.getPublic(); see exportPublicKeys
    static final Method getPublicMethod;

    static {
        Class<?> iface;
        try {
            iface = Util.class.getClassLoader().loadClass("org.keyczar.KeyczarPrivateKey");
            getPublicMethod = iface.getMethod("getPublic");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        getPublicMethod.setAccessible(true);
    }

    /**
     * Returns a new Keyczar with public keys in privateKey.
     *
     * @throws KeyczarException
     */
    // This is basically copied from GenericKeyczar.publicKeyExport, which is not public.
    // TODO: Get this into GenericKeyczar upstream?
    public static KeyczarReader exportPublicKeys(GenericKeyczar privateKey) throws KeyczarException {
        KeyMetadata kmd = privateKey.getMetadata();
        // Can only export if type is *_PRIV
        KeyMetadata publicKmd = null;
        if (kmd.getType() == DefaultKeyType.DSA_PRIV) {
            if (kmd.getPurpose() == KeyPurpose.SIGN_AND_VERIFY) {
                publicKmd = new KeyMetadata(kmd.getName(), KeyPurpose.VERIFY,
                        DefaultKeyType.DSA_PUB);
            }
        } else if (kmd.getType() == DefaultKeyType.RSA_PRIV) {
            switch (kmd.getPurpose()) {
                case DECRYPT_AND_ENCRYPT:
                    publicKmd = new KeyMetadata(kmd.getName(), KeyPurpose.ENCRYPT,
                            DefaultKeyType.RSA_PUB);
                    break;
                case SIGN_AND_VERIFY:
                    publicKmd = new KeyMetadata(kmd.getName(), KeyPurpose.VERIFY,
                            DefaultKeyType.RSA_PUB);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid key purpose: " + kmd.getPurpose());
            }
        }
        if (publicKmd == null) {
            throw new KeyczarException(
                    Messages.getString("KeyczarTool.CannotExportPubKey",
                            kmd.getType(), kmd.getPurpose()));
        }

        HashMap<Integer, KeyczarKey> keys = new HashMap<Integer, KeyczarKey>();
        for (KeyVersion version : privateKey.getVersions()) {
            // hack to work around keyczar's accessibility limits
            try {
                KeyczarKey key = privateKey.getKey(version);
                KeyczarKey publicKey = (KeyczarKey) getPublicMethod.invoke(key);
                publicKmd.addVersion(version);
                keys.put(version.getVersionNumber(), publicKey);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        return new MemoryKeyReader(publicKmd, keys);
    }

    /**
     * Create a KeyczarReader from an existing key. Useful for creating a GenericKeyczar
     * to manipulate a keyset, then "re-open" the key as the correct type to use it
     * Example: Crypter crypter = new Crypter(Util.readerFromKeyczar(keyczar));
     *
     * @param keyczar
     * @return
     */
    public static KeyczarReader readerFromKeyczar(GenericKeyczar keyczar) {
        HashMap<Integer, KeyczarKey> keys = new HashMap<Integer, KeyczarKey>();
        for (KeyVersion v : keyczar.getVersions()) {
            keys.put(v.getVersionNumber(), keyczar.getKey(v));
        }
        return new MemoryKeyReader(keyczar.getMetadata(), keys);
    }

    /**
     * Basically a copy of ImportedKeyReader which is not public.
     */
    public static final class MemoryKeyReader implements KeyczarReader {
        private final KeyMetadata metadata;
        private final Map<Integer, KeyczarKey> keys;

        public MemoryKeyReader(KeyMetadata metadata, Map<Integer, KeyczarKey> keys) {
            this.metadata = metadata;
            this.keys = keys;
            assert keys == null || metadata.getVersions().size() == keys.size();
        }

        @Override
        public String getKey(int version) throws KeyczarException {
            return keys.get(version).toString();
        }

        @Override
        public String getKey() throws KeyczarException {
            return keys.get(metadata.getPrimaryVersion().getVersionNumber()).toString();
        }

        @Override
        public String getMetadata() throws KeyczarException {
            return metadata.toString();
        }
    }
}
