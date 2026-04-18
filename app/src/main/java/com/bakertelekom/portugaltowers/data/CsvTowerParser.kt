package com.bakertelekom.portugaltowers.data

import com.bakertelekom.portugaltowers.domain.Operator
import com.bakertelekom.portugaltowers.domain.Tower
import java.io.Reader

class CsvTowerParser {
    fun parse(reader: Reader): List<Tower> {
        val grouped = linkedMapOf<String, MutableTower>()
        reader.buffered().useLines { lines ->
            lines.drop(1).forEach { line ->
                val parts = line.split(';')
                if (parts.size < MIN_COLUMNS) return@forEach

                val id = parts[0].trim()
                val latitude = parts[2].toDoubleOrNull() ?: return@forEach
                val longitude = parts[3].toDoubleOrNull() ?: return@forEach
                val key = "$id:$latitude:$longitude"
                val tower = grouped.getOrPut(key) {
                    MutableTower(
                        id = id,
                        address = parts[1].trim().ifBlank { id.ifBlank { "Torre" } },
                        latitude = latitude,
                        longitude = longitude,
                    )
                }

                tower.operators.add(Operator.fromPlmn(parts[9]))
                tower.bands4g.addAll(splitBands(parts.getOrNull(5).orEmpty()))
                tower.bands5g.addAll(splitBands(parts.getOrNull(7).orEmpty()))
            }
        }

        return grouped.values.map { tower ->
            Tower(
                id = tower.id,
                address = tower.address,
                latitude = tower.latitude,
                longitude = tower.longitude,
                operators = tower.operators.toSortedSet(compareBy<Operator> { it.ordinal }),
                bands4g = tower.bands4g.toSortedSet(),
                bands5g = tower.bands5g.toSortedSet(),
            )
        }
    }

    private fun splitBands(value: String): List<String> =
        value.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private data class MutableTower(
        val id: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val operators: MutableSet<Operator> = linkedSetOf(),
        val bands4g: MutableSet<String> = linkedSetOf(),
        val bands5g: MutableSet<String> = linkedSetOf(),
    )

    private companion object {
        const val MIN_COLUMNS = 10
    }
}
