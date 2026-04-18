package com.bakertelekom.portugaltowers.ui.map

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bakertelekom.portugaltowers.domain.Operator
import com.bakertelekom.portugaltowers.domain.Tower
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import kotlin.math.roundToInt

@Composable
fun TowerMap(
    towers: List<Tower>,
    onTowerSelected: (Tower) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val markerIconFactory = remember { TowerMarkerIconFactory(context) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    val macroClusterer = remember { RadiusMarkerClusterer(context).apply { setRadius(280) } }
    val clusterer = remember {
        object : RadiusMarkerClusterer(context) {
            override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
                val operators = mutableSetOf<Operator>()
                for (index in 0 until cluster.size) {
                    val item = cluster.getItem(index)
                    @Suppress("UNCHECKED_CAST")
                    operators.addAll(item.relatedObject as? Set<Operator> ?: emptySet())
                }
                return Marker(mapView).apply {
                    position = GeoPoint(cluster.position.latitude, cluster.position.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = markerIconFactory.clusterIconFor(operators, cluster.size)
                    infoWindow = null
                    setOnMarkerClickListener { clickedMarker, map ->
                        map.controller.stopAnimation(false)
                        map.controller.setZoom((map.zoomLevelDouble + 1.6).coerceAtMost(19.0))
                        map.controller.setCenter(GeoPoint(clickedMarker.position.latitude, clickedMarker.position.longitude))
                        true
                    }
                }
            }
        }.apply {
            setRadius(250)
        }
    }
    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
            tileFileSystemCacheMaxBytes = 200L * 1024L * 1024L
            tileFileSystemCacheTrimBytes = 160L * 1024L * 1024L
        }
        MapView(context).apply {
            setTileSource(OSM_SOURCE)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            controller.setZoom(PORTUGAL_ZOOM)
            controller.setCenter(PORTUGAL_CENTER)
            setScrollableAreaLimitLatitude(43.2, 30.0, 0)

            val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), this)
            compassOverlay.enableCompass()
            overlays.add(compassOverlay)

            if (context.hasLocationPermission()) {
                val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                overlay.enableMyLocation()
                overlays.add(overlay)
                locationOverlay = overlay
            }
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
        mapView.installStaticOverlays(context) { locationOverlay = it }
        fun refreshMarkers() {
            val detailed = mapView.zoomLevelDouble >= DETAIL_MARKER_ZOOM
            mapView.overlays.remove(clusterer)
            mapView.overlays.remove(macroClusterer)
            clusterer.items.clear()
            macroClusterer.items.clear()
            if (detailed) {
                towers.asSequence()
                    .filter { mapView.boundingBox.contains(GeoPoint(it.latitude, it.longitude)) }
                    .take(MAX_VISIBLE_DETAIL_MARKERS)
                    .forEach { tower ->
                        clusterer.add(tower.toMarker(mapView, markerIconFactory, onTowerSelected))
                    }
                mapView.overlays.add(clusterer)
                clusterer.invalidate()
            } else {
                buildMacroClusters(towers).forEach { cluster ->
                    macroClusterer.add(cluster.toMarker(mapView, markerIconFactory))
                }
                mapView.overlays.add(macroClusterer)
                macroClusterer.invalidate()
            }
            mapView.invalidate()
        }

        refreshMarkers()
        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                refreshMarkers()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                refreshMarkers()
                return false
            }
        })
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )
        MapRoundButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            icon = Icons.Default.MyLocation,
            contentDescription = "Ir para a minha localizacao",
            onClick = {
                val location = locationOverlay?.myLocation
                if (location != null) {
                    mapView.controller.animateTo(location)
                    mapView.controller.setZoom(16.0)
                }
            },
        )
        MapRoundButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 74.dp, end = 16.dp),
            icon = Icons.Default.Explore,
            contentDescription = "Centrar Portugal",
            onClick = {
                mapView.mapOrientation = 0f
                mapView.controller.animateTo(PORTUGAL_CENTER)
                mapView.controller.setZoom(PORTUGAL_ZOOM)
            },
        )
    }
}

private fun MapView.installStaticOverlays(
    context: Context,
    onLocationOverlayReady: (MyLocationNewOverlay) -> Unit,
) {
    overlays.clear()
    overlays.add(CompassOverlay(context, InternalCompassOrientationProvider(context), this).apply { enableCompass() })
    if (context.hasLocationPermission()) {
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this).apply { enableMyLocation() }
        overlays.add(overlay)
        onLocationOverlayReady(overlay)
    }
}

private fun Tower.toMarker(
    mapView: MapView,
    markerIconFactory: TowerMarkerIconFactory,
    onTowerSelected: (Tower) -> Unit,
): Marker = Marker(mapView).apply {
    position = GeoPoint(latitude, longitude)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    icon = markerIconFactory.iconFor(operators)
    infoWindow = null
    relatedObject = operators
    setOnMarkerClickListener { _, _ ->
        onTowerSelected(this@toMarker)
        true
    }
}

private data class MacroTowerCluster(
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val operators: Set<Operator>,
)

