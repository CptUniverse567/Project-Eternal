package com.projecteternal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.projecteternal.app.controller.GameController
import com.projecteternal.app.ui.EternalApp
import com.projecteternal.app.ui.EternalTheme

class MainActivity : ComponentActivity() {

    private val controller: GameController
        get() = (application as EternalApplication).container.gameController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent {
            EternalTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EternalApp(controller)
                }
                DisposableEffect(controller) {
                    onDispose { controller.onPause() }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller.onResume(System.currentTimeMillis())
    }

    override fun onStop() {
        controller.onPause()
        super.onStop()
    }
}

class EternalApplication : android.app.Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
