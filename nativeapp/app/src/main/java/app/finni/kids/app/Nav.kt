package app.finni.kids.app

import androidx.compose.runtime.mutableStateListOf

// Простая навигация «стопкой»: у каждого экрана одинаковая кнопка «назад», а системная кнопка ведёт себя так же.
// Маршруты: "/", "/plan", "/task/<id>", "/create?step=1" и т. п.

object Nav {
    val stack = mutableStateListOf("/")

    val current: String get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun go(route: String) {
        if (route == current) return
        // возврат на уже открытый раздел не раздувает стопку
        val at = stack.lastIndexOf(route)
        if (at >= 0 && route != "/") {
            while (stack.size > at + 1) stack.removeAt(stack.lastIndex)
        } else {
            stack.add(route)
        }
    }

    fun replace(route: String) {
        stack[stack.lastIndex] = route
    }

    fun reset(route: String = "/") {
        stack.clear()
        stack.add(route)
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}
