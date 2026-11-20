package com.metalchess

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.github.bhlangonijr.chesslib.Piece

/**
 * Procedurally rendered Staunton-inspired chessmen.
 * All geometry is drawn in a 100 x 100 coordinate space, so no bitmap
 * assets or network access are required.
 */
object ChessPieceRenderer {
    private val bitmaps = mutableMapOf<String, Bitmap>()

    fun load(context: Context) {
        val names = listOf("white_king", "black_king", "white_queen", "black_queen", "white_bishop", "black_bishop", "white_knight", "black_knight", "white_rook", "black_rook", "white_pawn", "black_pawn")
        names.forEach { name ->
            val id = context.resources.getIdentifier("piece_" + name, "drawable", context.packageName)
            if (id != 0) BitmapFactory.decodeResource(context.resources, id)?.let { bitmaps[name] = it }
        }
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val detail = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun draw(canvas: Canvas, piece: Piece, left: Float, top: Float, size: Float) {
        if (piece == Piece.NONE) return
        val assetName = piece.name.lowercase()
        val bitmap = bitmaps[assetName]
        if (bitmap != null) {
            val dst = RectF(left, top, left + size, top + size)
            canvas.drawBitmap(bitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            return
        }
        val white = piece.name.startsWith("WHITE_")
        val kind = piece.name.substringAfter('_')
        val save = canvas.save()
        canvas.translate(left, top)
        canvas.scale(size / 100f, size / 100f)

        val dark = if (white) 0xFFB9B9B4.toInt() else 0xFF090B0F.toInt()
        val mid = if (white) 0xFFF8F5E9.toInt() else 0xFF343A43.toInt()
        val bright = if (white) 0xFFFFFFFF.toInt() else 0xFF77716A.toInt()
        val accent = if (white) 0xFFD7D0BE.toInt() else 0xFFD6AD57.toInt()

        // Soft cast shadow and layered, slightly elliptical pedestal.
        fill.shader = null
        fill.color = 0x66000000
        canvas.drawOval(RectF(13f, 84f, 89f, 97f), fill)
        fill.color = dark
        canvas.drawOval(RectF(12f, 78f, 88f, 94f), fill)
        fill.shader = LinearGradient(0f, 78f, 0f, 94f, intArrayOf(bright, mid, dark), null, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(15f, 78f, 85f, 91f), fill)
        fill.shader = null
        edge.color = accent
        edge.strokeWidth = 1.6f
        canvas.drawOval(RectF(12f, 78f, 88f, 94f), edge)
        fill.color = mid
        canvas.drawOval(RectF(20f, 77f, 80f, 84f), fill)

        path.reset()
        when (kind) {
            "PAWN" -> pawn(path)
            "ROOK" -> rook(path)
            "KNIGHT" -> knight(path)
            "BISHOP" -> bishop(path)
            "QUEEN" -> queen(path)
            "KING" -> king(path)
            else -> pawn(path)
        }
        fill.shader = LinearGradient(15f, 18f, 85f, 78f, intArrayOf(bright, mid, dark), null, Shader.TileMode.CLAMP)
        fill.style = Paint.Style.FILL
        fill.setShadowLayer(3.2f, 1.5f, 3f, 0x99000000.toInt())
        canvas.drawPath(path, fill)
        fill.clearShadowLayer()
        fill.shader = null

        edge.color = accent
        edge.strokeWidth = 1.8f
        canvas.drawPath(path, edge)

        // Fine metallic highlights on the lit edge.
        edge.color = if (white) 0xCCFFFFFF.toInt() else 0x99E7C77F.toInt()
        edge.strokeWidth = 1.15f
        val highlight = Path(path)
        canvas.drawPath(highlight, edge)

        drawDetails(canvas, kind, white, accent, bright)
        canvas.restoreToCount(save)
    }

    private fun pawn(p: Path) {
        p.moveTo(29f, 78f); p.cubicTo(34f, 68f, 38f, 62f, 40f, 56f)
        p.cubicTo(31f, 49f, 33f, 39f, 42f, 34f)
        p.cubicTo(39f, 22f, 46f, 14f, 54f, 16f)
        p.cubicTo(67f, 19f, 69f, 34f, 58f, 41f)
        p.cubicTo(67f, 47f, 66f, 54f, 59f, 58f)
        p.cubicTo(62f, 66f, 67f, 72f, 72f, 78f)
        p.close()
        p.moveTo(38f, 65f); p.quadTo(50f, 70f, 63f, 65f)
    }

    private fun rook(p: Path) {
        p.moveTo(27f, 78f); p.lineTo(31f, 68f); p.lineTo(35f, 62f)
        p.lineTo(35f, 35f); p.lineTo(29f, 35f); p.lineTo(29f, 18f)
        p.lineTo(41f, 18f); p.lineTo(41f, 26f); p.lineTo(48f, 26f)
        p.lineTo(48f, 18f); p.lineTo(57f, 18f); p.lineTo(57f, 26f)
        p.lineTo(64f, 26f); p.lineTo(64f, 18f); p.lineTo(76f, 18f)
        p.lineTo(76f, 35f); p.lineTo(69f, 35f); p.lineTo(69f, 62f)
        p.lineTo(74f, 68f); p.lineTo(78f, 78f); p.close()
        p.moveTo(35f, 61f); p.lineTo(69f, 61f)
    }

    private fun knight(p: Path) {
        p.moveTo(27f, 78f); p.cubicTo(34f, 69f, 37f, 62f, 36f, 55f)
        p.cubicTo(27f, 45f, 30f, 31f, 43f, 24f)
        p.lineTo(38f, 13f); p.cubicTo(52f, 14f, 69f, 23f, 73f, 39f)
        p.cubicTo(78f, 50f, 68f, 60f, 66f, 68f)
        p.lineTo(76f, 78f); p.close()
        p.moveTo(40f, 27f); p.quadTo(50f, 32f, 62f, 45f)
        p.moveTo(47f, 19f); p.lineTo(56f, 34f)
    }

    private fun bishop(p: Path) {
        p.moveTo(28f, 78f); p.cubicTo(36f, 68f, 39f, 61f, 39f, 55f)
        p.cubicTo(29f, 43f, 35f, 28f, 48f, 23f)
        p.lineTo(44f, 16f); p.lineTo(50f, 10f); p.lineTo(56f, 16f)
        p.lineTo(52f, 23f); p.cubicTo(67f, 29f, 72f, 43f, 61f, 56f)
        p.cubicTo(62f, 64f, 68f, 71f, 75f, 78f); p.close()
        p.moveTo(45f, 30f); p.lineTo(58f, 48f)
    }

    private fun queen(p: Path) {
        p.moveTo(26f, 78f); p.cubicTo(32f, 68f, 36f, 61f, 36f, 55f)
        p.lineTo(30f, 32f); p.lineTo(42f, 43f); p.lineTo(50f, 25f)
        p.lineTo(58f, 43f); p.lineTo(72f, 31f); p.lineTo(65f, 56f)
        p.cubicTo(65f, 65f, 70f, 72f, 76f, 78f); p.close()
        p.addCircle(30f, 29f, 5f, Path.Direction.CW)
        p.addCircle(50f, 21f, 5f, Path.Direction.CW)
        p.addCircle(73f, 28f, 5f, Path.Direction.CW)
    }

    private fun king(p: Path) {
        p.moveTo(26f, 78f); p.cubicTo(33f, 68f, 37f, 61f, 37f, 55f)
        p.lineTo(42f, 43f); p.lineTo(46f, 35f); p.lineTo(46f, 26f)
        p.lineTo(39f, 26f); p.lineTo(39f, 19f); p.lineTo(46f, 19f)
        p.lineTo(46f, 10f); p.lineTo(54f, 10f); p.lineTo(54f, 19f)
        p.lineTo(61f, 19f); p.lineTo(61f, 26f); p.lineTo(54f, 26f)
        p.lineTo(54f, 35f); p.lineTo(59f, 43f); p.lineTo(64f, 55f)
        p.cubicTo(64f, 64f, 69f, 72f, 76f, 78f); p.close()
    }

    private fun drawDetails(canvas: Canvas, kind: String, white: Boolean, accent: Int, bright: Int) {
        detail.shader = null
        detail.style = Paint.Style.STROKE
        detail.strokeWidth = 1.5f
        detail.color = accent
        when (kind) {
            "KING" -> {
                canvas.drawLine(50f, 8f, 50f, 29f, detail)
                canvas.drawLine(40f, 19f, 60f, 19f, detail)
                canvas.drawLine(43f, 48f, 57f, 48f, detail)
            }
            "QUEEN" -> {
                canvas.drawLine(34f, 37f, 66f, 37f, detail)
                canvas.drawOval(RectF(39f, 48f, 61f, 54f), detail)
            }
            "BISHOP" -> {
                canvas.drawLine(48f, 17f, 55f, 28f, detail)
                canvas.drawOval(RectF(43f, 46f, 58f, 52f), detail)
            }
            "ROOK" -> canvas.drawLine(35f, 34f, 69f, 34f, detail)
            "KNIGHT" -> {
                detail.style = Paint.Style.FILL
                detail.color = if (white) 0xFF39332B.toInt() else 0xFFE5C77E.toInt()
                canvas.drawCircle(57f, 32f, 2.8f, detail)
            }
            "PAWN" -> canvas.drawOval(RectF(39f, 61f, 62f, 66f), detail)
        }
        detail.style = Paint.Style.FILL
        detail.color = bright
        canvas.drawOval(RectF(28f, 81f, 72f, 83f), detail)
    }
}
