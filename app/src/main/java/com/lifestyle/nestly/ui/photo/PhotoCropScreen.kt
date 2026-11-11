package com.lifestyle.nestly.ui.photo

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlin.math.max
import kotlin.math.roundToInt

/** Square crop. Drag and pinch to frame the photo; the circle is how the avatar will look. */
@Composable
fun PhotoCropScreen(
    source: Bitmap,
    onCancel: () -> Unit,
    onUse: (viewSize: Float, scale: Float, offsetX: Float, offsetY: Float) -> Unit,
) {
    val image = remember(source) { source.asImageBitmap() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableFloatStateOf(0f) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel", color = Color.White, style = NestlyType.Button) }
            Spacer(Modifier.weight(1f))
            Text("Crop", color = Color.White, style = NestlyType.SheetTitle)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onUse(viewSize, scale, offset.x, offset.y) }) {
                Text("Use Photo", color = Color(0xFFE08B6E), style = NestlyType.Button)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onSizeChanged { viewSize = it.width.toFloat() }
                    .pointerInput(source) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = nextScale
                            offset = clampOffset(offset + pan, nextScale, viewSize, source)
                        }
                    },
            ) {
                val side = size.minDimension
                val cover = max(side / source.width, side / source.height)
                val drawW = source.width * cover * scale
                val drawH = source.height * cover * scale
                val left = side / 2f + offset.x - drawW / 2f
                val top = side / 2f + offset.y - drawH / 2f
                drawImage(
                    image,
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
                )
                val dim = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addOval(Rect(0f, 0f, size.width, size.height))
                    fillType = PathFillType.EvenOdd
                }
                drawPath(dim, Color.Black.copy(alpha = 0.55f))
                drawOval(Color.White.copy(alpha = 0.9f), style = Stroke(width = 2.dp.toPx()))
            }
        }
        Text(
            "Drag and pinch to frame the photo.",
            color = Color.White.copy(alpha = 0.8f),
            style = NestlyType.Small,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp),
        )
    }
}

private fun clampOffset(offset: Offset, scale: Float, viewSize: Float, source: Bitmap): Offset {
    if (viewSize <= 0f) return Offset.Zero
    val cover = max(viewSize / source.width, viewSize / source.height)
    val maxX = ((source.width * cover * scale - viewSize) / 2f).coerceAtLeast(0f)
    val maxY = ((source.height * cover * scale - viewSize) / 2f).coerceAtLeast(0f)
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}
