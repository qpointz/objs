package org.poc.objs.core.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SeedResourceIdentityTest {
    @Test
    fun shouldFingerprintRawBytesWithSha256Prefix() {
        val fp = SeedResourceIdentity.fingerprint("hello".toByteArray())
        assertThat(fp).startsWith("sha256:")
        assertThat(fp).isEqualTo(SeedResourceIdentity.fingerprint("hello".toByteArray()))
        assertThat(fp).isNotEqualTo(SeedResourceIdentity.fingerprint("HELLO".toByteArray()))
    }

    @Test
    fun shouldUseSanitizedLocationAsLedgerKey() {
        assertThat(SeedResourceIdentity.ledgerKey("classpath:/seeds/a.yaml"))
            .isEqualTo("classpath:seeds/a.yaml")
        assertThat(SeedResourceIdentity.normalizeLocation("classpath:/seeds/a.yaml"))
            .isEqualTo("classpath:seeds/a.yaml")
        assertThat(
            SeedResourceIdentity.normalizeLocation("file:///tmp/a.yaml?token=secret#frag"),
        ).isEqualTo("file:/tmp/a.yaml")
        assertThat(
            SeedResourceIdentity.normalizeLocation("https://user:pass@example.com/path/a.yaml?x=1#y"),
        ).isEqualTo("https://example.com/path/a.yaml")
    }
}
