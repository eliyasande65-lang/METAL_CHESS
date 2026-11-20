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
import com.github.bhlangonijr.chesslib.move.Move

class ChessBoardView(
    context: Context,
    private val onStatus: (String) -> Unit,
    private val onGameOver: (String, List<String>) -> Unit
) : View(context) {
    private val board = Board()
    init { ChessPieceRenderer.load(context) }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var selected: Square? = null
    private var flipped = false
    private var legalTargets: Set<Square> = emptySet()
    private val positions = mutableListOf(board.fen)
    private val moves = mutableListOf<String>()
    private var gameFinished = false
    private var engine: UciEngine? = null
    private var engineDifficulty = 0
    private var engineEnabled = false
    private var engineThinking = false
    private var gameToken = 0
    private val humanSide = "WHITE"
    private val light = 0xFFF0EEE8.toInt()
    private val dark = 0xFF24272D.toInt()
    private val gold = 0xFFD3AE5B.toInt()

    fun startEngineGame(stockfish: UciEngine, difficulty: Int) {
        gameToken++
        engine = stockfish
        engineDifficulty = difficulty.coerceIn(0, 8)
        engineEnabled = true
        engineThinking = false
        reset()
        onStatus("YOU ARE WHITE · STOCKFISH ${UciEngine.labels[engineDifficulty].uppercase()}")
    }

    fun startLocalGame() {
        gameToken++
        engineEnabled = false
        engineThinking = false
        engine = null
        reset()
    }

    fun reset() {
        gameToken++
        board.loadFromFen(Board().fen)
        positions.clear()
        positions.add(board.fen)
        moves.clear()
        selected = null
        legalTargets = emptySet()
        gameFinished = false
        engineThinking = false
        invalidate()
        onStatus(if (engineEnabled) "YOUR MOVE · STOCKFISH ${UciEngine.labels[engineDifficulty].uppercase()}" else "White to move")
    }

    fun flip() { flipped = !flipped; invalidate() }

    fun undo() {
        if (positions.size <= 1 || engineThinking) return
        val plies = if (engineEnabled) minOf(2, positions.size - 1) else 1
        repeat(plies) {
            if (positions.size > 1) positions.removeAt(positions.lastIndex)
            if (moves.isNotEmpty()) moves.removeAt(moves.lastIndex)
        }
        board.loadFromFen(positions.last())
        selected = null
        legalTargets = emptySet()
        gameFinished = false
        gameToken++
        onStatus(if (engineEnabled) "YOUR MOVE" else board.sideToMove.toString() + " to move")
        invalidate()
    }

    fun positionHistory(): List<String> = positions.toList()
    fun moveHistory(): List<String> = moves.toList()

    fun loadPosition(fen: String) {
        try {
            gameToken++
            board.loadFromFen(fen)
            positions.clear()
            positions.add(fen)
            moves.clear()
            selected = null
            legalTargets = emptySet()
            gameFinished = false
            engineThinking = false
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
        paint.shader = android.graphics.LinearGradient(left, top, left + size, top + size,
            intArrayOf(0xFFFFE6A0.toInt(), gold, 0xFF75521F.toInt(), 0xFFE8C879.toInt()),
            null, android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRoundRect(left - dp(5).toFloat(), top - dp(5).toFloat(),
            left + size + dp(5), top + size + dp(5), dp(3).toFloat(), dp(3).toFloat(), paint)
        paint.shader = null

        for (row in 0..7) for (col in 0..7) {
            val file = if (flipped) 7 - col else col
            val rank = if (flipped) row + 1 else 8 - row
            val square = Square.valueOf("${"abcdefgh"[file]}$rank".uppercase())
            val x = left + col * cell
            val y = top + row * cell
            // Rich, subtly textured graphite and warm ivory squares.
            val isLight = (row + col) % 2 == 0
            paint.shader = null
            paint.color = if (isLight) 0xFFD8D1C2.toInt() else 0xFF343943.toInt()
            canvas.drawRect(x, y, x + cell, y + cell, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = cell * .012f
            paint.color = if (isLight) 0x22FFFFFF else 0x22000000
            canvas.drawRect(x + cell * .035f, y + cell * .035f, x + cell * .965f, y + cell * .965f, paint)
            paint.style = Paint.Style.FILL

            if (selected == square) {
                paint.color = 0x88D3AE5B.toInt()
                canvas.drawRect(x, y, x + cell, y + cell, paint)
                paint.color = 0xFFE7C77F.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = cell * .045f
                canvas.drawRect(x + cell * .07f, y + cell * .07f, x + cell * .93f, y + cell * .93f, paint)
                paint.style = Paint.Style.FILL
            }
            if (square in legalTargets) {
                paint.color = 0xBBD3AE5B.toInt()
                canvas.drawCircle(x + cell / 2, y + cell / 2, cell * .105f, paint)
                paint.color = 0x55FFFFFF
                canvas.drawCircle(x + cell / 2, y + cell / 2, cell * .145f, paint)
            }

            val piece = board.getPiece(square)
            if (piece != Piece.NONE) {
                val inset = cell * .045f
                ChessPieceRenderer.draw(canvas, piece, x + inset, y + inset, cell - inset * 2f)
            }
            paint.textSize = cell * .12f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.LEFT
            paint.color = if ((row + col) % 2 == 0) dark else light
            if (col == 0) canvas.drawText(rank.toString(), x + cell * .04f, y + cell * .15f, paint)
            if (row == 7) canvas.drawText("abcdefgh"[file].toString(), x + cell * .86f, y + cell * .96f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP || gameFinished || engineThinking) return true
        if (engineEnabled && board.sideToMove.toString() != humanSide) return true
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
                val wasCapture = board.getPiece(move.to) != Piece.NONE
                board.doMove(move)
                SoundEffects.play(context, wasCapture, board.isKingAttacked)
                moves.add(move.toString())
                positions.add(board.fen)
                selected = null
                legalTargets = emptySet()
                if (finishIfNeeded()) return true
                invalidate()
                if (engineEnabled) requestEngineMove()
                else onStatus(board.sideToMove.toString() + " to move" + if (board.isKingAttacked) " · Check" else "")
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

    private fun requestEngineMove() {
        val activeEngine = engine ?: return
        val token = gameToken
        val positionFen = board.fen
        engineThinking = true
        onStatus("STOCKFISH IS THINKING · ${UciEngine.labels[engineDifficulty].uppercase()}")
        Thread {
            try {
                val uci = activeEngine.bestMove(positionFen, engineDifficulty)
                post {
                    if (token != gameToken || !engineEnabled || gameFinished) return@post
                    engineThinking = false
                    val move = if (uci.isNotEmpty()) try { Move(uci, board.sideToMove) } catch (_: Exception) { null } else null
                    if (move == null || !board.legalMoves().contains(move)) {
                        onStatus("ENGINE MOVE ERROR · START A NEW GAME")
                        invalidate()
                        return@post
                    }
                    val wasCapture = board.getPiece(move.to) != Piece.NONE
                    board.doMove(move)
                    SoundEffects.play(context, wasCapture, board.isKingAttacked)
                    moves.add(move.toString())
                    positions.add(board.fen)
                    selected = null
                    legalTargets = emptySet()
                    if (!finishIfNeeded()) {
                        onStatus("YOUR MOVE" + if (board.isKingAttacked) " · CHECK" else "")
                    }
                    invalidate()
                }
            } catch (e: Exception) {
                post {
                    if (token == gameToken) {
                        engineThinking = false
                        onStatus("STOCKFISH ERROR · ${e.message ?: "ENGINE UNAVAILABLE"}")
                        invalidate()
                    }
                }
            }
        }.start()
    }

    private fun finishIfNeeded(): Boolean {
        val result = when {
            board.isMated -> if (board.sideToMove.toString() == "WHITE") "Black wins by checkmate" else "White wins by checkmate"
            board.isStaleMate -> "Draw by stalemate"
            board.isDraw -> "Draw"
            else -> null
        } ?: return false
        gameFinished = true
        engineThinking = false
        onStatus(if (board.isMated) "Checkmate" else result)
        onGameOver(result, moves.toList())
        return true
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
