package com.leyou.appraise.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "appraise")
public class Appraise  extends IncIdEntity<Long> {


    private Long skuId;
    private Long spuId;
    private Long orderId;
    /*
     * 昵称
     */
    // private String nickname;

    /*
     * 账号
     */
    private String username;
    /**
     * 用户头像
     */
    private String userImage;
    /**
     * 星级
     */
    private Integer star;
    /**
     * 评价内容
     */
    private String context;
    /**
     * 评价时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

}