package com.bakertelekom.portugaltowers.domain

data class Tower(
    val id: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val operators: Set<Operator>,
    val bands4gByOperator: Map<Operator, Set<String>>,
    val bands5gByOperator: Map<Operator, Set<String>>,
) {
    val primaryOperator: Operator
        get() = operators.firstOrNull { it != Operator.Unknown } ?: operators.firstOrNull() ?: Operator.Unknown

    val bands4g: Set<String>
        get() = bands4gByOperator.values.flatten().toSortedSet()

    val bands5g: Set<String>
        get() = bands5gByOperator.values.flatten().toSortedSet()
}
