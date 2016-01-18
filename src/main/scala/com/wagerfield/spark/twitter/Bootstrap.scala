package com.wagerfield.spark.twitter

object Bootstrap {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      System.err.println("You must provide the checkpoint directory")
      System.exit(1)
    }

    Program(args.head).run()
  }
}
