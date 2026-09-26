package com.example.bff.integration.http;

import com.example.bff.integration.ClientType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** HTTP クライアント版の設定. 接続先の設定は HTTP 版を使うときだけ読み込み・検証する. */
@Configuration
@ConditionalOnProperty(name = ClientType.PROPERTY, havingValue = ClientType.HTTP)
@EnableConfigurationProperties(ExternalApiProperties.class)
public class HttpClientConfig {}
