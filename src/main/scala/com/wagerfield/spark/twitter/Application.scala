package com.wagerfield.spark.twitter

import org.apache.spark.SparkConf
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.io.StdIn

object Application {
  def main(args: Array[String]): Unit = {
    val sparkConfig = new SparkConf().setAppName("Spark Twitter Example")

    // Creates the SparkContext automatically and stops it when the stream stops. Only one active
    // stream is allowed per SparkContext.
    val streamingContext = new StreamingContext(sparkConfig, Seconds(1))

    // Inputs:
    val allTweets = TwitterUtils.createStream(streamingContext, None)

    // Stateless transformations:
    val tweetText = allTweets.map(_.getText)
    val unifiedTermSequence = tweetText.flatMap(_.split(' ')).map(_.toLowerCase)
    val userMentions = unifiedTermSequence.filter(s => s.startsWith("@") && s.length > 1).map((_, 1))

    // Stateful transformation:
    val userMentionsWithCount = userMentions.reduceByKeyAndWindow((a: Int, b: Int) => a + b, Seconds(30), Seconds(5))

    // RDD transformation (sort operation):
    val usersSortedByMentionCount = userMentionsWithCount.transform { rdd =>
      rdd.sortBy(_._2, ascending = false)
    }

    // Output operation. Similar to an RDD action, except it automatically gets run on each time step.
    usersSortedByMentionCount.print()

    streamingContext.start()
    Console.println("Press [enter] to exit.")
    StdIn.readLine()
    streamingContext.stop()
  }
}
