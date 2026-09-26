package com.example.bff.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.model.DetailContext;
import java.util.Arrays;
import java.util.List;
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
    void 明細名でmain_sub_otherに振り分け外部API_3の結果をdetailsに設定する() {
        ApiResponse2 apiResponse2 = new ApiResponse2("summary", List.of());
        List<DetailContext> contexts =
                List.of(
                        context("main", 1, response3("t1")),
                        context("sub", 2, response3("t2")),
                        context("foo", 3, response3("t3")),
                        context("main", 4, null));

        Response response = mapper.toResponse(new ApiResponse1("res1"), apiResponse2, contexts);

        assertThat(response.getApiResponse1()).isEqualTo("res1");
        Response.Section section = response.getApiResponse2();
        assertThat(section.getSummary()).isEqualTo("summary");
        assertThat(section.getMain()).extracting(Response.Item::getPrice).containsExactly("1", "4");
        assertThat(section.getSub()).extracting(Response.Item::getName).containsExactly("sub");
        assertThat(section.getOther()).extracting(Response.Item::getName).containsExactly("foo");

        Response.Item first = section.getMain().getFirst();
        assertThat(first.getMemo()).isEqualTo("memo1");
        assertThat(first.getDetails())
                .singleElement()
                .satisfies(
                        detail -> {
                            assertThat(detail.getName()).isEqualTo("t1");
                            assertThat(detail.getTest()).isEqualTo("a");
                            assertThat(detail.getTest2()).isEqualTo("b");
                            assertThat(detail.getTest3()).isEqualTo("c");
                        });
        // 外部API_3の結果が無い明細は details が空
        assertThat(section.getMain().get(1).getDetails()).isEmpty();
    }

    @Test
    void 外部API_3のMyDetail3がnullの場合はdetailsが空() {
        List<DetailContext> contexts = List.of(context("main", 1, new ApiResponse3("t1", null)));

        Response response = mapper.toResponse(null, null, contexts);

        assertThat(response.getApiResponse2().getMain().getFirst().getDetails()).isEmpty();
    }

    @Test
    void 各入力がnullでも取得できた部分だけでレスポンスを返す() {
        Response response = mapper.toResponse(null, null, null);

        assertThat(response.getApiResponse1()).isNull();
        assertThat(response.getApiResponse2().getSummary()).isNull();
        assertThat(response.getApiResponse2().getMain()).isEmpty();
        assertThat(response.getApiResponse2().getSub()).isEmpty();
        assertThat(response.getApiResponse2().getOther()).isEmpty();
    }

    @Test
    void null要素やMyDetailがnull_名前がnullの明細はどこにも振り分けない() {
        List<DetailContext> contexts =
                Arrays.asList(
                        null,
                        new DetailContext(null, response3("t")),
                        context(null, 1, response3("t")));

        Response.Section section = mapper.toResponse(null, null, contexts).getApiResponse2();

        assertThat(section.getMain()).isEmpty();
        assertThat(section.getSub()).isEmpty();
        assertThat(section.getOther()).isEmpty();
    }

    private static DetailContext context(String name, int price, ApiResponse3 apiResponse3) {
        return new DetailContext(
                new ApiResponse2.MyDetail(name, price, "memo" + price), apiResponse3);
    }

    private static ApiResponse3 response3(String title) {
        return new ApiResponse3(title, new ApiResponse3.MyDetail3("a", "b", "c"));
    }
}
