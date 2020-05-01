package com.leyou.order.test;

import com.leyou.order.entity.Order;
import com.leyou.order.entity.OrderBacklog;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.repository.SearchRepository;
import com.leyou.order.scheduled.OrderScheduleTask;
import com.leyou.order.service.OrderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderScheduleTask orderScheduleTask;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SearchRepository searchRepository;

    public OrderTest() {
        //在构造函数上写上这个
        System.setProperty("es.set.netty.runtime.available.processors","false");
    }

    @Test
    public void updateTest(){
        //修改订单状态
        Order record = new Order();
        record.setOrderId(1245640157269463041L);
        record.setStatus(2);
        record.setPayTime(new Date());
        int count = orderMapper.updateByPrimaryKeySelective(record);
    }

    @Test
    public void aaa() {
        String test  = ("chaojimali");
        String chaoji = test.replace("chaoji", "");

        System.out.println("========="+chaoji);

        String test2 = ("chaojimali");
        String substring = test.substring(5, 7);
        System.out.println("-------" + substring);
    }


    /**
     * 将订单数据源的数据写入索引库
     */
    @Test
    public void orderIndexWrite() {
        // 查询出所有的订单
        List<Order> orderList = orderService.findAllOrders();
        orderList.forEach(order -> {
            //批量写入索引库
            orderService.buildOrderBacklog(order.getOrderId());
        });
    }


    @Test
    public void changeOrderStatus() {
            Optional<OrderBacklog> order = searchRepository.findById("1247851403607674880");
            OrderBacklog orderBacklog = order.get();
            orderBacklog.setStatus(4);
            searchRepository.save(orderBacklog);
        }

}
