package com.checkpoint.qrdetector.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.checkpoint.qrdetector.R

open class NotificationBuilder(var notificationManager: NotificationManager) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNotification(context: Context,resultIntent: Intent, icon: Icon) {
        val channelID = "com.checkpoint.qrdetector"
        val notificationID = 101
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelID, "QR Detection event", importance)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val action: Notification.Action =
            Notification.Action.Builder(icon, "Go", pendingIntent).build()
        channel.enableLights(true)
        channel.lightColor = Color.RED
        channel.enableVibration(true)
        channel.canShowBadge()
        channel.vibrationPattern =
            longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager?.createNotificationChannel(channel)

        val backgound = createImage(150,150, R.color.purple_700)
        val notification = Notification.Builder(context, channelID)
            .setContentText("A QR Code has been detected")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setChannelId(channel.id)
            .setContentIntent(pendingIntent)
            .setActions(action)
            .setLargeIcon(backgound)
            .setStyle(
                Notification.BigPictureStyle()
                    .bigPicture(backgound))
            .build()
        notificationManager?.notify(notificationID, notification)





    }
    open fun createImage(width: Int, height: Int, color: Int): Bitmap? {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.setColor(color)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }


}
