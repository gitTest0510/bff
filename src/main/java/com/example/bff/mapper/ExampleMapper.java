package com.example.bff.mapper;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.model.ExampleAggregate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/** リクエスト・外部APIの結果の変換. 外部APIは呼び出さず、入力だけから結果を決める. */
@Component
public class ExampleMapper {

  private static final String MAIN = "main";
  private static final String SUB = "sub";

  public ApiRequest toApiRequest(Request request) {
    if (Objects.isNull(request)) {
      return ApiRequest.builder().build();
    }

    return ApiRequest.builder().id(request.getNo()).build();
  }

  public ApiRequest3 toApiRequest3(String name) {
    return ApiRequest3.builder().name(name).build();
  }

  /** 集約した外部APIの結果から最終レスポンスを組み立てる. 取得できなかった部分は null / 空で返す. */
  public Response toResponse(ExampleAggregate aggregate) {
    ApiResponse1 apiResponse1 = aggregate.getApiResponse1();

    return Response.builder()
        .apiResponse1(Objects.isNull(apiResponse1) ? null : apiResponse1.test())
        .apiResponse2(toSection(aggregate))
        .build();
  }

  private Response.Section toSection(ExampleAggregate aggregate) {
    ApiResponse2 apiResponse2 = aggregate.getApiResponse2();
    List<ApiResponse2.MyDetail> myDetails =
        Objects.isNull(apiResponse2)
            ? List.of()
            : Objects.requireNonNullElse(apiResponse2.myDetails(), List.of());
    Map<String, ApiResponse3> apiResponse3ByName = aggregate.getApiResponse3ByName();

    return Response.Section.builder()
        .summary(Objects.isNull(apiResponse2) ? null : apiResponse2.summary())
        .main(toItems(myDetails, apiResponse3ByName, this::isMain))
        .sub(toItems(myDetails, apiResponse3ByName, this::isSub))
        .other(toItems(myDetails, apiResponse3ByName, this::isOther))
        .build();
  }

  private List<Response.Item> toItems(
      List<ApiResponse2.MyDetail> myDetails,
      Map<String, ApiResponse3> apiResponse3ByName,
      Predicate<String> nameCondition) {
    return myDetails.stream()
        .filter(Objects::nonNull)
        .filter(myDetail -> nameCondition.test(myDetail.name()))
        // 明細名をキーに、対応する外部API_3の結果を引いて紐付ける
        .map(myDetail -> toItem(myDetail, apiResponse3ByName.get(myDetail.name())))
        .toList();
  }

  private Response.Item toItem(ApiResponse2.MyDetail myDetail, ApiResponse3 apiResponse3) {
    return Response.Item.builder()
        .name(myDetail.name())
        .price(String.valueOf(myDetail.price()))
        .memo(myDetail.memo())
        .details(toDetails(apiResponse3))
        .build();
  }

  private List<Response.Detail> toDetails(ApiResponse3 apiResponse3) {
    if (Objects.isNull(apiResponse3) || Objects.isNull(apiResponse3.myDetail3())) {
      return List.of();
    }

    ApiResponse3.MyDetail3 myDetail3 = apiResponse3.myDetail3();
    return List.of(
        Response.Detail.builder()
            .name(apiResponse3.title())
            .test(myDetail3.test())
            .test2(myDetail3.test2())
            .test3(myDetail3.test3())
            .build());
  }

  private boolean isMain(String name) {
    return MAIN.equals(name);
  }

  private boolean isSub(String name) {
    return SUB.equals(name);
  }

  private boolean isOther(String name) {
    return Objects.nonNull(name) && !MAIN.equals(name) && !SUB.equals(name);
  }
}
