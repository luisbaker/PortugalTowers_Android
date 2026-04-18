package com.bakertelekom.portugaltowers.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bakertelekom.portugaltowers.domain.MapTowerCluster
import com.bakertelekom.portugaltowers.domain.Operator
import com.bakertelekom.portugaltowers.domain.Tower

class TowerDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE towers (
                row_id INTEGER PRIMARY KEY AUTOINCREMENT,
                tower_id TEXT NOT NULL,
                address TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                operators TEXT NOT NULL,
                bands4g TEXT NOT NULL,
                bands5g TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_towers_lat_lon ON towers(latitude, longitude)")
        db.execSQL("CREATE INDEX idx_towers_lon_lat ON towers(longitude, latitude)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS towers")
        onCreate(db)
    }

    fun replaceAll(towers: List<Tower>) {
        writableDatabase.useTransaction {
            delete("towers", null, null)
            val statement = compileStatement(
                """
                INSERT INTO towers (
                    tower_id,
                    address,
                    latitude,
                    longitude,
                    operators,
                    bands4g,
                    bands5g
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            )
            towers.forEach { tower ->
                statement.clearBindings()
                statement.bindString(1, tower.id)
                statement.bindString(2, tower.address)
                statement.bindDouble(3, tower.latitude)
                statement.bindDouble(4, tower.longitude)
                statement.bindString(5, tower.operators.joinToString(",") { it.name })
                statement.bindString(6, tower.bands4gByOperator.serializeBandsByOperator())
                statement.bindString(7, tower.bands5gByOperator.serializeBandsByOperator())
                statement.executeInsert()
            }
        }
    }

    fun count(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM towers", emptyArray()).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

    fun allTowers(): List<Tower> =
        readableDatabase.rawQuery(
            "SELECT tower_id, address, latitude, longitude, operators, bands4g, bands5g FROM towers",
            emptyArray(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toTower())
            }
        }

    fun towersInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        limit: Int,
    ): List<Tower> =
        readableDatabase.rawQuery(
            """
            SELECT tower_id, address, latitude, longitude, operators, bands4g, bands5g
            FROM towers
            WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?
            LIMIT ?
            """.trimIndent(),
            arrayOf(minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString(), limit.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toTower())
            }
        }

    fun macroClusters(cellSize: Double): List<MapTowerCluster> =
        readableDatabase.rawQuery(
            """
            SELECT
                ROUND(latitude / ?) * ? AS bucket_lat,
                ROUND(longitude / ?) * ? AS bucket_lon,
                COUNT(*) AS tower_count,
                GROUP_CONCAT(operators) AS operator_groups
            FROM towers
            GROUP BY bucket_lat, bucket_lon
            """.trimIndent(),
            arrayOf(cellSize.toString(), cellSize.toString(), cellSize.toString(), cellSize.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MapTowerCluster(
                            latitude = cursor.getDouble(0),
                            longitude = cursor.getDouble(1),
                            count = cursor.getInt(2),
                            operators = cursor.getString(3).toOperators(),
                        ),
                    )
                }
            }
        }

    private fun android.database.Cursor.toTower(): Tower =
        Tower(
            id = getString(0),
            address = getString(1),
            latitude = getDouble(2),
            longitude = getDouble(3),
            operators = getString(4).toOperators(),
            bands4gByOperator = getString(5).toBandsByOperator(),
            bands5gByOperator = getString(6).toBandsByOperator(),
        )

    private fun String.toOperators(): Set<Operator> =
        split(',')
            .mapNotNull { name -> Operator.entries.firstOrNull { it.name == name.trim() } }
            .ifEmpty { listOf(Operator.Unknown) }
            .toSet()

    private fun Map<Operator, Set<String>>.serializeBandsByOperator(): String =
        entries.joinToString("|") { (operator, bands) ->
            operator.name + ":" + bands.joinToString(",")
        }

    private fun String.toBandsByOperator(): Map<Operator, Set<String>> =
        split('|')
            .mapNotNull { group ->
                val operatorName = group.substringBefore(':', missingDelimiterValue = "").trim()
                val bands = group.substringAfter(':', missingDelimiterValue = "")
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSortedSet()
                val operator = Operator.entries.firstOrNull { it.name == operatorName }
                if (operator == null || bands.isEmpty()) null else operator to bands
            }
            .toMap()

    private inline fun SQLiteDatabase.useTransaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private companion object {
        const val DB_NAME = "portugal_towers.db"
        const val DB_VERSION = 4
    }
}
