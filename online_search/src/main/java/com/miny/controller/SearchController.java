package com.miny.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.miny.dto.SearchWordRequest;
import com.miny.dto.SearchWordResponse;
import com.miny.service.EsRestService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping
public class SearchController {
    @Autowired
    EsRestService esRestService;

    @ResponseBody
    @RequestMapping("/search1")
    public ArrayList<Map<String, Object>> result(@RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "pageNum", required = false) Integer pageNum) {
        return esRestService.searchByKeywordAndPageNum(keyword, pageNum);
    }

    @ResponseBody
    @RequestMapping("/search2")
    public List<SearchWordResponse> getRecommendBySearchWord(
            @RequestBody @Valid SearchWordRequest paramBean,
            HttpServletRequest request) {
        return esRestService.getRecommendBySearchWord(paramBean);
    }

    @ResponseBody
    @RequestMapping("/search")
    public List<SearchWordResponse> getRecommendBySearchWord2(@RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "pageNum", required = false) Integer pageNum) {
        SearchWordRequest request = new SearchWordRequest();
        request.setWordSource(1);
        request.setSearchWord(keyword);
        request.setPageNum(pageNum);
        return esRestService.getRecommendBySearchWord(request);
    }
    
}