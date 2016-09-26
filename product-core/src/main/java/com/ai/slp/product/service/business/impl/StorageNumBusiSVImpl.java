package com.ai.slp.product.service.business.impl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ai.opt.base.exception.BusinessException;
import com.ai.opt.sdk.util.BeanUtils;
import com.ai.opt.sdk.util.CollectionUtil;
import com.ai.paas.ipaas.mcs.interfaces.ICacheClient;
import com.ai.slp.product.api.storageserver.param.StorageNumRes;
import com.ai.slp.product.api.storageserver.param.StorageNumUseReq;
import com.ai.slp.product.constants.ErrorCodeConstants;
import com.ai.slp.product.constants.ProductConstants;
import com.ai.slp.product.constants.StorageConstants;
import com.ai.slp.product.dao.mapper.bo.product.ProdAudiences;
import com.ai.slp.product.dao.mapper.bo.product.ProdSku;
import com.ai.slp.product.dao.mapper.bo.product.Product;
import com.ai.slp.product.dao.mapper.bo.storage.SkuStorage;
import com.ai.slp.product.dao.mapper.bo.storage.Storage;
import com.ai.slp.product.service.atom.interfaces.product.IProdAudiencesAtomSV;
import com.ai.slp.product.service.atom.interfaces.product.IProdSkuAtomSV;
import com.ai.slp.product.service.atom.interfaces.product.IProductAtomSV;
import com.ai.slp.product.service.atom.interfaces.storage.ISkuStorageAtomSV;
import com.ai.slp.product.service.atom.interfaces.storage.IStorageAtomSV;
import com.ai.slp.product.service.atom.interfaces.storage.IStorageGroupAtomSV;
import com.ai.slp.product.service.business.interfaces.IStorageNumBusiSV;
import com.ai.slp.product.util.DateUtils;
import com.ai.slp.product.util.IPaasStorageUtils;
import com.ai.slp.product.vo.SkuStorageVo;

/**
 * Created by jackieliu on 16/5/25.
 */
@Service
@Transactional
public class StorageNumBusiSVImpl implements IStorageNumBusiSV {
    private static Logger logger = LoggerFactory.getLogger(StorageNumBusiSVImpl.class);
    @Autowired
    IProdSkuAtomSV skuAtomSV;
    @Autowired
    IProductAtomSV productAtomSV;
    @Autowired
    IProdAudiencesAtomSV prodAudiencesAtomSV;
    @Autowired
    IStorageGroupAtomSV storageGroupAtomSV;
    @Autowired
    IStorageAtomSV storageAtomSV;
    @Autowired
    ISkuStorageAtomSV skuStorageAtomSV;
    @Autowired
    StorageNumDbBusiSVImpl numDbBusiSV;
    /**
     * 使用库存量
     *
     * @param tenantId 租户ID
     * @param skuId    单品标识ID
     * @param skuNum   单品数量
     * @return
     */
    @Override
    public StorageNumRes userStorageNum(String tenantId, String skuId, int skuNum) {
        //查询SKU所属销售商品
        Product product = getProductBySkuId(tenantId,skuId);
        return userStorageNum(product,skuId,skuNum,null);
    }

    /**
     * 使用库存量
     *
     * @param useReq 使用信息
     * @return
     */
    @Override
    public StorageNumRes userNumWithAudiAndPrice(StorageNumUseReq useReq) {
        String tenantId = useReq.getTenantId(),skuId = useReq.getSkuId();
        //查询SKU所属销售商品
        Product product = getProductBySkuId(tenantId,skuId);
        //判断受众
        List<ProdAudiences> audiList = prodAudiencesAtomSV.queryByUserType(
                tenantId,product.getProdId(),useReq.getUserType(),useReq.getUserId(),false);
        if (CollectionUtil.isEmpty(audiList)){
            logger.warn("此商品不适用于该用户,租户ID:{},skuId:{},用户类型:{},用户标识:{}",
                    tenantId,skuId,useReq.getUserType(),useReq.getUserId());
            throw new BusinessException(ErrorCodeConstants.ProdAudiences.UNMATCHED,"此商品不适用于该用户");
        }
        return userStorageNum(product,skuId,useReq.getSkuNum(),useReq.getSalePrice());
    }


