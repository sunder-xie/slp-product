package com.ai.slp.product.service.business.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ai.opt.base.exception.BusinessException;
import com.ai.opt.base.vo.PageInfoResponse;
import com.ai.opt.sdk.util.BeanUtils;
import com.ai.slp.product.api.storage.param.StorageGroup4SaleList;
import com.ai.slp.product.api.storage.param.StorageGroupQueryPage;
import com.ai.slp.product.api.storage.param.StorageSalePrice;
import com.ai.slp.product.constants.StorageConstants;
import com.ai.slp.product.dao.mapper.bo.ProdPriceLog;
import com.ai.slp.product.dao.mapper.bo.storage.Storage;
import com.ai.slp.product.dao.mapper.bo.storage.StorageGroup;
import com.ai.slp.product.dao.mapper.bo.storage.StorageLog;
import com.ai.slp.product.dao.mapper.interfaces.storage.StorageMapper;
import com.ai.slp.product.service.atom.interfaces.IProdPriceLogAtomSV;
import com.ai.slp.product.service.atom.interfaces.IStandedProductAtomSV;
import com.ai.slp.product.service.atom.interfaces.storage.ISkuStorageAtomSV;
import com.ai.slp.product.service.atom.interfaces.storage.IStorageAtomSV;
import com.ai.slp.product.service.atom.interfaces.storage.IStorageGroupAtomSV;
import com.ai.slp.product.service.atom.interfaces.storage.IStorageLogAtomSV;
import com.ai.slp.product.service.business.interfaces.IStorageBusiSV;
import com.ai.slp.product.vo.StorageGroupPageQueryVo;

/**
 * 库存业务操作
 *
 * Created by jackieliu on 16/5/5.
 */
@Service
@Transactional
public class StorageBusiSVImpl implements IStorageBusiSV {
	private static Logger logger = LoggerFactory.getLogger(StorageBusiSVImpl.class);
	@Autowired
	IStorageGroupAtomSV storageGroupAtomSV;
	@Autowired
	IStorageAtomSV storageAtomSV;
	@Autowired
	IStorageLogAtomSV storageLogAtomSV;
	@Autowired
	ISkuStorageAtomSV skuStorageAtomSV;
	@Autowired
	private transient StorageMapper storageMapper;
	@Autowired
	IProdPriceLogAtomSV prodPriceLogAtomSV;
	@Autowired
	IStandedProductAtomSV standedProductAtomSV;

	/**
	 * 废弃库存
	 *
	 * @param storage
	 */
	@Override
	public void discardStorage(Storage storage) {
		// 废弃库存
		if (storageAtomSV.updateById(storage) > 0) {
			StorageLog storageLog = new StorageLog();
			BeanUtils.copyProperties(storageLog, storage);
			storageLogAtomSV.installLog(storageLog);
		}
		// 废弃SKU库存
		skuStorageAtomSV.discardSkuStorageOfStorage(storage.getStorageId(), storage.getOperId());
	}

	/**
	 * 自动停用库存
	 *
	 * @param storage
	 */
	@Override
	public void autoStopStorage(Storage storage) {
		// TODO...
	}

	/**
	 * 更改库存状态
	 *
	 * @param tenantId
	 * @param storageId
	 * @param state
	 * @param operId
	 */
	@Override
	public void changeStorageStats(String tenantId, String storageId, String state, Long operId) {
		// 查询库存是否存在
		Storage storage = storageAtomSV.queryById(storageId);
		if (storage == null) {
			throw new BusinessException("", "要查询库存不存在,库存标识:" + storageId);
		}
		// 查询状态是否变更
		if (storage.getState().equals(state)) {
			throw new BusinessException("", "库存状态已变更,不需重复变更");
		}
		// 废弃状态不允许变更
		if (StorageConstants.Storage.State.DISCARD.equals(storage.getState())
				|| StorageConstants.Storage.State.AUTO_DISCARD.equals(storage.getState()))
		{
			throw new BusinessException("", "库存已废弃,不允许变更状态");
		}

		switch (state) {
		case StorageConstants.Storage.State.ACTIVE:// 转启用
			//TODO 调用启用库存方法
			break;
		case StorageConstants.Storage.State.STOP:// 转停用
			//TODO 调用停用库存方法
			break;
		case StorageConstants.Storage.State.DISCARD:// 转废弃
			discardStorage(storage);
			break;
		default:
			throw new BusinessException("", "无法识别的状态:" + state);
		}
	}

