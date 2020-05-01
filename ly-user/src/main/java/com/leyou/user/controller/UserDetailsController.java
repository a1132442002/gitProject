package com.leyou.user.controller;

import com.leyou.user.entity.User;
import com.leyou.user.entity.UserDetails;
import com.leyou.user.service.UserDetailsService;
import com.leyou.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserDetailsController {
    @Autowired
    private UserDetailsService userDetailsService;


    @PostMapping("/saveUserDetails")
    public ResponseEntity<Void> saveUserDetails(@RequestBody UserDetails details) {
        System.out.println("details = " + details);
        userDetailsService.save(details);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/look/currentUserDetails")
    public ResponseEntity<UserDetails> currentUserDetails() {
        UserDetails userDetails = userDetailsService.currentUserDetails();
        return ResponseEntity.ok(userDetails);
    }

    @PutMapping("/updateUserImageUrl")
    public ResponseEntity<User> updateUserImageUrl(@RequestParam("userImageUrl") String userImageUrl) {
        userDetailsService.updateUserImageUrl(userImageUrl);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
