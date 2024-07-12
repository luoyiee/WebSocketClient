package com.yxc.websocketclientdemo

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.yxc.websocketclientdemo.adapter.Adapter_ChatMessage
import com.yxc.websocketclientdemo.databinding.ActivityMainBinding
import com.yxc.websocketclientdemo.im.JWebSocketClient
import com.yxc.websocketclientdemo.im.JWebSocketClientService
import com.yxc.websocketclientdemo.im.JWebSocketClientService.JWebSocketClientBinder
import com.yxc.websocketclientdemo.modle.ChatMessage
import com.yxc.websocketclientdemo.self.ChatMessageReceiver
import com.yxc.websocketclientdemo.self.FileUtil
import com.yxc.websocketclientdemo.self.PermissionRequestCallback
import com.yxc.websocketclientdemo.self.ValueUtil
import com.yxc.websocketclientdemo.util.Util
import java.io.File


/**
 * Android WebSocket实现即时通讯功能
 * 参考 https://blog.csdn.net/xch_yang/article/details/88888350
 * 参考 https://www.jianshu.com/p/9e47ecf5115b
 * 参考 https://github.com/TooTallNate/Java-WebSocket
 */
class MainActivity : AppCompatActivity(), View.OnClickListener, PermissionRequestCallback {
    private lateinit var mBinding: ActivityMainBinding
    private var client: JWebSocketClient? = null
    private var binder: JWebSocketClientBinder? = null
    private lateinit var jWebSClientService: JWebSocketClientService
    private val chatMessageList: MutableList<ChatMessage> = ArrayList() //消息列表
    private var adapter_chatMessage: Adapter_ChatMessage? = null
    private var chatMessageReceiver: ChatMessageReceiver? = null
    private var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.e("MainActivity", "服务与活动成功绑定")
            binder = iBinder as JWebSocketClientBinder
            jWebSClientService = binder!!.service
            client = jWebSClientService.client
            jWebSClientService.setPermissionRequestCallback(this@MainActivity)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e("MainActivity", "服务与活动成功断开")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        registerForActivityResult()
        //启动服务
        startJWebSClientService()
        //绑定服务
        bindService()
        //注册广播
        doRegisterReceiver()
        //检测通知是否开启
        checkNotification(this)
        findViewById()
        initView()
    }

    /**
     * 绑定服务
     */
    private fun bindService() {
        val bindIntent = Intent(this, JWebSocketClientService::class.java)
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    /**
     * 启动服务（websocket客户端服务）
     */
    private fun startJWebSClientService() {
        val intent = Intent(this, JWebSocketClientService::class.java)
        startService(intent)
    }

    /**
     * 动态注册广播
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun doRegisterReceiver() {
        chatMessageReceiver =
            ChatMessageReceiver(
                this,
                chatMessageList
            )
        val filter = IntentFilter("com.xch.servicecallback.content")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chatMessageReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(chatMessageReceiver, filter)
        }
    }

//    private fun doRegisterReceiver() {
//        chatMessageReceiver = ChatMessageReceiver(this, chatMessageList)
//        val filter = IntentFilter("com.xch.servicecallback.content")
//        registerReceiver(chatMessageReceiver, filter)
//    }

    private fun findViewById() {
//        listView = findViewById(R.id.chatmsg_listView)
//        btn_send = findViewById(R.id.btn_send)
//        et_content = findViewById(R.id.et_content)
//        ivAlbum = findViewById(R.id.iv_image)
        mBinding.layoutBottom.btnSend.setOnClickListener(this)
        mBinding.layoutBottom.ivAlbum.setOnClickListener(this)
    }

    private fun initView() {
        //监听输入框的变化
        mBinding.layoutBottom.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (mBinding.layoutBottom.etContent.getText().toString().isNotEmpty()) {
                    mBinding.layoutBottom.btnSend.visibility = View.VISIBLE
                } else {
                    mBinding.layoutBottom.btnSend.visibility = View.GONE
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        XXPermissions.with(this)
            .permission(Permission.POST_NOTIFICATIONS)
            .request { _, allGranted ->
                if (allGranted) {
                    // 权限已授予，继续执行
                } else {
                    // 权限被拒绝，处理拒绝情况
                }
            }

    }

    override fun onClick(view: View) {
        if (view.id == R.id.btn_send) {
            val content = mBinding.layoutBottom.etContent.getText().toString()
            if (content.isEmpty()) {
                Util.showToast(this, "消息不能为空哟")
                return
            }
            if (client != null && client!!.isOpen) {
                jWebSClientService.sendTextMessage(content)
                //暂时将发送的消息加入消息列表，实际以发送成功为准（也就是服务器返回你发的消息时）
                val chatMessage = ChatMessage()
                chatMessage.content = content
                chatMessage.isMeSend = 1
                chatMessage.isRead = 1
                chatMessage.time = System.currentTimeMillis().toString() + ""
                chatMessageList.add(chatMessage)
                initChatMsgListView()
                mBinding.layoutBottom.etContent.setText("")
            } else {
                Util.showToast(this, "连接已断开，请稍等或重启App哟")
            }
        } else if (view.id == R.id.iv_album) {
            pickMedia?.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ImageOnly)
                    .build()
            )
        }
    }

    fun initChatMsgListView() {
        adapter_chatMessage = Adapter_ChatMessage(this, chatMessageList)
        mBinding.chatListView.setAdapter(adapter_chatMessage)
        mBinding.chatListView.setSelection(chatMessageList.size)
    }

    /**
     * 检测是否开启通知
     */
    private fun checkNotification(context: Context) {
        if (!isNotificationEnabled(context)) {
            AlertDialog.Builder(context).setTitle("温馨提示")
                .setMessage("你还未开启系统通知，将影响消息的接收，要去开启吗？")
                .setPositiveButton("确定") { dialog, which -> setNotification(context) }
                .setNegativeButton("取消") { dialog, which -> }.show()
        }
    }

    /**
     * 如果没有开启通知，跳转至设置界面
     *
     * @param context
     */
    private fun setNotification(context: Context) {
        val localIntent = Intent()
        //直接跳转到应用通知设置的代码：
        localIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS")
        localIntent.putExtra("app_package", context.packageName)
        localIntent.putExtra("app_uid", context.applicationInfo.uid)
        context.startActivity(localIntent)
    }

    /**
     * 获取通知权限,监测是否开启了系统通知
     *
     * @param context
     */
    private fun isNotificationEnabled(context: Context): Boolean {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                return notificationManager.areNotificationsEnabled()
//            } else {
//                val channels = notificationManager.notificationChannels
//                for (channel in channels) {
//                    if (channel.importance != NotificationManager.IMPORTANCE_NONE) {
//                        return true
//                    }
//                }
//                return false
//            }
//        } else {
        val CHECK_OP_NO_THROW = "checkOpNoThrow"
        val OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION"
        val mAppOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val appInfo = context.applicationInfo
        val pkg = context.applicationContext.packageName
        val uid = appInfo.uid
        val appOpsClass: Class<*>
        try {
            appOpsClass = Class.forName(AppOpsManager::class.java.name)
            val checkOpNoThrowMethod = appOpsClass.getMethod(
                CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE,
                String::class.java
            )
            val opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION)
            val value = opPostNotificationValue.get(Int::class.java) as Int
            return checkOpNoThrowMethod.invoke(
                mAppOps,
                value,
                uid,
                pkg
            ) as Int == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
//        }
    }


    private fun sendNotification(messageTitle: String?, messageBody: String?) {
        val requestCode = 0
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = ""
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(messageTitle ?: "")
            .setContentText(messageBody ?: "")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }


    private fun registerForActivityResult() {
        pickMedia =
            registerForActivityResult<PickVisualMediaRequest, Uri>(ActivityResultContracts.PickVisualMedia()) {
                if (it != null) {
                    val file = File(ValueUtil.getStringFormat(FileUtil.getDriveFilePath(this, it)))
//                    val imageBytes = file.readBytes()
                    // 发送图片数据
                    if (client != null && client!!.isOpen) {
                        jWebSClientService.sendImageMessage(file.path)
                        //暂时将发送的消息加入消息列表，实际以发送成功为准（也就是服务器返回你发的消息时）
                        val chatMessage = ChatMessage()
                        chatMessage.content = ""
//                        chatMessage.image = byteArrayToBitmap(imageBytes)
                        chatMessage.imagePath = file.path
                        chatMessage.isMeSend = 1
                        chatMessage.isRead = 1
                        chatMessage.time = System.currentTimeMillis().toString() + ""
                        chatMessageList.add(chatMessage)
                        initChatMsgListView()
                        mBinding.layoutBottom.etContent.setText("")
                    } else {
                        Util.showToast(this, "连接已断开，请稍等或重启App哟")
                    }
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }
    }


    companion object {
        fun byteArrayToBitmap(imageBytes: ByteArray): Bitmap? {
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }


    override fun requestPermissions(content: String?) {
        XXPermissions.with(this)
            .permission(Permission.POST_NOTIFICATIONS)
            .request { permissions: List<String?>?, allGranted: Boolean ->
                if (allGranted) {
                    sendNotification(content!!)
                } else {
                    // 处理未授予权限的情况
                }
            }
    }


    private fun sendNotification(content: String) {
        val requestCode = 0
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "Test"
        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Test")
            .setContentText(content)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }


}