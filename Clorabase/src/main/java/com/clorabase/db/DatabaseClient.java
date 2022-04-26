package com.clorabase.db;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class DatabaseClient extends WebSocketClient {
    private String result;
    private CountDownLatch latch;
    private Exception error;

    protected DatabaseClient(String url, Map<String, String> httpHeaders) {
        super(URI.create(url), httpHeaders);
    }

    @Override
    public void onOpen(ServerHandshake session) {

    }

    @Override
    public void onMessage(String message) {
        try {
            if (message.startsWith("[") && message.endsWith("]")) {
                JSONArray jsonArray = new JSONArray(message);
                result = jsonArray.toString();
            }
            JSONObject json = new JSONObject(message);
            var error = json.optString("error");
            if (error.isEmpty()) {
                result = json.toString();
            } else
                this.error = new Exception(error);
        } catch (JSONException ignored){

        }

        latch.countDown();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (code == 500)
            error = new Exception(reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        error = ex;
    }


    public String sendMessage(JSONObject jsonObject) throws Exception {
        super.send(jsonObject.toString());
        latch = new CountDownLatch(1);
        latch.await();
        if (result == null)
            throw error instanceof WebsocketNotConnectedException ? new IllegalStateException("Database was not initialized successfully. Check your System.err for more information.") : error;
        else
            return result;
    }
}