    private StorageNumRes userStorageNum(Product product, String skuId, int skuNum,Long price){
        String tenantId = product.getTenantId();
        String groupId = product.getStorageGroupId();
        Timestamp nowTime = DateUtils.currTimeStamp();
        //若商品为预售,且当前不在预售期内,则不进行销售
        if(ProductConstants.Product.UpShelfType.PRE_SALE.equals(product.getUpshelfType()) &&
                (nowTime.before(product.getPresaleBeginTime()) || nowTime.after(product.getPresaleEndTime()))){
            logger.warn("商品为预售上架,不在预售期[{}]和[{}]",product.getPresaleBeginTime().toString(),
                    product.getPresaleEndTime().toString());
            throw new BusinessException("","不在预售期内,不允许销售");
        }
        //获取缓存客户端
        ICacheClient cacheClient = IPaasStorageUtils.getClient();
        //2. 检查库存组状态是否为"启用"
        //获取库存组的cacheKey
        String groupKey = IPaasStorageUtils.genMcsStorageGroupKey(tenantId,groupId);
        //获取当前库存组状态
        String groupState = cacheClient.hget(groupKey,StorageConstants.IPass.McsParams.GROUP_STATE_HTAGE);
        //若库存组不是启用状态,则不允许使用
        if (!StorageConstants.StorageGroup.State.ACTIVE.equals(groupState)
                && !StorageConstants.StorageGroup.State.AUTO_ACTIVE.equals(groupState)){
            logger.warn("库存组没有启用,无法使用,租户ID:{},库存组ID:{}",tenantId,groupId);
            throw new BusinessException("","库存组没有启用,无法使用");
        }

        //3. 确认当前使用优先级
        //3.1 确认当前是否在促销期内
        String priority = getPromotionPriority(cacheClient,tenantId,groupId);
        //优先级价格对应KEY
        String priceKey = IPaasStorageUtils.genMcsGroupSerialPriceKey(tenantId,groupId,priority);
        //优先级中库存可用量对应KEY
        String priorityUsable = IPaasStorageUtils.genMcsPriorityUsableKey(tenantId,groupId,priority);

        /*3.2 以下情况使用正常优先级
         *  .未找到促销优先级
         *  .促销价格不存在,则表明促销已过期;
         *  .促销优先级库存可用量不存在,则表明促销已过期
         *  .促销优先级库存可用量小于1,则表明促销商品已售完,切换正常优先级.
         */
        if (StringUtils.isBlank(priority)
                || !cacheClient.exists(priceKey)
                || !cacheClient.exists(priorityUsable)
                || Long.parseLong(cacheClient.get(priorityUsable))<1){
            //若促销价格不存在,表明促销已过期,删除当前优先级的促销时间
            if (StringUtils.isNotBlank(priority)
                    && StringUtils.isNotBlank(priceKey)
                    && !cacheClient.exists(priceKey)) {
                String serialsKey = IPaasStorageUtils.genMcsGroupSerialStartTimeKey(tenantId,groupId);
                //删除促销期的优先级时间 ZREM serialsKey serial
                cacheClient.zrem(serialsKey,priority);
            }
            //使用库存组指定优先级
            priority = cacheClient.hget(groupKey,StorageConstants.IPass.McsParams.GROUP_SERIAL_HTAGE);
            priceKey = IPaasStorageUtils.genMcsGroupSerialPriceKey(tenantId,groupId,priority);
            //库存组当前优先级库存可用量
            priorityUsable = IPaasStorageUtils.genMcsPriorityUsableKey(tenantId,groupId,priority);
        }

        //4.获取当前优先级中SKU的销售价
        long salePrice = Long.parseLong(cacheClient.hget(priceKey,skuId));
        //若价格不为空,则进行价格判断
        if (price!=null && price.longValue()!=salePrice){
            logger.warn("商品价格不符,SkuId:{},传入价格:{},当前价格:{}",skuId,price,salePrice);
            throw new BusinessException(ErrorCodeConstants.Storage.PRICE_UN_MATCH,"商品价格不符");
        }
        //5.进行减sku库存
        String usableNumKey = IPaasStorageUtils.genMcsSerialSkuUsableKey(tenantId,groupId,priority);
        //若减少库存之后,剩余库存小于零,表示库存不足
        if (cacheClient.hincrBy(usableNumKey,skuId,-skuNum)<0){
            cacheClient.hincrBy(usableNumKey,skuId,skuNum);//需要将库存加回
            logger.warn("该商品库存不足,租户ID:{},库存组ID:{}",tenantId,groupId);
            throw new BusinessException(ErrorCodeConstants.Storage.UNDER_STOCK,"该商品库存不足");
        }
        //6.进行减少优先级库存可用量
        Long priorityUsableNum = cacheClient.decrBy(priorityUsable,skuNum);
        String skuStoragekey = IPaasStorageUtils.genMcsSkuStorageUsableKey(tenantId,groupId,priority,skuId);
        //8.组装返回值
        StorageNumRes numRes = new StorageNumRes();
        BeanUtils.copyProperties(numRes,product);
        numRes.setProductCatId(product.getProductCatId());
        numRes.setSkuId(skuId);
        numRes.setSkuName(product.getProdName());
        numRes.setSalePrice(salePrice);
        numRes.setBasicOrgId(product.getBasicOrgId());
        numRes.setStorageNum(getSkuNumSource(cacheClient,skuStoragekey,new Double(skuNum)));
        //变更数据库信息
        numDbBusiSV.storageNumChange(tenantId,skuId,numRes.getStorageNum(),true,priorityUsableNum<1?true:false);
        return numRes;
    }

