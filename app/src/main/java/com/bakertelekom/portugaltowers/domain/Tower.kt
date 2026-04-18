package com.bakertelekom.portugaltowers.domain

data class Tower(
    val id: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val operators: Set<Operator>,
    val bands4g: Set<String>,
    val bands5g: Set<String>,
) {
    val primaryOperator: Operator
        get() = operators.firstOrNull { it != Operator.Unknown } ?: operators.firstOrNull() ?: Operator.Unknown
}
