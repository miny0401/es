package com.miny.bean;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsDoc {
    private String news_id;
    private String title;
    private String content;
    private String dt;
}