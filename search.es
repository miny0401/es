/* 查询集群状态 */
GET /_cluster/health


/* 索引管理 */
//创建索引
PUT news
{
  "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      // 定义title这个字段的类型为text，分析器用标准的分析器
      "title": {
        "type": "text",
        "analyzer": "standard" 
      },
      // 定义content这个字段的类型为text，分析器用ik_smart
      "content": {
        "type": "text",
        "analyzer": "ik_smart"
      }, 
      "value": {
        "type": "integer"
      }
    }
  }
}
// 修改索引设置
PUT /news/_settings
{
    "number_of_replicas": 1
}
// 查询index信息
GET news
{}
// 查询index的映射
GET news/_mapping
{}
// 查询index包含的文档
GET news/_search
{
    "size": 1,
    "_source": ["title", "content", "value"]
}
// 删除索引
DELETE /news
{}
// 索引别名
PUT /news/_alias/articles
{}
// 查询articles是哪个引用的别名
GET //_alias/articles
{}
// 查询news有哪些别名
GET /news/_alias 
{}
// 利用别名alias， 修改索引里文档字段的类别
// 1.给旧索引创建别名，2.创建新的索引（字段类型不同），3.删除旧索引的别名，同时将此别名指向新索引
// 所以最好用alias来替代索引，这样的话，以后更新索引结构时就比较方便
PUT /news2
{
  "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      // 定义title这个字段的类型为text，分析器用标准的分析器
      "title": {
        "type": "text",
        "analyzer": "standard" 
      },
      // 定义content这个字段的类型为text，分析器用ik_smart
      "content": {
        "type": "text",
        "analyzer": "ik_smart" 
      }, 
      "value": {
        "type": "text"
      }
    }
  }
}
POST /_aliases
{
    "actions": [
        { "remove": { "index": "news", "alias": "articles" }},
        { "add":    { "index": "news2", "alias": "articles" }}
    ]
}
// 新增映射（不能修改映射）
PUT news/_mapping
{
    "properties": {
        "dt": {
            "type": "date"
        }
    }
}


/* 添加文档 */
PUT /news/_doc/1
{
    "title": "排骨别再只会炖汤了，这么做香辣可口，好吃不腻，一上桌就抢光！",
    "content": "排骨做为平时日常生活中经常用到的食材，含有丰富的蛋白质和胶原蛋白，排骨不管是煲汤还是红烧都特别好吃，算得上是一种万能的食材，今天就和大家分享一道美味的椒盐排骨，排骨别再只会炖汤了，这么做香辣可口，好吃不腻，一上桌就抢光！​所需原料：新鲜猪肋骨600克、青椒半个、葱姜蒜少许、食盐、料酒、老抽、蚝油和椒盐等调味品适量；1.排骨用清水冲洗干净，用刀切成三厘米左右的小段，放入清水中浸泡十五分钟左右，将排骨中多余的血水泡出，捞出沥干水分放入空碗中，加入适量的淀粉、食盐、料酒和老抽，用手充分抓匀后，盖上保鲜膜腌制十五分钟左右；2.葱姜蒜和青椒用清水冲洗干净，用刀切成碎末后，摆入空盘中备用；3.大火将锅烧热，锅中加入适量的食用油，待到油温升至四层左右，将腌制好的排骨逐个放入碗中，转中小火慢炸，边炸边用锅铲不停翻动，直至锅中排骨表面炸至金黄捞出备用，转大火，将油温升至六层左右，将炸好的排骨倒入锅中复炸两分钟左右，捞出沥干油备用（可以用到切开一块，看看里面肉是否完全熟透）；4.锅中留少许食用油，将切好的葱姜蒜和青椒倒入锅中锅中煸炒出香味，然后再将炸好的排骨倒入锅中，撒上椒盐、辣椒粉翻炒均匀，是所有排骨表面能均匀的裹上一层调味品，即可关火出锅；5.这样一道美味的椒盐排骨就完成了！",
    "value": 12345
}
PUT /news/_doc/2
{
    "title": "中国男排决赛3：1力克韩国，时隔12年再夺冠",
    "content": "26号晚上，中国八一队对阵韩国队。最终，中国八一男排以3:1夺得冠军，这也是八一男排，继2007年后获得的第二枚军运会金牌。此前中国女足、中国女排、中国男篮均与冠军失之交臂，如今中国代表团终于在三大球项目夺冠，实现“奏国歌升国旗”的目标。八一男排队员张哲嘉：“我觉得这一场胜利，对于现如今状况下的中国男排也是一种促进和激励吧，最后这一场球赢得也非常不容易，希望能把这个状态保持到后面的比赛中。”八一男排主教练陈方：“作为一个军人，能站在军运会的舞台上，能为祖国的军队取得优异的成绩，我们感到很自豪。”中国队曾在2007年军运会夺冠（男排和女排双双夺冠），在2011年军运会决赛不敌巴西，获得亚军。2015年军运会，中国队状态低迷，仅收获第5名。",
    "value": 67890
}


/* 搜索 */
// 空搜索
GET /_search
{}
// 分页搜索
GET /news/_search
{
    "size": 1,
    "from": 0
}
// 指定查询内容
GET /news/_search
{
    "query": {
        "match": {
            "content": "在的"
        }
    }
}
// 指定查询内容，同时提供解释
GET /news/_search?explain
{
    "query": {
        "match": {
            "content": "在的"
        }
    }
}


/* analyze */
GET news/_analyze
{
    "analyzer": "standard",
    "text": "this is good boy"
}
GET news/_analyze
{
    "analyzer": "standard",
    "text": "中华人民共和国国歌"
}
GET news/_analyze
{
    "analyzer": "ik_smart",
    "text": "中华人民共和国国歌"
}
GET news/_analyze
{
    "analyzer": "ik_max_word",
    "text": "中华人民共和国国歌"
}
GET news/_analyze
{
    "field": "title", // news这个index创建的时候指定了title的analyzer
    "text": "中华人民共和国国歌"
}