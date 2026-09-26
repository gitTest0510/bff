package com.example.bff.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * 画面から受け取るリクエスト.
 *
 * <p>イミュータブル. 実行時は Spring のデータバインディングが唯一のコンストラクタ経由で生成し、テスト等で自分で組み立てる場合は {@link #builder()} を使う.
 * 項目数の多いリクエストでも同じ作りにするため、record ではなくクラスにしている.
 */
@Value
@Builder
@Jacksonized
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Request {

    /** No. */
    @NotBlank String no;
}
