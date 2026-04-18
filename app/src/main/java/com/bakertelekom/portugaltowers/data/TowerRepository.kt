package com.bakertelekom.portugaltowers.data

import android.content.Context
import com.bakertelekom.portugaltowers.domain.MapTowerCluster
import com.bakertelekom.portugaltowers.domain.Tower
import java.io.InputStreamReader

class TowerRepository(
    private val context: Context,
    private val parser: CsvTowerParser = CsvTowerParser(),
) {
    private val database = TowerDatabase(context)

    fun prepareDatabase(): Result<Int> = runCatching {
        ensureImported()
        database.count()
    }

    fun loadTowers(): Result<List<Tower>> = runCatching {
        ensureImported()
        database.allTowers()
    }

    fun loadTowersInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        limit: Int,
    ): Result<List<Tower>> = runCatching {
        ensureImported()
        database.towersInBounds(minLat, maxLat, minLon, maxLon, limit)
    }

    fun loadMacroClusters(cellSize: Double): Result<List<MapTowerCluster>> = runCatching {
        ensureImported()
        database.macroClusters(cellSize)
    }

    @Synchronized
    private fun ensureImported() {
        if (database.count() == 0) {
            database.replaceAll(parseAsset())
        }
    }

    private fun parseAsset(): List<Tower> =
        context.assets.open(ASSET_NAME).use { stream ->
            InputStreamReader(stream).use { reader ->
                parser.parse(reader)
            }
        }

    private companion object {
        const val ASSET_NAME = "portugal_telecom_towers.csv"
    }
}
