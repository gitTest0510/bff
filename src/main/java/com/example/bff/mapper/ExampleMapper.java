package com.example.bff.mapper;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.model.DetailContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ExampleMapper {

    public ApiRequest toApiRequest(Request request) {
        if (Objects.isNull(request)) {
            return ApiRequest.builder().build();
        }

        return ApiRequest.builder()
            .id(request.getNo())
            .build();
    }

    public ApiRequest3 toApiRequest3(String apiResponse2_name) {
        return ApiRequest3.builder()
            .apiResponse2_request_name(apiResponse2_name)
            .build();
    }

    public Response toResponse(ApiResponse1 apiResponse1, List<DetailContext> apiResponse2with3) {
        if (Objects.isNull(apiResponse1)) {
            return Response.builder().build();
        }

        return Response.builder()
            .Apiレスポンス1(apiResponse1.getTest())
            .Apiレスポンス2(toApiResponse2with3(apiResponse2with3))
            .build();
    }

    private Response.Apiレスポンス2 toApiResponse2with3(List<DetailContext> apiResponse2with3) {
        if (Objects.isNull(apiResponse2with3)) {
            return Response.Apiレスポンス2.builder().build();
        }

        return Response.Apiレスポンス2.builder()
            .summary(apiResponse2with3.getFirst().getApiResponse2().getSummary())
            .main(toMain(apiResponse2.getMyDetails()))
            .sub(toSub(apiResponse2.getMyDetails()))
            .other(toOther(apiResponse2.getMyDetails()))
            .build();
    }

    private List<Response.Apiレスポンス2.Details> toDetails(
        List<DetailContext> detailContexts) {

        return detailContexts.stream()
            .map(DetailContext::getApiResponse3)
            .filter(Objects::nonNull)
            .map(this::toDetail)
            .toList();
    }

    private Response.Apiレスポンス2.Details toDetail(ApiResponse3 apiResponse3) {
        return Response.Apiレスポンス2.Details.builder()
            .name()
            .test(apiResponse3.getMyDetail3().getTest())
            .test2(apiResponse3.getMyDetail3().getTest2())
            .test3(apiResponse3.getMyDetail3().getTest3())
            .build();
    }

    private List<Response.Apiレスポンス2.MainClazz> toMain(List<ApiResponse2.MyDetail> myDetailList) {
        return myDetailList.stream()
            .filter(this::isMain)
            .map(record -> Response.Apiレスポンス2.MainClazz.builder()
                .name(record.getName())
                .price(String.valueOf(record.getPrice()))
                .memo(record.getMemo())
                .build()
            ).toList();
    }

    private List<Response.Apiレスポンス2.SubClazz> toSub(List<ApiResponse2.MyDetail> myDetailList) {
        return myDetailList.stream()
            .filter(this::isSub)
            .map(record -> Response.Apiレスポンス2.SubClazz.builder()
                .name(record.getName())
                .price(String.valueOf(record.getPrice()))
                .memo(record.getMemo())
                .build()
            ).toList();
    }

    private List<Response.Apiレスポンス2.OtherClazz> toOther(List<ApiResponse2.MyDetail> myDetailList) {
        return myDetailList.stream()
            .filter(this::isOther)
            .map(record -> Response.Apiレスポンス2.OtherClazz.builder()
                .name(record.getName())
                .price(String.valueOf(record.getPrice()))
                .memo(record.getMemo())
                .build())
            .toList();
    }

    private boolean isMain(ApiResponse2.MyDetail myDetail) {
        return Objects.nonNull(myDetail) && "main".equals(myDetail.getName());
    }

    private boolean isSub(ApiResponse2.MyDetail myDetail) {
        return Objects.nonNull(myDetail) && "sub".equals(myDetail.getName());
    }

    private boolean isOther(ApiResponse2.MyDetail myDetail) {
        return Objects.nonNull(myDetail)
            && Objects.nonNull(myDetail.getName())
            && !Set.of("main", "sub").contains(myDetail.getName());
    }
}


