package com.miny.service;

import java.util.ArrayList;
import java.util.Map;

import com.miny.engine.EsEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EsRestService {
    @Autowired
    EsEngine esEngine;
    @Value("${elastic.index}")
    String indexName;
    @Value("${news.index.name}")
    String newsIndexName;
    @Value("${news.page_num}")
    Integer newsPageNum;
    @Value("${video.index.name}")
    String videoIndexName;
    @Value("${video.page_num}")
    Integer videoPageNum;
    private static Logger logger = LoggerFactory.getLogger(EsRestService.class);

    public ArrayList<Map<String, Object>> searchByKeywordAndPageNum(String keyword, Integer pageNum) {
        logger.info("****************** search start ******************");
        logger.info("getRecommendBySearchWord request, keyword:{}, pageNum:{}", keyword, pageNum);
        long timeStart = System.currentTimeMillis();
        // 搜索资讯
        String[] newsIndexNames = {newsIndexName};
        String[] newsSearchFields = {"title", "content"};
        if (pageNum == null) pageNum = 1;
        ArrayList<Map<String, Object>> newsList = esEngine.searchDocs(
            newsIndexNames, keyword, newsSearchFields, pageNum, newsPageNum);
        // 搜索视频
        String[] videoIndexNames = {videoIndexName};
        String[] videoSearchFields = {"title"};
        ArrayList<Map<String, Object>> videoList = esEngine.searchDocs(
            videoIndexNames, keyword, videoSearchFields, pageNum, videoPageNum);
        // 组合
        newsList.addAll(videoList);
        logger.info("Search cost time: {}ms", System.currentTimeMillis() - timeStart);
        return newsList;
    }
}