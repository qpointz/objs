package io.qpointz.poc.objs.service.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Scaffold health/status endpoint for the objs service.
 */
@RestController
@RequestMapping("/api/v1/objs")
public class ObjsStatusController {

    @GetMapping("/status")
    public ObjsStatus status() {
        return new ObjsStatus("ok", "objs-service");
    }

    public record ObjsStatus(String state, String module) {
    }
}
