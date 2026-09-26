package com.example.bff.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.model.ExampleAggregate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExampleMapperTest {

    private final ExampleMapper mapper = new ExampleMapper();

    @Test
    void RequestのnoをApiRequestのidに変換する() {
        Request request = new Request();
        request.setNo("001");

        ApiRequest apiRequest = mapper.toApiRequest(request);

        assertThat(apiRequest.getId()).isEqualTo("001");
    }

    @Test
    void Requestがnullの場合は空のApiRequestを返す() {
        assertThat(mapper.toApiRequest(null).getId()).isNull();
    }

    @Test
    void 明細名をApiRequest3に変換する() {
        assertThat(mapper.toApiRequest3("main").getName()).isEqualTo("main");
    }

    @Test
    void 明細名でmain_sub_otherに振り分け明細名をキーに外部API_3の結果を紐付ける() {
        ApiResponse2 apiResponse2 =
                new ApiResponse2(
                        "summary",
                        List.of(
                                detail("main", 1),
                                detail("sub", 2),
                                detail("foo", 3),
                                detail("main", 4),
                                detail("bar", 5)));
        Map<String, ApiResponse3> apiResponse3ByName =
                Map.of(
                        "main", response3("t-main"),
                        "sub", response3("t-sub"),
                        "foo", response3("t-foo"));

        Response response =
                mapper.toResponse(
                        ExampleAggregate.builder()
                                .apiResponse1(new ApiResponse1("res1"))
                                .apiResponse2(apiResponse2)
                                .apiResponse3ByName(apiResponse3ByName)
                                .build());

        assertThat(response.getApiResponse1()).isEqualTo("res1");
        Response.Section section = response.getApiResponse2();
        assertThat(section.getSummary()).isEqualTo("summary");
        assertThat(section.getMain()).extracting(Response.Item::getPrice).containsExactly("1", "4");
        assertThat(section.getSub()).extracting(Response.Item::getName).containsExactly("sub");
        assertThat(section.getOther())
                .extracting(Response.Item::getName)
                .containsExactly("foo", "bar");

        // 同じ明細名の明細には同じ外部API_3の結果が紐付く
        assertThat(section.getMain())
                .allSatisfy(
                        item ->
                                assertThat(item.getDetails())
                                        .singleElement()
                                        .satisfies(
                                                detail -> {
                                                    assertThat(detail.getName())
                                                            .isEqualTo("t-main");
                                                    assertThat(detail.getTest()).isEqualTo("a");
                                                    assertThat(detail.getTest2()).isEqualTo("b");
                                                    assertThat(detail.getTest3()).isEqualTo("c");
                                                }));
        assertThat(section.getMain().getFirst().getMemo()).isEqualTo("memo1");
        // 外部API_3の結果が無い明細は details が空
        assertThat(section.getOther().get(1).getDetails()).isEmpty();
    }

    @Test
    void 外部API_3のMyDetail3がnullの場合はdetailsが空() {
        ApiResponse2 apiResponse2 = new ApiResponse2("summary", List.of(detail("main", 1)));

        Response response =
                mapper.toResponse(
                        ExampleAggregate.builder()
                                .apiResponse2(apiResponse2)
                                .apiResponse3ByName(Map.of("main", new ApiResponse3("t", null)))
                                .build());

        assertThat(response.getApiResponse2().getMain().getFirst().getDetails()).isEmpty();
    }

    @Test
    void 外部APIの結果がnullでも取得できた部分だけでレスポンスを返す() {
        Response response = mapper.toResponse(ExampleAggregate.builder().build());

        assertThat(response.getApiResponse1()).isNull();
        assertThat(response.getApiResponse2().getSummary()).isNull();
        assertThat(response.getApiResponse2().getMain()).isEmpty();
        assertThat(response.getApiResponse2().getSub()).isEmpty();
        assertThat(response.getApiResponse2().getOther()).isEmpty();
    }

    @Test
    void 明細リストがnullの場合は空のリストを返す() {
        Response response =
                mapper.toResponse(
                        ExampleAggregate.builder()
                                .apiResponse2(new ApiResponse2("s", null))
                                .build());

        assertThat(response.getApiResponse2().getSummary()).isEqualTo("s");
        assertThat(response.getApiResponse2().getMain()).isEmpty();
    }

    @Test
    void null要素や名前がnullの明細はどこにも振り分けない() {
        ApiResponse2 apiResponse2 =
                new ApiResponse2("summary", Arrays.asList(null, detail(null, 1)));

        Response.Section section =
                mapper.toResponse(ExampleAggregate.builder().apiResponse2(apiResponse2).build())
                        .getApiResponse2();

        assertThat(section.getMain()).isEmpty();
        assertThat(section.getSub()).isEmpty();
        assertThat(section.getOther()).isEmpty();
    }

    private static ApiResponse2.MyDetail detail(String name, int price) {
        return new ApiResponse2.MyDetail(name, price, "memo" + price);
    }

    private static ApiResponse3 response3(String title) {
        return new ApiResponse3(title, new ApiResponse3.MyDetail3("a", "b", "c"));
    }
}
