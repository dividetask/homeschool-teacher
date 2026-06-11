package com.dividetask.homeschoolteacher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dividetask.homeschoolteacher.ui.HomeschoolTeacherApp
import com.dividetask.homeschoolteacher.ui.theme.HomeschoolTeacherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppConfig.load(applicationContext)
        Storage.init(applicationContext)
        Tts.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            HomeschoolTeacherTheme {
                HomeschoolTeacherApp()
            }
        }
    }
}
