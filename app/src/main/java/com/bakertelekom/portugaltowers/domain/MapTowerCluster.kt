package com.bakertelekom.portugaltowers.domain

data class MapTowerCluster(
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val operators: Set<Operator>,
)
