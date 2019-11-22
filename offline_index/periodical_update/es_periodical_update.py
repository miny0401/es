import logging
import argparse
from tqdm import tqdm
from datetime import datetime, timedelta
from pyspark import SparkConf
from pyspark.sql import SparkSession
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk, scan
from elasticsearch_dsl import connections, Q, Search
from elasticsearch_dsl import Index, Document, Text, Keyword, Date, Long


def create_spark():
    '''
    spark = SparkSession.builder.config(conf=SparkConf().setAll([
                                    ('spark.app.name', 'sim'),
                                    ('spark.master', 'yarn'),
                                    ('spark.driver.cores', 4),
                                    ('spark.executor.cores', 4),
                                    ('spark.debug.maxToStringFields', '200'),
                                    ('spark.driver.maxResultSize', '4g'),
                                    ('spark.driver.memory', '10g'),
                                    ('spark.executor.memory', '3g')]))\
                                .enableHiveSupport().getOrCreate()
    '''
    spark = SparkSession.builder.master('yarn').appName('search')\
                        .enableHiveSupport().getOrCreate()
    return spark


class Dataset(object):
    """数据集：专门用于将更新的资讯输入到elasticsearch中."""
    def __init__(self, spark: SparkSession, batch_size: int, third_source: list):
        self.spark = spark
        self.batch_size = batch_size
        self.third_source = third_source
        today_datetime = datetime.now()
        yesterday_datetime = today_datetime - timedelta(days=1)
        self.today = today_datetime.strftime(f'%Y-%m-%d')
        self.yesterday = yesterday_datetime.strftime(f'%Y-%m-%d')
        self._prepare_data()
    
    def iter_data(self, inserted_ids):
        """根据es里已经插入的news_id，把未插入的news_id封装成一个个批次返回."""
        today_count, yesterday_count = 0, 0
        for collected_data in self._iter_data(inserted_ids, self.yesterday_iter):
            yesterday_count += len(collected_data)
            yield collected_data
        for collected_data in self._iter_data(inserted_ids, self.today_iter):
            today_count += len(collected_data)
            yield collected_data
        logging.info(f"There are {today_count} today news and {yesterday_count} "
                     f"yesterday news that are new inserted.")

    def _iter_data(self, inserted_ids, iterator):
        """获取数据的生成器，以`self.batch_size`为一个数据."""
        collect_data = []
        for row in iterator:
            if row.news_id in inserted_ids:
                continue
            collect_data.append(row)
            if len(collect_data) >= self.batch_size:
                yield collect_data.copy()
                collect_data.clear()
        if len(collect_data) > 0:
            yield collect_data.copy()

    def _prepare_data(self):
        """获取相应的数据，并返回迭代器（以partition为单位）."""
        today_df = self.spark.sql(f"select news_id,title,content,news_tag,source,pure_url,"
                                  f"https_url,large_pic,mini_pic,third_source,content_type,"
                                  f"type as news_type,is_video,video_time,update_time,dt from "
                                  f"mlg.info_browser where dt='{self.today}'")
        yesterday_df = self.spark.sql(f"select news_id,title,content,news_tag,source,pure_url,"
                                      f"https_url,large_pic,mini_pic,third_source,content_type,"
                                      f"type as news_type,is_video,video_time,update_time,dt "
                                      f"from mlg.info_browser where dt='{self.yesterday}'")
        today_df = today_df.filter(
            today_df['third_source'].isin(*self.third_source)).cache()
        yesterday_df = yesterday_df.filter(
            yesterday_df['third_source'].isin(*self.third_source)).cache()
        today_count, yesterday_count = today_df.count(), yesterday_df.count()
        logging.info(f"Today has {today_count} news, Yesterday has {yesterday_count} news.")
        today_partition_num = (today_count - 1) // self.batch_size + 1
        yesterday_partition_num = (yesterday_count - 1) // self.batch_size + 1
        if today_partition_num > 0:
            today_df = today_df.repartition(today_partition_num)
        if yesterday_partition_num > 0:
            yesterday_df = yesterday_df.repartition(yesterday_partition_num)
        self.today_iter = today_df.toLocalIterator()
        self.yesterday_iter = yesterday_df.toLocalIterator()
        self.partition_num = today_partition_num + yesterday_partition_num


