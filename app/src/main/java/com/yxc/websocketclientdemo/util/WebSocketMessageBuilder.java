package com.yxc.websocketclientdemo.util;

import org.json.JSONObject;
import android.util.Base64;

public class WebSocketMessageBuilder {

    public static final int TYPE_TEXT = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_AUDIO = 3;

    public static String createTextMessage(String content) {
        return createMessage(TYPE_TEXT, content);
    }

    public static String createImageMessage(String imagePath) {
        return createMessage(TYPE_IMAGE, imagePath);
    }

    public static String createAudioMessage(String audioPath) {
        return createMessage(TYPE_AUDIO, audioPath);
    }

    private static String createMessage(int type, String content) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type);
            jsonObject.put("content", content);
            return jsonObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

