package com.clj.statistics
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * movies.csv
 * @param mid 电影ID
 * @param name 电影名称
 * @param descri 电影详情描述
 * @param timelong 电影时长
 * @param issue 电影发行日期
 * @param shoot 电影拍摄日期
 * @param language 电影语言
 * @param genres 电影类型
 * @param actors 电影演员表
 * @param directors 电影导演
 */
case class Movie(mid: Int, name: String, descri: String, timelong: String, issue: String,
                 shoot: String, language: String, genres: String, actors: String,
                 directors: String)
/**
 * MongoDB的连接配置
 * @param uri MongoDB的连接
 * @param db MongoDB操作的数据库
 */
case class MongoConfig(uri:String, db:String)

/**
 * rating.csv
 * @param uid 用户ID
 * @param mid 电影ID
 * @param score 用户对于电影的评分
 * @param timestamp 用户对于电影的评分的时间
 */
case class Rating(uid: Int, mid: Int, score: Double, timestamp: Int)

/**
 * 推荐电影
 * @param mid 电影推荐的id
 * @param score 电影推荐的评分
 */
case class Recommendation(mid:Int, score:Double)

/**
 * 电影类别推荐
 * @param genres 电影类别
 * @param recs top10的电影集合
 */
case class GenresRecommendation(genres:String, recs:Seq[Recommendation])


object StatisticsRecommender {
  // 数据集的绝对路径
  val MOVIE_DATA_PATH = "D:\\workspace\\MovieRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\movies.csv"
  val RATING_DATA_PATH = "D:\\workspace\\MovieRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\ratings.csv"

  // 设置topk中的k值
  val MOST_SCORE_OF_NUMBER = 10
  // MongoDB中的表名
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_MOVIE_COLLECTION = "Movie"

  // 统计表的名称
  val RATE_MORE_MOVIES = "RateMoreMovies"
  val RATE_MORE_RECENTLY_MOVIES = "RateMoreRecentlyMovies"
  val AVERAGE_MOVIES = "AverageMovies"
  val GENRES_TOP_MOVIES = "GenresTopMovies"

  def main(args: Array[String]): Unit = {

    /**
     * 设置配置信息
     */
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://192.168.118.130:27017/recommender",
      "mongo.db" -> "recommender"
    )

    /**
     * 创建spark配置
     */
    // 创建sparkConf配置
    val sparkConf = new SparkConf().setMaster(config.get("spark.cores").get).setAppName("StatisticsRecommender")
    // 创建sparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    /**
     * 加载数据
     */

    implicit val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    import spark.implicits._

    val ratingDF = spark
      .read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Rating]
      .toDF()

    val movieDF = spark
      .read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MOVIE_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Movie]
      .toDF()

    // 创建ratings表
    ratingDF.createOrReplaceTempView("ratings")

    /**
     * 统计推荐算法
     */


//以上借鉴GitHub
//以下借鉴资料


    // 1. 历史热门统计，历史评分数据最多，mid，count
    val rateMoreMoviesDF = spark.sql("select mid, count(mid) as count from ratings group by mid")
    // 把结果写入对应的mongodb表中
    storeDFInMongoDB( rateMoreMoviesDF, RATE_MORE_MOVIES )

    // 2. 近期热门统计，按照“yyyyMM”格式选取最近的评分数据，统计评分个数
    // 创建一个日期格式化工具
    val simpleDateFormat = new SimpleDateFormat("yyyyMM")
    // 注册udf，把时间戳转换成年月格式
    spark.udf.register("changeDate", (x: Int)=>simpleDateFormat.format(new Date(x * 1000L)).toInt )

    // 对原始数据做预处理，去掉uid
    val ratingOfYearMonth = spark.sql("select mid, score, changeDate(timestamp) as yearmonth from ratings")
    ratingOfYearMonth.createOrReplaceTempView("ratingOfMonth")

    // 从ratingOfMonth中查找电影在各个月份的评分，mid，count，yearmonth
    val rateMoreRecentlyMoviesDF = spark.sql("select mid, count(mid) as count, yearmonth from ratingOfMonth group by yearmonth, mid order by yearmonth desc, count desc")

    // 存入mongodb
    storeDFInMongoDB(rateMoreRecentlyMoviesDF, RATE_MORE_RECENTLY_MOVIES)

    // 3. 优质电影统计，统计电影的平均评分，mid，avg
    val averageMoviesDF = spark.sql("select mid, avg(score) as avg from ratings group by mid")
    storeDFInMongoDB(averageMoviesDF, AVERAGE_MOVIES)

    // 4. 各类别电影Top统计
    // 定义所有类别
    val genres = List("Action","Adventure","Animation","Comedy","Crime","Documentary","Drama","Family","Fantasy","Foreign","History","Horror","Music","Mystery"
      ,"Romance","Science","Tv","Thriller","War","Western")

    // 把平均评分加入movie表里，加一列，inner join
    val movieWithScore = movieDF.join(averageMoviesDF, "mid")

    // 为做笛卡尔积，把genres转成rdd
    val genresRDD = spark.sparkContext.makeRDD(genres)

    // 计算类别top10，首先对类别和电影做笛卡尔积
    val genresTopMoviesDF = genresRDD.cartesian(movieWithScore.rdd)
      .filter{
        // 条件过滤，找出movie的字段genres值(Action|Adventure|Sci-Fi)包含当前类别genre(Action)的那些
        case (genre, movieRow) => movieRow.getAs[String]("genres").toLowerCase.contains( genre.toLowerCase )
      }
      .map{
        case (genre, movieRow) => ( genre, ( movieRow.getAs[Int]("mid"), movieRow.getAs[Double]("avg") ) )
      }
      .groupByKey()
      .map{
        case (genre, items) => GenresRecommendation( genre, items.toList.sortWith(_._2>_._2).take(10).map( item=> Recommendation(item._1, item._2)) )
      }
      .toDF()

    storeDFInMongoDB(genresTopMoviesDF, GENRES_TOP_MOVIES)

    spark.stop()
  }

  def storeDFInMongoDB(df: DataFrame, collection_name: String)(implicit mongoConfig: MongoConfig): Unit ={
    df.write
      .option("uri", mongoConfig.uri)
      .option("collection", collection_name)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
  }
}
