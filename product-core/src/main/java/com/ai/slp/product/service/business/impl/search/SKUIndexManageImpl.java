package com.ai.slp.product.service.business.impl.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ai.opt.base.exception.SystemException;
import com.ai.opt.sdk.components.ses.SESClientFactory;
import com.ai.opt.sdk.util.BeanUtils;
import com.ai.opt.sdk.util.CollectionUtil;
import com.ai.paas.ipaas.search.ISearchClient;
import com.ai.paas.ipaas.search.vo.SearchCriteria;
import com.ai.paas.ipaas.search.vo.SearchOption;
import com.ai.slp.product.constants.ProductConstants;
import com.ai.slp.product.constants.SearchConstants;
import com.ai.slp.product.constants.SearchFieldConfConstants;
import com.ai.slp.product.dao.mapper.attach.ProdSkuInfoSes;
import com.ai.slp.product.dao.mapper.bo.ProductCat;
import com.ai.slp.product.dao.mapper.bo.product.ProdAudiences;
import com.ai.slp.product.dao.mapper.bo.product.ProdPicture;
import com.ai.slp.product.dao.mapper.bo.product.ProdSaleAll;
import com.ai.slp.product.dao.mapper.bo.product.ProdTargetArea;
import com.ai.slp.product.search.bo.*;
import com.ai.slp.product.service.atom.interfaces.IProdCatDefAtomSV;
import com.ai.slp.product.service.atom.interfaces.product.*;
import com.ai.slp.product.service.atom.interfaces.storage.ISkuStorageAtomSV;
import com.ai.slp.product.service.business.interfaces.search.ISKUIndexManage;

/**
 * 搜索信息管理
 * Created by xin on 16-6-1.
 */
@Component
public class SKUIndexManageImpl implements ISKUIndexManage {

    private Logger logger = LogManager.getLogger(SKUIndexManageImpl.class);

    @Autowired
    private IProdCatDefAtomSV prodCatDefAtomSV;
    @Autowired
    private IProdSkuAtomSV prodSkuAtomSV;
    @Autowired
    private IProdSaleAllAtomSV prodSaleAllAtomSV;
    @Autowired
    private IProdAttrAtomSV prodAttrAtomSV;
    @Autowired
    private IProdAudiencesAtomSV prodAudiencesAtomSV;
    @Autowired
    private IProdTargetAreaAtomSV prodTargetAreaAtomSV;
    @Autowired
    private ISkuStorageAtomSV skuStorageAtomSV;
    @Autowired
    private IProdPictureAtomSV prodPictureAtomSV;


    /**
     * 更新搜索信息
     * @param productId 商品标识
     * @return
     */
    @Override
    public boolean updateSKUIndex(String productId) {
        try {
            List<ProdSkuInfoSes> skuInfoSesList =prodSkuAtomSV.queryOfProdForSearch(productId);
            if (CollectionUtil.isEmpty(skuInfoSesList))
                return true;
            List<SKUInfo> skuInfoList = new ArrayList<>();
            for (ProdSkuInfoSes prodSkuInfo:skuInfoSesList){
                SKUInfo skuInfo = new SKUInfo();
                BeanUtils.copyProperties(skuInfo,prodSkuInfo);
                skuInfo.setUptime(prodSkuInfo.getProdUpTime().getTime());
                //类目
                skuInfo.setCategoryinfos(new ArrayList<CategoryInfo>());
                fetchCategory(skuInfo,prodSkuInfo.getProductcategoryid());
                //属性
                skuInfo.setAttrinfos(prodAttrAtomSV.queryAttrOfProdId(productId));
                // 销售量
                ProdSaleAll prodSaleAll = prodSaleAllAtomSV.querySaleAllOfSku(
                        prodSkuInfo.getTenantid(),prodSkuInfo.getSkuid());
                skuInfo.setSalenum(prodSaleAll==null?0:prodSaleAll.getSaleNum());
                // 图片
                fillSKUImageInfo(skuInfo,prodSkuInfo.getProductcategoryid(),productId,prodSkuInfo.getSkuid());
                // 价格
                skuInfo.setPrice(
                        skuStorageAtomSV.queryPriceOfSku(prodSkuInfo.getTenantid(),productId,prodSkuInfo.getSkuid()));
                // 受众
                skuInfo.setAudiences(fillSKUAudiences(prodSkuInfo.getTenantid(),productId));
                //销售地域
                List<SaleAreaInfo> areaInfoList = new ArrayList<>();
                //若不是全国销售,则查询销售地域
                if (ProductConstants.Product.IsSaleNationwide.NO.equals(prodSkuInfo.getSalenationwide()))
                    areaInfoList = fillSKUSaleArea(prodSkuInfo.getTenantid(),productId);
                skuInfo.setSaleareainfos(areaInfoList);
                skuInfoList.add(skuInfo);
            }
            if (!CollectionUtil.isEmpty(skuInfoList))
                SESClientFactory.getSearchClient(SearchConstants.SearchNameSpace).bulkInsert(skuInfoList);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update sku info", e);
            throw new SystemException("","商品加入搜索引擎失败,商品ID:"+productId);
        }

    }

