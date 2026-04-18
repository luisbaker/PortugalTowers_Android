package com.bakertelekom.portugaltowers.domain

enum class Operator(
    val displayName: String,
    val plmns: Set<String>,
    val brandColor: Long,
) {
    Meo("MEO", setOf("26806"), 0xFF005BAC),
    Nos("NOS", setOf("26803"), 0xFF1A1A1A),
    Vodafone("Vodafone", setOf("26801"), 0xFFE60000),
    Digi("Digi", setOf("26802"), 0xFF00AA44),
    Unknown("Desconhecido", emptySet(), 0xFF777777);

    companion object {
        fun fromPlmn(value: String): Operator {
            val normalized = value.trim()
            return entries.firstOrNull { normalized in it.plmns } ?: Unknown
        }

        fun fromDisplayName(value: String): Operator {
            val normalized = value.trim()
            return entries.firstOrNull { it.displayName.equals(normalized, ignoreCase = true) } ?: Unknown
        }
    }
}
