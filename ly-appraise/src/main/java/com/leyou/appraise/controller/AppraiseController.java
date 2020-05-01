package com.leyou.appraise.controller;

import com.leyou.appraise.entity.Appraise;
import com.leyou.appraise.service.AppraiseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <功能简述><br>
 * <>
 *
 * @author Zqh
 * @create 2020/4/9
 * @since 1.0.0
 */
@RestController
public class AppraiseController {
    @Autowired
    private AppraiseService appraiseService;
    @PostMapping
    public ResponseEntity<Void> addAppraise(@RequestBody Appraise appraise, HttpServletRequest request){
        appraiseService.addAppraise(appraise,request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/look/{spuId}")
    public ResponseEntity<List<Appraise>> getAppraisesBySpuId(@PathVariable("spuId")Long spuId){
        List<Appraise> appraiseList = appraiseService.getAppraisesBySpuId(spuId);
        return ResponseEntity.ok(appraiseList);
    }
}
