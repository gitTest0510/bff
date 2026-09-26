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

    @Test
    void スタブの外部APIを通して最終レスポンスを返却できる() throws Exception {
        mockMvc.perform(get("/example").param("no", "001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['Apiレスポンス1']").value("ApiResponse1"))
                .andExpect(jsonPath("$.['Apiレスポンス2'].summary").value("ApiResponse2"))
                .andExpect(jsonPath("$.['Apiレスポンス2'].main.length()").value(3))
                .andExpect(jsonPath("$.['Apiレスポンス2'].sub.length()").value(1))
                .andExpect(jsonPath("$.['Apiレスポンス2'].other.length()").value(2))
                .andExpect(jsonPath("$.['Apiレスポンス2'].main[0].details[0].name").value("title3"));
    }

    @Test
    void POSTでもスタブの外部APIを通して同じ最終レスポンスを返却できる() throws Exception {
        String getBody =
                mockMvc.perform(get("/example").param("no", "001"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(
                        post("/example/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"no\":\"001\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(getBody, JsonCompareMode.STRICT));
    }
}
