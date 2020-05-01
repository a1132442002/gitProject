package com.leyou.favorite.controller;

import com.leyou.favorite.entity.Favorite;
import com.leyou.favorite.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <功能简述><br>
 * <>
 *
 * @author Zqh
 * @create 2020/4/6
 * @since 1.0.0
 */
@RestController
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 添加关注
     */
    @PostMapping
    public ResponseEntity<Void> addFavorite(@RequestBody Favorite favorite) {
        favoriteService.addFavorite(favorite);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<Favorite>> findFavoriteList() {
        List<Favorite> favoriteList = favoriteService.findFavoriteList();
        return ResponseEntity.ok(favoriteList);
    }

    /**
     * 根据spuId删除单个收藏夹
     *
     * @param spuId
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFavorite(@PathVariable("id") Long spuId) {
        favoriteService.removeFavorite(spuId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/downMsg")
    public ResponseEntity<Boolean> updateDownMsg(@RequestParam("spuId") Long spuId){
        Boolean isDownMsg = favoriteService.updateDownMsg(spuId);
        return ResponseEntity.ok(isDownMsg);
    }

}
