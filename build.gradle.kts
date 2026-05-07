import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

plugins {
    kotlin("jvm") version "2.3.10"
    application
}

group = "net.rafkos.ojkipojki"
version = "0.3.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("net.rafkos.ojkipojki.MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

// ── constants ─────────────────────────────────────────────────────────────────

val appName: String = rootProject.name
val appVersion: String = version.toString().removeSuffix("-SNAPSHOT")
val currentOs: String = System.getProperty("os.name").lowercase()

// ── directories ───────────────────────────────────────────────────────────────

val outputDir         = layout.projectDirectory.dir("output")
val localResourcesDir = layout.projectDirectory.dir("local_resources")
val lastRunTmpDir     = layout.projectDirectory.dir("last_run_tmp")
val jpackageInputDir  = layout.buildDirectory.dir("jpackage/input")
val jpackageContentDir= layout.buildDirectory.dir("jpackage/content")
val jpackageOutputDir = layout.buildDirectory.dir("jpackage/output")

// ── run ───────────────────────────────────────────────────────────────────────

val prepareLastRunTmp by tasks.registering(Copy::class) {
    group = "application"
    description = "Copy local_resources into last_run_tmp (preserves runtime-generated files)"
    from(localResourcesDir)
    into(lastRunTmpDir)
}

tasks.named<JavaExec>("run") {
    dependsOn(prepareLastRunTmp)
    workingDir = lastRunTmpDir.asFile
}

// ── jpackage staging ──────────────────────────────────────────────────────────

// depends on build so tests must pass before packaging
val stageJpackageInput by tasks.registering(Copy::class) {
    group = "release"
    dependsOn(tasks.named("build"))
    from(configurations.named("runtimeClasspath"))
    from(tasks.named<Jar>("jar").map { it.outputs.files })
    into(jpackageInputDir)
}

val stageJpackageContent by tasks.registering(Copy::class) {
    group = "release"
    from(localResourcesDir) { exclude("sprites/**"); exclude("scenarios/**") }
    from(localResourcesDir.asFile) { include("sprites/**") }
    from(localResourcesDir.asFile) { include("scenarios/**") }
    into(jpackageContentDir)
}

val ensureOutputDir by tasks.registering {
    group = "release"
    doLast { outputDir.asFile.mkdirs() }
}

// ── jpackage helper ───────────────────────────────────────────────────────────

// called inside doFirst (execution time) — all .get() calls are safe there
fun jpackageCmd(type: String, destDir: File): List<String> {
    val args = mutableListOf(
        "jpackage",
        "--input",       jpackageInputDir.get().asFile.absolutePath,
        "--main-jar",    tasks.named<Jar>("jar").get().archiveFileName.get(),
        "--main-class",  application.mainClass.get(),
        "--name",        appName,
        "--app-version", appVersion,
        "--type",        type,
        "--dest",        destDir.absolutePath,
        "--java-options", "-Dapp.dir=\$APPDIR"
    )
    // Windows icon (.ico); Linux needs .png and macOS needs .icns — skip if unavailable
    if (currentOs.contains("windows")) {
        val ico = jpackageContentDir.get().asFile.resolve("icon.ico")
        if (ico.exists()) { args += "--icon"; args += ico.absolutePath }
    }
    // Each top-level item from local_resources as its own --app-content so they land at app root
    val contentItems = jpackageContentDir.get().asFile.listFiles()
    if (!contentItems.isNullOrEmpty()) {
        args += "--app-content"
        args += contentItems.joinToString(",") { it.absolutePath }
    }
    return args
}

// ── Windows ───────────────────────────────────────────────────────────────────

val jpackageWindowsAppImage by tasks.registering(Exec::class) {
    group = "release"
    description = "jpackage app-image for Windows (Windows only)"
    onlyIf { currentOs.contains("windows") }
    dependsOn(stageJpackageInput, stageJpackageContent)
    val destDir = jpackageOutputDir.map { it.dir("windows") }
    doFirst {
        destDir.get().asFile.let { it.deleteRecursively(); it.mkdirs() }
        commandLine(jpackageCmd("app-image", destDir.get().asFile))
    }
}

val zipWindowsRelease by tasks.registering(Zip::class) {
    group = "release"
    description = "Zip Windows app-image → output/${appName}_${appVersion}_windows_x64.zip"
    onlyIf { currentOs.contains("windows") }
    dependsOn(jpackageWindowsAppImage, ensureOutputDir)
    from(jpackageOutputDir.map { it.dir("windows") })
    archiveFileName.set("${appName}_${appVersion}_windows_x64.zip")
    destinationDirectory.set(outputDir)
}

// ── Linux deb ─────────────────────────────────────────────────────────────────

val jpackageLinuxDeb by tasks.registering(Exec::class) {
    group = "release"
    description = "jpackage .deb for Linux (Linux only)"
    onlyIf { currentOs.contains("linux") }
    dependsOn(stageJpackageInput, stageJpackageContent, ensureOutputDir)
    val destDir = jpackageOutputDir.map { it.dir("linux-deb") }
    doFirst {
        destDir.get().asFile.let { it.deleteRecursively(); it.mkdirs() }
        commandLine(jpackageCmd("deb", destDir.get().asFile))
    }
    doLast {
        val src = destDir.get().asFile.listFiles { f -> f.extension == "deb" }?.firstOrNull()
        val dst = outputDir.asFile.resolve("${appName}_${appVersion}_linux_x64.deb")
        if (src != null) Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

// ── Linux rpm ─────────────────────────────────────────────────────────────────

val jpackageLinuxRpm by tasks.registering(Exec::class) {
    group = "release"
    description = "jpackage .rpm for Linux (Linux only)"
    onlyIf { currentOs.contains("linux") }
    dependsOn(stageJpackageInput, stageJpackageContent, ensureOutputDir)
    val destDir = jpackageOutputDir.map { it.dir("linux-rpm") }
    doFirst {
        destDir.get().asFile.let { it.deleteRecursively(); it.mkdirs() }
        commandLine(jpackageCmd("rpm", destDir.get().asFile))
    }
    doLast {
        val src = destDir.get().asFile.listFiles { f -> f.extension == "rpm" }?.firstOrNull()
        val dst = outputDir.asFile.resolve("${appName}_${appVersion}_linux_x64.rpm")
        if (src != null) Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

// ── macOS ─────────────────────────────────────────────────────────────────────

val jpackageMacOsDmg by tasks.registering(Exec::class) {
    group = "release"
    description = "jpackage .dmg for macOS (macOS only)"
    onlyIf { currentOs.contains("mac") }
    dependsOn(stageJpackageInput, stageJpackageContent, ensureOutputDir)
    val destDir = jpackageOutputDir.map { it.dir("macos") }
    doFirst {
        destDir.get().asFile.let { it.deleteRecursively(); it.mkdirs() }
        commandLine(jpackageCmd("dmg", destDir.get().asFile))
    }
    doLast {
        val src = destDir.get().asFile.listFiles { f -> f.extension == "dmg" }?.firstOrNull()
        val dst = outputDir.asFile.resolve("${appName}_${appVersion}_macos_x64.dmg")
        if (src != null) Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

// ── release_all ───────────────────────────────────────────────────────────────

tasks.named("build") { mustRunAfter("clean") }

val release_all by tasks.registering {
    group = "release"
    description = "Clean + build + package current platform → ./output"
    dependsOn(
        tasks.named("clean"),
        tasks.named("build"),
        ensureOutputDir,
        zipWindowsRelease,
        jpackageLinuxDeb,
        jpackageLinuxRpm,
        jpackageMacOsDmg
    )
}
