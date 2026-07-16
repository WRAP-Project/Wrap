package com.wrap.global.testapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Test API", description = "Swagger connection test API")
@RestController
@RequestMapping("/api/test")
public class TestApi {

    @Operation(summary = "Swagger test", description = "Checks whether Swagger and API routing are working.")
    @GetMapping
    public String test() {
        return "Swagger is working";
    }
}
