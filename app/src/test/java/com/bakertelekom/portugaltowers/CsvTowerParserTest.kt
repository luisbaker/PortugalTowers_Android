package com.bakertelekom.portugaltowers

import com.bakertelekom.portugaltowers.data.CsvTowerParser
import com.bakertelekom.portugaltowers.domain.Operator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class CsvTowerParserTest {

    private val parser = CsvTowerParser()

    @Test
    fun parse_groups_rows_by_tower_and_merges_bands_and_operators() {
        val csv = """
            id;address;latitude;longitude;col4;bands4g;col6;bands5g;col8;plmn
            T1;Rua A;38.7223;-9.1393;;;;;;26806
            T1;Rua A;38.7223;-9.1393;;B20, B3;;n78, n1;;26803
        """.trimIndent()

        val towers = parser.parse(StringReader(csv))

        assertEquals(1, towers.size)

        val tower = towers.single()
        assertEquals("T1", tower.id)
        assertEquals("Rua A", tower.address)
        assertEquals(38.7223, tower.latitude, 0.0)
        assertEquals(-9.1393, tower.longitude, 0.0)
        assertEquals(listOf(Operator.Meo, Operator.Nos), tower.operators.toList())
        assertEquals(setOf("B20", "B3"), tower.bands4g)
        assertEquals(setOf("n1", "n78"), tower.bands5g)
        assertEquals(emptySet<String>(), tower.bands4gByOperator[Operator.Meo].orEmpty())
        assertEquals(setOf("B20", "B3"), tower.bands4gByOperator[Operator.Nos])
        assertEquals(setOf("n1", "n78"), tower.bands5gByOperator[Operator.Nos])
    }

    @Test
    fun parse_uses_id_as_fallback_address_and_skips_invalid_rows() {
        val csv = """
            id;address;latitude;longitude;col4;bands4g;col6;bands5g;col8;plmn
            ; ;38.7223;-9.1393;;;;;;26811
            T2;Rua B;not-a-number;-9.1393;;;;;;26806
            T3;Rua C;41.1579;-8.6291;;;;;;99999
        """.trimIndent()

        val towers = parser.parse(StringReader(csv))

        assertEquals(2, towers.size)

        val first = towers[0]
        assertEquals("", first.id)
        assertEquals("Torre", first.address)
        assertEquals(listOf(Operator.Digi), first.operators.toList())

        val second = towers[1]
        assertEquals("T3", second.id)
        assertEquals("Rua C", second.address)
        assertEquals(listOf(Operator.Unknown), second.operators.toList())
    }

    @Test
    fun parse_ignores_rows_with_too_few_columns() {
        val csv = """
            id;address;latitude;longitude;col4;bands4g;col6;bands5g;col8;plmn
            T1;Rua A;38.7223;-9.1393;;B20;;n78
            T2;Rua B;38.7223;-9.1393;;B3;;n1;;26806
        """.trimIndent()

        val towers = parser.parse(StringReader(csv))

        assertEquals(1, towers.size)
        assertEquals("T2", towers.single().id)
        assertTrue(towers.single().bands4g.contains("B3"))
    }
}
