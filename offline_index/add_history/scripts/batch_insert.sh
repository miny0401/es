source /etc/profile
spark-submit \
--master yarn-cluster \
--conf spark.default.parallelism=200  \
--conf spark.network.timeout=600 \
--conf spark.sql.shuffle.partitions=200 \
--conf spark.executor.memoryOverhead=2G \
--conf spark.driver.memoryOverhead=2G \
--conf spark.driver.maxResultSize=2G \
--conf spark.sql.broadcastTimeout=3000 \
--executor-cores 3 \
--num-executors 4 \
--executor-memory 4g \
--driver-memory 4g \
es_batch_insert_offline.py --end_date '2019-10-15' \
                           --batch_size 10000 \
                           --days_num 1 \
                           --news_index_name app_browser_search_news \
                           --video_index_name app_browser_search_videos \
                           --host 192.168.22.137:9200 \
                           --host 192.168.22.141:9200 \
                           --host 192.168.22.31:9200 \
                           --host 192.168.25.40:9200 \
                           --host 192.168.22.42:9200 \
                           --host 192.168.25.62:9200 \
                           --number_of_shards 3 \
                           --number_of_replicas 1 \
                           --third_news_source wangyi \
                           --third_news_source yidian_news \
                           --third_video_source rabbit