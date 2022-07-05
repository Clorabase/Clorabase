package com.clorabase.db.clorem;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CloremClient extends WebSocketClient {
    private String result;
    private final String url;
    private final Map<String,String> headers;
    private CountDownLatch latch;
    private Exception error;

    protected CloremClient(String url, Map<String, String> httpHeaders) {
        super(URI.create(url), httpHeaders);
        this.url = url;
        this.headers = httpHeaders;
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
            } else {
                JSONObject json = new JSONObject(message);
                var error = json.optString("error");
                if (error.isEmpty()) {
                    result = json.toString();
                } else
                    this.error = new Exception(error);
            }
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
        try {
            send(jsonObject.toString());
            latch = new CountDownLatch(1);
            latch.await();
            if (result == null)
                throw error instanceof WebsocketNotConnectedException ? new IllegalStateException("Database was not initialized successfully. Check your System.err for more information.") : error;
            else
                return result;
        } catch (WebsocketNotConnectedException e) {
            var client = new CloremClient(url,headers);
            if (client.connectBlocking(5, TimeUnit.SECONDS))
                return client.sendMessage(jsonObject);
            else
                throw new IllegalStateException("Could not connect to database. Please check your internet connection and try again.");
        }
    }
}
