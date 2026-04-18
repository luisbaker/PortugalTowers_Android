package com.bakertelekom.portugaltowers.ui.map

import android.graphics.PointF
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bakertelekom.portugaltowers.domain.Operator
import com.bakertelekom.portugaltowers.domain.Tower
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.match
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

@Composable
fun TowerMap(
    towers: List<Tower>,
    onTowerSelected: (Tower) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val towerLookup = remember(towers) { towers.associateBy { it.mapFeatureId } }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(Bundle())
            getMapAsync { map ->
                mapLibreMap = map
                map.uiSettings.apply {
                    isCompassEnabled = true
                    isLogoEnabled = false
                    isAttributionEnabled = true
                }
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(39.5, -8.0))
                    .zoom(5.7)
                    .build()
                map.setStyle(
                    Style.Builder().fromUri(MAP_STYLE_URL),
                ) { style ->
                    ensureTowerLayer(style)
                    updateTowerSource(style, towers)
                }
                map.addOnMapClickListener { latLng ->
                    val point = map.projection.toScreenLocation(latLng)
                    val features = map.queryRenderedFeatures(point, TOWER_LAYER_ID)
                    val featureId = features.firstOrNull()?.getStringProperty(PROPERTY_ID)
                    val tower = featureId?.let { towerLookup[it] }
                    if (tower != null) {
                        onTowerSelected(tower)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(towers, mapLibreMap) {
        mapLibreMap?.getStyle { style ->
            ensureTowerLayer(style)
            updateTowerSource(style, towers)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun ensureTowerLayer(style: Style) {
    if (style.getSource(TOWER_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(TOWER_SOURCE_ID, FeatureCollection.fromFeatures(emptyList())))
    }
    if (style.getLayer(TOWER_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(TOWER_LAYER_ID, TOWER_SOURCE_ID).withProperties(
                circleRadius(7f),
                circleStrokeWidth(1.8f),
                circleStrokeColor("#FFFFFF"),
                circleColor(
                    match(
                        get(PROPERTY_OPERATOR),
                        literal(Operator.Meo.name), literal("#005BAC"),
                        literal(Operator.Nos.name), literal("#1A1A1A"),
                        literal(Operator.Vodafone.name), literal("#E60000"),
                        literal(Operator.Digi.name), literal("#00AA44"),
                        literal("#777777"),
                    ),
                ),
            ),
        )
    }
}

private fun updateTowerSource(style: Style, towers: List<Tower>) {
    val source = style.getSourceAs<GeoJsonSource>(TOWER_SOURCE_ID) ?: return
    source.setGeoJson(
        FeatureCollection.fromFeatures(
            towers.map { tower ->
                Feature.fromGeometry(
                    Point.fromLngLat(tower.longitude, tower.latitude),
                ).apply {
                    addStringProperty(PROPERTY_ID, tower.mapFeatureId)
                    addStringProperty(PROPERTY_OPERATOR, tower.primaryOperator.name)
                }
            },
        ),
    )
}

private val Tower.mapFeatureId: String
    get() = "$id:$latitude:$longitude"

private const val MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json"
private const val TOWER_SOURCE_ID = "portugal-towers-source"
private const val TOWER_LAYER_ID = "portugal-towers-layer"
private const val PROPERTY_ID = "tower_id"
private const val PROPERTY_OPERATOR = "operator"