private fun MacroTowerCluster.toMarker(
    mapView: MapView,
    markerIconFactory: TowerMarkerIconFactory,
): Marker = Marker(mapView).apply {
    position = GeoPoint(latitude, longitude)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    icon = markerIconFactory.clusterIconFor(operators, count)
    infoWindow = null
    relatedObject = operators
    setOnMarkerClickListener { clickedMarker, map ->
        map.controller.stopAnimation(false)
        map.controller.setZoom((map.zoomLevelDouble + 1.8).coerceAtMost(19.0))
        map.controller.setCenter(GeoPoint(clickedMarker.position.latitude, clickedMarker.position.longitude))
        true
    }
}

private fun buildMacroClusters(towers: List<Tower>): List<MacroTowerCluster> =
    towers.groupBy { tower ->
        val latBucket = (tower.latitude * 4).roundToInt() / 4.0
        val lonBucket = (tower.longitude * 4).roundToInt() / 4.0
        latBucket to lonBucket
    }.map { (bucket, grouped) ->
        MacroTowerCluster(
            latitude = bucket.first,
            longitude = bucket.second,
            count = grouped.size,
            operators = grouped.flatMap { it.operators }.toSet(),
        )
    }

@Composable
private fun MapRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

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

private val PORTUGAL_CENTER = GeoPoint(39.5, -8.0)
private const val PORTUGAL_ZOOM = 6.0
private const val DETAIL_MARKER_ZOOM = 10.0
private const val MAX_VISIBLE_DETAIL_MARKERS = 1800

private class TowerMarkerIconFactory(private val context: Context) {
    private val cache = mutableMapOf<String, BitmapDrawable>()
    private val clusterCache = mutableMapOf<String, BitmapDrawable>()

    fun iconFor(operators: Set<Operator>): BitmapDrawable {
        val normalized = operators.ifEmpty { setOf(Operator.Unknown) }
        val key = normalized.sortedBy { it.ordinal }.joinToString("_") { it.name }
        return cache.getOrPut(key) { createIcon(normalized) }
    }

    fun clusterIconFor(operators: Set<Operator>, count: Int): BitmapDrawable {
        val normalized = operators.ifEmpty { setOf(Operator.Unknown) }
        val key = normalized.sortedBy { it.ordinal }.joinToString("_") { it.name } + "_$count"
        return clusterCache.getOrPut(key) { createClusterIcon(normalized, count) }
    }

    private fun createIcon(operators: Set<Operator>): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val size = (105 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = size / 230f
        canvas.scale(scale, scale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val center = 115f
        val pieRadius = 45f
        val rect = RectF(center - pieRadius, center - pieRadius, center + pieRadius, center + pieRadius)
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

        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(center, center, pieRadius * 0.40f, paint)
        paint.color = android.graphics.Color.parseColor("#EBEBEB")
        canvas.drawCircle(center, center, pieRadius * 0.80f, paint)

        paint.color = android.graphics.Color.parseColor("#263238")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        val top = center - 20f
        val bottom = center + 40f
        val towerHalfWidth = 21f
        canvas.drawLine(center - towerHalfWidth, bottom, center - 7f, top, paint)
        canvas.drawLine(center + towerHalfWidth, bottom, center + 7f, top, paint)
        canvas.drawLine(center - 7f, top, center + 7f, top, paint)
        canvas.drawLine(center - 14f, center + 16f, center + 14f, center + 16f, paint)
        canvas.drawLine(center - towerHalfWidth, bottom, center + 14f, center + 16f, paint)
        canvas.drawLine(center + towerHalfWidth, bottom, center - 14f, center + 16f, paint)

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun createClusterIcon(operators: Set<Operator>, count: Int): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val size = (45 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val ordered = operators
            .filter { it != Operator.Unknown }
            .ifEmpty { listOf(Operator.Unknown) }
            .sortedBy { it.ordinal }
            .take(4)
        paint.style = Paint.Style.FILL
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
                paint.color = ordered[0].brandColor.toInt()
                canvas.drawArc(rect, 210f, 120f, true, paint)
                paint.color = ordered[1].brandColor.toInt()
                canvas.drawArc(rect, 330f, 120f, true, paint)
                paint.color = ordered[2].brandColor.toInt()
                canvas.drawArc(rect, 90f, 120f, true, paint)
            }
            else -> {
                val angles = listOf(180f, 270f, 0f, 90f)
                ordered.take(4).forEachIndexed { index, operator ->
                    paint.color = operator.brandColor.toInt()
                    canvas.drawArc(rect, angles[index], 90f, true, paint)
                }
            }
        }

        val center = size / 2f
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(center, center, center * 0.80f, paint)

        paint.color = android.graphics.Color.parseColor("#37474F")
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = when {
            count.toString().length <= 2 -> size * 0.40f
            count.toString().length == 3 -> size * 0.32f
            count.toString().length == 4 -> size * 0.25f
            else -> size * 0.20f
        }
        val textOffset = (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(count.toString(), center, center - textOffset, paint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
