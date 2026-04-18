package com.bakertelekom.portugaltowers

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Tower(
    val id: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val operators: Set<String>,
    val bands4g: Set<String>,
    val bands5g: Set<String>,
)

object TowerRepository {
    private val executor = Executors.newSingleThreadExecutor()
    private var cached: List<Tower>? = null

    fun load(context: Context, callback: (List<Tower>) -> Unit) {
        cached?.let {
            callback(it)
            return
        }
        executor.execute {
            val towers = parseCsv(context)
            Handler(Looper.getMainLooper()).post {
                cached = towers
                callback(towers)
            }
        }
    }

    private fun parseCsv(context: Context): List<Tower> {
        val grouped = linkedMapOf<String, MutableTower>()
        context.assets.open("portugal_telecom_towers.csv").use { stream ->
            BufferedReader(InputStreamReader(stream)).useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split(';')
                    if (parts.size < 10) return@forEach
                    val id = parts[0].trim()
                    val lat = parts[2].toDoubleOrNull() ?: return@forEach
                    val lon = parts[3].toDoubleOrNull() ?: return@forEach
                    val key = "$id:$lat:$lon"
                    val tower = grouped.getOrPut(key) {
                        MutableTower(id, parts[1].trim().ifBlank { id }, lat, lon)
                    }
                    tower.operators.add(operatorFromPlmn(parts[9].trim()))
                    tower.bands4g.addAll(splitBands(parts.getOrNull(5).orEmpty()))
                    tower.bands5g.addAll(splitBands(parts.getOrNull(7).orEmpty()))
                }
            }
        }
        return grouped.values.map {
            Tower(
                it.id,
                it.address,
                it.lat,
                it.lon,
                it.operators.filter { op -> op.isNotBlank() }.toSortedSet(),
                it.bands4g.filter { band -> band.isNotBlank() }.toSortedSet(),
                it.bands5g.filter { band -> band.isNotBlank() }.toSortedSet(),
            )
        }
    }

    private fun splitBands(value: String) = value.split(',').map { it.trim() }.filter { it.isNotBlank() }

    private fun operatorFromPlmn(plmn: String) = when (plmn) {
        "26801" -> "Vodafone"
        "26803" -> "NOS"
        "26806" -> "MEO"
        "26811" -> "Digi"
        else -> if (plmn.isBlank()) "Desconhecido" else "PLMN $plmn"
    }

    private data class MutableTower(
        val id: String,
        val address: String,
        val lat: Double,
        val lon: Double,
        val operators: MutableSet<String> = linkedSetOf(),
        val bands4g: MutableSet<String> = linkedSetOf(),
        val bands5g: MutableSet<String> = linkedSetOf(),
    )
}

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var content: FrameLayout
    private lateinit var bottomNav: BottomNavigationView
    private var towers: List<Tower> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()
        showLoading("A carregar base local...")
        TowerRepository.load(this) {
            towers = it
            showHome()
        }
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(mdSurface())
        }
        toolbar = MaterialToolbar(this).apply {
            title = "Portugal Towers"
            setTitleTextColor(Color.WHITE)
            setBackgroundColor(mdPrimary())
            setNavigationIcon(R.drawable.icon)
            minimumHeight = dp(64)
        }
        content = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        bottomNav = BottomNavigationView(this).apply {
            inflateMenu(R.menu.bottom_nav)
            setBackgroundColor(Color.WHITE)
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> showHome()
                    R.id.nav_map -> showMap()
                    R.id.nav_nearby -> showNearby()
                    R.id.nav_settings -> showSettings()
                    R.id.nav_about -> showAbout()
                }
                true
            }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
        root.addView(content)
        root.addView(bottomNav, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun showHome() {
        toolbar.title = "Portugal Towers"
        val page = page()
        page.addView(hero())
        page.addView(statusCard())
        page.addView(sectionLabel("Explorar"))
        page.addView(navCard("Torres proximas", "Ordena por distancia usando GPS.", R.drawable.ic_nearby) {
            bottomNav.selectedItemId = R.id.nav_nearby
        })
        page.addView(navCard("Mapa", "Vista nacional com pontos por operadora.", R.drawable.ic_map) {
            bottomNav.selectedItemId = R.id.nav_map
        })
        page.addView(sectionLabel("Aplicacao"))
        page.addView(navCard("Definicoes", "Base local, estado e preferências.", R.drawable.ic_settings) {
            bottomNav.selectedItemId = R.id.nav_settings
        })
        page.addView(navCard("Sobre", "Dados, versao e comunidade.", R.drawable.ic_info) {
            bottomNav.selectedItemId = R.id.nav_about
        })
        setPage(page)
    }

    private fun showMap() {
        toolbar.title = "Mapa"
        val frame = FrameLayout(this).apply { setBackgroundColor(mdSurface()) }
        frame.addView(TowerMapView(this).apply { setTowers(towers) }, FrameLayout.LayoutParams(-1, -1))
        frame.addView(
            pill("${towers.size} torres"),
            FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(20) },
        )
        setView(frame)
    }

    private fun showNearby() {
        toolbar.title = "Torres proximas"
        if (!hasLocationPermission()) {
            requestLocationPermission()
            val page = page()
            page.addView(messageCard("Permissao de localizacao", "Autoriza a localizacao para calcular as torres mais proximas.", true) {
                showNearby()
            })
            setPage(page)
            return
        }

        val loc = lastLocation()
        if (loc == null) {
            val page = page()
            page.addView(messageCard("Sem GPS", "Nao consegui obter uma localizacao recente. Abre o mapa ou ativa o GPS.", true) {
                bottomNav.selectedItemId = R.id.nav_map
            })
            setPage(page)
            return
        }

        val nearest = towers.map { it to distanceMeters(loc.latitude, loc.longitude, it.lat, it.lon) }
            .sortedBy { it.second }
            .take(80)
        val page = page()
        page.addView(sectionHeader("Mais proximas", "%.5f, %.5f".format(Locale.US, loc.latitude, loc.longitude)))
        nearest.forEach { (tower, distance) -> page.addView(towerRow(tower, distance)) }
        setPage(page)
    }

    private fun showSettings() {
        toolbar.title = "Definicoes"
        val page = page()
        page.addView(sectionHeader("Armazenamento local", "Sem React Native. Dados embutidos na app nativa."))
        page.addView(dataCard("Ultima importacao", "CSV antigo copiado para assets"))
        page.addView(dataCard("Registos", "${towers.size} torres agregadas"))
        page.addView(dataCard("Mapa", "Canvas Android nativo, sem chave Google Maps"))
        setPage(page)
    }

    private fun showAbout() {
        toolbar.title = "Sobre"
        val page = page()
        page.addView(hero())
        page.addView(dataCard("Portugal Towers", "Versao nativa inicial baseada no antigo projeto React Native."))
        page.addView(dataCard("Operadoras", "MEO, NOS, Vodafone, Digi e PLMN desconhecidos quando existirem."))
        page.addView(navCard("Telegram", "Abrir comunidade CellMapper Portugal.", R.drawable.ic_info) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/cellmapperpt")))
        })
        setPage(page)
    }

    fun showTowerDialog(tower: Tower, distance: Double? = null) {
        val message = buildString {
            appendLine(tower.address)
            appendLine("%.6f, %.6f".format(Locale.US, tower.lat, tower.lon))
            distance?.let { appendLine("Distancia: ${formatDistance(it)}") }
            appendLine("Operadoras: ${tower.operators.joinToString(", ")}")
            if (tower.bands4g.isNotEmpty()) appendLine("4G: ${tower.bands4g.joinToString(", ")}")
            if (tower.bands5g.isNotEmpty()) appendLine("5G: ${tower.bands5g.joinToString(", ")}")
        }
        AlertDialog.Builder(this)
            .setTitle("Torre ${tower.id}")
            .setMessage(message)
            .setPositiveButton("Abrir no Maps") { _, _ ->
                val uri = Uri.parse("geo:${tower.lat},${tower.lon}?q=${tower.lat},${tower.lon}(Portugal Towers)")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun hero(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(8), dp(24), dp(8), dp(18))
        addView(ImageView(context).apply {
            setImageResource(R.drawable.logo_app)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(92), dp(92)))
        addView(TextView(context).apply {
            text = "Portugal Towers"
            textSize = 30f
            setTextColor(mdOnSurface())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        addView(TextView(context).apply {
            text = "Torres telecom de Portugal"
            textSize = 16f
            setTextColor(mdMuted())
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
    }

    private fun statusCard(): View = card().apply {
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            addView(TextView(context).apply {
                text = "Base pronta"
                textSize = 17f
                setTextColor(mdOnSurface())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(pill("${towers.size} torres"))
        })
    }

    private fun navCard(title: String, subtitle: String, icon: Int, action: () -> Unit): View = card().apply {
        isClickable = true
        setOnClickListener { action() }
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(12), dp(14))
            addView(iconBubble(icon))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, dp(8), 0)
                addView(titleText(title))
                addView(bodyText(subtitle))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(context).apply {
                text = ">"
                textSize = 24f
                setTextColor(mdMuted())
            })
        })
    }

    private fun towerRow(tower: Tower, distance: Double): View = card().apply {
        isClickable = true
        setOnClickListener { showTowerDialog(tower, distance) }
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(pill(formatDistance(distance)))
                addView(operatorGrid(tower), LinearLayout.LayoutParams(dp(54), dp(54)).apply { leftMargin = dp(12) })
                addView(TextView(context).apply {
                    text = tower.address
                    textSize = 16f
                    setTextColor(mdOnSurface())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(dp(12), 0, 0, 0)
                }, LinearLayout.LayoutParams(0, -2, 1f))
            })
            addView(operatorChips(tower), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            val tech = listOfNotNull(
                tower.bands4g.takeIf { it.isNotEmpty() }?.let { "4G ${it.joinToString(", ")}" },
                tower.bands5g.takeIf { it.isNotEmpty() }?.let { "5G ${it.joinToString(", ")}" },
            ).joinToString(" | ")
            if (tech.isNotBlank()) addView(bodyText(tech))
        })
    }

    private fun operatorGrid(tower: Tower): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        repeat(2) { row ->
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                repeat(2) { col ->
                    val op = when (row to col) {
                        0 to 0 -> "MEO"
                        0 to 1 -> "NOS"
                        1 to 0 -> "Vodafone"
                        else -> "Digi"
                    }
                    addView(FrameLayout(context).apply {
                        background = rounded(Color.rgb(236, 240, 246), dp(7).toFloat())
                        if (tower.operators.contains(op)) addView(operatorMark(op), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
                    }, LinearLayout.LayoutParams(dp(25), dp(25)).apply {
                        rightMargin = if (col == 0) dp(4) else 0
                        bottomMargin = if (row == 0) dp(4) else 0
                    })
                }
            })
        }
    }

    private fun operatorMark(op: String): View {
        val image = when (op) {
            "MEO" -> R.drawable.logo_meo
            "NOS" -> R.drawable.logo_nos
            "Digi" -> R.drawable.logo_digi
            else -> null
        }
        return if (image != null) {
            ImageView(this).apply {
                setImageResource(image)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        } else {
            TextView(this).apply {
                text = "V"
                textSize = 13f
                setTextColor(Color.rgb(230, 0, 0))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            }
        }
    }

    private fun operatorChips(tower: Tower): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        tower.operators.take(4).forEach { op ->
            addView(Chip(context).apply {
                text = op
                isClickable = false
                setTextColor(Color.WHITE)
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(operatorColor(op))
                chipCornerRadius = dp(12).toFloat()
            }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
        }
    }

    private fun messageCard(title: String, message: String, button: Boolean, action: () -> Unit): View = card().apply {
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(titleText(title))
            addView(bodyText(message))
            if (button) {
                addView(MaterialButton(context).apply {
                    text = "Continuar"
                    cornerRadius = dp(22)
                    setOnClickListener { action() }
                }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            }
        })
    }

    private fun dataCard(title: String, body: String): View = card().apply {
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            addView(titleText(title))
            addView(bodyText(body))
        })
    }

    private fun sectionHeader(title: String, subtitle: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(4), dp(8), dp(4), dp(12))
        addView(TextView(context).apply {
            text = title
            textSize = 24f
            setTextColor(mdOnSurface())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        addView(bodyText(subtitle))
    }

    private fun sectionLabel(text: String): View = TextView(this).apply {
        this.text = text.uppercase(Locale.ROOT)
        textSize = 13f
        setTextColor(mdPrimary())
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dp(4), dp(16), dp(4), dp(8))
    }

    private fun iconBubble(icon: Int): View = FrameLayout(this).apply {
        background = rounded(mdPrimaryContainer(), dp(20).toFloat())
        addView(ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(mdPrimary())
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }, FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER))
    }.also { it.layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)) }

    private fun titleText(value: String) = TextView(this).apply {
        text = value
        textSize = 17f
        setTextColor(mdOnSurface())
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun bodyText(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setTextColor(mdMuted())
        setPadding(0, dp(5), 0, 0)
    }

    private fun pill(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(mdPrimary())
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dp(12), dp(7), dp(12), dp(7))
        background = rounded(mdPrimaryContainer(), dp(16).toFloat())
    }

    private fun card() = MaterialCardView(this).apply {
        radius = dp(22).toFloat()
        cardElevation = dp(1).toFloat()
        setCardBackgroundColor(Color.WHITE)
        strokeWidth = 1
        strokeColor = Color.rgb(226, 230, 236)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
    }

    private fun page(): LinearLayout {
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(22))
        }
        val scroll = ScrollView(this).apply { addView(inner) }
        inner.tag = scroll
        return inner
    }

    private fun setPage(layout: LinearLayout) = setView(layout.tag as View)

    private fun setView(view: View) {
        content.removeAllViews()
        content.addView(view, FrameLayout.LayoutParams(-1, -1))
    }

    private fun showLoading(text: String) {
        setView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(ProgressBar(context))
            addView(bodyText(text))
        })
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
    }

    private fun lastLocation(): Location? {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).mapNotNull { provider ->
            try {
                if (hasLocationPermission()) manager.getLastKnownLocation(provider) else null
            } catch (_: Exception) {
                null
            }
        }.maxByOrNull { it.time }
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun formatDistance(m: Double) = if (m < 1000) "${m.toInt()} m" else "%.1f km".format(Locale.US, m / 1000.0)

    private fun rounded(color: Int, radius: Float) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun operatorColor(operator: String?) = when (operator) {
        "MEO" -> Color.rgb(0, 91, 172)
        "NOS" -> Color.rgb(26, 26, 26)
        "Vodafone" -> Color.rgb(230, 0, 0)
        "Digi" -> Color.rgb(0, 170, 68)
        else -> Color.rgb(120, 120, 120)
    }

    private fun mdPrimary() = Color.rgb(0, 85, 180)
    private fun mdPrimaryContainer() = Color.rgb(216, 227, 255)
    private fun mdSurface() = Color.rgb(247, 249, 252)
    private fun mdOnSurface() = Color.rgb(29, 32, 36)
    private fun mdMuted() = Color.rgb(91, 96, 108)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

