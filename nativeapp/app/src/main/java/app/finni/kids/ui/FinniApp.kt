package app.finni.kids.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.domain.Mode
import app.finni.kids.domain.activeProfile
import app.finni.kids.ui.screens.AdultScreen
import app.finni.kids.ui.screens.CreateScreen
import app.finni.kids.ui.screens.GoalsScreen
import app.finni.kids.ui.screens.PlanScreen
import app.finni.kids.ui.screens.ProgressScreen
import app.finni.kids.ui.screens.ReviewScreen
import app.finni.kids.ui.screens.ShopScreen
import app.finni.kids.ui.screens.TaskPlayerScreen
import app.finni.kids.ui.screens.TasksScreen
import app.finni.kids.ui.screens.GlossaryScreen
import app.finni.kids.ui.screens.HomeScreen
import app.finni.kids.ui.screens.IntroScreen
import app.finni.kids.ui.screens.WelcomeScreen
import app.finni.kids.ui.theme.FinniTheme

private val NO_PROFILE_OK = setOf("/welcome", "/intro", "/create")

@Composable
fun FinniApp() {
    val app = Game.app
    FinniTheme(app.settings.highContrast, app.settings.textScale) {
        Box(Modifier.fillMaxSize()) {
            SpaceBackground()
            Box(Modifier.fillMaxSize().widthIn(max = 640.dp), contentAlignment = Alignment.TopCenter) {
                val route = Nav.current
                val profile = activeProfile(app)
                val raw = route.substringBefore('?')
                val query = route.substringAfter('?', "")
                // Без профиля показываем только знакомство и создание питомца; с профилем — не возвращаем на приветствие.
                var path = raw
                if (profile == null && raw !in NO_PROFILE_OK) path = if (app.mode == Mode.Demo) "/intro" else "/welcome"
                if (profile != null && (raw == "/welcome" || raw == "/create")) path = "/"
                AnimatedContent(
                    targetState = path,
                    transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 60 }) togetherWith fadeOut(tween(120)) },
                    label = "page",
                ) { target ->
                    when {
                        target == "/welcome" -> WelcomeScreen()
                        target == "/intro" -> IntroScreen(again = query.contains("again=1") || profile != null)
                        target == "/create" -> CreateScreen()
                        target == "/glossary" -> GlossaryScreen()
                        profile == null -> WelcomeScreen()
                        target == "/plan" -> PlanScreen()
                        target == "/shop" -> ShopScreen()
                        target == "/goals" -> GoalsScreen()
                        target == "/tasks" -> TasksScreen()
                        target.startsWith("/task/") -> TaskPlayerScreen(target.removePrefix("/task/"))
                        target == "/progress" -> ProgressScreen()
                        target == "/review" -> ReviewScreen()
                        target == "/adult" -> AdultScreen()
                        else -> HomeScreen()
                    }
                }
            }
            OverlayHost()
        }
    }
}
