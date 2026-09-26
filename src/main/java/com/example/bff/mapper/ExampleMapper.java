package com.example.bff.mapper;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.model.DetailContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Component
public class ExampleMapper {

    private static final String MAIN = "main";
    private static final String SUB = "sub";

    public ApiRequest toApiRequest(Request request) {
        if (Objects.isNull(request)) {
            return ApiRequest.builder().build();
        }

        return ApiRequest.builder()
            .id(request.getNo())
            .build();
    }

    public ApiRequest3 toApiRequest3(String name) {
        return ApiRequest3.builder()
            .name(name)
            .build();
    }

    /**
     * 各外部APIの結果から最終レスポンスを組み立てる.
     * いずれかの結果が null でも、取得できた部分だけでレスポンスを返す.
     */
    public Response toResponse(
        ApiResponse1 apiResponse1,
        ApiResponse2 apiResponse2,
        List<DetailContext> detailContexts) {

        return Response.builder()
            .apiResponse1(Objects.isNull(apiResponse1) ? null : apiResponse1.getTest())
            .apiResponse2(toSection(apiResponse2, detailContexts))
            .build();
    }

    private Response.Section toSection(ApiResponse2 apiResponse2, List<DetailContext> detailContexts) {
        List<DetailContext> contexts = Objects.requireNonNullElse(detailContexts, List.of());

        return Response.Section.builder()
            .summary(Objects.isNull(apiResponse2) ? null : apiResponse2.getSummary())
            .main(toItems(contexts, this::isMain))
            .sub(toItems(contexts, this::isSub))
            .other(toItems(contexts, this::isOther))
            .build();
    }

    private List<Response.Item> toItems(List<DetailContext> contexts, Predicate<String> nameCondition) {
        return contexts.stream()
            .filter(Objects::nonNull)
            .filter(context -> Objects.nonNull(context.getMyDetail()))
            .filter(context -> nameCondition.test(context.getMyDetail().getName()))
            .map(this::toItem)
            .toList();
    }

    private Response.Item toItem(DetailContext context) {
        ApiResponse2.MyDetail myDetail = context.getMyDetail();

        return Response.Item.builder()
            .name(myDetail.getName())
            .price(String.valueOf(myDetail.getPrice()))
            .memo(myDetail.getMemo())
            .details(toDetails(context.getApiResponse3()))
            .build();
    }

    private List<Response.Detail> toDetails(ApiResponse3 apiResponse3) {
        if (Objects.isNull(apiResponse3) || Objects.isNull(apiResponse3.getMyDetail3())) {
            return List.of();
        }

        ApiResponse3.MyDetail3 myDetail3 = apiResponse3.getMyDetail3();
        return List.of(Response.Detail.builder()
            .name(apiResponse3.getTitle())
            .test(myDetail3.getTest())
            .test2(myDetail3.getTest2())
            .test3(myDetail3.getTest3())
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
