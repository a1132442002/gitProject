package com.leyou.search.dto;

import java.util.Map;

public class SearchRequest {
    private String key;// 搜索条件

    private Integer page;// 当前页

    private Integer sortNum;

    private Map<String, Object> filterParams;

    private Integer status; // 订单状态

    private static final Integer DEFAULT_SIZE = 20;// 每页大小，不从页面接收，而是固定大小

    private static final Integer DEFAULT_PAGE = 1;// 默认页

    public Integer getSortNum() {
        return sortNum;
    }

    public void setSortNum(Integer sortNum) {
        this.sortNum = sortNum;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPage() {
        if(page == null){
            return DEFAULT_PAGE;
        }
        // 获取页码时做一些校验，不能小于1
        return Math.max(DEFAULT_PAGE, page);
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return DEFAULT_SIZE;
    }

    public Map<String, Object> getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(Map<String, Object> filterParams) {
        this.filterParams = filterParams;
    }
}