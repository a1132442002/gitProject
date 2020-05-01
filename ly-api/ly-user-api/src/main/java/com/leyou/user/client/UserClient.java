package com.leyou.user.client;

import com.leyou.user.dto.AddressDTO;
import com.leyou.user.entity.User;
import com.leyou.user.entity.UserDetails;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("user-service")
public interface UserClient {
    /**
     * 根据用户名和密码查询用户信息
     */
    @GetMapping("/query")
    public User findUserByNameAndPassword(@RequestParam("username") String username,
                                          @RequestParam("password") String password);

    /**
     * 根据
     *
     * @param userId 用户id
     * @param id     地址id
     * @return 地址信息
     */
    @GetMapping("/address")
    AddressDTO queryAddressById(@RequestParam("userId") Long userId, @RequestParam("id") Long id);

    //防止用户直接根据id查看账号信息，所以不写入口
    //随便写的mapping
    @GetMapping
    public User findUserById(@RequestParam("userId") Long userId);

    @GetMapping("/look/currentUserDetails")
    public UserDetails currentUserDetails();
}
