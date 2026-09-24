package org.poc.objs.assetrepository.perf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Returns 503 for {@code /api/**} while perf noise fill is still running.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@org.springframework.context.annotation.Profile("perf")
public class PerfFillBusyFilter extends OncePerRequestFilter {

    private static final byte[] BODY = (
            "{\"error\":\"perf_fill_in_progress\","
                    + "\"detail\":\"Wait for 'Perf fill complete' (or skipped) in the application log before calling REST.\"}"
    ).getBytes(StandardCharsets.UTF_8);

    private final PerfFillGate gate;

    public PerfFillBusyFilter(PerfFillGate gate) {
        this.gate = gate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (gate.isReady()) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", "5");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(BODY);
    }
}
