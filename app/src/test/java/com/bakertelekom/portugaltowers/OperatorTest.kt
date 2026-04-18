package com.bakertelekom.portugaltowers

import com.bakertelekom.portugaltowers.domain.Operator
import org.junit.Assert.assertEquals
import org.junit.Test

class OperatorTest {

    @Test
    fun fromPlmn_maps_known_codes_and_defaults_to_unknown() {
        assertEquals(Operator.Meo, Operator.fromPlmn("26806"))
        assertEquals(Operator.Nos, Operator.fromPlmn(" 26803 "))
        assertEquals(Operator.Unknown, Operator.fromPlmn("00000"))
    }

    @Test
    fun fromDisplayName_is_case_insensitive_and_trims_input() {
        assertEquals(Operator.Vodafone, Operator.fromDisplayName(" vodafone "))
        assertEquals(Operator.Digi, Operator.fromDisplayName("DIGI"))
        assertEquals(Operator.Unknown, Operator.fromDisplayName("other"))
    }
}
