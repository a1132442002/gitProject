package com.leyou.order.interceptor;

import com.leyou.common.auth.pojo.UserHolder;
import com.leyou.common.constant.LyConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderFeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (!StringUtils.isEmpty(String.valueOf(UserHolder.getUserId()))) {
            requestTemplate.header(LyConstants.USER_HOLDER_KEY,String.valueOf(UserHolder.getUserId())) ;
        }
    }
}