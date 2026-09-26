package com.example.bff.integration.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 外部API_2 のレスポンス. Jackson が生成する. */
public record ApiResponse2(String summary, List<MyDetail> myDetails) {

    public ApiResponse2 {
        // 外部APIのデータは null 要素を含み得るため、List.copyOf ではなく null を許容する形で変更不可にする
        myDetails =
                Objects.isNull(myDetails)
                        ? null
                        : Collections.unmodifiableList(new ArrayList<>(myDetails));
    }

    public record MyDetail(String name, int price, String memo) {}
}