    /**
     * 回退库存量
     *
     * @param tenantId   租户id
     * @param skuId      单品标识
     * @param storageNum 库存回退集合
     */
    @Override
    public void backStorageNum(String tenantId, String skuId, Map<String, Integer> storageNum) {
        if (storageNum==null || storageNum.isEmpty())
            return;
        //检查SKU是否存在
        ICacheClient cacheClient = IPaasStorageUtils.getClient();
        for (Map.Entry<String, Integer> entry:storageNum.entrySet()){
            String skuStorageId = entry.getKey();
            Integer skuNum = entry.getValue();
            //1. 根据SKU库存查询所属优先级
            //1.1 查询SKU库存信息
            SkuStorage skuStorage = skuStorageAtomSV.queryById(skuStorageId,true);
            if (skuStorage==null){
                logger.warn("库存回退过程中,未找到对应SKU库存,SKU库存标识:{}",skuStorageId);
                continue;
            }
            //1.2 查询SKU库存对应库存信息
            Storage storage = storageAtomSV.queryNoDiscardById(skuStorage.getStorageId());
            if (storage==null){
                logger.warn("库存回退过程中,未找到对应库存,SKU库存标识:{},库存标识:{}"
                        ,skuStorageId,skuStorage.getStorageId());
                continue;
            }
            String groupId = storage.getStorageGroupId(),
                    serial = storage.getSerialNumber().toString();
            //2. 回退缓存中库存所用量
            //2.1 回退优先级中,SKU可用量
            String skuUsableKey = IPaasStorageUtils.genMcsSerialSkuUsableKey(tenantId,groupId,serial);
            if (cacheClient.exists(skuUsableKey)){
                cacheClient.hincrBy(skuUsableKey,skuId,skuNum);
            }
            //2.2 回退优先级中,SKU库存可用量
            String skuStorageKey = IPaasStorageUtils.genMcsSkuStorageUsableKey(tenantId,groupId,serial,skuId);
            if (cacheClient.exists(skuStorageKey)){
                cacheClient.zincrby(skuStorageKey,skuNum,skuStorageId);
            }
            String priorityUsable = IPaasStorageUtils.genMcsPriorityUsableKey(tenantId,groupId,serial);
            //2.3 回退优先级中库存可用量
            if (cacheClient.exists(priorityUsable)){
                cacheClient.incrBy(priorityUsable,skuNum);
            }
        }
        //调用数据库异步处理方法
        numDbBusiSV.storageNumChange(tenantId,skuId,storageNum,false,false);
    }

