package com.leyou;

import com.leyou.appraise.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <功能简述><br>
 * <>
 *
 * @author Zqh
 * @create 2020/4/9
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class LyAppraiseApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyAppraiseApplication.class, args);
    }
}
