package com.leyou.appraise.service;

import com.leyou.appraise.config.JwtProperties;
import com.leyou.appraise.entity.Appraise;
import com.leyou.appraise.repository.AppraiseRepository;
import com.leyou.common.auth.pojo.Payload;
import com.leyou.common.auth.pojo.UserHolder;
import com.leyou.common.auth.pojo.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.auth.utils.RsaUtils;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.utils.CookieUtils;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.client.ItemClient;
import com.leyou.item.entity.Spu;
import com.leyou.order.client.OrderClient;
import com.leyou.order.dto.OrderStatusEnum;
import com.leyou.user.client.UserClient;
import com.leyou.user.entity.UserDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * <功能简述><br>
 * <>
 *
 * @author Zqh
 * @create 2020/4/9
 * @since 1.0.0
 */
@Slf4j
@Service
public class AppraiseService {
    @Autowired
    private JwtProperties jwtProp;
    @Autowired
    private AppraiseRepository appraiseRepository;
    @Autowired
    private ItemClient itemClient;
    @Autowired
    private OrderClient orderClient;
    @Autowired
    private UserClient userClient;


    public void addAppraise(Appraise appraise, HttpServletRequest request) {

        UserDetails userDetails = userClient.currentUserDetails();
        if (userDetails == null) {
            throw new LyException(501, "请先完善个人信息后在进行评价");
        }
        try {
            Spu spu = itemClient.findSpuBySkuId(appraise.getSkuId());
            appraise.setSpuId(spu.getId());
            appraise.setCreateTime(new Date());
            //模拟五星好评
            appraise.setStar(5);

            //封装评论人信息
            appraise.setUsername(userDetails.getNickName());
            //用户头像
            String userImageUrlJson = userDetails.getUserImageUrl();
            if (StringUtils.isEmpty(userImageUrlJson)) {
                appraise.setUserImage("http://image.leyou.com/photo_icon.png");
            } else {
                LinkedList<String> urlsList = (LinkedList<String>) JsonUtils.toLinkedList(userImageUrlJson, String.class);
                appraise.setUserImage(urlsList.getLast());
            }
            appraiseRepository.insert(appraise);
            orderClient.updateOrderStatus(appraise.getOrderId(), OrderStatusEnum.RATED.value());
        } catch (Exception e) {
            throw new LyException(501, "评论失败");
        }
    }

    public List<Appraise> getAppraisesBySpuId(Long spuId) {
        Appraise appraise = new Appraise();
        appraise.setSpuId(spuId);
        Example<Appraise> example = Example.of(appraise);
        List<Appraise> appraiseList = appraiseRepository.findAll(example);
        if (CollectionUtils.isEmpty(appraiseList)) {
            throw new LyException(501, "无评论信息！");
        }
        return appraiseList;
    }
}
