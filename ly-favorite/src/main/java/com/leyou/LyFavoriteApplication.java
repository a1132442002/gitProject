package com.leyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <功能简述><br>
 * <>
 *
 * @author Zqh
 * @create 2020/4/6
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class LyFavoriteApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyFavoriteApplication.class, args);
    }
}