    /**
     * 查询SKU的库存和价格信息
     *
     * @param tenantId
     * @param skuId
     * @return
     */
    @Override
    public SkuStorageVo queryStorageOfSku(String tenantId, String skuId) {
        ICacheClient cacheClient = IPaasStorageUtils.getClient();
        //查询SKU所属销售商品
        ProdSku skuInfo = skuAtomSV.querySkuById(tenantId,skuId);
        if (skuInfo==null){
            logger.warn("单品信息不存在,租户ID:{},SKU标识:{}",tenantId,skuId);
            throw new BusinessException(ErrorCodeConstants.Product.SKU_NO_EXIST,"单品信息不存在,单品标识:"+skuId);
        }
        //
        Product product = productAtomSV.selectByProductId(tenantId,skuInfo.getProdId());
        if (product==null){
            logger.warn("销售商品不存在,租户ID:{},商品标识:{}",tenantId,skuInfo.getProdId());
            throw new BusinessException(ErrorCodeConstants.Product.PRODUCT_NO_EXIST,"销售商品不存在,单品标识:"+skuId);
        }
        String groupId = product.getStorageGroupId();
        //获取库存组的cacheKey
        String groupKey = IPaasStorageUtils.genMcsStorageGroupKey(tenantId,groupId);
        //使用当前优先级
        String priority = getPromotionPriority(cacheClient,tenantId,groupId);
        //优先级价格对应KEY
        String priceKey = IPaasStorageUtils.genMcsGroupSerialPriceKey(tenantId,groupId,priority);
        //优先级中库存可用量对应KEY
        String priorityUsable = IPaasStorageUtils.genMcsPriorityUsableKey(tenantId,groupId,priority);

        /* 以下情况使用正常优先级
         *  .未找到促销优先级
         *  .促销价格不存在,则表明促销已过期;
         *  .促销优先级库存可用量不存在,则表明促销已过期
         *  .促销优先级库存可用量小于1,则表明促销商品已售完,切换正常优先级.
         */
        if (StringUtils.isBlank(priority)
                || !cacheClient.exists(priceKey)
                || !cacheClient.exists(priorityUsable)
                || Long.parseLong(cacheClient.get(priorityUsable))<1){
            //使用库存组指定优先级
            priority = cacheClient.hget(groupKey,StorageConstants.IPass.McsParams.GROUP_SERIAL_HTAGE);
            priceKey = IPaasStorageUtils.genMcsGroupSerialPriceKey(tenantId,groupId,priority);
        }
        //获取当前优先级中SKU的销售价
        String skuPriceCache= cacheClient.hget(priceKey,skuId);
        Long salePrice = null;
        try{
            if (StringUtils.isNotBlank(skuPriceCache))
                salePrice = Long.parseLong(skuPriceCache);
        }catch (NumberFormatException e){
            logger.error("Formate sale price is error,salePrice="+skuPriceCache,e);
        }
        Long.parseLong(cacheClient.hget(priceKey,skuId));
        String skuUsableKey = IPaasStorageUtils.genMcsSerialSkuUsableKey(tenantId,groupId,priority);
        //
        String skuUsableStr = cacheClient.hget(skuUsableKey,skuId);
        Long skuUsable = StringUtils.isBlank(skuUsableStr)?null:Long.parseLong(cacheClient.hget(skuUsableKey,skuId));
        SkuStorageVo skuStorageVo = new SkuStorageVo();
        skuStorageVo.setUsableNum(skuUsable);
        skuStorageVo.setSalePrice(salePrice);
        skuStorageVo.setSkuId(skuId);
        return skuStorageVo;
    }

    /**
     * 查询当前库存组的可用量
     *
     * @param tenantId
     * @param groupId
     * @return
     */
    @Override
    public Long queryNowUsableNumOfGroup(String tenantId, String groupId) {
        ICacheClient cacheClient = IPaasStorageUtils.getClient();
        //获取库存组的cacheKey
        String groupKey = IPaasStorageUtils.genMcsStorageGroupKey(tenantId,groupId);
        //使用当前优先级
        String priority = getPromotionPriority(cacheClient,tenantId,groupId);
        //优先级价格对应KEY
        String priceKey = IPaasStorageUtils.genMcsGroupSerialPriceKey(tenantId,groupId,priority);
        //优先级中库存可用量对应KEY
        String priorityUsable = IPaasStorageUtils.genMcsPriorityUsableKey(tenantId,groupId,priority);
        /* 以下情况使用正常优先级
         *  .未找到促销优先级
         *  .促销价格不存在,则表明促销已过期;
         *  .促销优先级库存可用量不存在,则表明促销已过期
         *  .促销优先级库存可用量小于1,则表明促销商品已售完,切换正常优先级.
         */
        if (StringUtils.isBlank(priority)
                || !cacheClient.exists(priceKey)
                || !cacheClient.exists(priorityUsable)
                || Long.parseLong(cacheClient.get(priorityUsable))<1){
            //使用库存组指定优先级
            priority = cacheClient.hget(groupKey,StorageConstants.IPass.McsParams.GROUP_SERIAL_HTAGE);
            //库存组当前优先级库存可用量
            priorityUsable = IPaasStorageUtils.genMcsPriorityUsableKey(tenantId,groupId,priority);
        }
        String usableNumStr = cacheClient.get(priorityUsable);
        return StringUtils.isBlank(usableNumStr)?0:Long.parseLong(usableNumStr);
    }

