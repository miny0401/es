package com.miny.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;


/**
 * @author huangwm@2345.com
 * @date 2019/11/19
 */
@Component
@Data
@ConfigurationProperties(prefix = "elastic.search")
public class EsConf{
    /**
     * ElasticSearch server IP
     */
    private String host;

    /**
     * ElasticSearch server端口
     */
    private int port;

    /**
     * ElasticSearch cluster名称
     */
    private String clusterName;

    /**
     * 协议
     */
    private String schema;
}