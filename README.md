# bff

画面向けの BFF（Backend for Frontend）のサンプルです。

画面からのリクエストを受け取り、複数の外部API（API_1〜3）を依存関係どおりに呼び出して、結果を1つのレスポンスにまとめて返します。

- 外部APIは API 毎に「スタブ（固定のダミー値）」と「HTTP（実際の呼び出し）」を切り替えられます。外部APIが無い環境でも起動・動作確認ができます。
- 独立した外部APIは仮想スレッドで並列に呼び出します。

## 目次

- [技術スタック](#技術スタック)
- [クイックスタート](#クイックスタート)
- [エンドポイント](#エンドポイント)
- [処理の流れ](#処理の流れ)
- [ディレクトリ構成](#ディレクトリ構成)
- [クラスの役割](#クラスの役割)
- [設定](#設定)
- [スタブと HTTP の切り替え](#スタブと-http-の切り替え)
- [ログ](#ログ)
- [テスト](#テスト)
- [設計方針・約束事](#設計方針約束事)
- [外部APIを追加する手順](#外部apiを追加する手順)
- [注意点](#注意点)

## 技術スタック

| 種類 | 内容 |
|---|---|
| 言語 | Java 25 |
| フレームワーク | Spring Boot 4.1（Spring MVC / RestClient / Bean Validation） |
| JSON | Jackson 3（`tools.jackson`） |
| その他 | Lombok、仮想スレッド |
| 品質チェック | Spotless（google-java-format）、Checkstyle、SpotBugs、JaCoCo（行カバレッジ 80% 以上） |

## クイックスタート

```bash
# 起動（既定では外部APIはすべてスタブ）
./mvnw spring-boot:run

# 呼び出し
curl "http://localhost:8080/example?no=001"
curl -X POST http://localhost:8080/example/search -H "Content-Type: application/json" -d '{"no":"001"}'
```

| やりたいこと | コマンド |
|---|---|
| テストだけ実行 | `./mvnw test` |
| フォーマットを整える | `./mvnw spotless:apply` |
| すべてのチェック（フォーマット・Checkstyle・テスト・SpotBugs・カバレッジ） | `./mvnw verify` |

- フォーマット違反は `verify` の最初（validate フェーズ）で失敗します。コミット前に `spotless:apply` を実行してください。
- クラス名を変えたときは `./mvnw clean test` を実行してください。`target` に古いクラスファイルが残っていると、テスト結果が正しくなりません。

## エンドポイント

| メソッド | パス | 入力 | 用途 |
|---|---|---|---|
| GET | `/example` | クエリパラメータ `no`（必須） | 通常の検索 |
| POST | `/example/search` | JSON ボディ `{"no": "..."}` | 検索条件が多い場合や、URL に載せたくない値を含む場合 |

どちらも同じ処理を通り、同じレスポンスを返します。

### レスポンス例（スタブ構成、`no=001`）

スタブは「どの API が、どの入力で返したか」を値に埋め込んで返します。そのため、最終レスポンスを見るだけで紐付きを確認できます（[テスト](#テスト)を参照）。

```json
{
  "Apiレスポンス1": "API_1(id=001)",
  "Apiレスポンス2": {
    "summary": "API_2(id=001)",
    "main": [
      {
        "name": "main", "price": "100", "memo": "API_2(id=001) 明細1",
        "details": [
          { "name": "API_3(name=main)", "test": "API_3(name=main).test", "test2": "...", "test3": "..." }
        ]
      },
      { "name": "main", "price": "300", "memo": "API_2(id=001) 明細3", "details": [ "（main と同じ API_3 の結果）" ] }
    ],
    "sub":   [ { "name": "sub", "...": "...", "details": [ { "name": "API_3(name=sub)", "...": "..." } ] } ],
    "other": [ { "name": "other-a", "...": "..." }, { "name": "other-b", "...": "..." } ]
  }
}
```

### エラーレスポンス

エラーは RFC 9457（Problem Details）形式で返します。

| ステータス | 条件 |
|---|---|
| 400 | 入力チェックエラー（`no` が空など） |
| 502 | 欠かせない外部API（API_2）の呼び出しに失敗した |
| 504 | 欠かせない外部API（API_2）の呼び出しがタイムアウトした |
| 500 | 予期しないエラー |

外部APIの詳細（どこで何が起きたか）はログにだけ出し、レスポンスには含めません。

## 処理の流れ

```
画面
 │  GET /example?no=001
 ▼
ExampleController ── Request を受け取り、入力チェック
 │
 ▼
MyService ── 外部APIを依存関係どおりに呼び出す
 │   ├─ ExampleMapper.toApiRequest      : Request → 外部APIのリクエスト
 │   ├─ ApiCaller                       : 非同期実行・タイムアウト・失敗時の扱い・ログ
 │   │    └─ Api1Client / Api2Client / Api3Client（スタブ or HTTP）
 │   └─ 結果を ExampleAggregate に集約
 │
 ▼
ExampleMapper.toResponse ── 集約した結果だけから最終レスポンスを組み立てる
 │
 ▼
Response（JSON）
```

### 外部APIの依存関係

```
API_1 ─────────────────────────────┐
API_2 ──→ API_3 × 明細名の種類数 ───┴──→ ExampleAggregate → Response
```

- API_1 と API_2 は互いに独立しているので、並列に呼び出します。
- API_3 は API_2 の明細名を入力にするので、API_2 の完了後に呼び出します。
  - 明細名の種類ごとに並列で呼び出します（`fanOut`）。
  - 同じ明細名は1回しか呼び出しません。

### 外部APIの結果が得られなかったときの扱い

| API | 扱い | 使う `ApiCaller` のメソッド |
|---|---|---|
| API_1 | 無くても返せる。`Apiレスポンス1` を null にして続行する | `callOrEmpty` |
| API_2 | 欠かせない。レスポンスの骨格になるので、処理全体をエラー（502 / 504）にする | `callOrFail` |
| API_3 | 無くても返せる。取得できなかった明細の `details` を空にして続行する | `fanOut` |

## ディレクトリ構成

```
bff/
├── pom.xml                         依存関係・ビルド・品質チェックの設定
├── lombok.config                   Lombok の設定（Jackson 3 対応、@Generated 付与）
├── .editorconfig
├── config/
│   ├── checkstyle/checkstyle.xml   Checkstyle のルール
│   └── spotbugs/spotbugs-exclude.xml  SpotBugs の除外設定
└── src/
    ├── main/
    │   ├── java/com/example/bff/
    │   │   ├── BffApplication.java     起動クラス
    │   │   ├── controller/             画面との入出力（HTTP の受け口・エラーレスポンス）
    │   │   │   ├── request/            画面から受け取るリクエスト DTO
    │   │   │   └── response/           画面に返すレスポンス DTO
    │   │   ├── service/                処理の流れ（どの外部APIをどの順で呼ぶか）と外部APIクライアントのインターフェース
    │   │   ├── orchestration/          外部API呼び出しの実行基盤（並列実行・タイムアウト・ログ）
    │   │   ├── integration/            外部APIとの接続（スタブ / HTTP の実装と設定）
    │   │   │   ├── request/            外部APIに送るリクエスト DTO
    │   │   │   ├── response/           外部APIから受け取るレスポンス DTO
    │   │   │   ├── stub/               スタブ実装
    │   │   │   └── http/               HTTP 実装
    │   │   ├── mapper/                 DTO 間の変換
    │   │   ├── model/                  外部APIの結果を集約するモデル
    │   │   └── exception/              例外
    │   └── resources/
    │       └── application.properties
    └── test/
        ├── java/com/example/bff/       main と同じパッケージ構成でテストを置く
        │   └── testsupport/            テスト用の共通部品
        └── resources/fixtures/         外部APIレスポンスのテスト用 JSON
```

## クラスの役割

### controller（画面との入出力）

| クラス | 役割 |
|---|---|
| `ExampleController` | `GET /example` と `POST /example/search` の受け口。入力チェック（`@Valid`）をして `MyService` に渡す |
| `GlobalExceptionHandler` | 例外を Problem Details 形式のエラーレスポンスに変換する。`ExternalApiException` はタイムアウトなら 504、それ以外は 502 |
| `request/Request` | 画面から受け取るリクエスト（`no`） |
| `response/Response` | 画面に返すレスポンス。JSON のキー（`Apiレスポンス1` など）と項目の順序もここで決める |

### service（処理の流れ）

| クラス | 役割 |
|---|---|
| `MyService` | 外部APIを依存関係どおりに呼び出し、結果を `ExampleAggregate` に集約して、`ExampleMapper` でレスポンスに変換する |
| `Api1Client` / `Api2Client` / `Api3Client` | 外部APIクライアントのインターフェース。実装（スタブ / HTTP）は `integration` にある |

インターフェースを `service` に置いているのは、`MyService` が実装（スタブか HTTP か）を知らずに済むようにするためです。

### orchestration（外部API呼び出しの実行基盤）

| クラス | 役割 |
|---|---|
| `ApiCaller` | 外部APIを非同期（仮想スレッド）で呼び出す部品。タイムアウト、失敗時の扱い、呼び出しごとのログを担当する |
| `ApiCallConfig` | `ApiCaller` を Bean として登録する。仮想スレッドの Executor は `ApiCaller` の中に閉じ込める |
| `ApiCallProperties` | `bff.api-call.*` の設定（タイムアウト、fanOut の同時呼び出し数） |

`ApiCaller` の主なメソッド:

| メソッド | 用途 |
|---|---|
| `callOrFail` | 結果が欠かせない API を呼ぶ。失敗したら `ExternalApiException` で処理全体を失敗させる |
| `callOrEmpty` | 結果が無くても返せる API を呼ぶ。失敗したら `Optional.empty()` で続行する |
| `fanOut` | キー（例: 明細名）ごとに並列で呼ぶ。重複したキーは1回だけ呼び、同時呼び出し数を制限する |
| `awaitAll` | すべての完了を待つ。どれかが失敗した時点で、その例外を送出する |

呼び出し順序（依存関係）は `CompletableFuture` の合成で表します。後段の API は、前段の Future に `thenCompose` でつなぎます。

### integration（外部APIとの接続）

| クラス | 役割 |
|---|---|
| `ExternalApiMode` | 実装の切り替えに使うプロパティ名（`bff.external-api.apiN.mode`）と値（`stub` / `http`）の定数 |
| `ExternalApiProperties` | `bff.external-api.*` の設定（API 毎のモードと接続先）。`mode=http` なのに `base-url` が無い場合などは、起動時にエラーにする |
| `ExternalApiConfig` | `ExternalApiProperties` を Bean として登録する。**本番でも必要**（削除すると HTTP 版が作れず起動できない） |
| `stub/Api{1,2,3}StubClient` | スタブ実装。受け取った入力を埋め込んだダミー値を返す |
| `http/Api{1,2,3}HttpClient` | HTTP 実装（`RestClient`）。4xx / 5xx・通信エラー・タイムアウトは例外にする（API_3 の 404 だけは「該当なし」として null を返す） |
| `request/ApiRequest` | API_1 / API_2 のリクエスト（`id`） |
| `request/ApiRequest3` | API_3 のリクエスト（API_2 の明細名 `name`） |
| `response/ApiResponse{1,2,3}` | 外部APIのレスポンス（record） |

HTTP 版の呼び出し先:

| API | リクエスト |
|---|---|
| API_1 | `GET {base-url}/api1?id={id}` |
| API_2 | `GET {base-url}/api2/{id}` |
| API_3 | `POST {base-url}/api3`（JSON ボディ `{"name": "..."}`） |

### mapper / model / exception

| クラス | 役割 |
|---|---|
| `mapper/ExampleMapper` | `Request` → 外部APIのリクエスト、集約結果 → `Response` の変換。外部APIは呼ばず、入力だけから結果を決める。明細名で main / sub / other に振り分け、明細名をキーに API_3 の結果を紐付ける |
| `model/ExampleAggregate` | 最終レスポンスの組み立てに必要な外部APIの結果一式。`MyService` と `ExampleMapper` の受け渡しに使う。後段 API の結果は「前段のどのデータに対応するか」をキーにした Map で持つ |
| `exception/ExternalApiException` | 欠かせない外部APIの結果が得られなかったことを表す例外。`isTimeout()` で、原因がタイムアウトかどうかを判定する（HTTP の接続・読み取りタイムアウトも含む） |

## 設定

`src/main/resources/application.properties`

| プロパティ | 既定値 | 説明 |
|---|---|---|
| `bff.api-call.timeout` | `3s` | 1回の外部API呼び出しのタイムアウト（`ApiCaller` が打ち切る） |
| `bff.api-call.fan-out-concurrency` | `10` | `fanOut` で同時に呼び出す最大数。呼び出し先を過負荷にしないため |
| `spring.http.clients.connect-timeout` | `1s` | HTTP 版の接続タイムアウト |
| `spring.http.clients.read-timeout` | `2s` | HTTP 版の読み取りタイムアウト |
| `bff.external-api.apiN.mode` | `stub` | API_N の実装。`stub` または `http`（N = 1〜3） |
| `bff.external-api.apiN.base-url` | なし | API_N の接続先。`mode=http` のときだけ必須 |

## スタブと HTTP の切り替え

API 毎に `mode` を指定します。

```properties
bff.external-api.api1.mode=http
bff.external-api.api1.base-url=https://api1.example.com
bff.external-api.api2.mode=stub        # 指定しなければスタブ
bff.external-api.api3.mode=http
bff.external-api.api3.base-url=https://api3.example.com
```

起動引数や環境変数でも上書きできます（例: `--bff.external-api.api1.mode=http`）。

### 仕組み

スタブ実装と HTTP 実装の両方に `@Service` が付いています。そのうえで `@ConditionalOnProperty` によって、条件に一致した方だけが Bean として登録されます。

```java
@ConditionalOnProperty(name = ExternalApiMode.API1, havingValue = ExternalApiMode.STUB, matchIfMissing = true)
public class Api1StubClient implements Api1Client { ... }   // api1.mode が stub または未指定のとき登録

@ConditionalOnProperty(name = ExternalApiMode.API1, havingValue = ExternalApiMode.HTTP)
public class Api1HttpClient implements Api1Client { ... }   // api1.mode が http のとき登録
```

`MyService` はインターフェース `Api1Client` を受け取るだけなので、登録された方が注入されます。条件は必ずどちらか一方だけが成立するように作ってあります。

- 両方が登録されると、候補が2つになって起動エラーになります（`NoUniqueBeanDefinitionException`）。
- どちらも登録されないと、Bean が無くて起動エラーになります（例: `mode=` と空文字を指定した場合）。

## ログ

外部APIを呼ぶたびに、`ApiCaller` が1行ログを出します。スタブ / HTTP のどちらでも出ます。

```
INFO 外部API呼び出し api=API_1 result=成功 elapsed=4ms
INFO 外部API呼び出し api=API_3 key=main result=成功 elapsed=2ms
WARN 外部API呼び出し api=API_3 key=bar result=失敗(タイムアウト) elapsed=2001ms cause=java.util.concurrent.TimeoutException
```

| 項目 | 内容 |
|---|---|
| `api` | 呼び出した API |
| `key` | `fanOut` のときだけ出る。どのデータ（明細名）に対する呼び出しか |
| `result` | `成功` / `成功(結果なし)` は INFO、`失敗` / `失敗(タイムアウト)` は WARN |
| `elapsed` | 呼び出し開始から完了（またはタイムアウト）までの時間。`fanOut` では同時呼び出し数の制限による待ち時間も含む |
| `cause` | 失敗時の原因の要約 |

- スタックトレースは、呼び出し元の扱いを出すログに任せています。`callOrEmpty` の「結果なしで続行します」の WARN と、処理全体がエラーになったときの `GlobalExceptionHandler` の ERROR です。
- ログが多すぎる場合は `logging.level.com.example.bff.orchestration.ApiCaller=WARN` にすると、失敗時だけ出ます。

## テスト

| テスト | 種類 | 確認していること |
|---|---|---|
| `BffApplicationTests` | 全体（`@SpringBootTest`、スタブ構成） | 最終レスポンスを JSON 全体で比べ、依存関係どおりに紐付いていること |
| `HttpClientIntegrationTest` | 全体（HTTP 構成） | JDK の `HttpServer` で立てた偽の外部APIに実際に HTTP で接続し、404 / 5xx / タイムアウト時の扱いを含めて確認する |
| `ExternalApiModeSwitchingTest` | 全体 | API 毎に指定した実装（スタブ / HTTP）が注入されること |
| `ExternalApiPropertiesTest` | 設定 | 既定値と、設定ミスが起動エラーになること |
| `Api{1,2,3}HttpClientTest` | `@RestClientTest` | HTTP クライアントが送るリクエストと、レスポンスの変換 |
| `ExampleControllerTest` | `@WebMvcTest` | 入力チェックとエラーレスポンス |
| `MyServiceTest` | 単体（モック） | 呼び出し順序と失敗時の扱い |
| `ApiCallerTest` | 単体 | 並列実行・タイムアウト・fanOut・ログ |
| `ExampleMapperTest` など | 単体 | 変換・DTO の不変性 |

### テスト用 JSON（fixtures）

- 外部APIのレスポンスは、`src/test/resources/fixtures/<api>/<ケース>.json` に実際に近い JSON を置いて使います。
- 読み込みには `JsonFixtures.load(path, 型)` を使います。モックサーバーのレスポンス本文に使うときは `JsonFixtures.read(path)` です。
- null 要素などの境界値のケースは、テストコードの中で個別に組み立てます。

## 設計方針・約束事

### 層ごとの責務

- **Controller:** 入出力と入力チェックだけを担当します。処理は `MyService` に任せます。
- **Service:** 「どの外部APIを、どの順で、失敗したらどう扱うか」だけを担当します。変換は `ExampleMapper` に任せます。
- **Mapper:** 外部APIを呼ばず、受け取った入力だけから結果を決めます。そのため単体でテストできます。
- **ApiCaller:** 非同期実行・タイムアウト・ログなど、呼び出しの共通処理をまとめます。個々の API のことは知りません。

### DTO はすべて不変（イミュータブル）

外部APIの結果を仮想スレッド間で受け渡すため、DTO はすべて不変にしています。作り方は分類ごとに決めています。

| 分類 | 作り方 | 例 |
|---|---|---|
| 画面からのリクエスト | `@Value` + `@Builder` + `@Jacksonized` + private コンストラクタ | `Request` |
| 自前で生成するもの（外部APIのリクエスト、画面へのレスポンス、集約モデル） | `@Value` + `@Builder` + private コンストラクタ | `ApiRequest`、`Response`、`ExampleAggregate` |
| 外部APIのレスポンス | record（本番で生成するのは Jackson だけ） | `ApiResponse1`〜`3` |

- **画面からのリクエストを record にしない理由:** 項目数が多いリクエストでも同じ作りにするためです。
- **`@Jacksonized` が必要なのは JSON ボディ（`@RequestBody`）で受ける場合だけ:** Jackson 3 は private コンストラクタしか無いクラスを生成できないためです。
- **List 項目の扱い:** `@Singular` を使うか、コンストラクタの中で変更不可のコピーにします。
  - 外部APIのリストは null 要素を含むことがあります。その場合は `List.copyOf` ではなく `Collections.unmodifiableList(new ArrayList<>(...))` を使います。

### コーディング規約

- フォーマットは Spotless（google-java-format、GOOGLE スタイル = インデント2スペース）に従います。
- Checkstyle と SpotBugs の違反があると、ビルドが失敗します。
- テストの行カバレッジは 80% 以上が必要です（JaCoCo）。

## 外部APIを追加する手順

例として API_4 を追加する場合の手順です。

1. **DTO**
   - `integration/response/ApiResponse4`（record）を作ります。
   - 必要なら `integration/request` にリクエスト DTO も作ります。
2. **インターフェース**
   - `service/Api4Client` を作ります。
3. **実装**
   - `integration/stub/Api4StubClient` と `integration/http/Api4HttpClient` を作ります。
   - `@ConditionalOnProperty` をスタブ側（`matchIfMissing = true` あり）と HTTP 側の両方に付けます。
4. **設定**
   - `ExternalApiMode.API4` を追加します。
   - `ExternalApiProperties` に `api4` を追加します。
   - `application.properties` に `bff.external-api.api4.mode` を追記します。
5. **呼び出し**
   - `MyService` で `ApiCaller` を通して呼び出します。結果が欠かせないかどうかで、`callOrFail` / `callOrEmpty` / `fanOut` を選びます。
   - `ExampleAggregate`・`ExampleMapper`・`Response` に結果を追加します。
6. **テスト**
   - fixtures JSON、`Api4HttpClientTest`、`HttpClientIntegrationTest` の偽APIを追加・更新します。
   - `BffApplicationTests` の期待値も更新します。

## 注意点

- **タイムアウトの関係**
  - `spring.http.clients.read-timeout`（HTTP の読み取り）は、`bff.api-call.timeout`（`ApiCaller`）より短くしてください。
  - `ApiCaller` のタイムアウトは結果を待つのをやめるだけで、裏で動いている通信は止めません。実際の通信を打ち切るのは HTTP 側のタイムアウトです。
- **`awaitAll` は失敗しても他の呼び出しをキャンセルしない**
  - どれかが失敗するとすぐにエラーを返しますが、ほかの呼び出しはバックグラウンドで最後まで実行されます。
- **プロパティ名の変更履歴**
  - 切り替え用のプロパティは `bff.client.type`（全API一括）→ `bff.client.apiN.type` → `bff.external-api.apiN.mode` と変わってきました。
  - 古い名前を環境変数や起動引数で指定していると無視されます（スタブのまま動きます）。
- **`@SpringBootTest` で「未指定」の状態は作れない**
  - `application.properties` に `mode=stub` が書いてあるためです。
  - 未指定時の既定値を確かめるテストは、`ApplicationContextRunner`（`ExternalApiPropertiesTest`）で書きます。
