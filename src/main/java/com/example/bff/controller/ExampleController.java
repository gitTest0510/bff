package com.example.bff.controller;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.service.MyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/example")
@RequiredArgsConstructor
public class ExampleController {

    private final MyService service;

    @GetMapping("/")
    public Response test(@RequestBody Request request) {
        return service.execute(request);
    }
}
