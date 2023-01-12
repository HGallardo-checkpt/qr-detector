package com.checkpoint.qrdetector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.checkpoint.qrdetector.cache.BitmapLruCache
import com.checkpoint.qrdetector.utils.CacheFile

class EventDetailActivity : AppCompatActivity() {
    private var textDate: TextView? = null
    private var textDirectionDetected: TextView? = null
    private var textTranslationCode: TextView? = null
    private var imgDetection: ImageView? = null
    private var cacheFile:   CacheFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)
        cacheFile = CacheFile(this)


        textDate= findViewById(R.id.textDate)
        textDirectionDetected= findViewById(R.id.textDirectionDetected)
        textTranslationCode= findViewById(R.id.textTranslationCode)
        imgDetection= findViewById(R.id.imgDetection)



        var bundle :Bundle ?=intent.extras
        var date = bundle!!.getString("date")
        var translate = bundle!!.getString("translation")
        var direction = bundle!!.getString("direction")
        var idImage = bundle!!.getString("idImage")
        var bitmap = cacheFile!!.getImageFromCache("$idImage")


        textDate!!.text = date
        textDirectionDetected!!.text = direction
        textTranslationCode!!.text = translate

        runOnUiThread{
            imgDetection!!.setImageBitmap(bitmap)
        }


    }
}