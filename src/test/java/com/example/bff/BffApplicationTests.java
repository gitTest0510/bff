package com.example.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BffApplicationTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void contextLoads() {}

  /**
   * スタブは「どの API が、どの入力で返したか」を値に埋め込むため、最終レスポンスから依存関係どおりに紐付いたことが読み取れる.
   *
   * <ul>
   *   <li>リクエストの no=001 が API_1 / API_2 の id として渡る → {@code API_1(id=001)} / {@code API_2(id=001)}
   *   <li>API_2 の明細は明細名で main / sub / other に振り分けられる
   *   <li>各明細の details には、その明細名で呼んだ API_3 の結果が入る → main には {@code API_3(name=main)}
   * </ul>
   */
  @Test
  void スタブの外部APIを通して依存関係どおりに紐付いた最終レスポンスを返却できる() throws Exception {
    mockMvc
        .perform(get("/example").param("no", "001"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                    {
                      "Apiレスポンス1": "API_1(id=001)",
                      "Apiレスポンス2": {
                        "summary": "API_2(id=001)",
                        "main": [
                          {
                            "name": "main", "price": "100", "memo": "API_2(id=001) 明細1",
                            "details": [
                              {
                                "name": "API_3(name=main)",
                                "test": "API_3(name=main).test",
                                "test2": "API_3(name=main).test2",
                                "test3": "API_3(name=main).test3"
                              }
                            ]
                          },
                          {
                            "name": "main", "price": "300", "memo": "API_2(id=001) 明細3",
                            "details": [
                              {
                                "name": "API_3(name=main)",
                                "test": "API_3(name=main).test",
                                "test2": "API_3(name=main).test2",
                                "test3": "API_3(name=main).test3"
                              }
                            ]
                          }
                        ],
                        "sub": [
                          {
                            "name": "sub", "price": "200", "memo": "API_2(id=001) 明細2",
                            "details": [
                              {
                                "name": "API_3(name=sub)",
                                "test": "API_3(name=sub).test",
                                "test2": "API_3(name=sub).test2",
                                "test3": "API_3(name=sub).test3"
                              }
                            ]
                          }
                        ],
                        "other": [
                          {
                            "name": "other-a", "price": "400", "memo": "API_2(id=001) 明細4",
                            "details": [
                              {
                                "name": "API_3(name=other-a)",
                                "test": "API_3(name=other-a).test",
                                "test2": "API_3(name=other-a).test2",
                                "test3": "API_3(name=other-a).test3"
                              }
                            ]
                          },
                          {
                            "name": "other-b", "price": "500", "memo": "API_2(id=001) 明細5",
                            "details": [
                              {
                                "name": "API_3(name=other-b)",
                                "test": "API_3(name=other-b).test",
                                "test2": "API_3(name=other-b).test2",
                                "test3": "API_3(name=other-b).test3"
                              }
                            ]
                          }
                        ]
                      }
                    }
                    """,
                    JsonCompareMode.STRICT));
  }

  @Test
  void リクエストのnoが外部APIの入力として渡る() throws Exception {
    mockMvc
        .perform(get("/example").param("no", "999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.['Apiレスポンス1']").value("API_1(id=999)"))
        .andExpect(jsonPath("$.['Apiレスポンス2'].summary").value("API_2(id=999)"))
        .andExpect(jsonPath("$.['Apiレスポンス2'].main[0].memo").value("API_2(id=999) 明細1"));
  }

  @Test
  void POSTでもスタブの外部APIを通して同じ最終レスポンスを返却できる() throws Exception {
    String getBody =
        mockMvc
            .perform(get("/example").param("no", "001"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    mockMvc
        .perform(
            post("/example/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"no\":\"001\"}"))
        .andExpect(status().isOk())
        .andExpect(content().json(getBody, JsonCompareMode.STRICT));
  }
}
