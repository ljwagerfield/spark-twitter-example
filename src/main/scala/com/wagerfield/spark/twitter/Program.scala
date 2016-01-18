package com.wagerfield.spark.twitter

import org.apache.spark.SparkConf
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.stat.{Statistics, MultivariateStatisticalSummary}
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

case class Program(checkpointDirectory: String) {
  val windowDuration = Seconds(30)
  val slideDuration = Seconds(5)
  val topUserCount = 3
  val maxTwitterNameLength = 15
  val nameRegex = s"([a-zA-Z0-9_]{1,$maxTwitterNameLength})".r

  def run(): Unit = {
    // Checkpointing is required when using stateful transformations. See the sliding-window reduction below.
    val streamingContext = StreamingContext.getOrCreate(checkpointDirectory, createStreamingContext)
    streamingContext.start()
    streamingContext.awaitTermination()
  }

  private def createStreamingContext(): StreamingContext = {
    val sparkConfig = new SparkConf().setAppName("Spark Twitter Example")

    // Creates the SparkContext automatically and stops it when the stream stops. Only one active
    // input stream is allowed per SparkContext.
    val streamingContext = new StreamingContext(sparkConfig, Seconds(1))
    streamingContext.checkpoint(checkpointDirectory)

    // Input stream:
    val allTweets = TwitterUtils.createStream(streamingContext, None)

    // Stateless transformations:
    val tweetText = allTweets.map(_.getText)
    val unifiedTermSequence = tweetText.flatMap(_.split(' ')).map(_.toLowerCase)
    val potentialMentions = unifiedTermSequence.filter(_.startsWith("@"))
    val confirmedMentions = potentialMentions.flatMap(m => nameRegex.findFirstIn(m).toList)
    val seededMentionCounts = confirmedMentions.map((_, 1))

    // Stateful transformation:
    val userMentionsWithCount = seededMentionCounts.reduceByKeyAndWindow((a: Int, b: Int) => a + b, windowDuration, slideDuration)

    // RDD transformation (sort operation):
    val usersSortedByMentionCount = userMentionsWithCount.transform { rdd =>
      rdd.sortBy(_._2, ascending = false)
    }

    // Cache the transformed DStream ahead of it being referenced by multiple operations.
    val cachedSortedUsers = usersSortedByMentionCount.cache()

    // Output operations. Similar to RDD actions, except automatically run on each time step.
    cachedSortedUsers
      .map(kvp => Vectors.dense(kvp._2))
      .foreachRDD { rdd =>
        val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)
        val windowDurationSeconds = windowDuration.milliseconds / 1000
        val slideDurationSeconds = slideDuration.milliseconds / 1000
        println("-----------------------------")
        println("TWITTER 'MENTIONS' STATISTICS")
        println("-----------------------------")
        println(s"Recorded for the last $windowDurationSeconds seconds, updates every $slideDurationSeconds seconds.")
        println()
        println(s"Mean:     ${summary.mean(0)}")
        println(s"Variance: ${summary.variance(0)}")
        println(s"Range:    ${summary.min(0).toInt} to ${summary.max(0).toInt}")
        println()
      }

    cachedSortedUsers.foreachRDD { rdd =>
      println("Top recently mentioned users:")
      val topMentionedUsers = rdd.take(topUserCount)
      val formattedLines = topMentionedUsers.zipWithIndex.map {
        case ((user, mentions), index) => s"${index + 1}. @${user.padTo(maxTwitterNameLength, ' ')} ($mentions mentions)"
      }
      formattedLines.foreach(println)
      println()
    }

    streamingContext
  }
}
