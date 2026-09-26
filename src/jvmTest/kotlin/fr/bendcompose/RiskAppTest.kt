package fr.bendcompose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.toAwtImage
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class RiskAppTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `user evaluates with real Bend and can inspect architecture and clear history`() {
        compose.setContent { RiskApp() }
        awaitScore("95")
        compose.waitForIdle()
        val screenshot = File("build/screenshots/playground.png")
        screenshot.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", screenshot)

        compose.onNodeWithTag("amount").performTextReplacement("10000,00")
        compose.onNodeWithTag("evaluate").performClick()
        awaitScore("10")

        compose.onNodeWithTag("amount").performTextReplacement("10000,01")
        compose.onNodeWithTag("evaluate").performClick()
        awaitScore("95")

        compose.onNodeWithTag("verified").performClick()
        compose.onNodeWithTag("evaluate").performClick()
        awaitScore("0")

        compose.onNodeWithTag("amount").performTextReplacement("-1")
        compose.onNodeWithTag("evaluate").performClick()
        compose.onNodeWithTag("error").assertExists()
        compose.onNodeWithTag("score").assertTextEquals("—")

        compose.onNodeWithTag("clearHistory").performScrollTo().performClick()
        compose.onNodeWithTag("emptyHistory").assertExists()

        compose.onNodeWithText("Architecture", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Six lois vérifiées à chaque compilation").assertExists()
        compose.onNodeWithText("Playground", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("amount").assertExists()
    }

    private fun awaitScore(score: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("score") and hasText(score)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("score").assertTextEquals(score)
    }
}
