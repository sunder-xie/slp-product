package com.ai.slp.product.api.product.param;

import java.sql.Date;

/**
 * 商品管理审核中查询参数
 * 
 * Date: 2016年4月25日 <br>
 * Copyright (c) 2016 asiainfo.com <br>
 * 
 * @author lipeng
 */
public class ProductCheckingParam extends ProductBase {
    
    /**
     * 上架类型 1审核通过后立即上架 2审核通过后放入仓库 3定时上架
     */
    private String upshelfType;
    
    /**
     * 上架起始时间
     */
    private Date upStartTime;
    
    /**
     * 上架截止时间
     */
    private Date upEndTime;

    /**
     * 商品类目ID
     */
    private String productCatId;
    
    /**
     * 商品类型
     */
    private String productType;

    /**
     * 最高销售价
     */
    private long highSalePrice;
    
    /**
     * 最低销售价
     */
    private long lowSalePrice;
    
    /**
     * 商品名称
     */
    private String prodName;
    
    /**
     * 商品ID
     */
    private String prodId;

    public String getUpshelfType() {
        return upshelfType;
    }

    public void setUpshelfType(String upshelfType) {
        this.upshelfType = upshelfType;
    }

    public Date getUpStartTime() {
        return upStartTime;
    }

    public void setUpStartTime(Date upStartTime) {
        this.upStartTime = upStartTime;
    }

    public Date getUpEndTime() {
        return upEndTime;
    }

    public void setUpEndTime(Date upEndTime) {
        this.upEndTime = upEndTime;
    }

    public String getProductCatId() {
        return productCatId;
    }

    public void setProductCatId(String productCatId) {
        this.productCatId = productCatId;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public long getHighSalePrice() {
        return highSalePrice;
    }

    public void setHighSalePrice(long highSalePrice) {
        this.highSalePrice = highSalePrice;
    }

    public long getLowSalePrice() {
        return lowSalePrice;
    }

    public void setLowSalePrice(long lowSalePrice) {
        this.lowSalePrice = lowSalePrice;
    }

    public String getProdName() {
        return prodName;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public String getProdId() {
        return prodId;
    }

    public void setProdId(String prodId) {
        this.prodId = prodId;
    }

    
}