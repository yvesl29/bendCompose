package fr.bendcompose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.nio.file.Path
import java.text.NumberFormat
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.io.path.isExecutable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

data class Transaction(val cents: Long, val verified: Boolean) {
    init { require(cents in 0..MAX_CENTS) { "Montant hors limites." } }

    val formatted: String
        get() = NumberFormat.getCurrencyInstance(Locale.FRANCE).format(BigDecimal.valueOf(cents, 2))

    companion object {
        const val MAX_CENTS = 4_294_967_295L

        fun parse(input: String, verified: Boolean): Transaction {
            val normalized = input.trim().replace(Regex("[ \\u00a0\\u202f]"), "").replace(',', '.')
            require(normalized.matches(Regex("[0-9]+([.][0-9]{1,2})?"))) {
                "Saisissez un montant positif, avec au maximum 2 décimales."
            }
            val cents = normalized.toBigDecimal().movePointRight(2)
            require(cents <= BigDecimal.valueOf(MAX_CENTS)) {
                "Le montant maximum est de 42 949 672,95 €."
            }
            return Transaction(cents.longValueExact(), verified)
        }
    }
}

data class RiskResult(
    val transaction: Transaction,
    val score: Int,
    val duration: Duration,
    val time: LocalTime = LocalTime.now(),
) {
    val label: String get() = when (score) {
        0 -> "Transaction vérifiée"
        10 -> "Risque faible"
        else -> "Risque élevé"
    }
    val explanation: String get() = when (score) {
        0 -> "Le statut vérifié est prioritaire sur le montant : le moteur retourne 0."
        10 -> "Le montant ne dépasse pas 10 000 €. La règle Bend retourne un score de 10."
        else -> "Le montant dépasse 10 000 € et la transaction n’est pas vérifiée. La règle Bend retourne 95."
    }
}

/** Calls the compiled Bend program, never a Kotlin reimplementation of its rules. */
class RiskEngine(
    val executable: Path = Path.of(
        System.getProperty("bend.engine.path")
            ?: System.getenv("BEND_ENGINE_PATH")
            ?: "build/bend/risk-engine",
    ).toAbsolutePath(),
    private val timeoutMillis: Long = 5_000,
) {
    suspend fun evaluate(transaction: Transaction): RiskResult = withContext(Dispatchers.IO) {
        check(executable.isExecutable()) {
            "Moteur Bend introuvable. Lancez ./run.sh pour le compiler."
        }
        val started = System.nanoTime()
        val process = ProcessBuilder(
            executable.toString(), "--threads", "1", "--",
            transaction.cents.toString(), if (transaction.verified) "1" else "0",
        ).redirectErrorStream(true).start()
        try {
            process.outputStream.close()
            val output = coroutineScope {
                val reader = async(Dispatchers.IO) {
                    val bytes = process.inputStream.readNBytes(4097)
                    check(bytes.size <= 4096) { "Réponse du moteur Bend trop volumineuse." }
                    bytes.toString(Charsets.UTF_8).trim()
                }
                try {
                    val finished = runInterruptible { process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS) }
                    check(finished) { "Le moteur Bend a dépassé le délai de ${timeoutMillis / 1000} s." }
                    reader.await()
                } finally {
                    // Kill before coroutineScope joins its reader, including on cancellation.
                    if (process.isAlive) process.destroyForcibly()
                }
            }
            check(process.exitValue() == 0) { "Échec Bend (${process.exitValue()}) : ${output.take(250)}" }
            val score = Regex("BEND_RISK_V1:(0|10|95)").matchEntire(output)
                ?.groupValues?.get(1)?.toInt()
                ?: error("Réponse Bend invalide : ${output.take(120)}")
            RiskResult(transaction, score, (System.nanoTime() - started).nanoseconds)
        } finally {
            if (process.isAlive) process.destroyForcibly()
            process.inputStream.close()
            process.errorStream.close()
        }
    }
}
