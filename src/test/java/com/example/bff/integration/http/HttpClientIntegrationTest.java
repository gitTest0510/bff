package com.example.bff.integration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bff.testsupport.JsonFixtures;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * HTTP クライアント版で起動し、JDK 標準の HttpServer で立てた偽の外部APIに実際に HTTP で接続する結合テスト.
 *
 * <p>偽の外部APIの振る舞い:
 *
 * <ul>
 *   <li>API_1: GET /api1 → api-response1/normal.json
 *   <li>API_2: GET /api2/{id} → api-response2/normal.json（明細: main, sub, foo, main, bar）. モードで 500
 *       / 遅延を切り替える
 *   <li>API_3: POST /api3 → 明細名 foo は 404、bar は読み取りタイムアウトより遅延、それ以外は "title-{明細名}"
 * </ul>
 */
@SpringBootTest(
    properties = {
      "bff.client.type=http",
      "spring.http.clients.read-timeout=300ms",
      "bff.api-call.timeout=2s"
    })
@AutoConfigureMockMvc
class HttpClientIntegrationTest {

  private enum Api2Mode {
    NORMAL,
    ERROR,
    SLOW
  }

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Queue<String> RECEIVED = new ConcurrentLinkedQueue<>();
  private static volatile Api2Mode api2Mode = Api2Mode.NORMAL;
  private static final HttpServer SERVER = startFakeExternalApi();

  @Autowired private MockMvc mockMvc;

  @DynamicPropertySource
  static void externalApiProperties(DynamicPropertyRegistry registry) {
    String baseUrl = "http://localhost:" + SERVER.getAddress().getPort();
    registry.add("bff.client.api1.base-url", () -> baseUrl);
    registry.add("bff.client.api2.base-url", () -> baseUrl);
    registry.add("bff.client.api3.base-url", () -> baseUrl);
  }

  @AfterAll
  static void stopServer() {
    SERVER.stop(0);
  }

  @BeforeEach
  void reset() {
    api2Mode = Api2Mode.NORMAL;
    RECEIVED.clear();
  }

  @Test
  void 外部APIをHTTPで呼び出して最終レスポンスを返却する() throws Exception {
    mockMvc
        .perform(get("/example").param("no", "001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.['Apiレスポンス1']").value("res1"))
        .andExpect(jsonPath("$.['Apiレスポンス2'].summary").value("summary"))
        .andExpect(jsonPath("$.['Apiレスポンス2'].main.length()").value(2))
        .andExpect(jsonPath("$.['Apiレスポンス2'].main[0].details[0].name").value("title-main"))
        .andExpect(jsonPath("$.['Apiレスポンス2'].sub[0].details[0].name").value("title-sub"))
        // foo: 外部API_3 が 404（該当データなし） / bar: 外部API_3 が読み取りタイムアウト → どちらも details が空
        .andExpect(jsonPath("$.['Apiレスポンス2'].other[0].name").value("foo"))
        .andExpect(jsonPath("$.['Apiレスポンス2'].other[0].details").isEmpty())
        .andExpect(jsonPath("$.['Apiレスポンス2'].other[1].name").value("bar"))
        .andExpect(jsonPath("$.['Apiレスポンス2'].other[1].details").isEmpty());

    assertThat(RECEIVED)
        .contains("GET /api1?id=001", "GET /api2/001")
        // 外部API_3 は明細名の種類ごとに1回だけ呼ばれる
        .filteredOn(request -> request.startsWith("POST /api3"))
        .containsExactlyInAnyOrder(
            "POST /api3 main", "POST /api3 sub", "POST /api3 foo", "POST /api3 bar");
  }

  @Test
  void 外部API_2が5xxを返した場合は502を返却する() throws Exception {
    api2Mode = Api2Mode.ERROR;

    mockMvc.perform(get("/example").param("no", "001")).andExpect(status().isBadGateway());
  }

  @Test
  void 外部API_2がHTTPの読み取りタイムアウトになった場合は504を返却する() throws Exception {
    api2Mode = Api2Mode.SLOW;

    mockMvc.perform(get("/example").param("no", "001")).andExpect(status().isGatewayTimeout());
  }

  private static HttpServer startFakeExternalApi() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      // 遅延させる処理があっても他のリクエストを待たせないよう、仮想スレッドで並列に処理する
      server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
      server.createContext("/api1", HttpClientIntegrationTest::handleApi1);
      server.createContext("/api2/", HttpClientIntegrationTest::handleApi2);
      server.createContext("/api3", HttpClientIntegrationTest::handleApi3);
      server.start();
      return server;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void handleApi1(HttpExchange exchange) throws IOException {
    RECEIVED.add("GET /api1?" + exchange.getRequestURI().getQuery());
    respond(exchange, 200, JsonFixtures.read("api-response1/normal.json"));
  }

  private static void handleApi2(HttpExchange exchange) throws IOException {
    RECEIVED.add("GET " + exchange.getRequestURI().getPath());
    switch (api2Mode) {
      case ERROR -> respond(exchange, 500, "{}");
      case SLOW -> {
        sleep(1000);
        respond(exchange, 200, JsonFixtures.read("api-response2/normal.json"));
      }
      default -> respond(exchange, 200, JsonFixtures.read("api-response2/normal.json"));
    }
  }

  private static void handleApi3(HttpExchange exchange) throws IOException {
    String name = JSON.readTree(exchange.getRequestBody().readAllBytes()).get("name").asString();
    RECEIVED.add("POST /api3 " + name);
    switch (name) {
      case "foo" -> respond(exchange, 404, "{}");
      case "bar" -> {
        sleep(1000);
        respond(exchange, 200, "{}");
      }
      default ->
          respond(
              exchange,
              200,
              """
              {"title":"title-%s","myDetail3":{"test":"a","test2":"b","test3":"c"}}
              """
                  .formatted(name));
    }
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
