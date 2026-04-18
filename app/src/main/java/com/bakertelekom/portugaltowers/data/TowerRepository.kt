package com.bakertelekom.portugaltowers.data

import android.content.Context
import com.bakertelekom.portugaltowers.domain.Tower
import java.io.InputStreamReader

class TowerRepository(
    private val context: Context,
    private val parser: CsvTowerParser = CsvTowerParser(),
) {
    private var cache: List<Tower>? = null

    fun loadTowers(): Result<List<Tower>> = runCatching {
        cache?.let { return@runCatching it }
        context.assets.open(ASSET_NAME).use { stream ->
            InputStreamReader(stream).use { reader ->
                parser.parse(reader).also { cache = it }
            }
        }
    }

    private companion object {
        const val ASSET_NAME = "portugal_telecom_towers.csv"
    }
}
