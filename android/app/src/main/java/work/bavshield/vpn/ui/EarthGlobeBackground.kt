package work.bavshield.vpn.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

private const val LAT_NORTH = 74f
private const val LAT_SOUTH = -56f
private const val LON_WEST = -168f
private const val LON_EAST = 180f

private val Ocean = Color(0xFF1B1F36)
private val Land = Color(0xFF5A628C)

@Composable
fun EarthGlobeBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val landPath = remember(context) { loadWorldLandPath(context) }

    Canvas(modifier = modifier) {
        drawRect(Ocean)
        val geoAspect = (LON_EAST - LON_WEST) / (LAT_NORTH - LAT_SOUTH)
        val fit = min(size.width / geoAspect, size.height)
        val zoom = 3.024f
        val mapW = geoAspect * fit * zoom * 0.70f
        val mapH = fit * zoom
        val left = (size.width - mapW) / 2f
        val top = (size.height - mapH) / 2f

        clipRect(0f, 0f, size.width, size.height) {
            withTransform({
                translate(left = left, top = top)
                scale(scaleX = mapW, scaleY = mapH, pivot = Offset.Zero)
            }) {
                drawPath(landPath, Land)
            }
        }
    }
}

private fun loadWorldLandPath(context: Context): Path {
    val bytes = context.assets.open("world_land.bin").use { it.readBytes() }
    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val magic = ByteArray(4).also { bb.get(it) }
    require(magic.decodeToString() == "WMAP") { "Invalid world map asset" }
    require(bb.int == 1) { "Unsupported world map version" }

    val lonSpan = LON_EAST - LON_WEST
    val latSpan = LAT_NORTH - LAT_SOUTH
    val path = Path().apply { fillType = PathFillType.EvenOdd }
    val polygonCount = bb.int
    repeat(polygonCount) loop@{
        val ringCount = bb.int
        val rings = Array(ringCount) {
            val pointCount = bb.int
            FloatArray(pointCount * 2).also { pts ->
                repeat(pointCount) { i ->
                    pts[i * 2] = bb.float
                    pts[i * 2 + 1] = bb.float
                }
            }
        }
        var minLat = 90f
        var maxLat = -90f
        for (ring in rings) {
            var i = 1
            while (i < ring.size) {
                val lat = ring[i]
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                i += 2
            }
        }
        if (maxLat < LAT_SOUTH) return@loop
        if (minLat > LAT_NORTH) return@loop

        for (ring in rings) {
            if (ring.size < 6) continue
            var i = 0
            while (i < ring.size) {
                val lon = ring[i]
                val lat = ring[i + 1]
                val x = (lon - LON_WEST) / lonSpan
                val y = (LAT_NORTH - lat) / latSpan
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                i += 2
            }
            path.close()
        }
    }
    return path
}
