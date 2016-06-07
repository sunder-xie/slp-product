package com.ai.slp.product.service.business;

import com.ai.opt.base.vo.PageInfoResponse;
import com.ai.slp.product.api.product.param.ProductEditQueryReq;
import com.ai.slp.product.api.product.param.ProductEditUp;
import com.ai.slp.product.constants.CommonConstants;
import com.ai.slp.product.constants.ProductConstants;
import com.ai.slp.product.service.business.interfaces.IProductManagerBsuiSV;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by jackieliu on 16/6/6.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:context/core-context.xml")
public class IProductManagerBsuiSVTest {
    @Autowired
    IProductManagerBsuiSV productManagerBsuiSV;

    @Test
    public void queryPageForEditTest(){
        ProductEditQueryReq queryReq = new ProductEditQueryReq();
        queryReq.setTenantId(CommonConstants.COMMON_TENANT_ID);
        queryReq.setState(ProductConstants.Product.State.ADD);
        queryReq.setProductCatId("1");//
        PageInfoResponse<ProductEditUp> response = productManagerBsuiSV.queryPageForEdit(queryReq);
        System.out.println("\r"+response.getCount()+",size="+response.getResult().size());
    }
}