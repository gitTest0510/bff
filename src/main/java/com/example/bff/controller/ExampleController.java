package com.example.bff.controller;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.service.MyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/example")
@RequiredArgsConstructor
public class ExampleController {

    private final MyService service;

    /**
     * 検索条件をクエリパラメータで受け取る（例: GET /example?no=001）.
     *
     * <p>Request は Spring のデータバインディングがコンストラクタ経由で生成する.
     */
    @GetMapping
    public Response get(@Valid @ModelAttribute Request request) {
        return service.execute(request);
    }

    /**
     * 検索条件を JSON ボディで受け取る（例: POST /example/search {"no":"001"}）.
     *
     * <p>検索条件が多い・URL に載せたくない値を含む場合に使う. 「作成」と区別するため /search を付けている. Request は Jackson が
     * Builder（@Jacksonized）経由で生成する.
     */
    @PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Response search(@Valid @RequestBody Request request) {
        return service.execute(request);
    }
}
