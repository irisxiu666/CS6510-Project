package com.clj.recommender
import java.net.InetAddress
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

/*
Movie数据集：

 260                                                    电影id: mid
 Star Wars: Episode IV - A New Hope (1977)              电影名称: name
 Princess Leia is captured and held hostage             详情描述：descri
 121 minutes                                            时长：timelang
 September 21, 2004                                     发行时间：issue
 1977                                                   拍摄时间：shoot
 English                                                语言：language
 Action|Adventure|Sci-Fi                                类型：genres
 Mark Hamill|Harrison Ford|Carrie Fisher                演员表：actors
 George Lucas                                           导演：directors

 Rating数据集:
  用户id，电影id，评分，时间戳
  1,31,2.5,1260759144

 Tag数据集：
  uid, mid, tag, timestamp
  15,1955,dentist,1193435061
 */
case class Movie(mid: Int, name: String, descri: String, timelong: String, issue: String,
                 shoot: String, language: String, genres: String, actors: String, directors: String)

case class Rating(uid: Int, mid: Int, score: Double, timestamp: Int)//int 2*10^9

case class Tag(uid: Int, mid: Int, tag: String, timestamp: Int)
//把mongo 和 es 的配置封装成样例类
/**
 *
 * @param uri MongoDB连接
 * @param db  MongoDB数据库
 */
case class MongoConfig(uri: String, db: String)

/**
 *
 * @param httpHosts       http主机列表，逗号分割
 * @param transportHosts  transport主机列表
 * @param index           需要操作的索引
 * @param clustername     集群名称，默认
 */
case class ESConfig(httpHosts: String, transportHosts:String,
                    index: String, clustername: String)

object DataLoader {
  //定义常量
  val MOVIE_DATA_PATH = "D:\\project\\movie\\MovieRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\movies.csv"
  val RATING_DATA_PATH = "D:\\project\\movie\\MovieRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\ratings.csv"
  val TAG_DATA_PATH = "D:\\project\\movie\\MovieRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\tags.csv"

  val MONGODB_MOVIE_COLLECTION = "Movie"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_TAG_COLLECTION = "Tag"
  val ES_MOVIE_INDEX = "Movie"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://192.168.118.130:27017/recommender", //"mongodb://localhost:27017//recommender",
      "mongo.db" -> "recommender",
      "es.httpHosts" -> "192.168.118.130:9200",
      "es.transportHosts" -> "192.168.118.130:9300",
      "es.index" -> "recommender",
      "es.cluster.name" -> "es-cluster" //"elasticsearch"

    )
    //创建一个sparkConf对象
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("DataLoader")
    //创建一个sparkSession对象
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    //加载数据
    import spark.implicits._
    val movieRDD = spark.sparkContext.textFile(MOVIE_DATA_PATH)
    //val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
    //val tagRDD = spark.sparkContext.textFile(TAG_DATA_PATH)

    val movieDF = movieRDD.map(
      item => {
        val attr = item.split("\\^")
        Movie(attr(0).toInt, attr(1).trim, attr(2).trim, attr(3).trim, attr(4).trim, attr(5).trim, attr(6).trim, attr(7).trim, attr(8).trim, attr(9).trim)
      }
    ).toDF()
    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
    //将 ratingRDD 转换为 DataFrame
    val ratingDF = ratingRDD.map(
      item => {
        val attr = item.split(",")
        Rating(attr(0).toInt, attr(1).toInt, attr(2).toDouble, attr(3).toInt)
      }
    ).toDF()
    val tagRDD = spark.sparkContext.textFile(TAG_DATA_PATH)
    //将 tagRDD 装换为 DataFrame
    val tagDF = tagRDD.map(
      item => {
        val attr = item.split(",")
        Tag(attr(0).toInt, attr(1).toInt, attr(2).trim, attr(3).toInt)
      }
    ).toDF()

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))
    //将数据保存到MongoDB
    storeDataInMongoDB(movieDF, ratingDF, tagDF)
    //数据预处理，把movie对应的tag信息添加进去，加一列 tag1|tag2|tag2....
    import org.apache.spark.sql.functions._
    /** *
     * mid, tags
     *
     * tags: tag1|tag2|tag3...
     */
    val newTag = tagDF.groupBy($"mid")
      .agg(concat_ws("|", collect_set($"tag")).as("tags"))
      .select("mid", "tags")
    //对newTag和movie做join，数据合并在一起,左外链接
    val movieWithTagsDF = movieDF.join(newTag, Seq("mid"), "left")

    implicit val esConfig = ESConfig(
      config("es.httpHosts"),
      config("es.transportHosts"),
      config("es.index"),
      config("es.cluster.name")
    )

    //保存数据到ES
    storeDataInES(movieWithTagsDF)(esConfig)

    spark.stop()
  }

  def storeDataInMongoDB(movieDF: DataFrame, ratingDF: DataFrame, tagDF: DataFrame)(implicit mongoConfig: MongoConfig): Unit = {
    //新建一个mongodb的连接
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    //如果mongodb中已经有相应的数据库，先删除
    mongoClient(mongoConfig.db)(MONGODB_MOVIE_COLLECTION).dropCollection()
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).dropCollection()
    mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).dropCollection()
    //将DF数据写入对应的mongodb表中
    movieDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MOVIE_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
    ratingDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
    tagDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_TAG_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
    //对数据表建立索引
    mongoClient(mongoConfig.db)(MONGODB_MOVIE_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    //关闭 MongoDB 的连接
    mongoClient.close()
  }

  def storeDataInES(movieDF: DataFrame)(implicit esConfig: ESConfig): Unit = {
    /**
     * 新建ES配置
     */
    val settings: Settings = Settings.builder()
      .put("cluster.name", esConfig.clustername).build()

    /**
     * 新建ES客户端
     */
    val esClient = new PreBuiltTransportClient(settings)
    // 将TransportHosts添加到esClient中
    val REGEX_HOST_PORT = "(.+):(\\d+)".r
    esConfig.transportHosts.split(",").foreach {
      case REGEX_HOST_PORT(host: String, port: String) => {
        esClient.addTransportAddress(new
            InetSocketTransportAddress(InetAddress.getByName(host), port.toInt))
      }
    }
    /**
     * 清除ES中遗留的数据
     */
    if (esClient.admin().indices().exists(new IndicesExistsRequest(esConfig.index)).actionGet().isExists) {
      esClient.admin().indices().delete(new DeleteIndexRequest(esConfig.index))
    }

    esClient.admin().indices().create(new CreateIndexRequest(esConfig.index))

    /**
     * 将数据写入ES
     */
    movieDF.write
      .option("es.nodes", esConfig.httpHosts)
      .option("es.http.timeout", "100m")
      .option("es.mapping.id", "mid")
      .mode("overwrite")
      .format("org.elasticsearch.spark.sql")
      .save(esConfig.index + "/" + ES_MOVIE_INDEX)

  }
}