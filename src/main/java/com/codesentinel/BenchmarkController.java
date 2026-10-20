package com.codesentinel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService service;

    public BenchmarkController(BenchmarkService service) {
        this.service = service;
    }

    @GetMapping("/samples")
    public List<BenchmarkService.Sample> samples() {
        return service.samples();
    }

    @GetMapping("/run")
    public BenchmarkService.Result run(@RequestParam int id,
                                       @RequestParam(required = false) String model,
                                       @RequestParam(defaultValue = "true") boolean verify) {
        return service.run(id, model, verify);
    }
}