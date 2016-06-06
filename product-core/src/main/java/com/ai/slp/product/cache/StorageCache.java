package com.ai.slp.product.cache;

import com.ai.opt.sdk.cache.base.AbstractCache;
import com.ai.slp.product.dao.mapper.bo.storage.StorageGroup;
import com.ai.slp.product.service.atom.interfaces.storage.IStorageGroupAtomSV;
import com.ai.slp.product.service.business.interfaces.IStorageGroupBusiSV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 刷新库存相关缓存
 * Created by jackieliu on 16/6/6.
 */
@Component
public class StorageCache extends AbstractCache {
    private static Logger logger = LoggerFactory.getLogger(StorageCache.class);
    @Autowired
    IStorageGroupBusiSV groupBusiSV;
    @Autowired
    IStorageGroupAtomSV groupAtomSV;
    private static final int PAGE_SIZE = 200;

    @Override
    public void write() throws Exception {
        //获取库存组总数,进行多线程刷新
        //查询总条数
        int total = groupAtomSV.countOfNoDiscard();
        //获取分页数
        int pageNum = (total+PAGE_SIZE-1)/PAGE_SIZE;
        ExecutorService pool=null;
        try{

            //ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            pool = Executors.newCachedThreadPool();
            for (int i = 1; i <= pageNum; i++){
                List<StorageGroup> groupList = groupAtomSV.queryOfPage(i,PAGE_SIZE,false);
                Thread t =new StorageCacheFlushThread(groupBusiSV,groupList);
                pool.execute(t);
            }
        }catch(Exception e){
            logger.info(e.getMessage(),e);
        }finally{
            if(pool!=null){
                pool.shutdown();
            }
        }
    }
}
