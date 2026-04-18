package com.bakertelekom.portugaltowers.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bakertelekom.portugaltowers.domain.Operator
import com.bakertelekom.portugaltowers.domain.Tower
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun TowerMap(
    towers: List<Tower>,
    onTowerSelected: (Tower) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val markerIconFactory = remember { TowerMarkerIconFactory(context) }
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(OSM_SOURCE)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            controller.setZoom(6.0)
            controller.setCenter(GeoPoint(39.5, -8.0))
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(towers) {
        mapView.overlays.clear()
        towers.forEach { tower ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(tower.latitude, tower.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = markerIconFactory.iconFor(tower.operators)
                    infoWindow = null
                    setOnMarkerClickListener { _, _ ->
                        onTowerSelected(tower)
                        true
                    }
                },
            )
        }
        mapView.invalidate()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val OSM_SOURCE = object : OnlineTileSourceBase(
    "OSM",
    0,
    19,
    256,
    ".png",
    arrayOf("https://tile.openstreetmap.org/"),
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + ".png"
}

private class TowerMarkerIconFactory(private val context: Context) {
    private val cache = mutableMapOf<String, BitmapDrawable>()

    fun iconFor(operators: Set<Operator>): BitmapDrawable {
        val normalized = operators.ifEmpty { setOf(Operator.Unknown) }
        val key = normalized.sortedBy { it.ordinal }.joinToString("_") { it.name }
        return cache.getOrPut(key) { createIcon(normalized) }
    }

    private fun createIcon(operators: Set<Operator>): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val size = (46 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val ordered = operators
            .filter { it != Operator.Unknown }
            .ifEmpty { listOf(Operator.Unknown) }
            .sortedBy { it.ordinal }

        when (ordered.size) {
            1 -> {
                paint.color = ordered[0].brandColor.toInt()
                canvas.drawArc(rect, 0f, 360f, true, paint)
            }
            2 -> {
                paint.color = ordered[0].brandColor.toInt()
                canvas.drawArc(rect, 180f, 180f, true, paint)
                paint.color = ordered[1].brandColor.toInt()
                canvas.drawArc(rect, 0f, 180f, true, paint)
            }
            3 -> {
                ordered.take(3).forEachIndexed { index, operator ->
                    paint.color = operator.brandColor.toInt()
                    canvas.drawArc(rect, 210f + (index * 120f), 120f, true, paint)
                }
            }
            else -> {
                ordered.take(4).forEachIndexed { index, operator ->
                    paint.color = operator.brandColor.toInt()
                    canvas.drawArc(rect, 180f + (index * 90f), 90f, true, paint)
                }
            }
        }

        val center = size / 2f
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(center, center, center * 0.72f, paint)
        paint.color = android.graphics.Color.parseColor("#263238")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f * density
        paint.strokeCap = Paint.Cap.ROUND
        val towerHalfWidth = 8f * density
        val top = 14f * density
        val bottom = 32f * density
        canvas.drawLine(center - towerHalfWidth, bottom, center - 2f * density, top, paint)
        canvas.drawLine(center + towerHalfWidth, bottom, center + 2f * density, top, paint)
        canvas.drawLine(center - 4f * density, 22f * density, center + 4f * density, 22f * density, paint)
        canvas.drawLine(center - towerHalfWidth, bottom, center + 4f * density, 22f * density, paint)
        canvas.drawLine(center + towerHalfWidth, bottom, center - 4f * density, 22f * density, paint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
