package com.leyou.cart.controller;

import com.leyou.cart.entity.Cart;
import com.leyou.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     */
    @PostMapping
    public ResponseEntity<Void> addCart(@RequestBody Cart cart) {
        cartService.addCart(cart);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 查询购物车列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<Cart>> findCarts() {
        List<Cart> carts = cartService.findCarts();
        return ResponseEntity.ok(carts);
    }

    /**
     * 合并购物车，本质就是批量添加购物车
     */
    @PostMapping("/list")
    public ResponseEntity<Void> addCarts(@RequestBody List<Cart> carts) {
        cartService.addCarts(carts);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据skuId删除单个购物车
     *
     * @param skuId
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable("id") Long skuId) {
        cartService.deleteCart(skuId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
