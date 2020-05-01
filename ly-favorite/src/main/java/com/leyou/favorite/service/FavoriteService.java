package com.leyou.favorite.service;

import com.leyou.common.auth.pojo.UserHolder;
import com.leyou.common.constant.LyConstants;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.favorite.entity.Favorite;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.SpuDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <功能简述><br>
 * <>
 *
 * @author Zqh
 * @create 2020/4/6
 * @since 1.0.0
 */
@Service
public class FavoriteService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ItemClient itemClient;

    public void addFavorite(Favorite favorite) {
        //得到当前用户id
        Long userId = UserHolder.getUserId();
        //得到收藏夹的key
        String favoriteKey = LyConstants.FAVORITE_PRE + userId;
        //得到当前用户的收藏夹数据
        BoundHashOperations<String, String, String> favoriteHashMap = redisTemplate.boundHashOps(favoriteKey);
        //获取当前新添加的收藏夹对象的hashKey
        String hashKey = favorite.getSpuId().toString();
        //判断当前新添加的收藏夹对象是否在原来的收藏夹集合中
        if (!favoriteHashMap.hasKey(hashKey)) {
            //因为从搜索页面直接获取的spuName会存在高亮，所以需要从数据源中获取spuName
            SpuDTO spuDTO = itemClient.findSpuById(favorite.getSpuId());
            if (spuDTO == null) {
                throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
            }
            favorite.setSpuName(spuDTO.getName());
            favorite.setFullTitle(spuDTO.getName() + favorite.getSkuTitle());
            //默认不开启降价通知
            favorite.setDownMsg(false);
            //如果新添加的收藏夹对象不存在于用户的收藏夹列表，则更新收藏夹
            favoriteHashMap.put(hashKey, JsonUtils.toString(favorite));
        }
        //如果已存在则不作任何操作
    }

    public List<Favorite> findFavoriteList() {
        //得到当前用户id
        Long userId = UserHolder.getUserId();
        //得到收藏夹的key
        String favoriteKey = LyConstants.FAVORITE_PRE + userId;
        //判断当前用户是否有收藏夹
        if (!redisTemplate.hasKey(favoriteKey)) {
            //如果没有收藏夹 结束操作 返回null
            return null;
        }
        //得到收藏夹对象
        BoundHashOperations<String, String, String> favoriteHashMap = redisTemplate.boundHashOps(favoriteKey);
        List<String> favorites = favoriteHashMap.values();
        if (CollectionUtils.isEmpty(favorites)) {
            //如果收藏夹为空 结束操作 返回null
            return null;
        }
        //将字符串格式的收藏夹对象集合转成对象列表
        List<Favorite> favoriteList = favorites.stream()
                .map(cart -> JsonUtils.toBean(cart, Favorite.class))
                .collect(Collectors.toList());
        return favoriteList;
    }

    public void removeFavorite(Long spuId) {
        if (StringUtils.isEmpty(spuId.toString())) {
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        Long userId = UserHolder.getUserId();
        String key = LyConstants.FAVORITE_PRE + userId;
        if (!redisTemplate.hasKey(key)) {
            throw new LyException(501, "收藏夹不存在");
        }
        redisTemplate.opsForHash().delete(key, spuId.toString());
    }

    public Boolean updateDownMsg(Long spuId) {
        Long userId = UserHolder.getUserId();
        String favoriteKey = LyConstants.FAVORITE_PRE + userId;
        BoundHashOperations<String, String, String> favoriteHashMap = redisTemplate.boundHashOps(favoriteKey);
        String hashKey = spuId.toString();
        if (favoriteHashMap.hasKey(hashKey)) {
            String favoriteStr = favoriteHashMap.get(hashKey);
            Favorite favorite = JsonUtils.toBean(favoriteStr, Favorite.class);
            boolean isDownMsg = !favorite.getDownMsg();
            favorite.setDownMsg(isDownMsg);
            favoriteHashMap.put(hashKey, JsonUtils.toString(favorite));
            return isDownMsg;
        }else {
            throw new LyException(501,"该商品未被收藏！");
        }
    }
}
