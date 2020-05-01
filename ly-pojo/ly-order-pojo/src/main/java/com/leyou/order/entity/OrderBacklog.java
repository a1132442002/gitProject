package com.leyou.order.entity;




import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * Author :Wlz
 * Date   :2020-04-08 23:42.
 *
 *   存入索引库实体类
 *
 */
@Data
@Document(indexName = "orderbacklog", type = "docs", shards = 1, replicas = 1)
public class OrderBacklog {

    /**
     * 创建时间
     */
    @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="yyyy-MM-dd HH:mm",timezone="GMT+8")
    @Field(type = FieldType.Keyword)
    private Date createTime;

    /**
     * 订单编号
     */

    @Id
    private String orderId;

    /**
     * 商品金额
     */
    @Field(type = FieldType.Keyword)
    private Long totalFee;

    /**
     * 订单状态
     */
    @Field(type = FieldType.Keyword)
    private Integer status;

    /**
     * 用户id
     */
    @Field(type = FieldType.Keyword)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String orderDetails;// orderDetail 订单信息的json结构
}
