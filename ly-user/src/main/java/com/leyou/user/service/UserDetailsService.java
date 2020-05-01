package com.leyou.user.service;

import com.leyou.common.auth.pojo.UserHolder;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.user.entity.UserDetails;
import com.leyou.user.mapper.UserDetailsMapper;
import com.leyou.user.mapper.UserMapper;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Service
@Transactional//
public class UserDetailsService {

    @Autowired
    private UserDetailsMapper detailsMapper;

    /**
     * 保存用户详情
     *
     * @param details
     * @return
     */
    public void save(UserDetails details) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }
        UserDetails userDetails = queryById(userId);
        if (userDetails == null) {
            try {
                details.setId(userId);
                detailsMapper.insertSelective(details);
            } catch (Exception e) {
                throw new LyException(501, "保存用户资料失败");
            }
        } else {
            try {
                detailsMapper.updateByPrimaryKeySelective(details);
            } catch (Exception e) {
                throw new LyException(501, "修改用户资料失败");
            }
        }
    }

    public UserDetails queryById(Long userId) {
        UserDetails userDetails = detailsMapper.selectByPrimaryKey(userId);
        if (userDetails != null) {
            return userDetails;
        }
        return null;
    }


    public UserDetails currentUserDetails() {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }
        UserDetails userDetails = detailsMapper.selectByPrimaryKey(userId);
        if (userDetails == null) {
            throw new LyException(501, "个人信息未完善");
        }
        return userDetails;
    }

    public void updateUserImageUrl(String userImageUrl) {
        UserDetails userDetails = currentUserDetails();
        String imageUrlJson = userDetails.getUserImageUrl();
        String newUrlListJson = "";
        if (StringUtils.isEmpty(imageUrlJson)) {
            LinkedList<String> urlList = new LinkedList<>();
            urlList.addLast(userImageUrl);
            newUrlListJson = JsonUtils.toString(urlList);
        } else {
            LinkedList<String> urlList = (LinkedList<String>) JsonUtils.toLinkedList(imageUrlJson, String.class);
            if (urlList.size() < 3) {
                //最多存2张历史图片
                urlList.addLast(userImageUrl);
                newUrlListJson = JsonUtils.toString(urlList);
            } else {
                //超过两张 删除第一张 再向最后插入一张
                urlList.removeFirst();
                urlList.addLast(userImageUrl);
                newUrlListJson = JsonUtils.toString(urlList);
            }
        }
        userDetails.setUpdateTime(new Date());
        userDetails.setUserImageUrl(newUrlListJson);
        detailsMapper.updateByPrimaryKeySelective(userDetails);
    }
}
