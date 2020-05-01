package com.leyou.appraise.interceptor;

import com.leyou.appraise.scheduled.AppTokenScheduled;
import com.leyou.common.auth.pojo.UserHolder;
import com.leyou.common.constant.LyConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthFeignInterceptor implements RequestInterceptor {

    @Autowired
    private AppTokenScheduled appTokenScheduled;

    @Override
    public void apply(RequestTemplate template) {
        //在feign的请求中添加请求头信息
        template.header(LyConstants.APP_TOKEN_HEADER, appTokenScheduled.getToken());
        if (!StringUtils.isEmpty(String.valueOf(UserHolder.getUserId()))) {
            template.header(LyConstants.USER_HOLDER_KEY, String.valueOf(UserHolder.getUserId()));
        }
    }
}
