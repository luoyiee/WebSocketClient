package com.yxc.websocketclientdemo.im;

import static com.yxc.websocketclientdemo.util.WebSocketMessageBuilder.TYPE_TEXT;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.yxc.websocketclientdemo.MainActivity;
import com.yxc.websocketclientdemo.R;
import com.yxc.websocketclientdemo.self.PermissionRequestCallback;
import com.yxc.websocketclientdemo.util.Util;
import com.yxc.websocketclientdemo.util.WebSocketMessageBuilder;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;


public class JWebSocketClientService extends Service {
    public JWebSocketClient client;
    private JWebSocketClientBinder mBinder = new JWebSocketClientBinder();
    private final static int GRAY_SERVICE_ID = 1001;
    private PermissionRequestCallback permissionRequestCallback;


    public void setPermissionRequestCallback(PermissionRequestCallback callback) {
        this.permissionRequestCallback = callback;
    }

    private void someMethodToRequestPermission(String content) {
        if (permissionRequestCallback != null) {
            permissionRequestCallback.requestPermissions(content);
        }
    }

    //灰色保活
    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    PowerManager.WakeLock wakeLock;//锁屏唤醒

    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    @SuppressLint("InvalidWakeLockTag")
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    //用于Activity和service通讯
    public class JWebSocketClientBinder extends Binder {
        public JWebSocketClientService getService() {
            return JWebSocketClientService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //初始化websocket
        initSocketClient();
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测

        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < 18) {
            // Android 4.3以下，隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else if (Build.VERSION.SDK_INT >= 18 && Build.VERSION.SDK_INT < 26) {
            // Android 4.3 - Android 7.0，隐藏Notification上的图标
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            // Android 8.0以上，创建有效的通知渠道和通知
            String channelId = "ForegroundServiceChannel";
            String channelName = "Foreground Service Channel";
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
                manager.createNotificationChannel(channel);

                Notification notification = new NotificationCompat.Builder(this, channelId)
                        .setContentTitle("Foreground Service")
                        .setContentText("Service is running")
                        .setSmallIcon(R.drawable.icon) // 你需要替换这个图标
                        .build();

                startForeground(GRAY_SERVICE_ID, notification);
            }
        }

        acquireWakeLock();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        closeConnect();
        super.onDestroy();
    }

    public JWebSocketClientService() {
    }


    /**
     * 初始化websocket连接
     */
    private void initSocketClient() {
        URI uri = URI.create(Util.ws);
        client = new JWebSocketClient(uri) {
            @Override
            public void onMessage(String message) {
                Log.e("JWebSocketClientService", "收到的消息：" + message);
                try {
                    JSONObject jsonObject = new JSONObject(message);
                    int messageType = jsonObject.getInt("type");
                    Intent intent = new Intent();
                    intent.setAction("com.xch.servicecallback.content");
                    intent.putExtra("messageType", messageType);
                    String content = jsonObject.getString("content");
                    intent.putExtra("message", content);
                    intent.setPackage("com.yxc.websocketclientdemo");
                    sendBroadcast(intent);
//                    checkLockAndShowNotification(message);
                    // 请求权限
                    someMethodToRequestPermission(message);
                } catch (Exception e) {
                    e.printStackTrace();
                    Intent intent = new Intent();
                    intent.setAction("com.xch.servicecallback.content");
                    intent.setPackage("com.yxc.websocketclientdemo");
                    intent.putExtra("messageType", TYPE_TEXT); // 或 "image" 或 "audio"
                    intent.putExtra("message", message);
//                    sendBroadcast(intent);

                    sendBroadcast(intent);
                    Log.d("BroadcastSender", "Sending broadcast with message: " + message);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                        /// 跨应用间使用
////                this.registerReceiver(smsReceiver, filter, Context.RECEIVER_EXPORTED);
//                        // 应用内使用
//                        this.registerReceiver(smsReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
//                    } else {
//                        this.registerReceiver(smsReceiver, intentFilter)
//                    }
                }
            }

//            @Override
//            public void onMessage(ByteBuffer bytes) {
//                byte[] byteArray = new byte[bytes.remaining()];
//                bytes.get(byteArray);
//                // Handle received binary message (e.g., image)
//                handleBinaryMessage(byteArray);
//            }

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
                Log.e("JWebSocketClientService", "websocket连接成功");
            }
        };
        connect();
    }

    private void handleBinaryMessage(byte[] byteArray) {
        Log.e("JWebSocketClient", "Received binary message of length: " + byteArray.length);
        // 将图片数据广播出去，或者直接在这里处理
        Intent intent = new Intent("com.xch.servicecallback.content");
        intent.putExtra("image", byteArray);
        // 需要一个 Context 来发送广播，这里假设你有一个 context 对象
        sendBroadcast(intent);
    }

    /**
     * 连接websocket
     */
    private void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    client.connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendMsg(String msg) {
        if (null != client && client.isOpen()) {
            Log.e("JWebSocketClientService", "发送的消息：" + msg);
            client.send(msg);
        } else {
            Util.showToast(this, "连接已断开，请稍等或重启App哟");
        }
    }

    public void sendTextMessage(String content) {
        String message = WebSocketMessageBuilder.createTextMessage(content);
        sendMsg(message);
    }

    public void sendImageMessage(String filePath) {
        String message = WebSocketMessageBuilder.createImageMessage(filePath);
        sendMsg(message);
    }

    public void sendAudioMessage(String filePath) {
        String message = WebSocketMessageBuilder.createAudioMessage(filePath);
        sendMsg(message);
    }


    /**
     * 断开连接
     */
    private void closeConnect() {
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }


    /**
     * 发送通知
     *
     * @param content
     */
//    private void sendNotification(String content) {
//        String channelId = "Test";
//        Intent intent = new Intent();
//        intent.setClass(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        Notification notification = new NotificationCompat.Builder(this, channelId)
//                .setAutoCancel(true)
//                // 设置该通知优先级
////                .setPriority(Notification.PRIORITY_MAX)
//                .setSmallIcon(R.drawable.icon)
//                .setContentTitle("服务器")
//                .setContentText(content)
////                .setVisibility(VISIBILITY_PUBLIC)
////                .setWhen(System.currentTimeMillis())
//                // 向通知添加声音、闪灯和振动效果
////                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
//                .setContentIntent(pendingIntent)
//                .build();
//
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    channelId,
//                    "Channel human readable title",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            notifyManager.createNotificationChannel(channel);
//        }
//        notifyManager.notify(0, notification);//id要保证唯一
//    }
    private void sendNotification(String content) {
        int requestCode = 0;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "Test";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("Test")
                .setContentText(content)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = 0;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }


    //    -------------------------------------websocket心跳检测------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e("JWebSocketClientService", "心跳包检测websocket连接状态");
            if (client != null) {
                if (client.isClosed()) {
                    reconnectWs();
                }
            } else {
                //如果client已为空，重新初始化连接
                client = null;
                initSocketClient();
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    /**
     * 开启重连
     */
    private void reconnectWs() {
        mHandler.removeCallbacks(heartBeatRunnable);
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.e("JWebSocketClientService", "开启重连");
                    client.reconnectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
