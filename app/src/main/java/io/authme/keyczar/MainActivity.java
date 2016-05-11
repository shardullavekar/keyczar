package io.authme.keyczar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.keyczar.DefaultKeyType;
import org.keyczar.Encrypter;
import org.keyczar.GenericKeyczar;
import org.keyczar.KeyVersion;
import org.keyczar.KeyczarKey;
import org.keyczar.SessionCrypter;
import org.keyczar.Signer;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.exceptions.NoPrimaryKeyException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import keyczar.JsonWriter;
import keyczar.KeyPair;
import keyczar.KeyczarJsonReader;
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
                r = new Request();
                tryKeyczar();
                //generateKey_local();
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

    private void tryKeyczar() {
        final GenericKeyczar[] mySignVerify = new GenericKeyczar[1];
        final GenericKeyczar[] myEncryptDecrypt = new GenericKeyczar[1];
        final GenericKeyczar[] group_signVerify = new GenericKeyczar[1];
        final GenericKeyczar[] group_encryptDecrypt = new GenericKeyczar[1];
        RestClient client = new RestClient(new Callback() {
            @Override
            public void onExecuted(String response) {
                if (!TextUtils.equals(response, "error")) {
                    try {
                        JSONObject jsonresponse = new JSONObject(response);
                        mySignVerify[0] = new GenericKeyczar(new KeyczarJsonReader(jsonresponse.getString("signing")));
                        myEncryptDecrypt[0] = new GenericKeyczar(new KeyczarJsonReader(jsonresponse.getString("encryption")));
                        RestClient restClient = new RestClient(new Callback() {
                            @Override
                            public void onExecuted(String response) {
                                if (!TextUtils.equals(response, "error")) {
                                    try {
                                        JSONObject jsonresponse_2 = new JSONObject(response);
                                        group_signVerify[0] = new GenericKeyczar(new KeyczarJsonReader(jsonresponse_2.getString("signing")));
                                        group_encryptDecrypt[0] = new GenericKeyczar(new KeyczarJsonReader(jsonresponse_2.getString("encryption")));
                                        encryptKeywithPassword(mySignVerify[0], myEncryptDecrypt[0], group_signVerify[0], group_encryptDecrypt[0]);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (KeyczarException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                        restClient.getKeys();
                    } catch (KeyczarException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                else {
                    Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
                }
            }
        });

        client.getKeys();
    }

    private void encryptKeywithPassword(GenericKeyczar signkey, GenericKeyczar encryptionkey, GenericKeyczar group_signkey, GenericKeyczar group_encrptionkey){
        KeyVersion keyVersion = null;
        try {
            keyVersion = signkey.getMetadata().getPrimaryVersion();
        } catch (NoPrimaryKeyException e) {
            e.printStackTrace();
        }
        String password = UUID.randomUUID().toString();
        app.putSharedPreferences("password", password);

        StringBuffer myOutputEncryptionKeyEncrypted = new StringBuffer();
        StringBuffer myOutputEncryptionKey = new StringBuffer();
        StringBuffer myOutputSignKeyEncrypted = new StringBuffer();
        StringBuffer myOutputSignKey = new StringBuffer();
        StringBuffer groupEncryptDecryptPrivatekey = new StringBuffer();
        StringBuffer groupSignVerifyPrivatekey = new StringBuffer();

        JsonWriter.writeEncrypted(encryptionkey, password, myOutputEncryptionKeyEncrypted);

        JsonWriter.writeEncrypted(signkey, password, myOutputSignKeyEncrypted);
        JsonWriter.write(encryptionkey, myOutputEncryptionKey);
        JsonWriter.write(signkey, myOutputSignKey);
        JsonWriter.write(group_encrptionkey, groupEncryptDecryptPrivatekey);
        JsonWriter.write(group_signkey, groupSignVerifyPrivatekey);

        Map<Integer, KeyczarKey> localMap = new HashMap<>();
        try {
            localMap.put(encryptionkey.getMetadata().getPrimaryVersion().getVersionNumber(), encryptionkey.getKey(encryptionkey.getMetadata().getPrimaryVersion()));
        } catch (NoPrimaryKeyException e) {
            e.printStackTrace();
        }

        Encrypter crypter = null;
        try {
            crypter = new Encrypter(new Util.MemoryKeyReader(group_encrptionkey.getMetadata(), localMap));
        } catch (KeyczarException e) {
            e.printStackTrace();
        }
        SessionCrypter s = null;
        try {
            s = new SessionCrypter(crypter);
        } catch (KeyczarException e) {
            e.printStackTrace();
        }

        KeyPair groupKeyPair = new KeyPair();
        groupKeyPair.encryption = groupEncryptDecryptPrivatekey.toString();
        groupKeyPair.signing = groupSignVerifyPrivatekey.toString();
        byte[] groupKeyEncryptedForMe = new byte[0];
        try {
            groupKeyEncryptedForMe = s.encrypt(gson.toJson(groupKeyPair).getBytes());
        } catch (KeyczarException e) {
            e.printStackTrace();
        }
        byte[] sessionMaterial = s.getSessionMaterial();
        byte[] packed = packByteStrings(sessionMaterial, groupKeyEncryptedForMe);


        r.groupKeyEncryptedForMe = Base64.encodeToString(packed, Base64.NO_WRAP);
        r.analyticsId = UUID.randomUUID().toString();
        try {
            r.publicKey = gson.toJson(getPublicKeyPair(encryptionkey, signkey));
        } catch (KeyczarException e) {
            e.printStackTrace();
        }
        try {
            r.groupPublicKey = gson.toJson(getPublicKeyPair(group_encrptionkey, group_signkey));
        } catch (KeyczarException e) {
            e.printStackTrace();
        }

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
        keyMap.put(keyVersion.getVersionNumber(), signkey.getKey(keyVersion));
        Util.MemoryKeyReader reader = new Util.MemoryKeyReader(signkey.getMetadata(), keyMap);
        Signer signer = null;
        try {
            signer = new Signer(reader);
            m.signature = signer.sign(m.request);
        } catch (KeyczarException e) {
            e.printStackTrace();
        }

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
    }
}
