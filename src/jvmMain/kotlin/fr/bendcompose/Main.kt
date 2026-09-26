package fr.bendcompose

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Bend Studio · Risk playground",
        state = rememberWindowState(width = 1220.dp, height = 870.dp),
    ) {
        window.minimumSize = Dimension(1000, 760)
        RiskApp()
    }
}
