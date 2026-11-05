package com.abdulbasir.sdplayer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.abdulbasir.sdplayer.ui.HomeApp
import com.abdulbasir.sdplayer.ui.SDTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SDTheme {
                HomeApp(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.load()
        vm.refreshHistory()
    }
}
