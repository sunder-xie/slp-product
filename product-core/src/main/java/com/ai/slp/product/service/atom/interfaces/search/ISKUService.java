package com.ai.slp.product.service.atom.interfaces.search;

import com.ai.slp.product.search.bo.SKUInfo;

import java.util.List;

/**
 * Created by xin on 16-6-1.
 */
public interface ISKUService {

    List<SKUInfo> getSKUInfoByProductId(String productId);
}