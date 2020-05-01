package com.leyou.order.service;

import com.leyou.cart.client.CartClient;
import com.leyou.common.auth.pojo.UserHolder;
import com.leyou.common.constant.LyConstants;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.utils.IdWorker;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.client.ItemClient;
import com.leyou.item.entity.Sku;
import com.leyou.order.dto.CartDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.dto.OrderStatusEnum;
import com.leyou.order.dto.OrderVO;
import com.leyou.order.entity.Order;
import com.leyou.order.entity.OrderBacklog;
import com.leyou.order.entity.OrderDetail;
import com.leyou.order.entity.OrderLogistics;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderLogisticsMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.repository.SearchRepository;
import com.leyou.order.utils.PayHelper;
import com.leyou.user.client.UserClient;
import com.leyou.user.dto.AddressDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderLogisticsMapper orderLogisticsMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private ItemClient itemClient;

    @Autowired
    private UserClient userClient;

    @Autowired
    private CartClient cartClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PayHelper payHelper;

    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Autowired
    private SearchRepository searchRepository;


    //    @GlobalTransactional
    public Long buildOrder(OrderDTO orderDTO) {
        try {
            //第一部分：保存订单信息
            //获取订单号
            Long orderId = idWorker.nextId();
            //获取当前用户的id
            Long userId = UserHolder.getUserId();
            //创建订单订单对象
            Order order = new Order();
            order.setOrderId(orderId);
            order.setStatus(OrderStatusEnum.INIT.value());
            order.setUserId(userId);
            order.setInvoiceType(0);
            order.setPaymentType(orderDTO.getPaymentType());
            order.setPostFee(0L);
            order.setActualFee(1L);//实付金额写一分钱方便测试

            //得到所有提交的购物车数据
            List<CartDTO> carts = orderDTO.getCarts();
            //将列表类型的CartDTO集合转成键值对的集合
            Map<Long, Integer> cartMap = carts.stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
            //收集到购物车数据中所有的sku的id
            List<Long> skuIds = carts.stream().map(CartDTO::getSkuId).collect(Collectors.toList());
            //根据Sku的id的集合查询出Sku对象的集合
            List<Sku> skus = itemClient.findSkusByIds(skuIds);
            //计算订单总金额
            Long totalFee = skus.stream().mapToLong(sku -> sku.getPrice() * cartMap.get(sku.getId())).sum();
            //给订单总金额赋值
            order.setTotalFee(totalFee);
            //保存订单
            orderMapper.insertSelective(order);

            //第二部分：保存订单详情信息
            //初始化订单详情集合对象
            List<OrderDetail> orderDetails = new ArrayList<>();
            //遍历第一部分获取到的sku对象列表
            skus.forEach(sku -> {
                //初始化一个OrderDetail对象
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setId(idWorker.nextId());
                orderDetail.setOrderId(orderId);
                orderDetail.setTitle(sku.getTitle());
                orderDetail.setSkuId(sku.getId());
                orderDetail.setPrice(sku.getPrice());
                orderDetail.setOwnSpec(sku.getOwnSpec());
                orderDetail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
                orderDetail.setNum(cartMap.get(sku.getId()));
                orderDetail.setCreateTime(new Date());
                orderDetail.setUpdateTime(new Date());
                //把OrderDetail对象添加到OrderDetail集合中
                orderDetails.add(orderDetail);
            });
            //保存订单详情列表
            orderDetailMapper.insertList(orderDetails);

            //第三部分：保存物流信息
            //获取用户的物流信息
            AddressDTO addressDTO = userClient.queryAddressById(userId, orderDTO.getAddressId());
            //将AddressDTO转成OrderLogistics
            OrderLogistics orderLogistics = BeanHelper.copyProperties(addressDTO, OrderLogistics.class);
            //设置订单号
            orderLogistics.setOrderId(orderId);
            //保存订单物流信息
            orderLogisticsMapper.insertSelective(orderLogistics);

            //插入一步： 把订单添加到索引库
            buildOrderBacklog(orderId);

            //第四部分：减库存
            itemClient.minusStock(cartMap);

            //遗留功能：清空购物车。什么时候清空？下单【对卖家不友好】，支付【对买家不友好】。
            // 清空购物车
            skus.forEach(sku -> cartClient.deleteCart(sku.getId()));
            return orderId;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }


    public OrderVO findOrderById(Long id) {
        try {
            //根据订单号查询订单
            Order order = orderMapper.selectByPrimaryKey(id);
            //将Order对象转成OrderVO对象
            OrderVO orderVO = BeanHelper.copyProperties(order, OrderVO.class);

            //根据订单号查询订单详情列表
            OrderDetail record = new OrderDetail();
            record.setOrderId(id);
            List<OrderDetail> orderDetails = orderDetailMapper.select(record);
            //把OrderDetail集合赋值给OrderVO的属性
            orderVO.setDetailList(orderDetails);

            //根据订单号查询物流信息
            OrderLogistics orderLogistics = orderLogisticsMapper.selectByPrimaryKey(id);
            orderVO.setLogistics(orderLogistics);

            return orderVO;
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }

    }

    public String getPayUrl(Long id) {
        //得到当前支付链接在redis中存储的key
        String payUrlRedisKey = LyConstants.PAY_URL_PRE + id;
        //判断当前订单的支付链接是否已经生成
        Boolean hasKey = redisTemplate.hasKey(payUrlRedisKey);
        //如果存在
        if (hasKey) {
            // 查询库存  预扣库存
            OrderVO orderVO = findOrderById(id);
          if (orderVO.getStatus() != OrderStatusEnum.INIT.value()) {
              // 获取OrderDetail
              List<OrderDetail> orderdetail = orderVO.getDetailList();
              // 数据列表转map集合
              Map<Long, Integer> pdMap = orderdetail.stream().collect(Collectors.toMap(OrderDetail::getSkuId, OrderDetail::getNum));
              Boolean predict = itemClient.predict(pdMap);
              if (!predict) {
                  Order order = new Order();
                  order.setOrderId(id);
                  order.setStatus(OrderStatusEnum.USELESS.value());
                  order.setCreateTime(orderVO.getCreateTime());
                  orderMapper.updateByPrimaryKeySelective(order);
                  // 索引库修改订单状态
                  changeOrderStatus(orderVO.getOrderId(), order.getStatus());
                  log.error("【库存不足】购买失败！");
                  return "0";
              }
          }
            return redisTemplate.opsForValue().get(payUrlRedisKey);
        }


        //如果不存在，我们可以查询订单
        Order order = orderMapper.selectByPrimaryKey(id);
        //判断订单状态是否已经支付
        if (!(order.getStatus() == OrderStatusEnum.INIT.value())) {
            throw new LyException(501, "订单已支付，不要重复支付！");
        }
        //生成支付链接
        String payUrl = payHelper.getPayUrl(id, order.getActualFee());
        //将支付链接存储到redis中，有效期2小时
        redisTemplate.opsForValue().set(payUrlRedisKey, payUrl, 2, TimeUnit.HOURS);
        return payUrl;

    }

    public Integer queryOrderStatus(Long id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        return order.getStatus();
    }

    public void handlerWxResp(Map<String, String> wxNotifyParams) {
        log.info("【微信回调通知】业务开始！");
        //校验通信标识和业务标识
        payHelper.checkWxResp(wxNotifyParams);
        //获取微信通知中的商户订单号
        Long orderId = Long.valueOf(wxNotifyParams.get("out_trade_no"));
        //获取微信通知中的支付金额
        Long totalFee = Long.valueOf(wxNotifyParams.get("total_fee"));
        //根据微信通知中的商户订单号查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        //判断订单状态
        if (order.getStatus() != OrderStatusEnum.INIT.value() && order.getStatus() != OrderStatusEnum.PAST_DUE.value()) {
            throw new LyException(501, "此订单已支付!");
        }
        //比对金额
        if (!totalFee.equals(order.getActualFee())) {
            throw new LyException(501, "支付金额与订单应付金额不一致!");
        }

        try {
            // 判断订单状态是否为支付过期
            if (order.getStatus() == OrderStatusEnum.PAST_DUE.value()) {
                // 通过订单id获取订单的数据
                OrderVO orderVo = findOrderById(orderId);
                // 获取OrderDetail
                List<OrderDetail> orderDetail = orderVo.getDetailList();
                // 遍历
                Map<Long, Integer> paramMap = orderDetail.stream().collect(Collectors.toMap(OrderDetail::getSkuId, OrderDetail::getNum));
                // 减库存
                itemClient.minusStock(paramMap);
            }
        } catch (Exception e) {
            log.error("【库存不足】已付款！");
            throw new LyException(501, "【库存不足】已付款,购买失败！！");
        }

        //修改订单状态
        Order record = new Order();
        record.setOrderId(orderId);
        record.setStatus(OrderStatusEnum.PAY_UP.value());
        record.setPayTime(new Date());
        int count = orderMapper.updateByPrimaryKeySelective(record);
        // 索引库修改订单状态
        changeOrderStatus(order.getOrderId(), record.getStatus());

        if (count != 1) {
            //此处应该将订单状态更新失败的日志入库，后续有人工修改，保证正常给微信成功通知。
            log.error("【微信回调通知】修改订单状态失败！");
        }
        log.info("【微信回调通知】成功结束！");
    }

    /**
     * 定时清理失效订单
     */
    public void ClearanceOrder() {
        try {
            // 查询出状态是未支付的订单
            List<Order> allOrder = findAllOrderStatus(OrderStatusEnum.INIT.value());

            if (!CollectionUtils.isEmpty(allOrder)) {
                // 判断下单是否超过20分钟 如果超过就更改状态为已过期
                for (Order order : allOrder) {
                    if (System.currentTimeMillis() - order.getCreateTime().getTime() > LyConstants.TWENTY_MINUTE) {
                        // 调用findOrderById 获取到商品id 跟购买的数量
                        OrderVO orderVo = findOrderById(order.getOrderId());
                        // 获取OrderDetail
                        List<OrderDetail> orderDetail = orderVo.getDetailList();

                        // 遍历
                        Map<Long, Integer> plusMap = orderDetail.stream().collect(Collectors.toMap(OrderDetail::getSkuId, OrderDetail::getNum));
                        itemClient.minusStock(plusMap);

                        order.setOrderId(order.getOrderId());
                        order.setStatus(OrderStatusEnum.PAST_DUE.value());
                        order.setCreateTime(order.getCreateTime());
                        orderMapper.updateByPrimaryKeySelective(order);
                        // 索引库修改订单状态
                        changeOrderStatus(order.getOrderId(), order.getStatus());
                    }
                }
            }

            // 查询出状态是未支付的订单
            List<Order> orderList = findAllOrderStatus(OrderStatusEnum.PAST_DUE.value());

            if (!CollectionUtils.isEmpty(orderList)) {
                // 判断下单是否超过20分钟 如果超过就更改状态为已过期
                for (Order orders : orderList) {
                    if (System.currentTimeMillis() - orders.getCreateTime().getTime() > LyConstants.TWO_HOUR) {
                        orders.setOrderId(orders.getOrderId());
                        orders.setStatus(OrderStatusEnum.USELESS.value());
                        orders.setCreateTime(orders.getCreateTime());
                        orderMapper.updateByPrimaryKeySelective(orders);
                        // 索引库修改订单状态
                        changeOrderStatus(orders.getOrderId(), orders.getStatus());
                    }
                }
            }
            log.info("【定时清理失效订单成功】");

        } catch (Exception e) {
            throw new LyException(501, "定时清理失效订单失败！！");
        }
    }

    /**
     * 查询所有指定状态的订单
     */
    public List<Order> findAllOrderStatus(Integer status) {

        Order record = new Order();
        record.setStatus(status);
        List<Order> orderList = orderMapper.select(record);

        return orderList;
    }

    /**
     * 查询所有订单
     */
    public List<Order> findAllOrders() {

        List<Order> orderList = orderMapper.selectAll();

        return orderList;
    }

    /**
     * 将数据源【数据库】的数据转成索引库的文档对象
     */
    public void buildOrderBacklog(Long orderId){
        try {
            //根据订单号查询订单
            Order order = orderMapper.selectByPrimaryKey(orderId);
            //根据订单号查询订单详情列表
            OrderDetail record = new OrderDetail();
            record.setOrderId(orderId);
            List<OrderDetail> orderDetails = orderDetailMapper.select(record);

            // 第一步 构建一个OrderBacklog对象
            OrderBacklog orderBacklog = new OrderBacklog();
            // 第二步 先给可以直接通过spu对象来赋值的
            orderBacklog.setCreateTime(order.getCreateTime());
            orderBacklog.setOrderId(order.getOrderId().toString());
            orderBacklog.setTotalFee(order.getTotalFee());
            orderBacklog.setStatus(order.getStatus());
            orderBacklog.setUserId(order.getUserId());

            orderBacklog.setOrderDetails(JsonUtils.toString(orderDetails));

            // 添加索引库
            searchRepository.save(orderBacklog);
        } catch (Exception e) {
            throw new LyException(501, "订单添加索引库失败！！！");
        }
    }

    public PageResult<OrderBacklog> orderPage(Integer page, Integer rows, Integer status) {

        //创建一个封装复杂条件查询的对象
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //获取当前用户的id
        Long userId = UserHolder.getUserId();
            // 排序
            FieldSortBuilder fsb = null;
            fsb = SortBuilders.fieldSort("createTime").order(SortOrder.DESC);
/*            //添加要查询的字段条件
            queryBuilder.withQuery(QueryBuilders.termQuery("userId", userId)).withSort(fsb);*/
        //添加分页条件
        queryBuilder.withPageable(PageRequest.of(page - 1, rows));

        //提供一个过滤查询的Bool对象
        BoolQueryBuilder queryBuilders = QueryBuilders.boolQuery();

        //封装过滤条件
        queryBuilders.filter(QueryBuilders.termQuery("userId", userId));
        if (status != null) {
            queryBuilders.filter(QueryBuilders.termQuery("status", status));
        }
        //添加查询条件
        queryBuilder.withQuery(queryBuilders).withSort(fsb);
        //向索引库发起复杂查询请求
        AggregatedPage<OrderBacklog> orderBacklogAgg = esTemplate.queryForPage(queryBuilder.build(), OrderBacklog.class);
        PageResult<OrderBacklog> pageResult = new PageResult<>(
                orderBacklogAgg.getTotalElements(),
                orderBacklogAgg.getTotalPages(),
                BeanHelper.copyWithCollection(orderBacklogAgg.getContent(), OrderBacklog.class)
        );

        return pageResult;
    }

    /**
     *  修改索引库订单的状态
     */
    public void changeOrderStatus(Long orderId, Integer status) {
        try {
            Optional<OrderBacklog> order = searchRepository.findById(orderId.toString());
            OrderBacklog orderBacklog = order.get();
            orderBacklog.setStatus(status);
            searchRepository.save(orderBacklog);
        } catch (Exception e) {
            throw new LyException(501, "索引库修改订单状态失败 ！！");
        }
    }


    public OrderVO canAppraise(Long orderId, Long skuId) {
        OrderVO order = findOrderById(orderId);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        if (!UserHolder.getUserId().equals(order.getUserId())) {
            throw new LyException(501,"这个订单不属于您！");
        }
        List<OrderDetail> detailList = order.getDetailList();
        if (CollectionUtils.isEmpty(detailList)) {
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        for (OrderDetail orderDetail : detailList) {
            if (orderDetail.getSkuId().equals(skuId)) {
                Integer status = order.getStatus();
                if (status == OrderStatusEnum.CONFIRMED.value()) {
                    return order;
                }
            }
        }
        throw new LyException(501,"订单中不存在该商品或商品状态不是未评价！");
    }

    public void updateOrderStatus(Long orderId, Integer status) {
        try {
            Order record = new Order();
            record.setOrderId(orderId);
            record.setStatus(status);
            orderMapper.updateByPrimaryKeySelective(record);
        } catch (Exception e) {
            throw new LyException(501,"订单状态修改失败");
        }
    }
}