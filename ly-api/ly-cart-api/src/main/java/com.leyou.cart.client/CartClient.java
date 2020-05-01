package com.leyou.cart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("cart-service")
public interface CartClient {
    /**
     * 根据skuId删除单个购物车
     *
     * @param skuId
     * @return
     */
    @DeleteMapping("/{id}")
    public Void deleteCart(@PathVariable("id") Long skuId);
}