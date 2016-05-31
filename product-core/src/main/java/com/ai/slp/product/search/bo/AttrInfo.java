package com.ai.slp.product.search.bo;

import com.google.gson.annotations.Expose;

/**
 * Created by xin on 16-5-13.
 */
public class AttrInfo {
    @Expose
    private String attrvalue;
    @Expose
    private String attrid;
    @Expose
    private String attrvaluedefid;

    public AttrInfo() {
        super();
    }

    public AttrInfo(String attrID) {
        this.attrid = attrID;
    }

    public void setAttrvalue(String attrvalue) {
        this.attrvalue = attrvalue;
    }

	public String getAttrid() {
		return attrid;
	}

	public void setAttrid(String attrid) {
		this.attrid = attrid;
	}


	public String getAttrvaluedefid() {
		return attrvaluedefid;
	}

	public void setAttrvaluedefid(String attrvaluedefid) {
		this.attrvaluedefid = attrvaluedefid;
	}

	public String getAttrvalue() {
		return attrvalue;
	}
    
}
