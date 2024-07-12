// ChatMessageReceiver.java
package com.yxc.websocketclientdemo.self;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.yxc.websocketclientdemo.MainActivity;
import com.yxc.websocketclientdemo.modle.ChatMessage;
import com.yxc.websocketclientdemo.util.WebSocketMessageBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ChatMessageReceiver extends BroadcastReceiver {

    private MainActivity mainActivity;
    private List<ChatMessage> chatMessageList;

    public ChatMessageReceiver(MainActivity mainActivity, List<ChatMessage> chatMessageList) {
        this.mainActivity = mainActivity;
        this.chatMessageList = chatMessageList;
    }

    // 默认构造函数，必须添加
    public ChatMessageReceiver() {
        // 这里可以为空，不需要实现逻辑
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int messageType = intent.getIntExtra("messageType", -1);
        ChatMessage chatMessage = new ChatMessage();
        if (messageType == WebSocketMessageBuilder.TYPE_TEXT) {
            String message = intent.getStringExtra("message");
            if (message != null && !message.isEmpty()) {
                chatMessage.setContent(message);
            }
        } else if (messageType == WebSocketMessageBuilder.TYPE_IMAGE) {
            String image = intent.getStringExtra("message");
            if (image != null) {
                chatMessage.setImagePath(image);
            }
        } else if (messageType == WebSocketMessageBuilder.TYPE_AUDIO) {
            String audio = intent.getStringExtra("message");
            if (audio != null) {
                chatMessage.setAudioPath(audio);
            }
        }
        chatMessage.setIsMeSend(0);
        chatMessage.setIsRead(1);
        chatMessage.setTime(System.currentTimeMillis() + "");
        if(chatMessageList!=null){
            chatMessageList.add(chatMessage);
            mainActivity.initChatMsgListView();
        }
    }

    private String saveAudioToFile(Context context, byte[] audioBytes) {
        File audioFile = new File(context.getFilesDir(), "audio_" + System.currentTimeMillis() + ".mp3");
        try (FileOutputStream fos = new FileOutputStream(audioFile)) {
            fos.write(audioBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return audioFile.getAbsolutePath();
    }
}