package com.leyou.favorite.entity;

import lombok.Data;

/**
 * <功能简述><br>
 * <>
 *
 * @author Zqh
 * @create 2020/4/6
 * @since 1.0.0
 */
@Data
public class Favorite {
    //需要根据spuId查询SpuName，页面上的spuName直接拿会有高亮字段，所以要从数据源重新取
    private Long spuId;
    private Long skuId;
    private String skuTitle;
    private String image;
    private Long price;
    private String subTitle;

    private String spuName;
    private String fullTitle;//冗余字段，方便取

    private Boolean downMsg;

}
