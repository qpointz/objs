package org.poc.objs.policy.drools

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DroolsMessageSanitizeTest {

    @Test
    fun shouldStripJavaSourceDump() {
        val raw =
            """
            The method fail(String, String) in the type DroolsEvaluationScratch is not applicable for the arguments (String); Java source of src/main/java/org/poc/x/Lambda.java in error:
            package org.poc.x;
            public class Lambda {
              // ...
            }
            """.trimIndent()

        assertThat(sanitizeDroolsMessage(raw))
            .isEqualTo(
                "The method fail(String, String) in the type DroolsEvaluationScratch is not applicable for the arguments (String)",
            )
            .doesNotContain("Java source of")
            .doesNotContain("package org.poc.x")
    }

    @Test
    fun shouldStripStackFrames() {
        val raw =
            "boom\n\tat org.poc.Foo.bar(Foo.java:1)\n\tat org.poc.Foo.baz(Foo.java:2)"
        assertThat(sanitizeDroolsMessage(raw)).isEqualTo("boom")
    }
}
