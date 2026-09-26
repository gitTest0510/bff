package com.example.bff.controller;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.service.MyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/example")
@RequiredArgsConstructor
public class ExampleController {

    private final MyService service;

    /** 参照系のため GET とし、検索条件はクエリパラメータで受け取る（例: GET /example?no=001）. */
    @GetMapping
    public Response get(@Valid @ModelAttribute Request request) {
        return service.execute(request);
    }
}
