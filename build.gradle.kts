plugins {
    kotlin("multiplatform") version "2.2.21"
    id("org.jetbrains.compose") version "1.9.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

kotlin {
    jvm()
    jvmToolchain(21)
    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
            implementation(compose.desktop.uiTestJUnit4)
        }
    }
}

val verifyBend by tasks.registering(Exec::class) {
    group = "verification"
    description = "Check the formal laws with Bend."
    commandLine("bash", "scripts/bend.sh", "backend/PROOF.bend")
}

val buildBend by tasks.registering(Exec::class) {
    group = "build"
    description = "Compile the real Bend backend to a native CPU executable."
    dependsOn(verifyBend)
    inputs.files(fileTree("backend") { include("*.bend") })
    inputs.file("scripts/bend.sh")
    outputs.file(layout.buildDirectory.file("bend/risk-engine"))
    doFirst { layout.buildDirectory.dir("bend").get().asFile.mkdirs() }
    commandLine("bash", "scripts/bend.sh", "backend/main.bend", "-o", "build/bend/risk-engine")
}

tasks.withType<Test>().configureEach {
    dependsOn(buildBend)
    systemProperty("bend.engine.path", layout.buildDirectory.file("bend/risk-engine").get().asFile.absolutePath)
    testLogging { events("passed", "skipped", "failed") }
}

compose.desktop {
    application {
        mainClass = "fr.bendcompose.MainKt"
        jvmArgs += "-Dbend.engine.path=${layout.buildDirectory.file("bend/risk-engine").get().asFile.absolutePath}"
    }
}

tasks.matching { it.name == "run" || it.name == "createDistributable" }.configureEach {
    dependsOn(buildBend)
}
