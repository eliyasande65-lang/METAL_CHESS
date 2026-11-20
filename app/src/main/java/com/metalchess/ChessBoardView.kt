package com.metalchess

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square

class ChessBoardView(context: Context, private val onStatus: (String) -> Unit) : View(context) {
    private val board = Board()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var selected: Square? = null
    private var flipped = false
    private var legalTargets: Set<Square> = emptySet()
    private val light = 0xFFE1E4E8.toInt()
    private val dark = 0xFF555D68.toInt()

    fun reset() {
        board.loadFromFen(Board().fen)
        selected = null; legalTargets = emptySet(); invalidate()
        onStatus("White to move · New game")
    }
    fun flip() { flipped = !flipped; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        val cell = size / 8f
        for (row in 0..7) for (col in 0..7) {
            val file = if (flipped) 7 - col else col
            val rank = if (flipped) row + 1 else 8 - row
            val square = Square.valueOf("${"abcdefgh"[file]}$rank".uppercase())
            val x = left + col * cell
            val y = top + row * cell
            paint.color = if ((row + col) % 2 == 0) light else dark
            canvas.drawRect(x, y, x + cell, y + cell, paint)
            if (selected == square) {
                paint.color = 0x887EB6FF.toInt()
                canvas.drawRect(x, y, x + cell, y + cell, paint)
            }
            if (square in legalTargets) {
                paint.color = 0xAA1B7A54.toInt()
                canvas.drawCircle(x + cell/2, y + cell/2, cell * .13f, paint)
            }
            val piece = board.getPiece(square).toString()
            val glyph = glyph(piece)
            if (glyph.isNotEmpty()) {
                paint.color = if (piece.startsWith("W")) 0xFFF8F8F5.toInt() else 0xFF16191F.toInt()
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = cell * .77f
                paint.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
                canvas.drawText(glyph, x + cell/2, y + cell*.77f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = cell * .012f
                paint.color = if (piece.startsWith("W")) 0xFF30343B.toInt() else 0xFFBFC5CE.toInt()
                canvas.drawText(glyph, x + cell/2, y + cell*.77f, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    private fun glyph(piece: String) = when (piece) {
        "WK" -> "♔"; "WQ" -> "♕"; "WR" -> "♖"; "WB" -> "♗"; "WN" -> "♘"; "WP" -> "♙"
        "BK" -> "♚"; "BQ" -> "♛"; "BR" -> "♜"; "BB" -> "♝"; "BN" -> "♞"; "BP" -> "♟"
        else -> ""
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val size = minOf(width, height).toFloat()
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        val col = ((event.x - left) / (size / 8f)).toInt()
        val row = ((event.y - top) / (size / 8f)).toInt()
        if (row !in 0..7 || col !in 0..7) return true
        val file = if (flipped) 7-col else col
        val rank = if (flipped) row+1 else 8-row
        val square = Square.valueOf("${"abcdefgh"[file]}$rank".uppercase())
        val piece = board.getPiece(square).toString()
        val whiteTurn = board.sideToMove.toString().startsWith("WHITE")
        if (selected == null) {
            if (piece != "NONE" && piece.startsWith(if (whiteTurn) "W" else "B")) {
                selected = square
                legalTargets = board.legalMoves().filter { it.from == square }.map { it.to }.toSet()
            }
        } else {
            val move = board.legalMoves().firstOrNull { it.from == selected && it.to == square }
            if (move != null) {
                board.doMove(move)
                selected = null; legalTargets = emptySet()
                onStatus(when {
                    board.isMated -> "Checkmate"
                    board.isDraw -> "Draw"
                    else -> "${board.sideToMove} to move"
                })
            } else if (piece != "NONE" && piece.startsWith(if (whiteTurn) "W" else "B")) {
                selected = square
                legalTargets = board.legalMoves().filter { it.from == square }.map { it.to }.toSet()
            } else { selected = null; legalTargets = emptySet() }
        }
        invalidate()
        return true
    }
}
