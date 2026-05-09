package net.rafkos.ojkipojki.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Path

private data class Rule(
    val name: String,
    val source: String,
    val forbidden: List<String>,
    val exempt: List<String> = emptyList(),
    val allowedImports: List<String> = emptyList(),
)

private val BASE = "net.rafkos.ojkipojki"

private val rules = listOf(
    Rule(
        name = "R1: shared depends on nothing app-tier",
        source = "$BASE.shared",
        forbidden = listOf("$BASE.client", "$BASE.server", "$BASE.launcher"),
    ),
    Rule(
        name = "R2: client never sees server code",
        source = "$BASE.client",
        forbidden = listOf("$BASE.server"),
    ),
    Rule(
        name = "R3: server never sees client code",
        source = "$BASE.server",
        forbidden = listOf("$BASE.client"),
    ),
    Rule(
        name = "R4: client.application is pure domain",
        source = "$BASE.client.application",
        forbidden = listOf("$BASE.client.protocol", "$BASE.client.view", "javax.swing", "java.awt"),
        // java.awt.image.BufferedImage is legitimate in sprite loading (image processing, not UI)
        allowedImports = listOf("java.awt.image"),
    ),
    Rule(
        name = "R5: client.protocol is Swing-free",
        source = "$BASE.client.protocol",
        forbidden = listOf("$BASE.client.view", "javax.swing", "java.awt"),
    ),
    Rule(
        name = "R6: server.application does not know protocol",
        source = "$BASE.server.application",
        forbidden = listOf("$BASE.server.protocol"),
    ),
    Rule(
        name = "R7: server.model is leaf",
        source = "$BASE.server.model",
        forbidden = listOf("$BASE.server.protocol", "$BASE.server.application"),
    ),
    Rule(
        name = "R8a: shared.protocol.command not cross-coupled with event",
        source = "$BASE.shared.protocol.command",
        forbidden = listOf("$BASE.shared.protocol.event"),
    ),
    Rule(
        name = "R8b: shared.protocol.event not cross-coupled with command",
        source = "$BASE.shared.protocol.event",
        forbidden = listOf("$BASE.shared.protocol.command"),
    ),
)

private data class SourceFile(val path: Path, val pkg: String, val imports: List<String>)

private fun scan(root: Path): List<SourceFile> = root.toFile().walkTopDown()
    .filter { it.isFile && it.extension == "kt" }
    .map { f ->
        val lines = f.readLines()
        val pkg = lines.firstOrNull { it.startsWith("package ") }
            ?.removePrefix("package ")?.trim().orEmpty()
        val imports = lines.filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").substringBefore(" as ").trim() }
        SourceFile(f.toPath(), pkg, imports)
    }.toList()

class PackageBoundaryTest {

    @TestFactory
    fun packageBoundaries(): List<DynamicTest> {
        val files = scan(Path.of("src/main/kotlin"))
        return rules.map { rule ->
            DynamicTest.dynamicTest(rule.name) {
                val violations = files
                    .filter { it.pkg.startsWith(rule.source) }
                    .filterNot { sf -> rule.exempt.any { sf.pkg.startsWith(it) } }
                    .flatMap { sf ->
                        sf.imports
                            .filter { imp -> rule.forbidden.any { imp.startsWith(it) } }
                            .filterNot { imp -> rule.allowedImports.any { imp.startsWith(it) } }
                            .map { "${sf.path}: $it" }
                    }
                assertTrue(violations.isEmpty()) {
                    "Architecture violations — ${rule.name}:\n  " +
                        violations.joinToString("\n  ")
                }
            }
        }
    }
}
