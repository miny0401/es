package com.miny.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "elastic.search")
public class EsConf{
    private String host;
    private int port;
    private String clusterName;
    private String schema;
}