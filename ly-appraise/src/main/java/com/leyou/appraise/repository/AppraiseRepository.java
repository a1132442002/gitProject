package com.leyou.appraise.repository;

import com.leyou.appraise.entity.Appraise;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * <功能简述><br>
 * <>
 *
 * @author Zqh
 * @create 2020/4/9
 * @since 1.0.0
 */
public interface AppraiseRepository  extends MongoRepository<Appraise, Long> {

}
