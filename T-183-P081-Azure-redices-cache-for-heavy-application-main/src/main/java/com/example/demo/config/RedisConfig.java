package com.example.demo.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.AzureTokenAuthConfigBuilder;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.authentication.AuthXManager;

import java.util.Set;

@Configuration
public class RedisConfig {

    private static final String REDIS_HOST =
            "catalogueredis260926.eastus.redis.azure.net";

    private static final int REDIS_PORT = 10000;

    private static final String REDIS_SCOPE =
            "https://redis.azure.com/.default";

    @Bean
    public UnifiedJedis redisClient() {

        TokenAuthConfig tokenAuthConfig =
                AzureTokenAuthConfigBuilder.builder()
                        .defaultAzureCredential(
                                new DefaultAzureCredentialBuilder().build()
                        )
                        .scopes(Set.of(REDIS_SCOPE))
                        .tokenRequestExecTimeoutInMs(2000)
                        .build();

        DefaultJedisClientConfig config =
                DefaultJedisClientConfig.builder()
                        .authXManager(
                                new AuthXManager(tokenAuthConfig)
                        )
                        .ssl(true)
                        .build();

        return new UnifiedJedis(
                new HostAndPort(
                        REDIS_HOST,
                        REDIS_PORT
                ),
                config
        );
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}