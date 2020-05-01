package com.leyou.user.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

@Data
@Table(name = "tb_user_details")
public class UserDetails {
    @Id
    private Long id;
    private String nickName;
    private Integer sex;


    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    private String province;
    private String city;
    private String district;
    private Integer job;

    private String userImageUrl;

    private Date createTime;
    private Date updateTime;


}