    /**
     * 获知商品使用量的SKU库存来源
     *
     * @param cacheClient
     * @param cacheKey sku库存缓存KEY
     * @param skuNum 单品数量
     * @return
     */
    private Map<String,Integer> getSkuNumSource(ICacheClient cacheClient,String cacheKey,Double skuNum){
        Map<String,Integer> skuNumMap = new HashMap<>();
        //查询库存量大于零的 ZRANGE 1  +inf
        Set<String> storageSet = cacheClient.zrangeByScore(cacheKey,1,Long.MAX_VALUE);
        if (CollectionUtil.isEmpty(storageSet)){
            logger.error("Query storage is error,cache key is [{}]",cacheKey);
        }else {

            logger.info("storage num:{}", storageSet.size());
        }
        //获取库存大于1的SKU库存标识
        String storageId = storageSet.toArray(new String[0])[0];
        //修改库存 ZINCRBY -skuNum
        Double skuStorage = cacheClient.zincrby(cacheKey,-skuNum,storageId);
        Integer num = (int)(skuStorage+skuNum);

        //若库存还有剩余,表示库存足够,直接返回
        if (skuStorage>=0){
            skuNumMap.put(storageId,skuNum.intValue());
            return skuNumMap;
        }
        //若库存小于0,且操作数与数量之和大于零,则表示从该库存中出库一部分
        else if (skuStorage<0 && num>0){
            cacheClient.zincrby(cacheKey,-skuStorage,storageId);//返回多减
            skuNumMap.put(storageId,num);
        }
        //库存小于0,且操作数与使用量之和小于零,则标识库存不足
        else if(skuStorage<0 && num<0){
            skuStorage = -skuNum;
            cacheClient.zincrby(cacheKey,skuNum,storageId);//返回多减
        }
        skuNumMap.putAll(getSkuNumSource(cacheClient,cacheKey,-skuStorage));
        return skuNumMap;
    }

    /**
     * 获取促销优先级
     * @param cacheClient
     * @param tenantId
     * @param groupId
     * @return
     */
    private String getPromotionPriority(ICacheClient cacheClient,String tenantId,String groupId){
        String serialsKey = IPaasStorageUtils.genMcsGroupSerialStartTimeKey(tenantId,groupId);
        long nowTime = System.currentTimeMillis();
        //使用优先级
        String serial = "";
        Set<String> serialSet = cacheClient.zrevrangeByScore(serialsKey,nowTime,0);
        if (!CollectionUtil.isEmpty(serialSet)){
            serial = serialSet.toArray(new String[0])[0];
        }
        return serial;
    }

    /**
     * 根据SKU标识查询商品信息
     * @param tenantId
     * @param skuId
     * @return
     */
    private Product getProductBySkuId(String tenantId,String skuId){
        //查询SKU所属销售商品
        ProdSku skuInfo = skuAtomSV.querySkuById(tenantId,skuId);
        if (skuInfo==null){
            logger.warn("单品信息不存在,租户ID:{},SKU标识:{}",tenantId,skuId);
            throw new BusinessException(ErrorCodeConstants.Product.SKU_NO_EXIST,"单品信息不存在,单品标识:"+skuId);
        }
        //1. 查询商品是否为"在售"状态
        Product product = productAtomSV.selectByProductId(tenantId,skuInfo.getProdId());
        if (product==null || !ProductConstants.Product.State.IN_SALE.equals(product.getState())){
            logger.warn("销售商品不存在,或已下架,租户ID:{},SKU标识:{},销售商品标识{}"
                    ,tenantId,skuId,skuInfo.getProdId());
            throw new BusinessException(ErrorCodeConstants.Product.PRODUCT_NO_EXIST,"销售商品不存在,或已下架状态");
        }
        return product;
    }

}
