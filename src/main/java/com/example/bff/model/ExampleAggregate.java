package com.example.bff.model;

import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 最終レスポンスを組み立てるために必要な外部APIの結果一式.
 *
 * <p>呼び出し（Service）と変換（Mapper）の受け渡し用. 後続APIの結果は「前段のどのデータに対応するか」をキーにした Map で持ち、 Mapper
 * は前段のデータからキーで引いて紐付ける.
 *
 * @param apiResponse1 外部API_1 の結果（呼び出しに失敗した場合は null）
 * @param apiResponse2 外部API_2 の結果（呼び出しに失敗した場合はここまで来ない. 外部APIが null を返した場合のみ null）
 * @param apiResponse3ByName 外部API_2 の明細名をキーにした外部API_3 の結果（取得できなかった名前は含まない）
 */
public record ExampleAggregate(
        @Nullable ApiResponse1 apiResponse1,
        @Nullable ApiResponse2 apiResponse2,
        Map<String, ApiResponse3> apiResponse3ByName) {

    public ExampleAggregate {
        apiResponse3ByName = Map.copyOf(apiResponse3ByName);
    }
}
