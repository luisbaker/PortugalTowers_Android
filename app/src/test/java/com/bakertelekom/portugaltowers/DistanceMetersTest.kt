package com.bakertelekom.portugaltowers

import com.bakertelekom.portugaltowers.domain.distanceMeters
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceMetersTest {

    @Test
    fun distanceMeters_returns_zero_for_same_point() {
        assertEquals(0.0, distanceMeters(38.7223, -9.1393, 38.7223, -9.1393), 0.0001)
    }

    @Test
    fun distanceMeters_matches_known_distance_between_lisbon_and_porto() {
        val distance = distanceMeters(38.7223, -9.1393, 41.1579, -8.6291)

        assertEquals(274000.0, distance, 1500.0)
    }
}
