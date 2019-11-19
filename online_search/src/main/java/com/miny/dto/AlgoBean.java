package com.miny.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlgoBean {

    private String algoId;
    private String dataAbtestTaskId;
    private String sortAbtestTaskId;
    /**
     * 顺序：0，1，2，3，...，值越小优先级越高
     */
    private Integer order;

    /**
     * 推荐场景
     */
    private Integer condition;

    /**
     * 存放json串
     */
    private String algoData;

}
