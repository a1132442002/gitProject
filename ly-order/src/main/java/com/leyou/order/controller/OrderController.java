package com.leyou.order.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.dto.OrderVO;
import com.leyou.order.entity.OrderBacklog;
import com.leyou.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 下单
     */
    @PostMapping("/order")
    public ResponseEntity<Long> buildOrder(@RequestBody OrderDTO orderDTO) {
        Long orderId = orderService.buildOrder(orderDTO);
        return ResponseEntity.ok(orderId);
    }

    /**
     * 订单查询
     */
    @GetMapping("/order/{id}")
    public ResponseEntity<OrderVO> findOrderById(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.findOrderById(id);
        return ResponseEntity.ok(orderVO);
    }

    /**
     * 生成支付链接
     */
    @GetMapping("/order/url/{id}")
    public ResponseEntity<String> getPayUrl(@PathVariable("id") Long id) {
        String payUrl = orderService.getPayUrl(id);
        return ResponseEntity.ok(payUrl);
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/order/state/{id}")
    public ResponseEntity<Integer> queryOrderStatus(@PathVariable("id") Long id) {
        Integer orderStatus = orderService.queryOrderStatus(id);
        return ResponseEntity.ok(orderStatus);
    }

    @GetMapping("/order/canAppraise")
    public ResponseEntity<OrderVO> canAppraise(@RequestParam("orderId") Long orderId, @RequestParam("skuId") Long skuId) {
        OrderVO orderVO = orderService.canAppraise(orderId, skuId);
        return ResponseEntity.ok(orderVO);
    }

    @PutMapping("/order/state/update")
    public ResponseEntity<Void> updateOrderStatus(@RequestParam("orderId") Long orderId, @RequestParam("status") Integer status) {
         orderService.updateOrderStatus(orderId,status);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    /**
     *  订单的分页查询
     */
    @GetMapping("/order/list")
    public ResponseEntity<PageResult<OrderBacklog>> orderPage(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                              @RequestParam(value = "rows", defaultValue = "5") Integer rows,
                                                              @RequestParam(value = "status", required = false) Integer status) {
        PageResult<OrderBacklog> pageResult = orderService.orderPage(page, rows, status);
        return ResponseEntity.ok(pageResult);
    }


}