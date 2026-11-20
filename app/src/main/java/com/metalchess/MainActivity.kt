package com.metalchess

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.FrameLayout

class MainActivity : Activity() {
    private lateinit var boardView: ChessBoardView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 9, 12)
        window.navigationBarColor = Color.rgb(8, 9, 12)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 11, 14))
            setPadding(dp(18), dp(22), dp(18), dp(18))
        }
        root.addView(TextView(this).apply {
            text = "METAL  /  CHESS"; textSize = 13f; letterSpacing = .22f
            setTextColor(Color.rgb(184, 190, 201)); gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-1, dp(36)))
        root.addView(TextView(this).apply {
            text = "The board is yours."; textSize = 27f
            setTextColor(Color.rgb(245, 246, 248)); setPadding(0, dp(8), 0, dp(4))
        })
        status = TextView(this).apply {
            text = "WHITE TO MOVE  ·  CLASSICAL"; textSize = 11f; letterSpacing = .12f
            setTextColor(Color.rgb(151, 160, 174)); setPadding(0, dp(10), 0, dp(14))
        }
        root.addView(status)
        boardView = ChessBoardView(this) { status.text = it.uppercase() }
        val holder = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(34, 37, 43)); setPadding(dp(4), dp(4), dp(4), dp(4))
            addView(boardView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        }
        root.addView(holder, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(16), 0, 0) }
        actions.addView(actionButton("NEW GAME") { boardView.reset() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(actionButton("FLIP BOARD") { boardView.flip() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(10) })
        root.addView(actions)
        root.addView(TextView(this).apply {
            text = "PLAY  ·  TRAIN  ·  ENDGAME LAB"; textSize = 10f; letterSpacing = .16f
            gravity = Gravity.CENTER; setTextColor(Color.rgb(111, 120, 134)); setPadding(0, dp(18), 0, 0)
        })
        setContentView(root)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 10f; letterSpacing = .08f
        setTextColor(Color.rgb(222, 226, 233)); setBackgroundColor(Color.rgb(35, 39, 47))
        setOnClickListener { action() }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
