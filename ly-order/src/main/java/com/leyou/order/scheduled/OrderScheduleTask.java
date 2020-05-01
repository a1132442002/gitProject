package com.leyou.order.scheduled;


import com.leyou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * Author :Wlz
 * Date   :2020-04-06 16:07.
 */
@Component
@Slf4j
public class OrderScheduleTask {

    @Autowired
    private OrderService orderService;


    @Scheduled(cron = "0 0/20 * * * ?")
    public void configTasks() {
        // 调用定时清理失效订单
        log.info("【调用定时清理失效订单开始】");
        orderService.ClearanceOrder();
        log.info("【调用定时清理失效订单结束】");

    }
}
