package com.wagerfield.spark.twitter

import org.apache.spark.SparkConf
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.io.StdIn

object Application {
  def main(args: Array[String]): Unit = {
    val sparkConfig = new SparkConf()
      .setAppName("Spark Twitter Example")
      .setMaster("local[4]")

    // Creates the SparkContext automatically and stops it when the stream stops. Only one active
    // stream is allowed per SparkContext.
    val streamingContext = new StreamingContext(sparkConfig, Seconds(1))

    // Input discretized stream, from Twitter:
    val allTweets = TwitterUtils.createStream(streamingContext, None)

    // Stateless transformations:
    val englishLanguageTweets = allTweets.filter(_.getLang == "en")
    val tweetText = englishLanguageTweets.map(_.getText)
    val unifiedWordSequence = tweetText.flatMap(_.split(' ')).map((_, 1))

    // Stateful transformation:
    val wordsWithCount = unifiedWordSequence.reduceByKeyAndWindow((a: Int, b: Int) => a + b, Seconds(20), Seconds(5))

    // RDD transformation:
    val wordsSortedByCount = wordsWithCount.transform { rdd =>
      rdd.sortBy(_._2, ascending = false)
    }

    // Output operation. Similar to an RDD action, except it automatically gets run on each time step.
    wordsSortedByCount.print()

    streamingContext.start()
    Console.println("Press [enter] to exit.")
    StdIn.readLine()
    streamingContext.stop()
  }
}
