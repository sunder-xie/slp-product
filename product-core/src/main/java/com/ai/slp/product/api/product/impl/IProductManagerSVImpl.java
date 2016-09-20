package com.ai.slp.product.api.product.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ai.opt.base.exception.BusinessException;
import com.ai.opt.base.exception.SystemException;
import com.ai.opt.base.vo.BaseResponse;
import com.ai.opt.base.vo.PageInfoResponse;
import com.ai.slp.product.api.product.interfaces.IProductManagerSV;
import com.ai.slp.product.api.product.param.*;
import com.ai.slp.product.service.business.interfaces.IProductBusiSV;
import com.ai.slp.product.service.business.interfaces.IProductManagerBusiSV;
import com.ai.slp.product.service.business.interfaces.search.ISKUIndexManage;
import com.ai.slp.product.util.CommonUtils;
import com.alibaba.dubbo.config.annotation.Service;

/**
 * Created by jackieliu on 16/6/6.
 */
@Service(validation = "true")
@Component
public class IProductManagerSVImpl implements IProductManagerSV {
    @Autowired
    IProductManagerBusiSV productManagerBusiSV;
    @Autowired
    IProductBusiSV productBusiSV;
    @Autowired
    ISKUIndexManage skuIndexManage;
    /**
     * 商品管理查询商品编辑状态<br>
     * 状态 1未编辑2已编辑<br>
     * 按生成时间排序<br>
     *
     * @param productEditParam
     * @return 满足条件的商品集合
     * @throws BusinessException
     * @throws SystemException
     * @author lipeng16
     */
    @Override
    public PageInfoResponse<ProductEditUp> queryProductEdit(ProductEditQueryReq productEditParam) throws BusinessException, SystemException {
        CommonUtils.checkTenantId(productEditParam.getTenantId());
        CommonUtils.checkSupplierId(productEditParam.getSupplierId());
        return productManagerBusiSV.queryPageForEdit(productEditParam);
    }

    /**
     * 商品管理查询商品被拒绝信息-与商品审核中查询共用<br>
     * 拒绝时间为操作时间<br>
     *
     * @param productRefuseParam
     * @return 满足条件的商品集合
     * @throws BusinessException
     * @throws SystemException
     * @author lipeng16
     */
    @Override
    public PageInfoResponse<ProductEditUp> queryProductRefuse(ProductEditQueryReq productRefuseParam) throws BusinessException, SystemException {
    	CommonUtils.checkTenantId(productRefuseParam.getTenantId());
        CommonUtils.checkSupplierId(productRefuseParam.getSupplierId());
    	return productManagerBusiSV.queryProductRefuse(productRefuseParam);
    }

    /**
     * 审核是否通过调用方法<br>
     * 通过上架类型 1审核通过后立即上架 2审核通过后放入仓库 3定时上架<br>
     * 判断改变为哪种状态<br>
     * 销售商品状态 4审核未通过<br>
     * 拒绝后把拒绝类型和原因存入销售商品流程日志表<br>
     *
     * @param productCheckParam
     * @return 基本信息
     * @throws BusinessException
     * @throws SystemException
     * @author lipeng16
     */
    @Override
    public BaseResponse productCheck(ProductCheckParam productCheckParam) throws BusinessException, SystemException {
        CommonUtils.checkTenantId(productCheckParam.getTenantId());
        List<String> prodIndexList = productManagerBusiSV.auditProduct(productCheckParam);
        for (String prodId:prodIndexList){
            skuIndexManage.updateSKUIndex(prodId);
        }
        return CommonUtils.genSuccessResponse("OK");
    }

    /**
     * 商品申请优先审核调用方法<br>
     *
     * @param productPriorityParam
     * @return 基本信息
     * @throws BusinessException
     * @throws SystemException
     * @author lipeng16
     */
    @Override
    public BaseResponse productPriority(ProductPriorityParam productPriorityParam) throws BusinessException, SystemException {
        return null;
    }

    /**
     * 查询单个商品的其他设置信息<br>
     *
     * @param productInfoQuery 单个商品的标识信息
     * @return 单个商品的受众信息
     * @throws BusinessException
     * @throws SystemException
     * @author liutong5
     */
    @Override
    public OtherSetOfProduct queryOtherSetOfProduct(ProductInfoQuery productInfoQuery) throws BusinessException, SystemException {
        CommonUtils.checkTenantId(productInfoQuery.getTenantId());
        OtherSetOfProduct otherSet =  productManagerBusiSV.queryOtherSetOfProd(
                productInfoQuery.getTenantId(),productInfoQuery.getSupplierId(),productInfoQuery.getProductId());
        CommonUtils.addSuccessResHeader(otherSet,"OK");
        return otherSet;
    }

