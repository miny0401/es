package com.miny.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.miny.dto.AlgoBean;
import com.miny.dto.ImgBean;
import com.miny.dto.SearchWordRequest;
import com.miny.dto.SearchWordResponse;
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

    public List<SearchWordResponse> getRecommendBySearchWord(SearchWordRequest searchWordRequest) {
        logger.info("****************** search start ******************");
        logger.info("getRecommendBySearchWord request, keyword:{}, pageNum:{}",
            searchWordRequest.getSearchWord(), searchWordRequest.getPageNum());
        long timeStart = System.currentTimeMillis();
        // 搜索资讯
        String[] newsIndexNames = {newsIndexName};
        String[] newsSearchFields = {"title", "content"};
        if (searchWordRequest.getPageNum() == null) searchWordRequest.setPageNum(1);
        ArrayList<Map<String, Object>> newsList = esEngine.searchDocs(
            newsIndexNames, searchWordRequest.getSearchWord(),
            newsSearchFields, searchWordRequest.getPageNum(), newsPageNum);
        List<SearchWordResponse> newsResponse = convertResultToResponse(newsList);
        // 搜索视频
        String[] videoIndexNames = {videoIndexName};
        String[] videoSearchFields = {"title"};
        ArrayList<Map<String, Object>> videoList = esEngine.searchDocs(
            videoIndexNames, searchWordRequest.getSearchWord(),
            videoSearchFields, searchWordRequest.getPageNum(), videoPageNum);
        List<SearchWordResponse> videoResponse = convertResultToResponse(videoList);
        // 组合
        newsResponse.addAll(videoResponse);
        logger.info("Search cost time: {}ms", System.currentTimeMillis() - timeStart);
        return newsResponse;
    }

    private List<SearchWordResponse> convertResultToResponse(ArrayList<Map<String, Object>> newsList) {
        List<SearchWordResponse> responseList = new ArrayList<SearchWordResponse>();
        int order = 0;
        for (Map<String, Object> news : newsList) {
            order++;
            SearchWordResponse response = new SearchWordResponse();
            // 算法信息
            AlgoBean algoBean = new AlgoBean();
            response.setAlgoBean(algoBean);
            // 具体的资讯数据
            response.setOrder(order);
            response.setNewsId((String) news.get("news_id"));
            response.setDate((String) news.get("update_time"));
            response.setIsvideo((Integer) news.get("is_video"));
            response.setNewstag((String) news.get("news_tag"));
            String[] largePics = ((String) news.get("large_pic")).split(";");
            String[] miniPics = ((String) news.get("mini_pic")).split(";");
            List<ImgBean> largeImgList = new ArrayList<ImgBean>();
            for (String largePic: largePics) {
                ImgBean img = new ImgBean();
                img.setSrc(largePic);
                largeImgList.add(img);
            }
            List<ImgBean> miniImgList = new ArrayList<ImgBean>();
            for (String miniPic: miniPics) {
                ImgBean img = new ImgBean();
                img.setSrc(miniPic);
                miniImgList.add(img);
            }
            response.setLbimg(largeImgList);
            response.setMiniimg(miniImgList);
            response.setSource((String) news.get("source"));
            response.setTopic(((StringBuffer) news.get("title")).toString());
            response.setType((String) news.get("news_type"));
            response.setUrl((String) news.get("info_url"));
            response.setHttpsurl((String) news.get("https_url"));
            response.setVideoalltime((Integer) news.get("video_time"));
            responseList.add(response);
        }
        return responseList;
    }
}