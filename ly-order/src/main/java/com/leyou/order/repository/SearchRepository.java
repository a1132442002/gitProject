package com.leyou.order.repository;

import com.leyou.order.entity.OrderBacklog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface SearchRepository extends ElasticsearchRepository<OrderBacklog, String> {


}
