package com.miny.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class BaseResponse implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 3895166301334213833L;
    /**
     * 新闻id
     */
    private String newsId;
    /**
     * databox
     */
    private String dataBox;
    /**
     * 排序号
     */
    private Integer order;

    /**
     * 算法相关参数
     */
    private AlgoBean algoBean;
}

