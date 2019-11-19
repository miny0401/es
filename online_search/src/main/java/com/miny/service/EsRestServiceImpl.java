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


/**
 * @author huangwm@2345.com
 * @date 2019/11/19
 */
@Service
public class EsRestServiceImpl implements EsRestService {
    private static Logger logger = LoggerFactory.getLogger(EsRestServiceImpl.class);
    @Autowired
    EsEngine esEngine;

    @Value("${elastic.index}")
    String indexName;

    /**
     * 资讯索引的名称
     */
    @Value("${news.index.name}")
    String newsIndexName;

    /**
     * 资讯搜索每次返回的数量
     */
    @Value("${news.page_num}")
    Integer newsPageNum;

    /**
     * 视频索引的名称
     */
    @Value("${video.index.name}")
    String videoIndexName;

    /**
     * 视频搜索每次返回的数量
     */
    @Value("${video.page_num}")
    Integer videoPageNum;

    /**
     * 根据输入的搜索词及展现页，获得相应的搜索结果
     * @param keyword 搜索词
     * @param pageNum 展现页
     * @return
     */
    public ArrayList<Map<String, Object>> searchByKeywordAndPageNum(String keyword, Integer pageNum) {
        logger.info("****************** search start ******************");
        logger.info("getRecommendBySearchWord request, keyword:{}, pageNum:{}", keyword, pageNum);
        long timeStart = System.currentTimeMillis();
        if (pageNum == null) pageNum = 1;
        // 搜索资讯
        String[] newsIndexNames = {newsIndexName};
        String[] newsSearchFields = {"title", "content"};
        ArrayList<Map<String, Object>> newsList = esEngine.searchDocs(
            newsIndexNames, keyword, newsSearchFields, pageNum, newsPageNum);
        // 搜索视频
        String[] videoIndexNames = {videoIndexName};
        String[] videoSearchFields = {"title"};
        ArrayList<Map<String, Object>> videoList = esEngine.searchDocs(
            videoIndexNames, keyword, videoSearchFields, pageNum, videoPageNum);
        // 组合资讯与视频的结果
        newsList.addAll(videoList);
        logger.info("Search cost time: {}ms", System.currentTimeMillis() - timeStart);
        return newsList;
    }

    /**
     * 根据输入的搜索词及展现页，获得相应的搜索结果
     * @param searchWordRequest 搜索输入（包括搜索词、展现页等）
     * @return List<SearchWordResponse>
     */
    public List<SearchWordResponse> getRecommendBySearchWord(SearchWordRequest searchWordRequest) {
        logger.info("****************** search start ******************");
        logger.info("getRecommendBySearchWord request, keyword:{}, pageNum:{}",
            searchWordRequest.getSearchWord(), searchWordRequest.getPageNum());
        long timeStart = System.currentTimeMillis();
        if (searchWordRequest.getPageNum() == null) searchWordRequest.setPageNum(1);
        // 搜索资讯
        String[] newsIndexNames = {newsIndexName};
        String[] newsSearchFields = {"title", "content"};
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
        // 组合资讯与视频的结果
        newsResponse.addAll(videoResponse);
        logger.info("Search cost time: {}ms", System.currentTimeMillis() - timeStart);
        return newsResponse;
    }

    /**
     * 将搜索的结果转换成约定的格式
     * @param newsList 搜索的结果列表
     * @return List<SearchWordResponse>
     */
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
            response.setNewsId((String)news.get("news_id"));
            response.setDate((String)news.get("update_time"));
            response.setIsvideo((Integer)news.get("is_video"));
            response.setNewstag((String)news.get("news_tag"));
            String[] largePics = ((String)news.get("large_pic")).split(";");
            String[] miniPics = ((String)news.get("mini_pic")).split(";");
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
            response.setSource((String)news.get("source"));
            response.setTopic(((StringBuffer)news.get("title")).toString());
            response.setType((String)news.get("news_type"));
            response.setUrl((String)news.get("info_url"));
            response.setHttpsurl((String)news.get("https_url"));
            response.setVideoalltime((Integer)news.get("video_time"));
            responseList.add(response);
        }
        return responseList;
    }
}