	/**
	 * 批量更新库存销售价
	 *
	 * @param storageSalePrice
	 * @return
	 * @author lipeng16
	 */
	@Override
	public int updateMultiStorageSalePrice(StorageSalePrice storageSalePrice) {
		Map<String,Long> priceMap = storageSalePrice.getStorageSalePrice();
		if (priceMap==null||priceMap.isEmpty())
			return 0;
		String tenantId = storageSalePrice.getTenantId();
		Long operId = storageSalePrice.getOperId();
		int count = 0;
		for (String storageId : priceMap.keySet()) {
			//库存标识为空,库存对应价格为空,库存销售价小于等于0,均不处理
			if (StringUtils.isBlank(storageId)
					||priceMap.get(storageId)==null || priceMap.get(storageId)<=0){
				logger.warn("库存标识为空或销售价不大于零,库存标识[{0}],销售价[{1}]",storageId,priceMap.get(storageId));
				continue;
			}

			// 查看对应的标识下是否存在库存信息
			Storage storage0 = storageMapper.selectByPrimaryKey(storageId);
			if (storage0 == null) {
				logger.warn("未查询到指定库存,库存标识[{0}]",storageId);
				continue;
			}
			// 获取当前库存对应库存组
			StorageGroup storageGroup = storageGroupAtomSV.queryByGroupId(tenantId,storage0.getStorageGroupId());
			if (storageGroup==null
					|| storageGroup.getHighSalePrice()==null || storageGroup.getLowSalePrice()==null){
				logger.warn("未找到对应库存组或库存组为设置最低最高销售价,租户ID:{0},库存组标识:{1}"
						,tenantId,storage0.getStorageGroupId());
				throw new BusinessException("","未找到对应库存组或库存组未设置最低最高销售价,租户ID:"+tenantId+",库存组标识:"+storage0.getStorageGroupId());
			}
			// 判断销售价是否在库存组价格区间
			Long salePrice = priceMap.get(storageId);
			if (salePrice > storageGroup.getHighSalePrice()
					|| salePrice < storageGroup.getLowSalePrice()){
				throw new BusinessException("","库存["+storage0.getStorageName()+"]的价格必须在库存组的最低最高销售价范围内");
			}

			// 更新库存价格信息
			Storage storage = new Storage();
			storage.setStorageId(tenantId);
			storage.setSalePrice(salePrice);
			storage.setOperId(operId);
			int installNum = storageAtomSV.updateSaleById(storage);
			if (installNum > 0) {
				ProdPriceLog prodPriceLog = new ProdPriceLog();
				prodPriceLog.setObjId(storageId);
				prodPriceLog.setObjType("SO");
				prodPriceLog.setOperId(operId);
				prodPriceLog.setUpdatePrice(salePrice);
				prodPriceLogAtomSV.insert(prodPriceLog);
			}
			count++;
		}
		return count;
	}

	/**
	 * 启用库存
	 * 
	 * @param storage
	 */
	private void activeStorage(Storage storage) {
		// 检查库存可用量,若没有则设置为自动停用.

		// 若有库存,则检查库存组是否自动停用,若是,则自动启用
	}

	/**
	 * 查询标准品列表,包含标准品的库存组,适用于库存组定最低最高销售价初始页面信息查询<br>
	 * 库存组不包括废弃状态的
	 */
	@Override
	public PageInfoResponse<StorageGroup4SaleList> queryGroupsForSalePrice(StorageGroupQueryPage groupQuery) {
		StorageGroupPageQueryVo storageGroupPageQueryVo = new StorageGroupPageQueryVo();
		BeanUtils.copyProperties(storageGroupPageQueryVo, groupQuery);
		PageInfoResponse<StorageGroup> StorageGroupPage = storageGroupAtomSV.queryPageOfSearch(storageGroupPageQueryVo);
		// 设置返回对象
		PageInfoResponse<StorageGroup4SaleList> StorageGroup4SaleListPage = new PageInfoResponse<>();
		StorageGroup4SaleListPage.setPageNo(groupQuery.getPageNo());
		StorageGroup4SaleListPage.setPageSize(groupQuery.getPageSize());
		StorageGroup4SaleListPage.setPageCount(StorageGroupPage.getPageCount());
		
		List<StorageGroup> storageGroupList = StorageGroupPage.getResult();
		//新建结果集
		List<StorageGroup4SaleList> storGroup4SaleList = new ArrayList<>();
		for (StorageGroup storageGroup : storageGroupList) {
			StorageGroup4SaleList storageGroup4SaleList = new StorageGroup4SaleList();
			BeanUtils.copyProperties(storageGroup4SaleList, storageGroup);
			//通过ID查标准品名称
			storageGroup4SaleList.setStandedProductName(standedProductAtomSV
					.selectById(groupQuery.getTenantId(), storageGroup.getStandedProdId()).getStandedProductName());
			//填充结果集
			storGroup4SaleList.add(storageGroup4SaleList);
		}
		//设置结果集
		StorageGroup4SaleListPage.setResult(storGroup4SaleList);
		return StorageGroup4SaleListPage;
	}
}
