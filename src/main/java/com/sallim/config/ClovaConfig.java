package com.sallim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClovaConfig.ClovaOcrProperties.class)
public class ClovaConfig {
    @ConfigurationProperties(prefix = "clova.ocr")
    public record ClovaOcrProperties(String secretKey, String invokeUrl) {}
}