    /**
     * 分页查询仓库中的商品列表<br>
     * 商品状态:6仓库中（审核通过放入） 61售罄下架 定时上架-通过上架类型判断  62废弃下架<br>
     *
     * @param productStorageSaleParam
     * @return 商品管理售中与仓库商品返回类集合
     * @throws BusinessException
     * @throws SystemException
     * @author lipeng16
     */
    @Override
    public PageInfoResponse<ProductStorageSale> queryStorageProdByState(ProductStorageSaleParam productStorageSaleParam) throws BusinessException, SystemException {
    	CommonUtils.checkTenantId(productStorageSaleParam.getTenantId());
        return productManagerBusiSV.queryStorageProdByState(productStorageSaleParam);
    }

    /**
     * 对商品进行上架处理
     *
     * @param query
     * @return
     * @throws BusinessException
     * @throws SystemException
     * @author liutong5
     */
    @Override
    public BaseResponse changeToInSale(ProductInfoQuery query) throws BusinessException, SystemException {
        CommonUtils.checkTenantId(query.getTenantId());
        productBusiSV.changeToInSale(query.getTenantId(),query.getSupplierId(),query.getProductId(),query.getOperId());
        //将商品添加至搜索引擎
        skuIndexManage.updateSKUIndex(query.getProductId());
        return CommonUtils.addSuccessResHeader(new BaseResponse(),"");
    }

    /**
     * 为编辑页面查询商品非关键属性
     *
     * @param query
     * @return
     * @throws BusinessException
     * @throws SystemException
     */
    @Override
    public ProdNoKeyAttr queryNoKeyAttrOfProd(ProductInfoQuery query) throws BusinessException, SystemException {
        CommonUtils.checkTenantId(query.getTenantId());
        return productBusiSV.queryNoKeyAttrForEdit(query.getTenantId(),query.getProductId());
    }

    /**
     * 更新商品信息
     * @param product
     * @return
     * @throws BusinessException
     * @throws SystemException
     */
    @Override
    public BaseResponse updateProduct(ProductInfoForUpdate product) throws BusinessException, SystemException {
        CommonUtils.checkTenantId(product.getTenantId());
        productManagerBusiSV.updateProdEdit(product);
        return CommonUtils.addSuccessResHeader(new BaseResponse(),"");
    }

    /**
     * 对商品进行手动下架处理
     * @param query
     * @return
     * @throws BusinessException
     * @throws SystemException
     * @author jiawen
     */
	@Override
	public BaseResponse changeToInStore(ProductInfoQuery query) throws BusinessException, SystemException {
		CommonUtils.checkTenantId(query.getTenantId());
        productBusiSV.changeSaleToStore(
                query.getTenantId(),query.getSupplierId(),query.getProductId(),query.getOperId());
        return CommonUtils.genSuccessResponse("");
	}

	/**
	 * 查询在售商品 -- 按上架时间排序
	 * 
	 * @param queryInSale
     * @return
     * @throws BusinessException
     * @throws SystemException
     * @author jiawen
	 */
	@Override
	public PageInfoResponse<ProductEditUp> searchInSale(ProductQueryInfo queryInSale) throws BusinessException, SystemException {
		CommonUtils.checkTenantId(queryInSale.getTenantId());
        CommonUtils.checkSupplierId(queryInSale.getSupplierId());
        return productManagerBusiSV.queryInSale(queryInSale);
	}

	/**
	 * 查询商品审核
	 * 
	 * @param queryInfo
     * @return
     * @throws BusinessException
     * @throws SystemException
     * @author jiawen
	 */
	@Override
	public PageInfoResponse<ProductEditUp> searchAudit(ProductQueryInfo queryInfo)
			throws BusinessException, SystemException {
		CommonUtils.checkTenantId(queryInfo.getTenantId());
        CommonUtils.checkSupplierId(queryInfo.getSupplierId());
        return productManagerBusiSV.queryPageForAudit(queryInfo);
	}

	/**
	 * 查询商品拒绝原因
	 * @param productRefuseParam
     * @return 满足条件的商品集合
     * @throws BusinessException
     * @throws SystemException
     * @author jiawen
	 */
	@Override
	public PageInfoResponse<ProductEditUp> queryRefuse(ProductEditQueryReq productRefuseParam)
			throws BusinessException, SystemException {
		CommonUtils.checkTenantId(productRefuseParam.getTenantId());
        CommonUtils.checkSupplierId(productRefuseParam.getSupplierId());
    	return productManagerBusiSV.queryRefuse(productRefuseParam);
	}
}
