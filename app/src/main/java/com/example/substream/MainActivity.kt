package com.example.substream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.substream.ui.screens.AlbumsScreen
import com.example.substream.ui.theme.SubStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SubStreamTheme {
                AlbumsScreen()
            }
        }
    }
}
