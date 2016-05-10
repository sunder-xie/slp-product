package com.ai.slp.product.service.business.interfaces;

import com.ai.slp.product.api.storage.param.StorageGroup;
import com.ai.slp.product.api.storage.param.StorageGroupRes;
import com.ai.slp.product.api.storage.param.StorageGroupSalePrice;
import com.ai.slp.product.api.storage.param.StorageGroupUpName;

import java.util.List;

/**
 * 库存组业务操作
 * Created by jackieliu on 16/5/4.
 */
public interface IStorageGroupBusiSV {

    /**
     * 添加库存组
     *
     * @param storageGroup
     * @return
     */
    public int addGroup(StorageGroup storageGroup);

    /**
     * 更新库存组
     *
     * @param storageGroup
     * @return
     */
    public int updateGroupName(StorageGroupUpName storageGroup);

    /**
     * 查询指定标准品下的库存组信息,包括库存信息
     *
     * @param tenantId
     * @param productId
     * @return
     */
    public List<StorageGroupRes> queryGroupInfoByNormProId(String tenantId, String productId);

    /**
     * 查询单个库存组的信息
     *
     * @param tenantId
     * @param groupId
     * @return
     */
    public StorageGroupRes queryGroupInfoByGroupId(String tenantId, String groupId);
    
    /**
     * 更新库存组价格信息
     * @param salePrice
     * @return
     * @author lipeng
    *  @ApiCode
     */
    public int updateStorageGroupPrice(StorageGroupSalePrice salePrice);

    /**
     * 变更库存组状态
     * @param tenantId 租户ID
     * @param groupId 库存组ID
     * @param state 要变更状态
     * @param operId 操作者ID
     */
    public void updateGroupState(String tenantId,String groupId,String state,Long operId);
}
