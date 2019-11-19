package com.miny.controller;

import java.util.ArrayList;
import java.util.Map;

import com.miny.service.EsRestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping
public class SearchController {
    private static Logger logger = LoggerFactory.getLogger(SearchController.class);
    @Autowired
    EsRestService esRestService;
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

    @ResponseBody
    @RequestMapping("/search")
    public ArrayList<Map<String, Object>> result(@RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "pageNum", required = false) Integer pageNum) {
        logger.info("****************** search start ******************");
        logger.info("getRecommendBySearchWord request, keyword:{}, pageNum:{}", keyword, pageNum);
        long timeStart = System.currentTimeMillis();
        String[] newsIndexNames = {newsIndexName};
        String[] newsSearchFields = {"title", "content"};
        if (pageNum == null) pageNum = 1;
        ArrayList<Map<String, Object>> newsList = esRestService.searchDocs(
            newsIndexNames, keyword, newsSearchFields, pageNum, newsPageNum);
        String[] videoIndexNames = {videoIndexName};
        String[] videoSearchFields = {"title"};
        ArrayList<Map<String, Object>> videoList = esRestService.searchDocs(
            videoIndexNames, keyword, videoSearchFields, pageNum, videoPageNum);
        newsList.addAll(videoList);
        logger.info("Search cost time: {}ms", System.currentTimeMillis() - timeStart);
        return newsList;
    }
}