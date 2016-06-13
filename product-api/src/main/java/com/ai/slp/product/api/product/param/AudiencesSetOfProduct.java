package com.ai.slp.product.api.product.param;

import java.io.Serializable;
import java.util.Map;

/**
 * 单个商品各受众类型的信息集合<br>
 *
 * Date: 2016年4月26日 <br>
 * Copyright (c) 2016 asiainfo.com <br>
 * @author liutong5
 */
public class AudiencesSetOfProduct implements Serializable {
    private static final long serialVersionUID = 1L;

	/**
     * 个人受众信息
     */
    private ProdAudiencesInfo personAudiences;

    /**
     * 企业用户受众集合<br>
     * K:用户标识; V:受众信息
     */
    private Map<String,ProdAudiencesInfo> enterpriseMap;
    /**
     * 代理商受众集合
     * K:用户标识; V:受众信息
     */
    private Map<String,ProdAudiencesInfo> agentsMap;

    public ProdAudiencesInfo getPersonAudiences() {
        return personAudiences;
    }

    public void setPersonAudiences(ProdAudiencesInfo personAudiences) {
        this.personAudiences = personAudiences;
    }

    public Map<String, ProdAudiencesInfo> getEnterpriseMap() {
        return enterpriseMap;
    }

    public void setEnterpriseMap(Map<String, ProdAudiencesInfo> enterpriseMap) {
        this.enterpriseMap = enterpriseMap;
    }

    public Map<String, ProdAudiencesInfo> getAgentsMap() {
        return agentsMap;
    }

    public void setAgentsMap(Map<String, ProdAudiencesInfo> agentsMap) {
        this.agentsMap = agentsMap;
    }
}
