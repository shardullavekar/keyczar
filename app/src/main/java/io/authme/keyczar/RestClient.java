package io.authme.keyczar;

import android.os.AsyncTask;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by shardul on 10-05-2016.
 */
public class RestClient {
    OkHttpClient client;
    JSONObject keys;
    Callback callback;
    private static String url = "http://authme.io:3000/key/new";
    public RestClient(Callback callback) {
        client = new OkHttpClient();
        this.callback = callback;
    }

    public void getKeys() {
        new ExecuteGet().execute();
    }

    public String getSigningKey() throws JSONException {
        return keys.getJSONObject("signing").toString();
    }

    public String getEncryptionKey() throws JSONException {
        return keys.getJSONObject("encryption").toString();
    }

    private class ExecuteGet extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
                return "ioException";
            }
            try {
                keys = new JSONObject(response.body().string());
            } catch (JSONException e) {
                e.printStackTrace();
                return "jsonException";
            } catch (IOException e) {
                e.printStackTrace();
                return "ioException";
            }

            return "ok";
        }

        @Override
        protected void onPostExecute(String s) {
            if (TextUtils.equals(s, "ok")) {
                callback.onExecuted(keys.toString());
            }
            else {
                callback.onExecuted("error");
            }
        }
    }
}
