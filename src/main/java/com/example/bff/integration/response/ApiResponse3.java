package com.example.bff.integration.response;

/** 外部API_3 のレスポンス. Jackson が生成する. */
public record ApiResponse3(String title, MyDetail3 myDetail3) {

    public record MyDetail3(String test, String test2, String test3) {}
}
