package com.ai.slp.product.api.normproduct;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ai.opt.base.vo.BaseResponse;
import com.ai.opt.base.vo.PageInfoResponse;
import com.ai.paas.ipaas.util.JSonUtil;
import com.ai.slp.product.api.normproduct.interfaces.INormProductSV;
import com.ai.slp.product.api.normproduct.param.*;
import com.ai.slp.product.constants.CommonTestConstants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Created by jiawen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:context/core-context.xml")
//public class IProductSVTest {
public class INormProductSVTest {
    @Autowired
    INormProductSV normProductSV;

    @Test
    public void queryNormProductTest(){
        NormProdRequest infoQuery = new NormProdRequest();
        infoQuery.setTenantId(CommonTestConstants.COMMON_TENANT_ID);
        infoQuery.setProductCatId("00000000000160");
        //infoQuery.setProductId("100000000100");
        PageInfoResponse<NormProdResponse> productInfo = normProductSV.queryNormProduct(infoQuery);
        System.out.println(productInfo.toString());
    }

    @Test
    public void queryNormProductAndKeyAttr(){
        NormProdRequest prodRequest = new NormProdRequest();
        prodRequest.setTenantId(CommonTestConstants.COMMON_TENANT_ID);
        prodRequest.setSupplierId("-1");
        prodRequest.setProductCatId("1");
        PageInfoResponse<NormProdAndKeyAttrRes> resPage = normProductSV.queryNormProductAndKeyAttr(prodRequest);
        System.out.println(resPage.getCount());
    }
    
    @Test
    public void addNormProduct(){
    	Gson gson = new Gson();
   // 	String data = "{'productCatId':'10000010010000','productName':'测试数据','state':'1','productType':'2','createId':1,'operId':1,'attrValList':[{'attrId':100001,'attrValId':'100003','attrVal':'','attrVal2':''},{'attrId':100002,'attrValId':'100008','attrVal':'','attrVal2':''},{'attrId':100004,'attrValId':'100013','attrVal':'','attrVal2':''},{'attrId':100004,'attrValId':'100014','attrVal':'','attrVal2':''}],'supplierId':'-1','tenantId':'changhong'}";
   // 	String data = "{'productCatId':'00000000000160','productName':'w测试数据','state':'1','productType':'2','createId':1,'operId':1,'attrValList':[{'attrId':101,'attrValId':'000000000071','attrVal':'红色','attrVal2':''}],'supplierId':'-1','tenantId':'changhong'}";
   // 	String data = "{'productCatId':'00000000000160','productName':'w测试数据','state':'1','productType':'2','createId':1,'operId':1,'supplierId':'-1','tenantId':'changhong'}";
     	String data = "{'productCatId':'000000000160','productName':'w1测试数据','state':'1','productType':'2','createId':1,'operId':1,'attrValList':[{'attrId':100001,'attrValId':'100003','attrVal':'','attrVal2':''},{'attrId':100002,'attrValId':'100008','attrVal':'','attrVal2':''},{'attrId':100004,'attrValId':'100013','attrVal':'','attrVal2':''},{'attrId':100004,'attrValId':'100014','attrVal':'','attrVal2':''}],'supplierId':'-1','tenantId':'changhong'}";
    	NormProdSaveRequest request = gson.fromJson(data, new TypeToken<NormProdSaveRequest>() {
		}.getType());
    	BaseResponse baseResponse = normProductSV.createProductAndStoGroup(request);
        System.out.println(JSonUtil.toJSon(baseResponse));
    }
    
    @Test
    public void updateNormProduct(){
    	Gson gson = new Gson();
    	String data = "{'productCatId':'1','productId':'1','productName':'测试销售属性商品 20160603无限额超级商品','state':'1','productType':'2','createId':1,'operId':1,'attrValList':[{'attrId':100001,'attrValId':'','attrVal':'','attrVal2':''},{'attrId':100004,'attrValId':'100013','attrVal':'','attrVal2':''},{'attrId':100004,'attrValId':'100014','attrVal':'','attrVal2':''},{'attrId':100004,'attrValId':'100015','attrVal':'','attrVal2':''},{'attrId':100010,'attrValId':'100082','attrVal':'','attrVal2':''},{'attrId':100010,'attrValId':'100084','attrVal':'','attrVal2':''}],'supplierId':'-1','tenantId':'changhong'}";
    	NormProdSaveRequest request = gson.fromJson(data, new TypeToken<NormProdSaveRequest>() {
		}.getType());
    	BaseResponse baseResponse = normProductSV.updateProductAndStoGroup(request);
        System.out.println(JSonUtil.toJSon(baseResponse));
    }

    @Test
    public void discardProductWithStorage(){
        NormProdUniqueReq uniqueReq = new NormProdUniqueReq();
        uniqueReq.setTenantId(CommonTestConstants.COMMON_TENANT_ID);
        uniqueReq.setSupplierId("-1");
        uniqueReq.setProductId("0000000000000103");
        uniqueReq.setOperId(1l);
        BaseResponse response = normProductSV.discardProductWithStorage(uniqueReq);
        System.out.print(response.getResponseHeader().isSuccess());
    }
    
    @Test
    public void discardProductTest(){
    	
    	NormProdUniqueReq req = new NormProdUniqueReq();
    	req.setTenantId("changhong");
    	req.setOperId(1l);
    	req.setSupplierId("-1");
    	req.setProductId("9000000000000360");
		BaseResponse discardProduct = normProductSV.discardProduct(req);
		System.out.print(discardProduct.getResponseHeader().isSuccess());
    	
    }
    
    @Test
    public void updateMarketPriceTest(){
    	MarketPriceUpdate priceUpdate = new MarketPriceUpdate();
    	priceUpdate.setTenantId("changhong");
    	priceUpdate.setSupplierId("-1");
    	priceUpdate.setProductId("0000000000000368");
    	priceUpdate.setOperId(1l);
    	priceUpdate.setMarketPrice(210);
    	BaseResponse price = normProductSV.updateMarketPrice(priceUpdate);
    	System.out.println(price.getResponseHeader().isSuccess());
    }
    
}
