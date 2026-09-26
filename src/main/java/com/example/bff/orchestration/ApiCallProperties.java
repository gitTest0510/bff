package com.example.bff.orchestration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 外部API呼び出しの設定.
 *
 * @param timeout 1回の外部API呼び出しのタイムアウト
 * @param fanOutConcurrency ファンアウト時に同時に呼び出す最大数（呼び出し先を過負荷にしないため）
 */
@ConfigurationProperties("bff.api-call")
public record ApiCallProperties(
        @DefaultValue("3s") Duration timeout, @DefaultValue("10") int fanOutConcurrency) {}
