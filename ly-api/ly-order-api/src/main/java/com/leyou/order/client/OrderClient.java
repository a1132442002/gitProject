package com.leyou.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient("order-service")
public interface OrderClient {
    @PutMapping("/order/state/update")
    public Void updateOrderStatus(@RequestParam("orderId") Long orderId, @RequestParam("status") Integer status);
}
