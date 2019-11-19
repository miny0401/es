package com.miny.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author xiongwt@2345.com
 * @date 2019/11/14
 */
@Getter
@Setter
public class ImgBean implements Serializable {
    private static final long serialVersionUID = 3895166301334213833L;

    /**
     * 图片链接
     */
    private String src;

}