class ElasticInsert(object):
    """此类为工具类，用于将更新的资讯插入至elasticsearch中."""
    def __init__(self, client, index_name: str):
        self.client = client
        self.index_name = index_name
        self.index = Index(name=self.index_name, using=self.client)
        self._init()

    def get_news_ids(self, dt: str):
        """取出给定日期的news_id集合，从而可以判断哪些资讯已插入，从而只插入新资讯."""
        scan_generator = scan(self.client, query={'query': {'match': {'dt': dt}}},
                              index=self.index_name, _source=['news_id'])
        news_ids = set()
        news_id_list = list()
        for item in scan_generator:
            news_id = item['_source']['news_id']
            news_ids.add(news_id)
            news_id_list.append(news_id)
        if len(news_ids) != len(news_id_list):
            logging.warning(f"There are {len(news_id_list)-len(news_ids)} news " +
                            f"repeatedly inserted.")
        logging.info(f"There are {len(news_ids)} news in index {self.index_name} on {dt}")
        return news_ids

    def _init(self):
        self._document()
        if not self.index.exists():
            self.index.create()
            logging.info(f"Create index {self.index_name} successfully.")
        else:
            logging.info(f"Index {self.index_name} already existed.")

    def _document(self):
        @self.index.document
        class News(Document):
            news_id = Keyword()
            title = Text()
            content = Text()
            news_tag = Text()
            source = Keyword()
            info_url = Keyword()
            https_url = Keyword()
            large_pic = Keyword()
            mini_pic = Keyword()
            third_source = Text()
            content_type = Keyword()
            news_type = Keyword()
            is_video = Keyword()
            video_time = Long()
            update_time = Date()
            utc_update_time = Date()
            dt = Date()
        self.News = News



def main(args):
    # 1. 初始化 logging
    logging.basicConfig(level=logging.INFO)
    # 2. 初始化 ElasticSearch
    client = Elasticsearch(hosts=args.hosts)
    # 3. 创建 spark context
    spark = create_spark()
    # 4. 创建 Dataset 类，用于从hive中收集相应日期内的数据
    ds = Dataset(spark, batch_size=args.batch_size, third_source=args.third_source)
    # 5. es环境
    ei_news = ElasticInsert(client, args.news_index_name)
    ei_video = ElasticInsert(client, args.video_index_name)
    yesterday_ids = ei_news.get_news_ids(dt=ds.yesterday) | ei_video.get_news_ids(dt=ds.yesterday)
    today_ids = ei_news.get_news_ids(dt=ds.today) | ei_video.get_news_ids(dt=ds.today)
    inserted_ids = yesterday_ids | today_ids
    print(len(inserted_ids))

    for batch_data in tqdm(ds.iter_data(inserted_ids), total=ds.partition_num):
        batch_news_or_videos = []
        for row in batch_data:
            # 转utc时间，方便在kibana中查询
            time_format = f'%Y-%m-%d %H:%M:%S'
            ut_datetime = datetime.strptime(row.update_time, time_format)
            utc_ut_datetime = ut_datetime - timedelta(hours=8)
            # 解析 large_pic 和 mini_pic
            large_pic, mini_pic = "", ""
            large_pic_list = eval(row.large_pic.replace('\\', ''))
            mini_pic_list = eval(row.mini_pic.replace('\\', ''))
            for idx, large_pic_item in enumerate(large_pic_list):
                large_pic += f"{large_pic_item['src']}"
                if idx != len(large_pic_list) - 1:
                    large_pic += ';'
            for idx, mini_pic_item in enumerate(mini_pic_list):
                mini_pic += f"{mini_pic_item['src']}"
                if idx != len(mini_pic_list) - 1:
                    mini_pic += ';'
            # 创建文档
            if row.third_source in args.third_news_source:
                News = ei_news.News
                info_url = row.pure_url
                source = row.source
            elif row.third_source in args.third_video_source:
                News = ei_video.News
                info_url = row.info_url
                source = row.nickname
            doc = News(news_id=row.news_id,
                       title=row.title,
                       content=row.content,
                       news_tag=row.news_tag,
                       source=row.source,
                       info_url=row.pure_url,
                       https_url=row.https_url,
                       large_pic=large_pic,
                       mini_pic=mini_pic,
                       third_source=row.third_source,
                       content_type=row.content_type,
                       news_type=row.news_type,
                       is_video=row.is_video,
                       video_time=row.video_time,
                       update_time=ut_datetime,
                       utc_update_time=utc_ut_datetime,
                       dt=row.dt)
            batch_news_or_videos.append(doc)
        bulk(client, (d.to_dict(True) for d in batch_news_or_videos))


def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument('--batch_size', type=int, default=10000)
    parser.add_argument('--news_index_name', type=str, default='app_browser_search_news')
    parser.add_argument('--video_index_name', type=str, default='app_browser_search_videos')
    parser.add_argument('--hosts', action='append', type=str)
    parser.add_argument('--third_news_source', action='append', type=str)
    parser.add_argument('--third_video_source', action='append', type=str)
    return parser.parse_args()


if __name__ == '__main__':
    args = parse_arguments()
    args.third_news_source = [] if args.third_news_source is None else args.third_news_source
    args.third_video_source = [] if args.third_video_source is None else args.third_video_source
    args.third_source = args.third_news_source + args.third_video_source
    print("=======================FLAGS=======================")
    for k, v in args.__dict__.items():
        print('{}: {}'.format(k, v))
    print("===================================================")
    main(args)
    print("done!")