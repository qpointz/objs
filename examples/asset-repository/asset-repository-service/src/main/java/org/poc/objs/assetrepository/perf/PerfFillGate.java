package org.poc.objs.assetrepository.perf;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Boot accepts HTTP before {@link PerfDataFiller} finishes. Concurrent REST contends for the DB
 * pool/locks (especially H2) and stalls the fill — this gate blocks {@code /api/**} until ready.
 */
@Component
@Profile("perf")
public class PerfFillGate {

    private final AtomicBoolean ready = new AtomicBoolean(false);

    public boolean isReady() {
        return ready.get();
    }

    public void markReady() {
        ready.set(true);
    }
}
