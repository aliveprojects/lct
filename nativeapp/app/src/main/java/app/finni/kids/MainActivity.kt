package app.finni.kids

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.platform.Music
import app.finni.kids.ui.FinniApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Game.init(applicationContext)
        val dark = android.graphics.Color.parseColor("#0A0D35")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(dark),
            navigationBarStyle = SystemBarStyle.dark(dark),
        )
        setContent {
            BackHandler {
                if (Ui.closeTop()) return@BackHandler
                if (Nav.back()) return@BackHandler
                Game.flush()
                finish()
            }
            FinniApp()
        }
    }

    override fun onStart() {
        super.onStart()
        Music.setBackgrounded(false)
    }

    override fun onStop() {
        Music.setBackgrounded(true)
        Game.flush()
        super.onStop()
    }
}
