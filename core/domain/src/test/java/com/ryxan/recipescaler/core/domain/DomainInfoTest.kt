package com.ryxan.recipescaler.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainInfoTest {
    @Test
    fun `domain info exposes module name`() {
        assertEquals("domain", DomainInfo.NAME)
    }
}
