package com.ai.slp.product.service.atom.impl.product;

import com.ai.slp.product.dao.mapper.bo.product.Product;
import com.ai.slp.product.dao.mapper.interfaces.product.ProductMapper;
import com.ai.slp.product.service.atom.interfaces.product.IProductAtomSV;
import com.ai.slp.product.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jackieliu on 16/5/5.
 */
@Component
public class ProductAtomSVImpl implements IProductAtomSV {
    @Autowired
    ProductMapper productMapper;
    /**
     * 添加销售商品信息
     *
     * @param product
     * @return
     */
    @Override
    public int installProduct(Product product) {
        if (product.getCreateTime() == null)
            product.setCreateTime(DateUtils.currTimeStamp());
        product.setOperTime(product.getCreateTime());
        return productMapper.insert(product);
    }
}