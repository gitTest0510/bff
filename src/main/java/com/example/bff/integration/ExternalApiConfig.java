package com.example.bff.integration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExternalApiProperties.class)
public class ExternalApiConfig {}
