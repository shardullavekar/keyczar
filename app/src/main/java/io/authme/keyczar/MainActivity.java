package io.authme.keyczar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.gson.Gson;

import org.keyczar.DefaultKeyType;
import org.keyczar.Encrypter;
import org.keyczar.GenericKeyczar;
import org.keyczar.KeyVersion;
import org.keyczar.KeyczarKey;
import org.keyczar.SessionCrypter;
import org.keyczar.Signer;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.exceptions.KeyczarException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import keyczar.JsonWriter;
import keyczar.KeyPair;
import keyczar.Message;
import keyczar.NewUserRequest;
import keyczar.Request;
import keyczar.Util;

public class MainActivity extends AppCompatActivity {
    private static final Gson gson = new Gson();
    Button generate;
    private Request r;
    MitroApplication app;
    String request, phoneNumber, finalName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        phoneNumber = "9591953812";
        finalName = "shardultest";
        app = new MitroApplication(getApplicationContext());
        generate = (Button) this.findViewById(R.id.generate);
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateKey_local();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void generateKey_local() {
        try {

            GenericKeyczar myEncryptDecrypt = Util.createKey(DefaultKeyType.RSA_PRIV, KeyPurpose.DECRYPT_AND_ENCRYPT);

            GenericKeyczar mySignVerify = Util.createKey(DefaultKeyType.RSA_PRIV, KeyPurpose.SIGN_AND_VERIFY);

            GenericKeyczar groupEncryptDecrypt = Util.createKey(DefaultKeyType.RSA_PRIV, KeyPurpose.DECRYPT_AND_ENCRYPT);

            GenericKeyczar groupSignVerify = Util.createKey(DefaultKeyType.RSA_PRIV, KeyPurpose.SIGN_AND_VERIFY);

            KeyVersion keyVersion = mySignVerify.getMetadata().getPrimaryVersion();
            String password = UUID.randomUUID().toString();
            app.putSharedPreferences("password", password);
            /*globalSettings.setCrypt(password);*/
            StringBuffer myOutputEncryptionKeyEncrypted = new StringBuffer();
            StringBuffer myOutputEncryptionKey = new StringBuffer();
            StringBuffer myOutputSignKeyEncrypted = new StringBuffer();
            StringBuffer myOutputSignKey = new StringBuffer();
            StringBuffer groupEncryptDecryptPrivatekey = new StringBuffer();
            StringBuffer groupSignVerifyPrivatekey = new StringBuffer();

            JsonWriter.writeEncrypted(myEncryptDecrypt, password, myOutputEncryptionKeyEncrypted);

            JsonWriter.writeEncrypted(mySignVerify, password, myOutputSignKeyEncrypted);
            JsonWriter.write(myEncryptDecrypt, myOutputEncryptionKey);
            JsonWriter.write(mySignVerify, myOutputSignKey);
            JsonWriter.write(groupEncryptDecrypt, groupEncryptDecryptPrivatekey);
            JsonWriter.write(groupSignVerify, groupSignVerifyPrivatekey);

            Map<Integer, KeyczarKey> localMap = new HashMap<>();
            localMap.put(myEncryptDecrypt.getMetadata().getPrimaryVersion().getVersionNumber(), myEncryptDecrypt.getKey(myEncryptDecrypt.getMetadata().getPrimaryVersion()));

            Encrypter crypter = new Encrypter(new Util.MemoryKeyReader(groupEncryptDecrypt.getMetadata(), localMap));
            SessionCrypter s = new SessionCrypter(crypter);

            KeyPair groupKeyPair = new KeyPair();
            groupKeyPair.encryption = groupEncryptDecryptPrivatekey.toString();
            groupKeyPair.signing = groupSignVerifyPrivatekey.toString();
            byte[] groupKeyEncryptedForMe = s.encrypt(gson.toJson(groupKeyPair).getBytes());
            byte[] sessionMaterial = s.getSessionMaterial();
            byte[] packed = packByteStrings(sessionMaterial, groupKeyEncryptedForMe);


            r.groupKeyEncryptedForMe = Base64.encodeToString(packed, Base64.NO_WRAP);
            r.analyticsId = UUID.randomUUID().toString();
            r.publicKey = gson.toJson(getPublicKeyPair(myEncryptDecrypt, mySignVerify));
            r.groupPublicKey = gson.toJson(getPublicKeyPair(groupEncryptDecrypt, groupSignVerify));


            KeyPair myKeyPairEncrypted = new KeyPair();
            KeyPair myKeyPair = new KeyPair();
            myKeyPair.encryption = myOutputEncryptionKey.toString();
            myKeyPair.signing = myOutputSignKey.toString();
            app.putSharedPreferences("myKeyPair", gson.toJson(myKeyPair));

            myKeyPairEncrypted.encryption = myOutputEncryptionKeyEncrypted.toString();
            myKeyPairEncrypted.signing = myOutputSignKeyEncrypted.toString();
            r.encryptedPrivateKey = gson.toJson(myKeyPairEncrypted);

            Message m = new Message();
            m.identity = r.userId;
            m.request = gson.toJson(r);

            Map<Integer, KeyczarKey> keyMap = new HashMap<>();
            keyMap.put(keyVersion.getVersionNumber(), mySignVerify.getKey(keyVersion));
            Util.MemoryKeyReader reader = new Util.MemoryKeyReader(mySignVerify.getMetadata(), keyMap);
            Signer signer = new Signer(reader);
            m.signature = signer.sign(m.request);

            NewUserRequest nur = new NewUserRequest();
            app.putSharedPreferences("myEmail", r.userId);
            app.putSharedPreferences("myPhoneNumber", phoneNumber);
            app.putSharedPreferences("myName", finalName);
            app.putSharedPreferences("password", password);
            nur.Email = r.userId;
            nur.Phone = phoneNumber;
            nur.Name = finalName;
            nur.XmppResource = UUID.randomUUID().toString();
            nur.MitroSignedRequest = gson.toJson(m);
            nur.Identifier = app.getDeviceId();

            request = gson.toJson(nur);

        } catch (KeyczarException e) {
            Log.e("KEYKEY", "Failed to generate key ", e);
        }


    }

