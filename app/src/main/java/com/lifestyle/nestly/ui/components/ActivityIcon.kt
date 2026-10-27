package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.data.prefs.CardKind
import com.lifestyle.nestly.ui.theme.blobColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class IconKind(val card: CardKind) {
    BOTTLE(CardKind.FEED),
    BREAST(CardKind.FEED),
    SOLIDS(CardKind.FEED),
    COMBO(CardKind.FEED),
    PUMP(CardKind.PUMP),
    DIAPER(CardKind.DIAPER),
    SLEEP(CardKind.SLEEP),
    ROUTINE(CardKind.ROUTINE),
    BABY_FIRST(CardKind.BABY_FIRSTS),
    WEIGHT(CardKind.GROWTH),
    HEIGHT(CardKind.GROWTH),
    HEAD(CardKind.GROWTH),
    MILESTONE(CardKind.GROWTH),
    MEDICAL(CardKind.HEALTH),
    VACCINE(CardKind.HEALTH),
}

/** Hand-drawn style icon: a colored blob behind a white line drawing. */
@Composable
fun ActivityIcon(
    kind: IconKind,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    blob: Color = kind.card.blobColor,
    line: Color = Color.White,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawBlob(blob, center = Offset(s * 0.56f, s * 0.6f), radius = s * 0.3f, seed = kind.ordinal)
        val stroke = Stroke(width = s * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            IconKind.BOTTLE -> drawBottle(s, line, stroke)
            IconKind.BREAST -> drawBreast(s, line, stroke)
            IconKind.SOLIDS -> drawSpoon(s, line, stroke)
            IconKind.COMBO -> {
                translate(-s * 0.18f, s * 0.05f) { scaleDraw(0.7f, s) { drawBreast(s, line, stroke) } }
                translate(s * 0.2f, -s * 0.02f) { scaleDraw(0.75f, s) { drawBottle(s, line, stroke) } }
            }
            IconKind.PUMP -> drawPump(s, line, stroke)
            IconKind.DIAPER -> drawDiaper(s, line, stroke)
            IconKind.SLEEP -> drawMoon(s, line, stroke)
            IconKind.ROUTINE -> drawBook(s, line, stroke)
            IconKind.BABY_FIRST -> drawPopper(s, line, stroke)
            IconKind.WEIGHT -> drawScale(s, line, stroke)
            IconKind.HEIGHT -> drawRuler(s, line, stroke)
            IconKind.HEAD -> drawHead(s, line, stroke)
            IconKind.MILESTONE -> drawStar(s, Offset(s * 0.5f, s * 0.5f), s * 0.32f, line, stroke)
            IconKind.MEDICAL -> drawMedicine(s, line, stroke)
            IconKind.VACCINE -> drawSyringe(s, line, stroke)
        }
    }
}

private inline fun DrawScope.scaleDraw(factor: Float, s: Float, crossinline block: DrawScope.() -> Unit) =
    scale(factor, Offset(s / 2, s / 2)) { block() }

