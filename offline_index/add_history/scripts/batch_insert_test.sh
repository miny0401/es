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
                           --news_index_name search_app_browser_news \
                           --video_index_name search_app_browser_videos \
                           --host 172.16.0.115:9200 \
                           --number_of_shards 3 \
                           --number_of_replicas 1 \
                           --third_news_source wangyi \
                           --third_news_source yidian_news \
                           --third_video_source rabbit