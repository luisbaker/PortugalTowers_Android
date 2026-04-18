package com.bakertelekom.portugaltowers.domain

enum class Operator(
    val displayName: String,
    val plmn: String?,
    val brandColor: Long,
) {
    Meo("MEO", "26806", 0xFF005BAC),
    Nos("NOS", "26803", 0xFF1A1A1A),
    Vodafone("Vodafone", "26801", 0xFFE60000),
    Digi("Digi", "26811", 0xFF00AA44),
    Unknown("Desconhecido", null, 0xFF777777);

    companion object {
        fun fromPlmn(value: String): Operator {
            val normalized = value.trim()
            return entries.firstOrNull { it.plmn == normalized } ?: Unknown
        }

        fun fromDisplayName(value: String): Operator {
            val normalized = value.trim()
            return entries.firstOrNull { it.displayName.equals(normalized, ignoreCase = true) } ?: Unknown
        }
    }
}