    /**
     * 删除指定SKU搜索信息
     * @param skuId sku标识
     * @return
     */
    @Override
    public boolean deleteSKUIndexBySKUId(String skuId) {
        try {
            List<SearchCriteria> searchfieldVos = new ArrayList<SearchCriteria>();
            searchfieldVos.add(new SearchCriteria(SearchFieldConfConstants.SKU_ID, skuId,
                    new SearchOption(SearchOption.SearchLogic.must, SearchOption.SearchType.term)));
            ISearchClient client = getSearchClient();
            return client.delete(searchfieldVos);
        } catch (Exception e) {
            logger.error("Failed to delete sku info", e);
        }
        return false;
    }

    /**
     * 删除指定商品搜索信息
     * @param productId 商品标识
     * @return
     */
    @Override
    public boolean deleteSKUIndexByProductId(String productId) {
        try {
            List<SearchCriteria> searchfieldVos = new ArrayList<SearchCriteria>();
            searchfieldVos.add(new SearchCriteria(
                    SearchFieldConfConstants.PRODUCT_ID, productId,
                    new SearchOption(SearchOption.SearchLogic.must, SearchOption.SearchType.term)));
            ISearchClient client = getSearchClient();
            return client.delete(searchfieldVos);
        } catch (Exception e) {
            logger.error("Failed to delete sku info", e);
        }
        return false;
    }

    private ISearchClient getSearchClient(){
        return SESClientFactory.getSearchClient(SearchConstants.SearchNameSpace);
    }

    /**
     * 商品类目链信息
     * @param skuInfo
     * @param prodCatId
     */
    private void fetchCategory(SKUInfo skuInfo, String prodCatId) {
        ProductCat productCat = prodCatDefAtomSV.selectById(prodCatId);
        if (productCat == null)
            return;

        CategoryInfo categoryInfo = new CategoryInfo(productCat.getProductCatId(),
                productCat.getProductCatName());
        skuInfo.addCategoryInfo(categoryInfo);
        String categoryId = productCat.getParentProductCatId();

        if (!"0".equals(categoryId)) {
            fetchCategory(skuInfo, categoryId);
        } else {
            skuInfo.setRootcategorid(categoryId);
        }
    }

    /**
     * 商品受众信息
     * @param productId
     */
    private List<ProdAudiencesSes> fillSKUAudiences(String tenantId, String productId) {
        List<ProdAudiences> audiencesList = prodAudiencesAtomSV.queryOfProductByProdId(tenantId,
                productId, false);
        List<ProdAudiencesSes> sesAudiList = new ArrayList<>();
        if (CollectionUtil.isEmpty(audiencesList))
            return sesAudiList;
        for (ProdAudiences prodAudiences : audiencesList) {
            sesAudiList.add(
                    new ProdAudiencesSes(prodAudiences.getUserType(), prodAudiences.getUserId()));
        }
        return sesAudiList;
    }

    /**
     * 商品目标地域
     * @param tenantId
     * @param productId
     * @return
     */
    public List<SaleAreaInfo> fillSKUSaleArea(String tenantId,String productId){
        List<ProdTargetArea> targetAreaList = prodTargetAreaAtomSV.searchProdTargetArea(tenantId,productId);
        List<SaleAreaInfo> saleAreaInfoList = new ArrayList<>();
        if (CollectionUtil.isEmpty(targetAreaList))
            return saleAreaInfoList;
        for (ProdTargetArea targetArea:targetAreaList){
            saleAreaInfoList.add(new SaleAreaInfo(targetArea.getProvCode().toString()));
        }
        return saleAreaInfoList;
    }

    public void fillSKUImageInfo(SKUInfo skuInfo,String prodCatId,String prodId,String skuId) {
        //查询商品对应的属性值是否有图片
        ImageInfo imageInfo = prodPictureAtomSV.queryMainOfSku(prodCatId,skuId);
        //若SKU没有属性值图片,则查询所属商品的主图
        if (imageInfo==null) {
            ProdPicture prodPicture = prodPictureAtomSV.queryMainOfProd(prodId);
            imageInfo = new ImageInfo(prodPicture.getPicType(),prodPicture.getVfsId());
        }
        skuInfo.setImageinfo(imageInfo);
        //查询该商品其他属性值的主图
        skuInfo.setThumbnail(prodPictureAtomSV.queryAttrValOfProd(prodId));
    }
}
