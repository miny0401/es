package com.miny.dto;

import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@ToString(exclude = {"logger"})
public class BaseRequest {
    private Logger logger = LoggerFactory.getLogger(BaseRequest.class);

    @NotBlank(message = "userId是必须的")
    private String userId;
    @NotBlank(message = "businessId是必须的")
    private String businessId;
    @NotBlank(message = "sceneId是必须的")
    private String sceneId;

    /**
     * 请求东方头条接口的参数如下：另需自己补上 key, code, qid 三个字段
     */
    private String ts;
    private String type;
    private String passback;
    private String direction;
    private String appver;
    private String os;
    private String newsnum;
}
