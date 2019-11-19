package com.miny.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchWordResponse extends BaseResponse {
    /**
     *
     */
    private static final long serialVersionUID = 3895166301334213834L;

    /**
     * 发布日期
     */
    private String date;

    /**
     * 是否是视频新闻（0：不是视频新闻;1:是视频新闻）
     */
    private Integer isvideo;

    /**
     *
     */
    private String newstag;

    /**
     * 资讯大图链接列表 (N组)
     */
    private List<ImgBean> lbimg;

    /**
     * 资讯小图链接列表 (N组)
     */
    private List<ImgBean> miniimg;

    /**
     * 资讯来源
     */
    private String source;

    /**
     * 资讯标题
     */
    private String topic;

    /**
     * 资讯类别，如：toutiao（头条）、shehui（社会）、guonei（国内）...
     */
    private String type;

    /**
     * 资讯链接
     */
    private String url;

    /**
     * 资讯https链接
     */
    private String httpsurl;

    /**
     * 视频时间(单位：ms)
     */
    private Integer videoalltime;

}

