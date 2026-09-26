package com.example.bff.model;

import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * 最終レスポンスを組み立てるために必要な外部APIの結果一式.
 *
 * <p>呼び出し（Service）と変換（Mapper）の受け渡し用. 後続APIの結果は「前段のどのデータに対応するか」をキーにした Map で持ち、 Mapper
 * は前段のデータからキーで引いて紐付ける.
 *
 * <p>イミュータブル. 生成は {@link #builder()} からのみ行う（コンストラクタは private）.
 */
@Value
public class ExampleAggregate {

    /** 外部API_1 の結果（呼び出しに失敗した場合は null）. */
    @Nullable ApiResponse1 apiResponse1;

    /** 外部API_2 の結果（呼び出しに失敗した場合はここまで来ない. 外部APIが null を返した場合のみ null）. */
    @Nullable ApiResponse2 apiResponse2;

    /** 外部API_2 の明細名をキーにした外部API_3 の結果（取得できなかった名前は含まない）. */
    Map<String, ApiResponse3> apiResponse3ByName;

    @Builder
    private ExampleAggregate(
            @Nullable ApiResponse1 apiResponse1,
            @Nullable ApiResponse2 apiResponse2,
            @Nullable Map<String, ApiResponse3> apiResponse3ByName) {
        this.apiResponse1 = apiResponse1;
        this.apiResponse2 = apiResponse2;
        // 未設定なら空、設定されていれば変更不可のコピーを保持する
        this.apiResponse3ByName =
                Map.copyOf(Objects.requireNonNullElse(apiResponse3ByName, Map.of()));
    }
}