    private void generateKey_server() {

    }

    private byte[] packByteStrings(byte[]... strings) {
        List<Byte> byteList = new LinkedList<>();
        byte[] lengthByte = encodeBigEndian(strings.length);
        appendBytes(byteList, lengthByte);

        for (byte[] string : strings) {
            // string length
            appendBytes(byteList, encodeBigEndian(string.length));
            // the string itself
            // TODO: Check that the string doesn't include out of range values?
            appendBytes(byteList, string);
        }
        byte[] finalB = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            Byte byt = byteList.get(i);
            finalB[i] = byt;
        }
        return finalB;
    }

    private void appendBytes(List<Byte> byteList, byte[] lengthByte) {
        for (byte b : lengthByte) {
            byteList.add(b);
        }
    }


    private byte[] encodeBigEndian(Integer number) {
        byte[] res = new byte[4];
        if (!(0 <= number && number <= 2147483647)) {
            throw new Error("number is out of range: " + number);
        }
        res[0] = (byte) ((number >> 24) & 0xff);
        res[1] = (byte) ((number >> 16) & 0xff);
        res[2] = (byte) ((number >> 8) & 0xff);
        res[3] = (byte) (number & 0xff);
        return res;
    }

    private KeyPair getPublicKeyPair(GenericKeyczar myEncryptDecrypt, GenericKeyczar mySignVerify) throws KeyczarException {
        KeyPair publicKeyPair = new KeyPair();

        publicKeyPair.encryption = JsonWriter.toString(new GenericKeyczar(Util.exportPublicKeys(myEncryptDecrypt)));

        if (mySignVerify != null) {
            publicKeyPair.signing = JsonWriter.toString(new GenericKeyczar(Util.exportPublicKeys(mySignVerify)));
        }

        return publicKeyPair;
    }
}
