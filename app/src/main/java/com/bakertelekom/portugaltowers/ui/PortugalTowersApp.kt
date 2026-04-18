package com.bakertelekom.portugaltowers.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bakertelekom.portugaltowers.AppState
import com.bakertelekom.portugaltowers.MainViewModel
import com.bakertelekom.portugaltowers.R
import com.bakertelekom.portugaltowers.domain.MapTowerCluster
import com.bakertelekom.portugaltowers.domain.Tower
import com.bakertelekom.portugaltowers.domain.UserLocation
import com.bakertelekom.portugaltowers.domain.distanceMeters
import com.bakertelekom.portugaltowers.domain.formatDistance
import com.bakertelekom.portugaltowers.location.LocationProvider
import com.bakertelekom.portugaltowers.ui.components.OperatorGrid
import com.bakertelekom.portugaltowers.ui.components.TowerDetailDialog
import com.bakertelekom.portugaltowers.ui.map.TowerMap
import java.util.Locale

private enum class Destination(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    Home("home", "Inicio", Icons.Outlined.SignalCellularAlt),
    Map("map", "Mapa", Icons.Outlined.Map),
    Nearby("nearby", "Perto", Icons.Outlined.NearMe),
    Settings("settings", "Definicoes", Icons.Outlined.Settings),
    About("about", "Sobre", Icons.Outlined.Info),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortugalTowersApp(viewModel: MainViewModel) {
    val appState by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.Home.route
    val currentDestination = Destination.entries.firstOrNull { it.route == currentRoute } ?: Destination.Home

    Scaffold(
        topBar = {
            if (currentDestination != Destination.Map) {
                TopAppBar(
                    title = { Text(currentDestination.title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.title) },
                        label = { Text(destination.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(appState, onRetry = viewModel::loadTowers, onNavigate = { route ->
                    navController.navigate(route)
                })
            }
            composable(Destination.Map.route) {
                TowersContent(appState, onRetry = viewModel::loadTowers) { towerCount ->
                    MapScreen(
                        towerCount = towerCount,
                        loadVisibleTowers = viewModel::towersInBounds,
                        loadMacroClusters = viewModel::macroClusters,
                    )
                }
            }
            composable(Destination.Nearby.route) {
                TowersContent(appState, onRetry = viewModel::loadTowers) {
                    NearbyScreen(loadTowers = viewModel::allTowers)
                }
            }
            composable(Destination.Settings.route) {
                SettingsScreen(appState)
            }
            composable(Destination.About.route) {
                AboutScreen()
            }
        }
    }
}

@Composable
private fun TowersContent(
    appState: AppState,
    onRetry: () -> Unit,
    content: @Composable (Int) -> Unit,
) {
    when (appState) {
        AppState.Empty -> StatusScreen("Sem dados", "A base local nao tem torres para mostrar.", onRetry)
        is AppState.Error -> StatusScreen("Erro ao carregar", appState.message, onRetry)
        AppState.Loading -> StatusScreen("A carregar", "A preparar a base local.", null)
        is AppState.Ready -> content(appState.towerCount)
    }
}

@Composable
private fun HomeScreen(
    appState: AppState,
    onRetry: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Hero()
        }
        if (appState is AppState.Error) {
            item {
                StatusCard("Erro na base local", appState.message, action = "Tentar de novo", onAction = onRetry)
            }
        }
        item { SectionLabel("Explorar") }
        item {
            ActionCard(
                title = "Torres perto de mim",
                subtitle = "Ordena as torres por distancia usando a localizacao do dispositivo.",
                icon = Icons.Outlined.NearMe,
                onClick = { onNavigate(Destination.Nearby.route) },
            )
        }
        item {
            ActionCard(
                title = "Mapa",
                subtitle = "Vista nacional com pontos por operadora.",
                icon = Icons.Outlined.Map,
                onClick = { onNavigate(Destination.Map.route) },
            )
        }
        item { SectionLabel("Aplicacao") }
        item {
            ActionCard(
                title = "Definicoes",
                subtitle = "Estado da base local e informacao da app.",
                icon = Icons.Outlined.Settings,
                onClick = { onNavigate(Destination.Settings.route) },
            )
        }
        item {
            ActionCard(
                title = "Sobre",
                subtitle = "Dados, operadoras e comunidade.",
                icon = Icons.Outlined.Info,
                onClick = { onNavigate(Destination.About.route) },
            )
        }
    }
}

@Composable
private fun MapScreen(
    towerCount: Int,
    loadVisibleTowers: suspend (Double, Double, Double, Double, Int) -> List<Tower>,
    loadMacroClusters: suspend (Double) -> List<MapTowerCluster>,
) {
    var selectedTower by remember { mutableStateOf<Tower?>(null) }
    Box(Modifier.fillMaxSize()) {
        TowerMap(
            towerCount = towerCount,
            loadVisibleTowers = loadVisibleTowers,
            loadMacroClusters = loadMacroClusters,
            onTowerSelected = { selectedTower = it },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 4.dp,
        ) {
            Text(
                text = "$towerCount torres",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
    selectedTower?.let { tower ->
        TowerDetailDialog(tower = tower, distanceMeters = null, onDismiss = { selectedTower = null })
    }
}

@Composable
private fun NearbyScreen(loadTowers: suspend () -> List<Tower>) {
    val context = LocalContext.current
    val locationProvider = remember { LocationProvider(context.applicationContext) }
    var towers by remember { mutableStateOf<List<Tower>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    var userLocation by remember { mutableStateOf<UserLocation?>(null) }
    var needsPermission by remember { mutableStateOf(!locationProvider.hasLocationPermission()) }
    var selected by remember { mutableStateOf<Pair<Tower, Double>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        needsPermission = !granted
        if (granted) {
            userLocation = locationProvider.lastKnownLocation()
        }
    }

    LaunchedEffect(reloadToken) {
        runCatching { loadTowers() }
            .onSuccess { towers = it }
            .onFailure { loadError = it.message ?: "Nao foi possivel carregar as torres." }
        if (locationProvider.hasLocationPermission()) {
            needsPermission = false
            userLocation = locationProvider.lastKnownLocation()
        }
    }

    when {
        loadError != null -> StatusScreen(
            title = "Erro ao carregar",
            message = loadError ?: "Nao foi possivel carregar as torres.",
            action = "Tentar de novo",
            onAction = {
                loadError = null
                towers = null
                reloadToken += 1
            },
        )
        towers == null -> StatusScreen(
            title = "A carregar",
            message = "A preparar as torres mais proximas.",
            onAction = null,
        )
        needsPermission -> StatusScreen(
            title = "Permissao de localizacao",
            message = "Autoriza a localizacao para calcular as torres mais proximas.",
            action = "Autorizar",
            onAction = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
        )
        userLocation == null -> StatusScreen(
            title = "Sem localizacao",
            message = "Nao foi possivel obter uma localizacao recente. Ativa o GPS e tenta de novo.",
            action = "Tentar de novo",
            onAction = { userLocation = locationProvider.lastKnownLocation() },
        )
        else -> {
            val location = userLocation ?: return
            val loadedTowers = towers.orEmpty()
            val nearest = remember(loadedTowers, location) {
                loadedTowers.map { tower ->
                    tower to distanceMeters(location.latitude, location.longitude, tower.latitude, tower.longitude)
                }.sortedBy { it.second }.take(80)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text("Mais proximas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "%.5f, %.5f".format(Locale.US, location.latitude, location.longitude),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(nearest, key = { it.first.id + it.first.latitude + it.first.longitude }) { (tower, distance) ->
                    TowerRow(tower = tower, distance = distance, onClick = { selected = tower to distance })
                }
            }
        }
    }

    selected?.let { (tower, distance) ->
        TowerDetailDialog(tower = tower, distanceMeters = distance, onDismiss = { selected = null })
    }
}

@Composable
private fun SettingsScreen(appState: AppState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionLabel("Base local") }
        item {
            val count = (appState as? AppState.Ready)?.towerCount ?: 0
            InfoCard("Fonte de dados", "CSV embutido importado para SQLite local.")
            Spacer(Modifier.height(10.dp))
            InfoCard("Registos", "$count torres agregadas.")
            Spacer(Modifier.height(10.dp))
            InfoCard("Sincronizacao", "A arquitetura esta preparada para sync futuro sem alterar a UI.")
        }
        item { SectionLabel("Mapa") }
        item {
            InfoCard("Renderizacao", "OSM com clusters e queries SQLite por zona visivel.")
        }
    }
}

@Composable
private fun AboutScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Hero() }
        item { InfoCard("Portugal Towers", "Aplicacao Android nativa para explorar torres telecom em Portugal.") }
        item { InfoCard("Operadoras", "MEO, NOS, Vodafone, Digi e PLMN desconhecidos quando existirem.") }
        item { InfoCard("Comunidade", "Telegram: t.me/cellmapperpt") }
    }
}

@Composable
private fun Hero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_app),
            contentDescription = "Portugal Towers",
            modifier = Modifier.size(88.dp),
        )
        Text("Portugal Towers", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Torres telecom de Portugal",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TowerRow(
    tower: Tower,
    distance: Double,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = formatDistance(distance),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            OperatorGrid(tower.operators)
            Column(Modifier.weight(1f)) {
                Text(tower.address, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val tech = listOfNotNull(
                    tower.bands4g.takeIf { it.isNotEmpty() }?.let { "4G ${it.joinToString(", ")}" },
                    tower.bands5g.takeIf { it.isNotEmpty() }?.let { "5G ${it.joinToString(", ")}" },
                ).joinToString(" | ")
                if (tech.isNotBlank()) {
                    Text(tech, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StatusCard(
    title: String,
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(action)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, message: String) {
    StatusCard(title = title, message = message)
}

@Composable
private fun StatusScreen(
    title: String,
    message: String,
    onAction: (() -> Unit)?,
) {
    StatusScreen(title = title, message = message, action = onAction?.let { "Tentar de novo" }, onAction = onAction)
}

@Composable
private fun StatusScreen(
    title: String,
    message: String,
    action: String?,
    onAction: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(action)
                }
            }
        }
    }
}
