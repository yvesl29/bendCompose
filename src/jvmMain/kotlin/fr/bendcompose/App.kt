package fr.bendcompose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Ink = Color(0xFF202923)
private val Muted = Color(0xFF798078)
private val Green = Color(0xFF286247)
private val Mint = Color(0xFFEAF3E8)
private val Paper = Color(0xFFF7F8F4)
private val Line = Color(0xFFE5E8E0)
private val Red = Color(0xFFB45537)
private val Peach = Color(0xFFFCF0E9)
private val Scheme = lightColorScheme(
    primary = Green, onPrimary = Color.White, background = Paper,
    surface = Color.White, onSurface = Ink, onBackground = Ink,
    surfaceVariant = Mint, outline = Line, error = Red,
)

@Composable
fun RiskApp(engine: RiskEngine = remember { RiskEngine() }) {
    MaterialTheme(colorScheme = Scheme) {
        var amount by remember { mutableStateOf("12500") }
        var verified by remember { mutableStateOf(false) }
        var busy by remember { mutableStateOf(false) }
        var result by remember { mutableStateOf<RiskResult?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var connected by remember { mutableStateOf(false) }
        var showArchitecture by remember { mutableStateOf(false) }
        val history = remember { mutableStateListOf<RiskResult>() }
        val scope = rememberCoroutineScope()

        fun evaluate() {
            if (busy) return
            val transaction = try {
                Transaction.parse(amount, verified)
            } catch (e: IllegalArgumentException) {
                error = e.message
                return
            }
            busy = true
            error = null
            scope.launch {
                try {
                    val next = engine.evaluate(transaction)
                    result = next
                    connected = true
                    history.add(0, next)
                    if (history.size > 20) history.removeAt(history.lastIndex)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    connected = false
                    result = null
                    error = e.message ?: "Impossible de joindre le moteur Bend."
                } finally {
                    busy = false
                }
            }
        }

        LaunchedEffect(Unit) { evaluate() }

        Row(Modifier.fillMaxSize().background(Paper)) {
            Sidebar(connected, showArchitecture, { showArchitecture = it })
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(36.dp, 30.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("WORKSPACE  /  ${if (showArchitecture) "ARCHITECTURE" else "RISK PLAYGROUND"}",
                        fontSize = 10.sp, letterSpacing = 1.5.sp, color = Muted, modifier = Modifier.weight(1f))
                    Pill("JVM ONLY", Paper, Muted)
                    Spacer(Modifier.width(10.dp))
                    Pill("PROTOTYPE  /  01", Mint, Green)
                }
                if (showArchitecture) {
                    Architecture(engine)
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("Le risque, en toute clarté.", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Une interface Compose. Des règles exécutées par Bend.", fontSize = 13.sp, color = Muted)
                        }
                        Text("01 — ÉVALUER", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Green)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Panel(Modifier.weight(1.06f)) {
                            Eyebrow("TRANSACTION")
                            Spacer(Modifier.height(10.dp))
                            Text("Évaluer un montant", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(22.dp))
                            Text("Montant de la transaction", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { if (it.length <= 24) { amount = it; result = null; error = null } },
                                enabled = !busy,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("amount"),
                                suffix = { Text("EUR", color = Muted, fontSize = 12.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 25.sp, fontWeight = FontWeight.Medium),
                                shape = RoundedCornerShape(10.dp),
                                isError = error != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { evaluate() }),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Line),
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("250" to "250 €", "10000" to "10 000 €", "12500" to "12 500 €").forEach { (value, label) ->
                                    SuggestionChip(
                                        onClick = { amount = value; result = null; error = null },
                                        label = { Text(label, fontSize = 11.sp) }, enabled = !busy,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Transaction vérifiée", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Prioritaire sur le montant", fontSize = 11.sp, color = Muted)
                                }
                                Switch(
                                    checked = verified,
                                    onCheckedChange = { verified = it; result = null; error = null },
                                    enabled = !busy, modifier = Modifier.testTag("verified"),
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { evaluate() }, enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(49.dp).testTag("evaluate"),
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(if (busy) "Calcul en cours…" else "Évaluer avec Bend", fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                Text("↗", fontSize = 20.sp)
                            }
                            if (error != null) {
                                Text(error!!, color = Red, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp).testTag("error"))
                            }
                        }
                        ResultPanel(result, busy, Modifier.weight(1f))
                    }

                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Mint).padding(18.dp, 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⌘", fontSize = 23.sp, color = Green)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Des règles explicites, un résultat reproductible.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Green)
                            Text("Vérifiée → 0   ·   Jusqu’à 10 000 € → 10   ·   Au-delà → 95", fontSize = 11.sp, color = Green)
                        }
                        TextButton(onClick = { showArchitecture = true }) { Text("Voir le moteur →", fontSize = 11.sp) }
                    }

                    History(history, onClear = { history.clear() })
                    Row(Modifier.fillMaxWidth()) {
                        Text("BEND 2  ×  COMPOSE MULTIPLATFORM", fontSize = 9.sp, letterSpacing = 1.sp, color = Muted)
                        Spacer(Modifier.weight(1f))
                        Text("Exécution locale · CPU · Aucun service distant", fontSize = 10.sp, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(connected: Boolean, architecture: Boolean, navigate: (Boolean) -> Unit) {
    Column(Modifier.width(205.dp).fillMaxHeight().background(Color.White).padding(23.dp, 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Green), contentAlignment = Alignment.Center) {
                Text("b", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text("bend", fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Text(" studio", fontSize = 14.sp, color = Muted)
        }
        Spacer(Modifier.height(54.dp))
        Eyebrow("EXPLORER")
        Spacer(Modifier.height(15.dp))
        NavigationItem("◈", "Playground", !architecture) { navigate(false) }
        Spacer(Modifier.height(8.dp))
        NavigationItem("⌘", "Architecture", architecture) { navigate(true) }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Paper).padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(if (connected) Green else Muted, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(if (connected) "Moteur connecté" else "Moteur en attente", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text("Bend natif / CPU\nPont de processus local", color = Muted, fontSize = 10.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("Un petit laboratoire\npour des règles précises.", fontSize = 10.sp, color = Muted, lineHeight = 17.sp)
    }
}

@Composable
private fun NavigationItem(icon: String, text: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.textButtonColors(containerColor = if (selected) Mint else Color.Transparent,
            contentColor = if (selected) Green else Muted),
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResultPanel(result: RiskResult?, busy: Boolean, modifier: Modifier) {
    val accent = if (result?.score == 95) Red else Green
    val progress by animateFloatAsState((result?.score ?: 0) / 100f)
    Panel(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("RÉSULTAT DU MOTEUR")
            Spacer(Modifier.weight(1f))
            Pill(if (result != null) "BEND / CPU" else "EN ATTENTE", Paper, Muted)
        }
        Box(Modifier.fillMaxWidth().height(204.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(180.dp)) {
                val stroke = 12.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(Line, 140f, 260f, false, Offset(stroke / 2, stroke / 2), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (progress > 0) drawArc(accent, 140f, 260f * progress, false, Offset(stroke / 2, stroke / 2), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (busy) "…" else result?.score?.toString() ?: "—", fontSize = 57.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-3).sp, color = Ink, modifier = Modifier.testTag("score"))
                Text("SUR 100", fontSize = 10.sp, letterSpacing = 2.sp, color = Muted)
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Pill(result?.label ?: "Prêt pour une évaluation", if (result?.score == 95) Peach else Mint, accent)
            Spacer(Modifier.height(10.dp))
            Text(result?.transaction?.formatted ?: "Choisissez un montant, puis lancez le calcul.", fontSize = 12.sp, color = Muted)
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(14.dp))
        Text(result?.explanation ?: "Le résultat provient directement de l’exécutable Bend.", fontSize = 12.sp, color = Muted, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        Text(result?.let { "↳ aller-retour local : ${latency(it)} ms" } ?: "↳ protocole BEND_RISK_V1",
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = accent)
    }
}

@Composable
private fun History(history: List<RiskResult>, onClear: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Dernières évaluations", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Pill(history.size.toString(), Paper, Muted)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear, enabled = history.isNotEmpty(), modifier = Modifier.testTag("clearHistory")) {
                Text("Effacer", fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text("MONTANT", Modifier.weight(1.1f), fontSize = 9.sp, color = Muted, letterSpacing = 1.sp)
            Text("STATUT", Modifier.weight(1.3f), fontSize = 9.sp, color = Muted, letterSpacing = 1.sp)
            Text("SCORE", Modifier.weight(.7f), fontSize = 9.sp, color = Muted, letterSpacing = 1.sp)
            Text("ALLER-RETOUR", Modifier.weight(1f), fontSize = 9.sp, color = Muted, letterSpacing = 1.sp)
            Text("HEURE", Modifier.width(62.dp), fontSize = 9.sp, color = Muted, letterSpacing = 1.sp)
        }
        HorizontalDivider(color = Line)
        if (history.isEmpty()) {
            Text("Vos évaluations apparaîtront ici. Historique limité à cette session.", fontSize = 12.sp, color = Muted,
                modifier = Modifier.padding(vertical = 20.dp).testTag("emptyHistory"))
        }
        history.take(5).forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.transaction.formatted, Modifier.weight(1.1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(item.label, Modifier.weight(1.3f), fontSize = 11.sp, color = if (item.score == 95) Red else Green)
                Text("${item.score} / 100", Modifier.weight(.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("${latency(item)} ms", Modifier.weight(1f), fontSize = 11.sp, color = Muted)
                Text(item.time.format(DateTimeFormatter.ofPattern("HH:mm:ss")), Modifier.width(62.dp), fontSize = 10.sp, color = Muted)
            }
        }
        if (history.size > 5) Text("5 dernières affichées · ${history.size} évaluations conservées (maximum 20)", fontSize = 10.sp, color = Muted)
    }
}

@Composable
private fun Architecture(engine: RiskEngine) {
    Text("Une interface. Un vrai moteur.", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
    Text("La logique de risque vit entièrement dans risk_engine.bend.", color = Muted, fontSize = 14.sp)
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("LE CHEMIN D’UNE ÉVALUATION")
        Spacer(Modifier.height(24.dp))
        Text("Compose / JVM   →   ProcessBuilder   →   Bend / C / CPU", fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = Green)
        Spacer(Modifier.height(20.dp))
        Text("Kotlin valide le montant en centimes, puis appelle l’exécutable compilé dans une coroutine IO. Bend applique les règles et renvoie un score via un protocole texte versionné. L’interface se met à jour sur le thread graphique.", lineHeight = 24.sp, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Text("Le pont impose un délai de 5 secondes, contrôle le code de sortie et valide la réponse. Aucun score de secours n’est calculé dans Kotlin.", color = Muted, lineHeight = 22.sp, fontSize = 13.sp)
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ink).padding(26.dp)) {
        Text("risk_engine.bend", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFFA9C6A7))
        Spacer(Modifier.height(20.dp))
        Text("def calculate_risk(amount_cents: U32, verified: Bool) -> U32:\n  score(U32.is_gt(amount_cents, 1000000), verified)",
            color = Color(0xFFE0ECDD), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 23.sp)
    }
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("PREUVES ET PÉRIMÈTRE")
        Spacer(Modifier.height(14.dp))
        Text("Six lois vérifiées à chaque compilation", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text("Priorité du statut vérifié · Scores 10 et 95 · Borne à 100 · Seuil exact · Premier centime au-dessus", color = Green, fontSize = 13.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(12.dp))
        Text("Les preuves couvrent les règles pures déclarées dans LAWS.bend. Elles ne prouvent pas l’interface, le pont, le compilateur ou le système d’exploitation. Ce prototype est un moteur de règles déterministe, sans modèle d’IA ni exécution GPU.", color = Muted, fontSize = 13.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(16.dp))
        Text("Exécutable : ${engine.executable}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Muted)
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
        .border(1.dp, Line, RoundedCornerShape(16.dp)).padding(24.dp), content = content)
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, fontSize = 9.sp, color = Muted, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun Pill(text: String, background: Color, foreground: Color) {
    Text(text, color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(background).padding(horizontal = 9.dp, vertical = 5.dp))
}

private fun latency(result: RiskResult): String = String.format(Locale.FRANCE, "%.1f", result.duration.inWholeNanoseconds / 1_000_000.0)
