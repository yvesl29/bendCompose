package fr.bendcompose

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.*

class RiskEngineTest {
    @Test fun `amounts use exact cents and French separators`() {
        assertEquals(1_250_050L, Transaction.parse("12 500,50", false).cents)
        assertEquals(1_000_001L, Transaction.parse("10\u202f000.01", false).cents)
        assertEquals(0L, Transaction.parse("0", false).cents)
        assertEquals(Transaction.MAX_CENTS, Transaction.parse("42949672,95", false).cents)
    }

    @Test fun `invalid input is never silently converted to zero`() {
        listOf("", "-1", "NaN", "1e3", "0.001", "12,3,4", "42949672.96", "999999999999999999999").forEach {
            assertFailsWith<IllegalArgumentException>(it) { Transaction.parse(it, false) }
        }
    }

    @Test fun `real Bend binary handles boundaries and verified precedence`() = runBlocking {
        val engine = RiskEngine()
        for ((amount, score) in listOf(0L to 10, 999_999L to 10, 1_000_000L to 10,
            1_000_001L to 95, 1_250_000L to 95, Transaction.MAX_CENTS to 95)) {
            assertEquals(score, engine.evaluate(Transaction(amount, false)).score, "Unverified $amount")
            assertEquals(0, engine.evaluate(Transaction(amount, true)).score, "Verified $amount")
        }
    }

    @Test fun `missing executable yields an actionable error`() = runBlocking {
        val error = assertFailsWith<IllegalStateException> {
            RiskEngine(java.nio.file.Path.of("/nonexistent/bend-risk-test")).evaluate(Transaction(0, false))
        }
        assertContains(error.message!!, "introuvable")
    }

    @Test fun `malformed response and failed process never become a score`() = runBlocking {
        for (script in listOf("echo BEND_RISK_V1:101", "echo 95", "echo BEND_RISK_V1:95; exit 1", "echo BEND_RISK_V1:95; echo extra")) {
            withFakeEngine(script) { engine ->
                assertFailsWith<IllegalStateException> { engine.evaluate(Transaction(0, false)) }
            }
        }
    }

    @Test fun `hung engine times out`() = runBlocking {
        withFakeEngine("exec sleep 30", 150) { engine ->
            withTimeout(3000) {
                val error = assertFailsWith<IllegalStateException> { engine.evaluate(Transaction(0, false)) }
                assertContains(error.message!!, "délai")
            }
        }
    }

    @Test fun `cancellation releases a running engine promptly`() = runBlocking {
        withFakeEngine("exec sleep 30") { engine ->
            withTimeout(3000) {
                val job = async { engine.evaluate(Transaction(0, false)) }
                delay(150)
                job.cancelAndJoin()
                assertTrue(job.isCancelled)
            }
        }
    }

    private suspend fun withFakeEngine(body: String, timeout: Long = 5000, block: suspend (RiskEngine) -> Unit) {
        val script = Files.createTempFile("bend-bridge-test-", ".sh")
        try {
            script.writeText("#!/bin/sh\n$body\n")
            check(script.toFile().setExecutable(true))
            block(RiskEngine(script, timeout))
        } finally {
            script.deleteIfExists()
        }
    }
}