private fun DrawScope.drawBlob(color: Color, center: Offset, radius: Float, seed: Int) {
    val points = 7
    val path = Path()
    val pts = (0 until points).map { i ->
        val angle = 2 * PI * i / points + seed * 0.7
        val wobble = 0.82f + 0.22f * sin(i * 2.3 + seed).toFloat()
        Offset(center.x + (radius * wobble * cos(angle)).toFloat(), center.y + (radius * wobble * sin(angle)).toFloat())
    }
    val mid = { a: Offset, b: Offset -> Offset((a.x + b.x) / 2, (a.y + b.y) / 2) }
    path.moveTo(mid(pts.last(), pts.first()).x, mid(pts.last(), pts.first()).y)
    pts.forEachIndexed { i, p ->
        val next = pts[(i + 1) % points]
        val m = mid(p, next)
        path.quadraticTo(p.x, p.y, m.x, m.y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawBottle(s: Float, c: Color, st: Stroke) = rotate(-25f) {
    val w = s * 0.32f
    val left = s * 0.5f - w / 2
    drawRoundRect(c, Offset(left, s * 0.38f), Size(w, s * 0.5f), CornerRadius(s * 0.08f), style = st)
    drawRoundRect(c, Offset(left - s * 0.02f, s * 0.29f), Size(w + s * 0.04f, s * 0.09f), CornerRadius(s * 0.02f), style = st)
    val nipple = Path().apply {
        moveTo(s * 0.42f, s * 0.29f)
        cubicTo(s * 0.42f, s * 0.12f, s * 0.58f, s * 0.12f, s * 0.58f, s * 0.29f)
    }
    drawPath(nipple, c, style = st)
    drawLine(c, Offset(left, s * 0.55f), Offset(left + w * 0.35f, s * 0.55f), st.width, StrokeCap.Round)
    drawLine(c, Offset(left, s * 0.68f), Offset(left + w * 0.35f, s * 0.68f), st.width, StrokeCap.Round)
}

private fun DrawScope.drawBreast(s: Float, c: Color, st: Stroke) {
    drawArc(c, 200f, 290f, false, Offset(s * 0.16f, s * 0.18f), Size(s * 0.68f, s * 0.68f), style = st)
    drawCircle(c, s * 0.07f, Offset(s * 0.5f, s * 0.52f))
}

private fun DrawScope.drawSpoon(s: Float, c: Color, st: Stroke) = rotate(-30f) {
    drawOval(c, Offset(s * 0.36f, s * 0.1f), Size(s * 0.28f, s * 0.36f), style = st)
    drawLine(c, Offset(s * 0.5f, s * 0.46f), Offset(s * 0.5f, s * 0.9f), st.width * 1.2f, StrokeCap.Round)
}

private fun DrawScope.drawPump(s: Float, c: Color, st: Stroke) {
    val tri = Path().apply {
        moveTo(s * 0.42f, s * 0.12f)
        lineTo(s * 0.12f, s * 0.72f)
        lineTo(s * 0.72f, s * 0.72f)
        close()
    }
    drawPath(tri, c, style = st)
    rotate(-35f, Offset(s * 0.68f, s * 0.62f)) {
        drawRoundRect(c, Offset(s * 0.58f, s * 0.5f), Size(s * 0.22f, s * 0.3f), CornerRadius(s * 0.04f), style = st)
        drawLine(c, Offset(s * 0.58f, s * 0.6f), Offset(s * 0.8f, s * 0.6f), st.width, StrokeCap.Round)
    }
}

private fun DrawScope.drawDiaper(s: Float, c: Color, st: Stroke) {
    val path = Path().apply {
        moveTo(s * 0.12f, s * 0.28f)
        lineTo(s * 0.88f, s * 0.28f)
        lineTo(s * 0.84f, s * 0.48f)
        cubicTo(s * 0.72f, s * 0.52f, s * 0.64f, s * 0.62f, s * 0.62f, s * 0.8f)
        lineTo(s * 0.38f, s * 0.8f)
        cubicTo(s * 0.36f, s * 0.62f, s * 0.28f, s * 0.52f, s * 0.16f, s * 0.48f)
        close()
    }
    drawPath(path, c, style = st)
    drawLine(c, Offset(s * 0.14f, s * 0.38f), Offset(s * 0.86f, s * 0.38f), st.width * 0.7f, StrokeCap.Round)
}

private fun DrawScope.drawMoon(s: Float, c: Color, st: Stroke) {
    val moon = Path().apply {
        moveTo(s * 0.52f, s * 0.14f)
        cubicTo(s * 0.12f, s * 0.2f, s * 0.12f, s * 0.86f, s * 0.6f, s * 0.86f)
        cubicTo(s * 0.74f, s * 0.86f, s * 0.84f, s * 0.78f, s * 0.88f, s * 0.68f)
        cubicTo(s * 0.5f, s * 0.74f, s * 0.36f, s * 0.36f, s * 0.52f, s * 0.14f)
        close()
    }
    drawPath(moon, c, style = st)
    drawStar(s, Offset(s * 0.74f, s * 0.3f), s * 0.1f, c, Stroke(st.width * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawStar(s: Float, center: Offset, r: Float, c: Color, st: Stroke) {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val angle = -PI / 2 + i * PI / 5
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, c, style = st)
}

private fun DrawScope.drawBook(s: Float, c: Color, st: Stroke) {
    drawRoundRect(c, Offset(s * 0.2f, s * 0.14f), Size(s * 0.6f, s * 0.72f), CornerRadius(s * 0.04f), style = st)
    drawLine(c, Offset(s * 0.3f, s * 0.14f), Offset(s * 0.3f, s * 0.86f), st.width, StrokeCap.Round)
    val fold = Path().apply {
        moveTo(s * 0.62f, s * 0.86f)
        lineTo(s * 0.8f, s * 0.68f)
    }
    drawPath(fold, c, style = st)
}

private fun DrawScope.drawPopper(s: Float, c: Color, st: Stroke) {
    val cone = Path().apply {
        moveTo(s * 0.16f, s * 0.86f)
        lineTo(s * 0.34f, s * 0.38f)
        lineTo(s * 0.62f, s * 0.66f)
        close()
    }
    drawPath(cone, c, style = st)
    drawLine(c, Offset(s * 0.5f, s * 0.32f), Offset(s * 0.6f, s * 0.14f), st.width, StrokeCap.Round)
    drawLine(c, Offset(s * 0.66f, s * 0.48f), Offset(s * 0.86f, s * 0.4f), st.width, StrokeCap.Round)
    drawCircle(c, s * 0.035f, Offset(s * 0.76f, s * 0.2f))
    drawCircle(c, s * 0.035f, Offset(s * 0.84f, s * 0.62f))
}

private fun DrawScope.drawScale(s: Float, c: Color, st: Stroke) {
    drawRoundRect(c, Offset(s * 0.16f, s * 0.2f), Size(s * 0.68f, s * 0.64f), CornerRadius(s * 0.1f), style = st)
    drawArc(c, 200f, 140f, false, Offset(s * 0.32f, s * 0.3f), Size(s * 0.36f, s * 0.3f), style = st)
    drawLine(c, Offset(s * 0.5f, s * 0.45f), Offset(s * 0.58f, s * 0.34f), st.width, StrokeCap.Round)
}

private fun DrawScope.drawRuler(s: Float, c: Color, st: Stroke) = rotate(-40f) {
    drawRoundRect(c, Offset(s * 0.36f, s * 0.08f), Size(s * 0.28f, s * 0.84f), CornerRadius(s * 0.03f), style = st)
    for (i in 1..5) {
        val y = s * (0.08f + i * 0.14f)
        val len = if (i % 2 == 0) 0.14f else 0.08f
        drawLine(c, Offset(s * 0.36f, y), Offset(s * (0.36f + len), y), st.width * 0.8f, StrokeCap.Round)
    }
}

private fun DrawScope.drawHead(s: Float, c: Color, st: Stroke) {
    drawCircle(c, s * 0.3f, Offset(s * 0.5f, s * 0.5f), style = st)
    drawArc(c, 20f, 140f, false, Offset(s * 0.26f, s * 0.26f), Size(s * 0.48f, s * 0.48f), style = st)
    drawLine(c, Offset(s * 0.14f, s * 0.42f), Offset(s * 0.86f, s * 0.42f), st.width * 0.8f, StrokeCap.Round)
}

private fun DrawScope.drawMedicine(s: Float, c: Color, st: Stroke) {
    drawRoundRect(c, Offset(s * 0.24f, s * 0.3f), Size(s * 0.44f, s * 0.56f), CornerRadius(s * 0.06f), style = st)
    drawRoundRect(c, Offset(s * 0.28f, s * 0.14f), Size(s * 0.36f, s * 0.16f), CornerRadius(s * 0.03f), style = st)
    drawLine(c, Offset(s * 0.46f, s * 0.46f), Offset(s * 0.46f, s * 0.7f), st.width, StrokeCap.Round)
    drawLine(c, Offset(s * 0.34f, s * 0.58f), Offset(s * 0.58f, s * 0.58f), st.width, StrokeCap.Round)
    drawLine(c, Offset(s * 0.8f, s * 0.2f), Offset(s * 0.8f, s * 0.86f), st.width, StrokeCap.Round)
}

private fun DrawScope.drawSyringe(s: Float, c: Color, st: Stroke) = rotate(-45f) {
    drawRoundRect(c, Offset(s * 0.4f, s * 0.26f), Size(s * 0.2f, s * 0.44f), CornerRadius(s * 0.03f), style = st)
    drawLine(c, Offset(s * 0.5f, s * 0.7f), Offset(s * 0.5f, s * 0.92f), st.width, StrokeCap.Round)
    drawLine(c, Offset(s * 0.5f, s * 0.26f), Offset(s * 0.5f, s * 0.1f), st.width, StrokeCap.Round)
    drawLine(c, Offset(s * 0.38f, s * 0.1f), Offset(s * 0.62f, s * 0.1f), st.width, StrokeCap.Round)
    drawLine(c, Offset(s * 0.4f, s * 0.42f), Offset(s * 0.5f, s * 0.42f), st.width * 0.8f, StrokeCap.Round)
    drawLine(c, Offset(s * 0.4f, s * 0.54f), Offset(s * 0.5f, s * 0.54f), st.width * 0.8f, StrokeCap.Round)
}
