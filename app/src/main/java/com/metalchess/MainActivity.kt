package com.metalchess

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var boardView: ChessBoardView
    private lateinit var status: TextView
    private lateinit var menuButton: TextView
    private val gold = Color.rgb(211, 174, 91)
    private val panel = Color.rgb(17, 18, 21)
    private val prefs by lazy { getSharedPreferences("chess_saves", MODE_PRIVATE) }\n    private var stockfish: UciEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(5, 6, 8)
        window.navigationBarColor = Color.rgb(5, 6, 8)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(5, 6, 8))
            setPadding(dp(14), dp(8), dp(14), dp(12))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = metalText("CHESS", 23f, Color.WHITE).apply {
            letterSpacing = .24f
        }
        toolbar.addView(brand, LinearLayout.LayoutParams(0, dp(54), 1f))
        menuButton = metalText("☰", 25f, gold).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(24, 25, 29), gold, 1)
            setOnClickListener { showMenu(it) }
        }
        toolbar.addView(menuButton, LinearLayout.LayoutParams(dp(52), dp(42)))
        root.addView(toolbar)

        status = metalText("WHITE TO MOVE", 11f, gold).apply {
            letterSpacing = .15f
            setPadding(dp(4), dp(8), dp(4), dp(12))
        }
        root.addView(status)

        boardView = ChessBoardView(this,
            onStatus = { status.text = it.uppercase() },
            onGameOver = { result, moves -> showAnalysis(result, moves) }
        )
        val boardFrame = FrameLayout(this).apply {
            background = rounded(Color.rgb(12, 13, 16), gold, 2)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            addView(boardView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        }
        root.addView(boardFrame, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(2))
        }
        footer.addView(actionButton("UNDO") { boardView.undo() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        footer.addView(actionButton("FLIP") { boardView.flip() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(8) })
        footer.addView(actionButton("NEW GAME") { confirmNewGame() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(8) })
        root.addView(footer)
        setContentView(root)
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        listOf("New game", "Play Stockfish", "Save current game", "Saved games", "Replay current game", "Flip board", "Undo move").forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener {
            when (it.title.toString()) {
                "New game" -> confirmNewGame()\n                "Play Stockfish" -> showEngineLevels()
                "Save current game" -> saveCurrentGame()
                "Saved games" -> showSavedGames()
                "Replay current game" -> replayCurrentGame()
                "Flip board" -> boardView.flip()
                "Undo move" -> boardView.undo()
            }
            true
        }
        popup.show()
    }

    private fun saveCurrentGame() {
        val fens = boardView.positionHistory()
        if (fens.size < 2) {
            toastDialog("Nothing to save", "Play at least one move before saving a game.")
            return
        }
        val saves = readSaves()
        val item = JSONObject().put("name", "Game " + (saves.length() + 1))
            .put("date", java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            .put("fens", JSONArray(fens)).put("moves", JSONArray(boardView.moveHistory()))
        saves.put(item)
        prefs.edit().putString("games", saves.toString()).apply()
        toastDialog("Game saved", "Your game is now available in Saved games.")
    }

    private fun showSavedGames() {
        val saves = readSaves()
        if (saves.length() == 0) {
            toastDialog("No saved games", "Save a game after making a few moves.")
            return
        }
        val labels = Array(saves.length()) { i ->
            val item = saves.getJSONObject(i)
            item.optString("name", "Saved game") + "    " + item.optString("date", "")
        }
        AlertDialog.Builder(this).setTitle("SAVED GAMES").setItems(labels) { _, which ->
            showReplay(saves.getJSONObject(which))
        }.setNegativeButton("Close", null).show().styleDialog()
    }

    private fun replayCurrentGame() {
        val fens = boardView.positionHistory()
        if (fens.size < 2) {
            toastDialog("No moves to replay", "Play a game first.")
            return
        }
        val item = JSONObject().put("name", "Current game")
            .put("fens", JSONArray(fens)).put("moves", JSONArray(boardView.moveHistory()))
        showReplay(item)
    }

    private fun showReplay(item: JSONObject) {
        val fensJson = item.optJSONArray("fens") ?: JSONArray()
        val movesJson = item.optJSONArray("moves") ?: JSONArray()
        val fens = (0 until fensJson.length()).map { fensJson.getString(it) }
        if (fens.isEmpty()) return
        var index = 0
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(8))
        }
        val counter = metalText("", 13f, gold).apply { gravity = Gravity.CENTER }
        val moveText = metalText("", 12f, Color.LTGRAY).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(12)) }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val previous = actionButton("PREVIOUS") { }
        val next = actionButton("NEXT") { }
        fun refresh() {
            counter.text = "POSITION " + (index + 1) + " OF " + fens.size
            moveText.text = if (index == 0) "Starting position" else movesJson.optString(index - 1, "Move")
            previous.isEnabled = index > 0
            next.isEnabled = index < fens.lastIndex
        }
        previous.setOnClickListener { if (index > 0) { index--; refresh() } }
        next.setOnClickListener { if (index < fens.lastIndex) { index++; refresh() } }
        controls.addView(previous, LinearLayout.LayoutParams(0, dp(44), 1f))
        controls.addView(next, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8) })
        content.addView(counter)
        content.addView(moveText)
        content.addView(controls)
        refresh()
        val dialog = AlertDialog.Builder(this).setTitle(item.optString("name", "REPLAY"))
            .setView(content).setNegativeButton("Close", null)
            .setPositiveButton("Load this position", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                boardView.loadPosition(fens[index])
                dialog.dismiss()
            }
            dialog.styleDialog()
        }
        dialog.show()
    }

    private fun showAnalysis(result: String, moves: List<String>) {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
        }
        body.addView(metalText(result, 18f, gold))
        body.addView(metalText("GAME SUMMARY", 10f, Color.LTGRAY).apply { setPadding(0, dp(12), 0, dp(6)) })
        val details = if (moves.isEmpty()) "No moves recorded." else moves.takeLast(12).joinToString("\n")
        body.addView(metalText(details, 12f, Color.WHITE))
        body.addView(metalText("Moves played: " + moves.size, 11f, Color.LTGRAY).apply { setPadding(0, dp(10), 0, 0) })
        AlertDialog.Builder(this).setTitle("GAME ANALYSIS").setView(body)
            .setPositiveButton("New game") { _, _ -> boardView.startLocalGame(); status.text = "WHITE TO MOVE" }
            .setNegativeButton("Review moves", null).show().styleDialog()
    }

    private fun showEngineLevels() {\n        AlertDialog.Builder(this)\n            .setTitle("CHOOSE STOCKFISH LEVEL")\n            .setItems(UciEngine.labels.toTypedArray()) { _, which ->\n                try {\n                    if (stockfish == null) stockfish = UciEngine(this)\n                    boardView.startEngineGame(stockfish!!, which)\n                    status.text = "YOU ARE WHITE · STOCKFISH ${UciEngine.labels[which].uppercase()}"\n                } catch (e: Exception) {\n                    toastDialog("Stockfish unavailable", e.message ?: "Unable to start the chess engine.")\n                }\n            }.setNegativeButton("Cancel", null).show().styleDialog()\n    }\n\n    private fun confirmNewGame() {
        AlertDialog.Builder(this).setTitle("Start a new game?")
            .setMessage("The current game will be cleared unless you save it first.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("New game") { _, _ -> boardView.startLocalGame(); status.text = "WHITE TO MOVE" }
            .show().styleDialog()
    }

    override fun onDestroy() {\n        stockfish?.close()\n        super.onDestroy()\n    }\n\n    private fun readSaves(): JSONArray = try { JSONArray(prefs.getString("games", "[]")) } catch (_: Exception) { JSONArray() }

    private fun toastDialog(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show().styleDialog()
    }

    private fun metalText(value: String, size: Float, color: Int): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setShadowLayer(2.2f, 0f, 2f, Color.rgb(0, 0, 0))
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 10f
        letterSpacing = .06f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(Color.rgb(242, 226, 188))
        background = rounded(panel, gold, 1)
        setOnClickListener { action() }
    }

    private fun rounded(fill: Int, stroke: Int, width: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(9).toFloat()
            setStroke(dp(width), stroke)
        }

    private fun AlertDialog.styleDialog() {
        window?.setBackgroundDrawable(rounded(Color.rgb(20, 21, 24), gold, 1))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