class TowerMapView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var towers: List<Tower> = emptyList()
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var tapX = 0f
    private var tapY = 0f

    fun setTowers(value: List<Tower>) {
        towers = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(231, 238, 245))
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale, width / 2f, height / 2f)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(208, 225, 214)
        canvas.drawRoundRect(RectF(dp(26f), dp(36f), width - dp(26f), height - dp(46f)), dp(28f), dp(28f), paint)

        towers.forEach { tower ->
            paint.color = operatorColor(tower.operators.firstOrNull())
            canvas.drawCircle(lonToX(tower.lon), latToY(tower.lat), max(2.3f, 4.2f / scale), paint)
        }
        canvas.restore()

        paint.color = Color.rgb(29, 32, 36)
        paint.textSize = dp(13f)
        canvas.drawText("Arrasta para mover. Toca numa torre.", dp(16f), dp(26f), paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                tapX = event.x
                tapY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                offsetX += event.x - lastX
                offsetY += event.y - lastY
                lastX = event.x
                lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                scale = min(5f, scale * 1.25f)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (abs(event.x - tapX) < dp(8f) && abs(event.y - tapY) < dp(8f)) {
                    nearestTower(event.x, event.y)?.let { (context as? MainActivity)?.showTowerDialog(it, null) }
                }
            }
        }
        return true
    }

    private fun nearestTower(x: Float, y: Float): Tower? {
        var best: Tower? = null
        var bestDistance = Float.MAX_VALUE
        towers.forEach { tower ->
            val sx = ((lonToX(tower.lon) - width / 2f) * scale) + width / 2f + offsetX
            val sy = ((latToY(tower.lat) - height / 2f) * scale) + height / 2f + offsetY
            val d = (sx - x) * (sx - x) + (sy - y) * (sy - y)
            if (d < bestDistance) {
                bestDistance = d
                best = tower
            }
        }
        return best.takeIf { bestDistance < dp(26f) * dp(26f) }
    }

    private fun lonToX(lon: Double): Float = (((lon + 32.5) / 26.5) * width).toFloat().coerceIn(0f, width.toFloat())
    private fun latToY(lat: Double): Float = (height - ((lat - 32.0) / 10.5) * height).toFloat().coerceIn(0f, height.toFloat())

    private fun operatorColor(operator: String?) = when (operator) {
        "MEO" -> Color.rgb(0, 91, 172)
        "NOS" -> Color.rgb(26, 26, 26)
        "Vodafone" -> Color.rgb(230, 0, 0)
        "Digi" -> Color.rgb(0, 170, 68)
        else -> Color.rgb(120, 120, 120)
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
