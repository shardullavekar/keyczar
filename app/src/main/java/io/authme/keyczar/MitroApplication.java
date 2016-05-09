package io.authme.keyczar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import keyczar.NewUserResponse;


// TODO: this should be a manager for all secrets instead of being a kludge to 
// communicate between activities.
// TODO: inheritance implies an "is a" relationship which is not true here.
public class MitroApplication {
    private Context context = null;
    boolean savePrivateKey;
    boolean isUserLoggedIn, isIntrodone;
    String loginErrorMessage = "Incorrect login info or bad network connection.";
    SharedPreferences.Editor editor;
    public SharedPreferences settings;
    private static final Gson gson = new Gson();
    private NewUserResponse user;
    private String deviceId;

    public MitroApplication(Context context) {
        this.context = context;
        settings = this.context.getSharedPreferences(this.context.getPackageName(), Context.MODE_PRIVATE);

        String user = getSharedPreference("user");
        if (user != null && user.length() > 2) {
            this.isUserLoggedIn = true;
            try {
                this.user = gson.fromJson(user, NewUserResponse.class);
            } catch (JsonSyntaxException e) {
                this.isUserLoggedIn = false;
                putSharedPreferences("user", null);
            }
        } else {
            this.isUserLoggedIn = false;
        }

    }

    public String getDeviceId() {
        if (null == getSharedPreference("deviceId")) {
            putSharedPreferences("deviceId", getRealDeviceId(context));
        }
        return getSharedPreference("deviceId");
    }

    private static String getRealDeviceId(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }


    public void setDeviceId(String deviceid) {
        putSharedPreferences("deviceId", deviceid);
    }

    public void setSavePrivateKey(boolean state) {
        savePrivateKey = state;
    }

    public boolean shouldSavePrivateKey() {
        return savePrivateKey;
    }

    public boolean isLoggedIn() {
        String isLoggedIn = getSharedPreference("isLoggedIn");
        if (isLoggedIn == null) {
            return false;
        }
        return Boolean.parseBoolean(isLoggedIn);
    }

    public boolean isPatternset() {
        return settings.getBoolean("ispatternset", false);
    }

    public void setPatternFlag(boolean status) {
        editor = settings.edit();
        editor.putBoolean("ispatternset", status);
        editor.apply();
    }

    public void setIntrodone(String status) {
        editor = settings.edit();
        editor.putString("intro_done_boolean", status);
        editor.commit();
    }

    public boolean getintroStatus() {
        String result = settings.getString("intro_done_boolean", "false");
        return Boolean.parseBoolean(String.valueOf(result));
    }

    public void setIsLoggedIn(boolean loginStatus) {
        isUserLoggedIn = loginStatus;
        putSharedPreferences("isLoggedIn", String.valueOf(loginStatus));
    }

    public void setLoginErrorMessage(String error) {
        loginErrorMessage = error;
    }

    public String getLoginErrorMessage() {
        return loginErrorMessage;
    }

    public void putSharedPreferences(String key, String value) {
        editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public Map<String, ?> getAllSettings() {
        return settings.getAll();
    }

    public void putSharedPreferences(String key, int value) {
        editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public String getSharedPreference(String key) {
        if (settings == null) {
            return null;
        }
        return settings.getString(key, null);
    }

    public String getSharedPreference(String key, String defaultValue) {
        if (settings == null) {
            return defaultValue;
        }
        return settings.getString(key, defaultValue);
    }



    public void setUser(NewUserResponse user) {
        this.user = user;
        putSharedPreferences("user", gson.toJson(user));
    }

    public NewUserResponse getUser() {
        return user;
    }

    public void deleteAllPreferences() {
        settings.edit().clear().apply();
    }

    public void writeDataToBackupFile(byte[] bytes, String fileName) {

        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            if (file.exists()) {
                file.delete();
                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            }

            Log.d("AUTHMEIO", "File saved to : " + file.getAbsolutePath());
            putSharedPreferences("keyfile_path", file.getAbsolutePath());
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(bytes);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
