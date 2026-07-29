package org.poc.objs.core.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoMSeedResourceIdentityTest {
    @Test
    fun shouldFingerprintRawBytesWithSha256Prefix() {
        val fp = BoMSeedResourceIdentity.fingerprint("hello".toByteArray())
        assertThat(fp).startsWith("sha256:")
        assertThat(fp).isEqualTo(BoMSeedResourceIdentity.fingerprint("hello".toByteArray()))
        assertThat(fp).isNotEqualTo(BoMSeedResourceIdentity.fingerprint("HELLO".toByteArray()))
    }

    @Test
    fun shouldUseSanitizedLocationAsLedgerKey() {
        assertThat(BoMSeedResourceIdentity.ledgerKey("classpath:/seeds/a.yaml"))
            .isEqualTo("classpath:seeds/a.yaml")
        assertThat(BoMSeedResourceIdentity.normalizeLocation("classpath:/seeds/a.yaml"))
            .isEqualTo("classpath:seeds/a.yaml")
        assertThat(
            BoMSeedResourceIdentity.normalizeLocation("file:///tmp/a.yaml?token=secret#frag"),
        ).isEqualTo("file:/tmp/a.yaml")
        assertThat(
            BoMSeedResourceIdentity.normalizeLocation("https://user:pass@example.com/path/a.yaml?x=1#y"),
        ).isEqualTo("https://example.com/path/a.yaml")
    }
}
