package com.example.bff.controller.request;

import jakarta.validation.constraints.NotBlank;

/**
 * リクエストパラメータ. Spring のデータバインディングがコンストラクタ経由で生成する.
 *
 * @param no No.
 */
public record Request(@NotBlank String no) {}
