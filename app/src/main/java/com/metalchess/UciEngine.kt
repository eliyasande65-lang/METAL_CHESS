package com.metalchess

import android.content.Context
import android.os.Build
import java.io.File
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Local Stockfish UCI process. The official Android engine is bundled by CI
 * under assets/engine/<abi>/ together with its NNUE network and license.
 */
class UciEngine(context: Context) {
    companion object {
        val labels = listOf("Beginner", "Easy", "Middle", "Hard", "Advanced", "Expert", "Candidate", "Master", "Grandmaster")
        // Stockfish Skill Level is a real weakening control, not a UI-only label.
        private val skills = listOf(0, 2, 4, 7, 10, 13, 16, 18, 20)
        private val moveTimes = listOf(250, 350, 500, 700, 900, 1200, 1600, 2200, 3000)
    }

    private val appContext = context.applicationContext
    private var process: Process? = null
    private var input: BufferedReader? = null
    private var output: BufferedWriter? = null

    @Synchronized
    fun start() {
        check(Build.VERSION.SDK_INT >= 29) { "Stockfish requires Android 10 or newer on this build." }
        if (process?.isAlive == true) return
        val abi = when {
            Build.SUPPORTED_ABIS.any { it == "arm64-v8a" } -> "arm64-v8a"
            Build.SUPPORTED_ABIS.any { it == "armeabi-v7a" } -> "armeabi-v7a"
            else -> error("This device CPU is not supported by the bundled Stockfish engine.")
        }
        val engineDir = File(appContext.filesDir, "stockfish/$abi")
        copyAssetDirectory("engine/$abi", engineDir)
        // Android 10+ blocks exec() from writable app data. The binary is
        // packaged as a native-library payload and executed from /data/app.
        val executable = File(appContext.applicationInfo.nativeLibraryDir, "libstockfish.so")
        check(executable.exists()) { "Stockfish engine binary is missing from this APK." }
        val p = ProcessBuilder(executable.absolutePath)
            .directory(engineDir)
            .redirectErrorStream(true)
            .start()
        process = p
        input = BufferedReader(InputStreamReader(p.inputStream))
        output = BufferedWriter(OutputStreamWriter(p.outputStream))
        send("uci")
        waitFor("uciok")
        send("isready")
        waitFor("readyok")
    }

    @Synchronized
    fun bestMove(fen: String, difficulty: Int): String {
        start()
        val level = difficulty.coerceIn(0, skills.lastIndex)
        send("setoption name Threads value 1")
        send("setoption name Hash value 64")
        send("setoption name Skill Level value ${skills[level]}")
        send("ucinewgame")
        send("isready")
        waitFor("readyok")
        send("position fen $fen")
        send("go movetime ${moveTimes[level]}")
        while (true) {
            val line = input?.readLine() ?: error("Stockfish stopped unexpectedly.")
            if (line.startsWith("bestmove ")) {
                val move = line.substringAfter("bestmove ").substringBefore(' ').trim()
                return if (move == "(none)" || move == "0000") "" else move
            }
        }
    }

    @Synchronized
    fun close() {
        try { send("quit") } catch (_: Exception) { }
        try { process?.destroy() } catch (_: Exception) { }
        process = null
        input = null
        output = null
    }

    private fun send(command: String) {
        val writer = output ?: error("Stockfish is not running.")
        writer.write(command)
        writer.newLine()
        writer.flush()
    }

    private fun waitFor(expected: String) {
        while (true) {
            val line = input?.readLine() ?: error("Stockfish did not respond.")
            if (line.trim() == expected) return
        }
    }

    private fun copyAssetDirectory(path: String, destination: File) {
        destination.mkdirs()
        val names = appContext.assets.list(path) ?: emptyArray()
        for (name in names) {
            val childPath = "$path/$name"
            val child = File(destination, name)
            val children = appContext.assets.list(childPath) ?: emptyArray()
            if (children.isNotEmpty()) {
                copyAssetDirectory(childPath, child)
            } else {
                appContext.assets.open(childPath).use { source ->
                    child.outputStream().use { target -> source.copyTo(target) }
                }
            }
        }
    }
}
