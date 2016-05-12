package com.ai.slp.product.service.atom.impl.product;

import com.ai.slp.product.constants.ProductConstants;
import com.ai.slp.product.dao.mapper.bo.product.ProdSku;
import com.ai.slp.product.dao.mapper.bo.product.ProdSkuCriteria;
import com.ai.slp.product.dao.mapper.interfaces.product.ProdSkuMapper;
import com.ai.slp.product.service.atom.interfaces.product.IProdSkuAtomSV;
import com.ai.slp.product.util.DateUtils;
import com.ai.slp.product.util.SequenceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jackieliu on 16/5/6.
 */
@Component
public class ProdSkuAtomSVImpl implements IProdSkuAtomSV {
    @Autowired
    ProdSkuMapper prodSkuMapper;
    /**
     * 查询商品的SKU信息
     *
     * @param tenantId
     * @param prodId
     * @return
     */
    @Override
    public List<ProdSku> querySkuOfProd(String tenantId, String prodId) {
        ProdSkuCriteria example = new ProdSkuCriteria();
        example.createCriteria().andTenantIdEqualTo(tenantId)
                .andProdIdEqualTo(prodId)
                .andStateNotEqualTo(ProductConstants.ProdSku.State.ACTIVE);
        return prodSkuMapper.selectByExample(example);
    }

    /**
     * 废弃指定SKU单品
     *
     * @param prodSku
     * @return
     */
    @Override
    public int updateSkuById(ProdSku prodSku) {
        prodSku.setOperTime(DateUtils.currTimeStamp());
        return prodSkuMapper.updateByPrimaryKey(prodSku);
    }

    /**
     * 添加商品SKU信息
     *
     * @param prodSku
     * @return
     */
    @Override
    public int createObj(ProdSku prodSku) {
        prodSku.setSkuId(SequenceUtil.genProdSkuId());
        prodSku.setOperTime(DateUtils.currTimeStamp());
        return prodSkuMapper.insert(prodSku);
    }
}
