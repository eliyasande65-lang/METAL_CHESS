package com.metalchess

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var appRoot: FrameLayout
    private val gold = Color.rgb(211, 174, 91)
    private val panel = Color.rgb(17, 18, 21)
    private val prefs by lazy { getSharedPreferences("chess_saves", MODE_PRIVATE) }
    private var stockfish: UciEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(5, 6, 8)
        window.navigationBarColor = Color.rgb(5, 6, 8)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE

        appRoot = FrameLayout(this)
        showHomeScreen()
    }

    private fun showHomeScreen() {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(6, 7, 9))
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mark = metalText("M", 26f, gold).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(30, 26, 18), gold, 1)
        }
        top.addView(mark, LinearLayout.LayoutParams(dp(48), dp(48)))
        val brandBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brandBlock.addView(metalText("METAL CHESS", 18f, Color.WHITE).apply { letterSpacing = .16f })
        brandBlock.addView(metalText("THE METALLIC EDITION", 9f, gold).apply { letterSpacing = .18f; setPadding(0, dp(3), 0, 0) })
        top.addView(brandBlock, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(metalText("OFFLINE", 9f, Color.rgb(176, 190, 169)).apply {
            gravity = Gravity.CENTER
            letterSpacing = .12f
            background = rounded(Color.rgb(18, 27, 21), Color.rgb(75, 111, 78), 1)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        })
        page.addView(top)
        val intro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(38), 0, dp(22))
        }
        intro.addView(metalText("YOUR NEXT MOVE.", 29f, Color.WHITE).apply { letterSpacing = .035f })
        intro.addView(metalText("A refined chess experience. Powered by Stockfish. Ready anywhere.", 13f, Color.rgb(157, 159, 164)).apply {
            setPadding(0, dp(8), 0, 0)
        })
        page.addView(intro)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(42, 35, 23), Color.rgb(19, 19, 20), Color.rgb(12, 13, 16))
            ).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.rgb(125, 98, 48)) }
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        hero.addView(metalText("PLAY CHESS", 11f, gold).apply { letterSpacing = .2f })
        hero.addView(metalText("Challenge your mind.", 23f, Color.WHITE).apply { setPadding(0, dp(9), 0, dp(5)) })
        hero.addView(metalText("Choose your side. Face Stockfish or play with a friend.", 12f, Color.rgb(184, 181, 174)))
        val play = actionButton("START A GAME  〉") {
            showGameScreen()
            confirmNewGame()
        }.apply {
            textSize = 12f
            background = rounded(Color.rgb(153, 116, 48), Color.rgb(222, 190, 121), 1)
            setTextColor(Color.rgb(14, 13, 11))
        }
        hero.addView(play, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(18) })
        page.addView(hero)

        page.addView(metalText("THE CHESS STUDIO", 10f, Color.rgb(137, 139, 145)).apply {
            letterSpacing = .18f
            setPadding(0, dp(28), 0, dp(12))
        })
        fun studioCard(title: String, subtitle: String, symbol: String, action: () -> Unit): View {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(Color.rgb(19, 20, 23), Color.rgb(49, 47, 42), 1)
                setPadding(dp(14), dp(15), dp(14), dp(15))
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }
            card.addView(metalText(symbol, 22f, gold))
            card.addView(metalText(title, 13f, Color.WHITE).apply { setPadding(0, dp(10), 0, dp(4)) })
            card.addView(metalText(subtitle, 10f, Color.rgb(151, 153, 158)))
            return card
        }
        val rowOne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowOne.addView(studioCard("ANALYSIS", "Review saved games", "⌁") {
            showGameScreen()
            showSavedGames()
        }, LinearLayout.LayoutParams(0, dp(116), 1f))
        rowOne.addView(studioCard("TRAINING", "Coach and tactics", "♟") {
            showGameScreen()
            if (stockfish == null) stockfish = UciEngine(this)
            prefs.edit().putBoolean("coach_mode", true).apply()
            boardView.setCoachMode(true, stockfish)
            toastDialog("COACH MODE", "Coach mode is enabled. Make a move in a local game to receive engine guidance.")
        }, LinearLayout.LayoutParams(0, dp(116), 1f).apply { leftMargin = dp(10) })
        page.addView(rowOne)
        val rowTwo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowTwo.addView(studioCard("MY GAMES", "Continue and replay", "▤") {
            showGameScreen()
            showSavedGames()
        }, LinearLayout.LayoutParams(0, dp(116), 1f).apply { topMargin = dp(10) })
        rowTwo.addView(studioCard("SETTINGS", "Sounds and preferences", "⚙") {
            showSettingsDialog()
        }, LinearLayout.LayoutParams(0, dp(116), 1f).apply { leftMargin = dp(10); topMargin = dp(10) })
        page.addView(rowTwo)
        page.addView(metalText("BUILT FOR THE LOVE OF CHESS", 9f, Color.rgb(101, 103, 108)).apply {
            gravity = Gravity.CENTER
            letterSpacing = .16f
            setPadding(0, dp(32), 0, dp(8))
        })
        scroll.addView(page)
        appRoot.removeAllViews()
        appRoot.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(appRoot)
    }

    private fun showGameScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(6, 7, 9))
            setPadding(dp(10), dp(8), dp(10), dp(10))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = metalText("METAL CHESS", 17f, Color.WHITE).apply {
            letterSpacing = .12f
            setOnClickListener { showHomeScreen() }
        }
        toolbar.addView(brand, LinearLayout.LayoutParams(0, dp(44), 1f))
        val home = actionButton("HOME") { showHomeScreen() }
        toolbar.addView(home, LinearLayout.LayoutParams(dp(72), dp(38)))
        menuButton = metalText("⋮", 25f, gold).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(24, 25, 29), gold, 1)
            setOnClickListener { showMenu(it) }
        }
        toolbar.addView(menuButton, LinearLayout.LayoutParams(dp(42), dp(38)).apply { leftMargin = dp(8) })
        root.addView(toolbar)
        status = metalText("WHITE TO MOVE", 10f, gold).apply {
            letterSpacing = .15f
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(24, 22, 18), Color.rgb(91, 73, 42), 1)
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(36)).apply { topMargin = dp(8); bottomMargin = dp(8) })
        boardView = ChessBoardView(this,
            onStatus = { status.text = it.uppercase() },
            onGameOver = { result, moves -> showAnalysis(result, moves) },
            onCoach = { message -> showCoachSpeech(message) }
        )
        val boardFrame = FrameLayout(this).apply {
            background = rounded(Color.rgb(13, 14, 17), Color.rgb(139, 107, 51), 2)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            addView(boardView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        }
        if (prefs.getBoolean("coach_mode", false)) {
            if (stockfish == null) stockfish = UciEngine(this)
            boardView.setCoachMode(true, stockfish)
        }
        root.addView(boardFrame, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(9), 0, 0)
        }
        footer.addView(actionButton("UNDO") { boardView.undo() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        footer.addView(actionButton("FLIP") { boardView.flip() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(8) })
        footer.addView(actionButton("NEW GAME") { confirmNewGame() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(8) })
        root.addView(footer)
        appRoot.removeAllViews()
        appRoot.addView(root, FrameLayout.LayoutParams(-1, -1))
        setContentView(appRoot)
    }

    private fun showSettingsDialog() {
        val soundLabel = if (prefs.getBoolean("move_sounds", true)) "Move sounds: ON" else "Move sounds: OFF"
        val coachLabel = if (prefs.getBoolean("coach_mode", false)) "Coach mode: ON" else "Coach mode: OFF"
        AlertDialog.Builder(this).setTitle("SETTINGS")
            .setItems(arrayOf(soundLabel, coachLabel, "Close")) { _, which ->
                when (which) {
                    0 -> {
                        val enabled = !prefs.getBoolean("move_sounds", true)
                        prefs.edit().putBoolean("move_sounds", enabled).apply()
                        SoundEffects.setEnabled(enabled)
                        showSettingsDialog()
                    }
                    1 -> {
                        val enabled = !prefs.getBoolean("coach_mode", false)
                        prefs.edit().putBoolean("coach_mode", enabled).apply()
                        if (enabled && stockfish == null) stockfish = UciEngine(this)
                        if (::boardView.isInitialized) boardView.setCoachMode(enabled, if (enabled) stockfish else null)
                        showSettingsDialog()
                    }
                }
            }.setNegativeButton("Done", null).show().styleDialog()
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        listOf("New game", "Play Stockfish", "Save current game", "Saved games", "Replay current game", "Flip board", "Undo move", if (prefs.getBoolean("move_sounds", true)) "Move sounds: ON" else "Move sounds: OFF", if (prefs.getBoolean("coach_mode", false)) "Coach mode: ON" else "Coach mode: OFF").forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener {
            when (it.title.toString()) {
                "New game" -> confirmNewGame()
                "Play Stockfish" -> showEngineLevels("WHITE")
                "Save current game" -> saveCurrentGame()
                "Saved games" -> showSavedGames()
                "Replay current game" -> replayCurrentGame()
                "Flip board" -> boardView.flip()
                "Undo move" -> boardView.undo()
                "Coach mode: ON", "Coach mode: OFF" -> {
                    val enabled = !prefs.getBoolean("coach_mode", false)
                    prefs.edit().putBoolean("coach_mode", enabled).apply()
                    if (enabled && stockfish == null) stockfish = UciEngine(this)
                    boardView.setCoachMode(enabled, if (enabled) stockfish else null)
                    toastDialog("Coach mode", if (enabled) "Coach hints are enabled. Make a move to receive a highlighted engine suggestion." else "Coach hints are disabled.")
                }
                "Move sounds: ON", "Move sounds: OFF" -> {
                    val enabled = !prefs.getBoolean("move_sounds", true)
                    prefs.edit().putBoolean("move_sounds", enabled).apply()
                    SoundEffects.setEnabled(enabled)
                    toastDialog("Move sounds", if (enabled) "Piece-drop sounds enabled." else "Piece-drop sounds disabled.")
                }
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
        var playing = false
        val handler = Handler(Looper.getMainLooper())
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val preview = ChessBoardView(this, {}, { _, _ -> }).apply {
            loadPosition(fens.first())
            setOnTouchListener { _, _ -> true }
        }
        content.addView(preview, LinearLayout.LayoutParams(-1, dp(300)))
        val counter = metalText("", 12f, gold).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(4)) }
        val moveText = metalText("", 12f, Color.LTGRAY).apply { gravity = Gravity.CENTER; setPadding(0, dp(2), 0, dp(10)) }
        val play = actionButton("PLAY") {}
        content.addView(counter)
        content.addView(moveText)
        content.addView(play, LinearLayout.LayoutParams(-1, dp(44)))
        fun refresh() {
            counter.text = "MOVE $index / ${fens.lastIndex}"
            moveText.text = if (index == 0) "Starting position" else movesJson.optString(index - 1, "Move")
        }
        lateinit var ticker: Runnable
        ticker = Runnable {
            if (!playing) return@Runnable
            if (index < fens.lastIndex) {
                index++
                preview.loadPosition(fens[index])
                refresh()
                handler.postDelayed(ticker, 780L)
            } else {
                playing = false
                play.text = "PLAY AGAIN"
            }
        }
        play.setOnClickListener {
            if (playing) {
                playing = false
                handler.removeCallbacks(ticker)
                play.text = "PLAY"
            } else {
                if (index >= fens.lastIndex) {
                    index = 0
                    preview.loadPosition(fens[0])
                    refresh()
                }
                playing = true
                play.text = "PAUSE"
                handler.postDelayed(ticker, 250L)
            }
        }
        refresh()
        val dialog = AlertDialog.Builder(this).setTitle(item.optString("name", "GAME REVIEW"))
            .setView(content).setNegativeButton("Close", null).create()
        dialog.setOnDismissListener { playing = false; handler.removeCallbacks(ticker) }
        dialog.show()
        dialog.styleDialog()
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
            .setNegativeButton("Review game") { _, _ -> replayCurrentGame() }.show().styleDialog()
    }

    private fun showEngineLevels(playerSide: String = "WHITE") {
        AlertDialog.Builder(this)
            .setTitle("CHOOSE STOCKFISH LEVEL")
            .setItems(UciEngine.labels.toTypedArray()) { _, which ->
                try {
                    if (stockfish == null) stockfish = UciEngine(this)
                    boardView.startEngineGame(stockfish!!, which, playerSide)
                    status.text = "YOU ARE $playerSide · STOCKFISH ${UciEngine.labels[which].uppercase()}"
                } catch (e: Exception) {
                    toastDialog("Stockfish unavailable", e.message ?: "Unable to start the chess engine.")
                }
            }.setNegativeButton("Cancel", null).show().styleDialog()
    }

    private fun confirmNewGame() {
        AlertDialog.Builder(this).setTitle("CHOOSE YOUR SIDE")
            .setMessage("Start a game against Stockfish, or play locally with another person.")
            .setItems(arrayOf("Play as White", "Play as Black", "Local two-player game")) { _, which ->
                when (which) {
                    0 -> showEngineLevels("WHITE")
                    1 -> showEngineLevels("BLACK")
                    else -> { boardView.startLocalGame(); status.text = "WHITE TO MOVE" }
                }
            }.setNegativeButton("Cancel", null).show().styleDialog()
    }

    private fun showCoachSpeech(message: String) {
        AlertDialog.Builder(this).setTitle("CHESS COACH")
            .setMessage(message)
            .setPositiveButton("GOT IT", null)
            .show().styleDialog()
    }

    override fun onDestroy() {
        stockfish?.close()
        super.onDestroy()
    }

    private fun readSaves(): JSONArray = try { JSONArray(prefs.getString("games", "[]")) } catch (_: Exception) { JSONArray() }

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
