package com.miny.service;

import java.util.List;
import java.util.Map;

import com.miny.dto.SearchWordRequest;
import com.miny.dto.SearchWordResponse;


/**
 * @author huangwm@2345.com
 * @date 2019/11/19
 */
public interface EsRestService {
    /**
     * 根据输入的搜索词及展现页，获得相应的搜索结果
     * @param keyword 搜索词
     * @param pageNum 展现页
     * @return
     */
    public List<Map<String, Object>> searchByKeywordAndPageNum(String keyword, Integer pageNum);

    /**
     * 根据输入的搜索词及展现页，获得相应的搜索结果
     * @param searchWordRequest 搜索输入（包括搜索词、展现页等）
     * @return List<SearchWordResponse>
     */
    public List<SearchWordResponse> getRecommendBySearchWord(SearchWordRequest searchWordRequest);
}