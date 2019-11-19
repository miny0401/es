package com.miny;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miny.bean.NewsDoc;
import com.miny.service.EsRestService;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SearchRunner implements ApplicationRunner {
    private static Logger logger = LoggerFactory.getLogger(SearchRunner.class);
    @Autowired(required = true)
    EsRestService esRestService;
    @Value("${runner.preload}")
    boolean preload;

    @Override
    public void run(ApplicationArguments arg) throws Exception {
        if (!preload) return;
        String indexName = "news";
        boolean ret;
        ret = esRestService.deleteIndex(indexName);
        logger.info("Delete index '{}' {}.", indexName, ret ? "successfully":"failed");
        ret = esRestService.createIndex(indexName, 3, 0, createBuilder());
        logger.info("Create index '{}' {}.", indexName, ret ? "successfully":"failed");
        Map<String, String> indexInfo = esRestService.getIndexInfo(indexName);
        logger.info("index info: {}.", indexInfo);
        // 读取文件数据
        ArrayList<String> docList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        //Reader in = new FileReader("/home/huangweimin/info_browser_sea.csv");
        Reader in = new FileReader("/home/huangweimin/workspace/transfer/info_browser_sub.csv");
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            //NewsDoc newsDoc = tikaService.parse(record.get("title"), record.get("content"));
            NewsDoc newsDoc = new NewsDoc(record.get("news_id"), record.get("title"),
                                          record.get("content"), record.get("dt"));
            String json = objectMapper.writeValueAsString(newsDoc);
            docList.add(json);
        }
        logger.info("Insert doc num: {}", docList.size());
        int batch_size = 10000;
        int batch_num = (int)Math.ceil(docList.size() / batch_size);
        for (int i = 0; i < batch_num; i++){
            logger.info("Inserting doc {}/{}...", i, batch_num-1);
            ret = esRestService.indexDocs(indexName, docList.subList(i*batch_size, (i+1)*batch_size));
            logger.info("Insert index '{}' {}.", indexName, ret ? "successfully":"failed");
        }
    }

    private XContentBuilder createBuilder() {
        XContentBuilder builder = null;
        try {
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("properties");
                {
                    builder.startObject("news_id");
                    {
                        builder.field("type", "keyword");
                    }
                    builder.endObject();
                    builder.startObject("title");
                    {
                        builder.field("type", "text");
                        //builder.field("analyzer", "ik_max_word");
                    }
                    builder.endObject();
                    builder.startObject("content");
                    {
                        builder.field("type", "text");
                        //builder.field("analyzer", "ik_max_word");
                        builder.field("term_vector", "with_positions_offsets");
                    }
                    builder.endObject();
                    builder.startObject("dt");
                    {
                        builder.field("type", "date");
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder;
    }
}