package com.myproject.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Sample REST controller.
 * All endpoints are under /api/** (Nginx strips /api prefix before forwarding).
 * Replace with your real controllers / services.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")   // allow React dev server / same-origin in prod
public class HealthController {

    /** GET /api/health → {"status":"ok"} */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "java-api");
    }

    /** Example: GET /api/hello?name=World */
    @GetMapping("/hello")
    public Map<String, String> hello(@RequestParam(defaultValue = "World") String name) {
        return Map.of("message", "Hello, " + name + "!");
    }
}
