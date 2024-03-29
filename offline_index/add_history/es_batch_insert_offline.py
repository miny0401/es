import logging
import argparse
from tqdm import tqdm
from datetime import datetime, timedelta
from pyspark import SparkConf
from pyspark.sql import SparkSession
from elasticsearch.helpers import bulk
from elasticsearch_dsl import connections
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
    """数据集：专门用于将历史的资讯输入到elasticsearch中."""
    def __init__(self, spark: SparkSession, start_date: str, end_date: str,
                 batch_size: int, third_source: list):
        self.spark = spark
        self.batch_size = batch_size
        self.third_source = third_source
        self.end_date = end_date
        self.start_date = start_date
        self._prepare_data()
    
    def iter_data(self):
        """获取数据的生成器，以`self.batch_size`为一个数据."""
        collect_data = []
        for row in self.iter:
            collect_data.append(row)
            if len(collect_data) >= self.batch_size:
                yield collect_data.copy()
                collect_data.clear()
        if len(collect_data) > 0:
            yield collect_data.copy()

    def _prepare_data(self):
        """获取相应的数据，并返回迭代器（以partition为单位）."""
        df = self.spark.sql(f"select news_id,title,content,news_tag,source,pure_url,https_url,"
                            f"large_pic,mini_pic,third_source,content_type,type as news_type,"
                            f"is_video,video_time,update_time,dt,nickname,info_url from " +
                            f"mlg.info_browser where dt>='{self.start_date}'" +
                            f" and dt<='{self.end_date}'")
        # 根据资讯源及视频源的要求过滤
        df = df.filter(df['third_source'].isin(*self.third_source)).cache()
        self.count = df.count()
        logging.info(f"There are {self.count} news.")
        self.partition_num = (self.count - 1) // self.batch_size + 1
        df = df.repartition(self.partition_num)
        self.iter = df.toLocalIterator()


class ElasticInsert(object):
    """此类为工具类，用于将历史的资讯插入至elasticsearch中."""
    def __init__(self, index_name: str, using: str = 'default',
                 number_of_shards: int = 3, number_of_replicas: int = 1):
        self.using = using
        self.index_name = index_name
        self.number_of_shards = number_of_shards
        self.number_of_replicas = number_of_replicas
        self.index = Index(name=self.index_name, using=self.using)
        self.index.settings(number_of_shards=self.number_of_shards,
                            number_of_replicas=self.number_of_replicas)
        self._init()
 
    def create_document(self, **kwargs):
        return self.News(kwargs)

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
    connections.create_connection(alias='default', hosts=args.hosts)
    # 3. 创建 spark context
    spark = create_spark()
    # 4. 确定插入数据的日期
    end_date_structed = datetime.strptime(args.end_date, f'%Y-%m-%d')
    start_date_structed = end_date_structed - timedelta(days=args.days_num)
    end_date = end_date_structed.strftime(f'%Y-%m-%d')
    start_date = start_date_structed.strftime(f'%Y-%m-%d')
    logging.info(f"Inserting data from {start_date} to {end_date}")
    # 5. 创建 Dataset 类，用于从hive中收集相应日期内的数据
    ds = Dataset(spark, start_date, end_date, batch_size=args.batch_size,
                 third_source=args.third_source)
    # 6.1 创建相应的index（如果未创建），并设置相应的参数
    # 6.2 批量插入数据
    ei_news = ElasticInsert(args.news_index_name, using='default',
                            number_of_shards=args.number_of_shards,
                            number_of_replicas=args.number_of_replicas)
    ei_video = ElasticInsert(args.video_index_name, using='default',
                             number_of_shards=args.number_of_shards,
                             number_of_replicas=args.number_of_replicas)
    for batch_data in tqdm(ds.iter_data(), total=ds.partition_num):
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
                       source=source,
                       info_url=info_url,
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
        bulk(connections.get_connection(), (d.to_dict(True) for d in batch_news_or_videos))


def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument('--batch_size', type=int, default=10000)
    parser.add_argument('--days_num', type=int, default=1)
    parser.add_argument('--end_date', type=str, default='2019-10-15')
    parser.add_argument('--news_index_name', type=str, default='news')
    parser.add_argument('--video_index_name', type=str, default='video')
    parser.add_argument('--hosts', action='append', type=str)
    parser.add_argument('--number_of_shards', type=int, default=3)
    parser.add_argument('--number_of_replicas', type=int, default=1)
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