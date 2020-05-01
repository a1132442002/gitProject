package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.constant.MQConstants;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.entity.*;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.leyou.common.constant.MQConstants.RoutingKey.ITEM_UP_KEY;

/*
 * (propagation = Propagation.REQUIRED)
 * 事务的传播行为中，REQUIRED表示，当前service如果有事务，
 * 那么当前service中调用的其他所有service，都不再开启新事务，直接使用当前service的事务
 * 如果当前service没有事务，那么就默认使用第一个调用的service的事务
 */
@Service
@Transactional
@Slf4j
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private RedissonClient client;

    public PageResult<SpuDTO> goodsPageQuery(Integer page, Integer rows, String key, Boolean saleable) {
        //提供分页信息
        PageHelper.startPage(page, rows);
        //创建一个封装复杂条件的对象
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //封装复杂条件
        if (!StringUtils.isBlank(key)) {
            criteria.andLike("name", "%" + key + "%");
        }
        /*
         * 注意mysql数据中tinyint(1)表示布尔值
         * java向改字段传true，则自动保存为1，传false则自动保存为0
         * 该字段如果是0，java可以直接用false获取，非0就是true
         * */
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        //数据库查询
        List<Spu> spus = spuMapper.selectByExample(example);
        //判空
        if (CollectionUtils.isEmpty(spus)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //封装一个PageHelper的分页对象
        PageInfo<Spu> pageInfo = new PageInfo<>(spus);
        //获取到分页对象中的列表数据
        List<Spu> list = pageInfo.getList();
        //将List<Spu>转成List<SpuDTO>
        List<SpuDTO> spuDTOS = BeanHelper.copyWithCollection(list, SpuDTO.class);
        //给SpuDTO集合中每个SpuDTO对象的分类和品牌名称赋值
        handlerBrandNameAndCategoryName(spuDTOS);
        //得到自定义的分页对象
        PageResult<SpuDTO> pageResult = new PageResult<>(pageInfo.getTotal(),
                pageInfo.getPages(),
                spuDTOS);
        return pageResult;
    }

    /*给SpuDTO集合中每个SpuDTO对象的分类和品牌名称赋值*/
    private void handlerBrandNameAndCategoryName(List<SpuDTO> spuDTOS) {
        spuDTOS.forEach(spuDTO -> {
            //根据品牌id获取品牌对象
            Brand brand = brandService.findBrandById(spuDTO.getBrandId());
            //给品牌名称赋值
            spuDTO.setBrandName(brand.getName());
            //获取分类名称的拼接字符串
            String categoryNames = categoryService.findCategorysByIds(spuDTO.getCategoryIds())//获取到分类对象的集合List<Category>
                    .stream()//将集合转成流
                    .map(Category::getName)//收集Category中的name值
                    .collect(Collectors.joining("|"));//把多个分类的名称以|拼接到一起
            //给分类的名称赋值
            spuDTO.setCategoryName(categoryNames);
        });
    }

    public void saveGoods(SpuDTO spuDTO) {
        try {
            //获取Spu对象
            Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
            //设置商品为下架
            spu.setSaleable(false);
            //保存Spu
            spuMapper.insertSelective(spu);

            //获取SpuDetail对象
            SpuDetail spuDetail = spuDTO.getSpuDetail();
            //给主键赋值
            spuDetail.setSpuId(spu.getId());
            //保存SpuDetail
            detailMapper.insertSelective(spuDetail);

            //获取sku集合
            List<Sku> skus = spuDTO.getSkus();
            //对Sku集合中的元素进行处理
            skus.forEach(sku -> {
                //给外键设置值
                sku.setSpuId(spu.getId());
                //给保存时间设置值
                sku.setCreateTime(new Date());
                //给更新时间设置值
                sku.setUpdateTime(new Date());
            });
            //保存Sku集合
            skuMapper.insertList(skus);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }

    public void updateSaleable(Long id, Boolean saleable) {
        try {
            //凡是根据id做条件的修改都是简单修改，否则就是要用Example对象来封装复杂修改
            Spu record = new Spu();
            record.setId(id);
            record.setSaleable(saleable);
            spuMapper.updateByPrimaryKeySelective(record);
            //商品上下架的同时，要处理索引库和静态化页面的同步问题,根据上下架来指定不同的routingKey
            String routingKey = saleable ? ITEM_UP_KEY : MQConstants.RoutingKey.ITEM_DOWN_KEY;
            //参数一：exchange交换机, 参数二：routingKey, 参数三：要发送的数据，可以是任何格式
            amqpTemplate.convertAndSend(MQConstants.Exchange.ITEM_EXCHANGE_NAME,
                    routingKey, id);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
    }

    public List<Sku> findSkusBySpuId(Long id) {
        Sku record = new Sku();
        record.setSpuId(id);
        List<Sku> skus = skuMapper.select(record);
        if (CollectionUtils.isEmpty(skus)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return skus;
    }

    public SpuDetail findSpuDetailById(Long id) {
        SpuDetail spuDetail = detailMapper.selectByPrimaryKey(id);
        if (spuDetail == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return spuDetail;
    }

    public SpuDTO findSpuById(Long id) {
        try {
            //根据spu主键查询spu对象
            Spu spu = spuMapper.selectByPrimaryKey(id);
            //将spu转成spuDTO
            SpuDTO spuDTO = BeanHelper.copyProperties(spu, SpuDTO.class);
            //根据spuid获取SpuDetail对象
            SpuDetail spuDetail = findSpuDetailById(spu.getId());
            //将SpuDetail对象赋值给SpuDTO属性
            spuDTO.setSpuDetail(spuDetail);
            //根据spuId查询sku集合
            List<Sku> skus = findSkusBySpuId(spu.getId());
            //将sku集合赋值给SpuDTO属性
            spuDTO.setSkus(skus);
            return spuDTO;
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
    }

    public List<Sku> findSkusByIds(List<Long> ids) {
        List<Sku> list = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return list;
    }

    // 减库存
    public void minusStock(Map<Long, Integer> paramMap) {
        paramMap.entrySet().forEach(entry -> {
            //获取skuId
            Long skuId = entry.getKey();
            //要减的数量
            Integer num = entry.getValue();
            //封装减库存的条件
            Sku record = new Sku();
            record.setId(skuId);
            Sku sku = skuMapper.selectByPrimaryKey(skuId);
            record.setStock(sku.getStock() - num);
            //减库存
            skuMapper.updateByPrimaryKeySelective(record);
        });
    }

    // 加库存
    public void plusStock(Map<Long, Integer> plusMap) {
        // 创建锁对象
        RLock rLock = client.getLock("lock");
        try {
            // 获取锁,设置自动失效时间为10s
            boolean isLock = rLock.tryLock(10, TimeUnit.SECONDS);
            // 判断是否获取锁
            if (!isLock) {
                // 获取失败
                log.info("获取锁失败，停止定时任务");
                return;
            }

            // 执行业务
            log.info("获取锁成功，执行定时任务。");
            // 归还库存
            plusMap.entrySet().forEach(entry -> {
                //获取skuId
                Long skuId = entry.getKey();
                //要减的数量
                Integer num = entry.getValue();
                //封装加库存的条件
                Sku record = new Sku();
                record.setId(skuId);
                Sku sku = skuMapper.selectByPrimaryKey(skuId);
                record.setStock(sku.getStock() + num);
                //加库存
                skuMapper.updateByPrimaryKeySelective(record);
            });

        } catch (Exception e) {
            log.error("任务执行异常", e);
        } finally {
            // 释放锁
            rLock.unlock();
        }

    }

    // 预减库存
    public Boolean predict(Map<Long, Integer> pdMap) {
        Boolean flag = false;
        for (Map.Entry<Long, Integer> entry : pdMap.entrySet()) {
            //获取skuId
            Long skuId = entry.getKey();
            //要减的数量
            Integer num = entry.getValue();
            //封装减库存的条件
            Sku record = new Sku();
            record.setId(skuId);
            Sku sku = skuMapper.selectByPrimaryKey(skuId);
            int result = sku.getStock() - num;
            if (result <= 0) {
                return false;
            } else {
                flag = true;
            }
        }
        return flag;

    }

    public void updateGoods(SpuDTO spuDTO) {
        try {
            Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
            spu.setSaleable(false);
            spu.setUpdateTime(new Date());
            spuMapper.updateByPrimaryKeySelective(spu);

            SpuDetail spuDetail = spuDTO.getSpuDetail();
            spuDetail.setUpdateTime(new Date());
            detailMapper.updateByPrimaryKeySelective(spuDetail);

            //修改Sku列表的时候，其实有可能是添加Sku，有可能是删除Sku，也有可能是修改Sku
            //所以应该先删除所有sku，然后在新增skus
            // skuMapper.deleteBySpuId(spu.getId());
            Sku s = new Sku();
            s.setSpuId(spu.getId());
            skuMapper.delete(s);

            List<Sku> skus = spuDTO.getSkus();
            for (Sku sku : skus) {
                sku.setSpuId(spu.getId());
                sku.setCreateTime(new Date());
                sku.setUpdateTime(new Date());
            }

            skuMapper.insertList(skus);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
    }

    public Sku findSkuById(Long id) {
        Sku sku = skuMapper.selectByPrimaryKey(id);
        if (sku == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return sku;
    }

    public Spu findSpuBySkuId(Long id) {
        Sku sku = skuMapper.selectByPrimaryKey(id);
        if (sku == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        Long spuId = sku.getSpuId();
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        if (spu == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return spu;
    }
}
