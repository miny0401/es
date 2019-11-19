package com.miny.controller;

import java.util.ArrayList;
import java.util.Map;

import com.miny.service.EsRestService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping
public class SearchController {
    @Autowired
    EsRestService esRestService;

    @ResponseBody
    @RequestMapping("/search")
    public ArrayList<Map<String, Object>> result(@RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "pageNum", required = false) Integer pageNum) {
        return esRestService.searchByKeywordAndPageNum(keyword, pageNum);
    }
}