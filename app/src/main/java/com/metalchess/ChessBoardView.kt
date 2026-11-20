package com.metalchess

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square

class ChessBoardView(
    context: Context,
    private val onStatus: (String) -> Unit,
    private val onGameOver: (String, List<String>) -> Unit
) : View(context) {
    private val board = Board()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var selected: Square? = null
    private var flipped = false
    private var legalTargets: Set<Square> = emptySet()
    private val positions = mutableListOf(board.fen)
    private val moves = mutableListOf<String>()
    private var gameFinished = false
    private val light = 0xFFF0EEE8.toInt()
    private val dark = 0xFF24272D.toInt()
    private val gold = 0xFFD3AE5B.toInt()

    fun reset() {
        board.loadFromFen(Board().fen)
        positions.clear()
        positions.add(board.fen)
        moves.clear()
        selected = null
        legalTargets = emptySet()
        gameFinished = false
        invalidate()
        onStatus("White to move")
    }

    fun flip() { flipped = !flipped; invalidate() }

    fun undo() {
        if (positions.size <= 1 || gameFinished) return
        positions.removeAt(positions.lastIndex)
        if (moves.isNotEmpty()) moves.removeAt(moves.lastIndex)
        board.loadFromFen(positions.last())
        selected = null
        legalTargets = emptySet()
        gameFinished = false
        onStatus(board.sideToMove.toString() + " to move")
        invalidate()
    }

    fun positionHistory(): List<String> = positions.toList()
    fun moveHistory(): List<String> = moves.toList()

    fun loadPosition(fen: String) {
        try {
            board.loadFromFen(fen)
            selected = null
            legalTargets = emptySet()
            gameFinished = false
            onStatus(board.sideToMove.toString() + " to move · Replay position")
            invalidate()
        } catch (_: Exception) { onStatus("Unable to load saved position") }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        val cell = size / 8f

        paint.style = Paint.Style.FILL
        paint.color = gold
        canvas.drawRect(left - dp(2), top - dp(2), left + size + dp(2), top + size + dp(2), paint)

        for (row in 0..7) for (col in 0..7) {
            val file = if (flipped) 7 - col else col
            val rank = if (flipped) row + 1 else 8 - row
            val square = Square.valueOf("${"abcdefgh"[file]}$rank".uppercase())
            val x = left + col * cell
            val y = top + row * cell
            paint.color = if ((row + col) % 2 == 0) light else dark
            canvas.drawRect(x, y, x + cell, y + cell, paint)

            if (selected == square) {
                paint.color = 0x88D3AE5B.toInt()
                canvas.drawRect(x, y, x + cell, y + cell, paint)
            }
            if (square in legalTargets) {
                paint.color = 0xBBD3AE5B.toInt()
                canvas.drawCircle(x + cell / 2, y + cell / 2, cell * .12f, paint)
            }
            val piece = board.getPiece(square)
            val glyph = glyph(piece)
            if (glyph.isNotEmpty()) {
                val white = piece.name.startsWith("WHITE_")
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = cell * .76f
                paint.typeface = Typeface.create("serif", Typeface.BOLD)
                paint.style = Paint.Style.FILL
                paint.color = if (white) 0xFFFFFFFF.toInt() else 0xFF090A0C.toInt()
                paint.setShadowLayer(cell * .035f, 0f, cell * .035f, if (white) 0x99000000.toInt() else 0x99D3AE5B.toInt())
                canvas.drawText(glyph, x + cell / 2, y + cell * .77f, paint)
                paint.clearShadowLayer()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = cell * .012f
                paint.color = if (white) 0xFFB9B9B5.toInt() else 0xFFE4C77F.toInt()
                canvas.drawText(glyph, x + cell / 2, y + cell * .77f, paint)
                paint.style = Paint.Style.FILL
            }
            paint.textSize = cell * .12f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.LEFT
            paint.color = if ((row + col) % 2 == 0) dark else light
            if (col == 0) canvas.drawText(rank.toString(), x + cell * .04f, y + cell * .15f, paint)
            if (row == 7) canvas.drawText("abcdefgh"[file].toString(), x + cell * .86f, y + cell * .96f, paint)
        }
    }

    private fun glyph(piece: Piece): String = when (piece) {
        Piece.WHITE_KING -> "♔"; Piece.WHITE_QUEEN -> "♕"; Piece.WHITE_ROOK -> "♖"
        Piece.WHITE_BISHOP -> "♗"; Piece.WHITE_KNIGHT -> "♘"; Piece.WHITE_PAWN -> "♙"
        Piece.BLACK_KING -> "♚"; Piece.BLACK_QUEEN -> "♛"; Piece.BLACK_ROOK -> "♜"
        Piece.BLACK_BISHOP -> "♝"; Piece.BLACK_KNIGHT -> "♞"; Piece.BLACK_PAWN -> "♟"
        else -> ""
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP || gameFinished) return true
        val size = minOf(width, height).toFloat()
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        val col = ((event.x - left) / (size / 8f)).toInt()
        val row = ((event.y - top) / (size / 8f)).toInt()
        if (row !in 0..7 || col !in 0..7) return true
        val file = if (flipped) 7 - col else col
        val rank = if (flipped) row + 1 else 8 - row
        val square = Square.valueOf("${"abcdefgh"[file]}$rank".uppercase())
        val piece = board.getPiece(square)
        val whiteTurn = board.sideToMove.toString() == "WHITE"
        val ownPiece = piece != Piece.NONE && piece.pieceSide.toString() == if (whiteTurn) "WHITE" else "BLACK"

        if (selected == null) {
            if (ownPiece) {
                selected = square
                legalTargets = board.legalMoves().filter { it.from == square }.map { it.to }.toSet()
            }
        } else {
            val move = board.legalMoves().firstOrNull { it.from == selected && it.to == square }
            if (move != null) {
                val notation = move.toString()
                board.doMove(move)
                moves.add(notation)
                positions.add(board.fen)
                selected = null
                legalTargets = emptySet()
                when {
                    board.isMated -> {
                        gameFinished = true
                        val winner = if (board.sideToMove.toString() == "WHITE") "Black wins by checkmate" else "White wins by checkmate"
                        onStatus("Checkmate")
                        invalidate()
                        onGameOver(winner, moves.toList())
                        return true
                    }
                    board.isStaleMate -> {
                        gameFinished = true
                        onStatus("Draw by stalemate")
                        onGameOver("Draw by stalemate", moves.toList())
                    }
                    board.isDraw -> {
                        gameFinished = true
                        onStatus("Draw")
                        onGameOver("Draw", moves.toList())
                    }
                    else -> onStatus(board.sideToMove.toString() + " to move" + if (board.isKingAttacked) " · Check" else "")
                }
            } else if (ownPiece) {
                selected = square
                legalTargets = board.legalMoves().filter { it.from == square }.map { it.to }.toSet()
            } else {
                selected = null
                legalTargets = emptySet()
            }
        }
        invalidate()
        return true
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
