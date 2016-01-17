# Spark Twitter Example

## Setup

Instructions tested against OS X 10.11.

### 1) Install Spark 1.6

1.  Ensure Maven is installed:

        mvn --version

    If not:

        brew install maven

2.  Build Spark `1.6.0` against Scala `2.11` from source (by default it builds against `2.10`):

        git clone git://git.apache.org/spark.git
        cd spark
        git checkout tags/v1.6.0
        ./dev/change-scala-version.sh 2.11
        build/mvn -Pyarn -Phive -Phive-thriftserver -Phadoop-2.6 -Dhadoop.version=2.7.0 -Dscala-2.11 -DskipTests clean package

    Note:

    -   Spark does not yet support its JDBC component for Scala `2.11`.

    -   We use Hadoop profile version `2.6` for `2.7.0`.

    -   Be sure to use `build/mvn` not `mvn`. The former will download `zinc` (the incremental compiler extracted from SBT) which
        will speed up compilation. Without this you will get warnings (and experience slower compilation!).

3.  Export `SPARK_HOME` as an environmental variable. It needs to point to the root of this directory.

4.  Check Spark works by running a simple operation in the `spark-shell`:

        $SPARK_HOME/bin/spark-shell
        sc.parallelize(1 to 1000).sum()

5.  Check the Spark Web UI works:

        http://localhost:4040

    Note: Every SparkContext (driver node) launches a web UI, by default on port `4040`. If multiple SparkContexts are running on
    the same host, they will bind to successive ports (`4041`, `4042`, etc).

### 2) Get Twitter API Keys

1.  Sign-in to Twitter.

2.  Create a new app here:

    https://apps.twitter.com

    (enter anything you like for the 3 required fields)

3.  Create an access token.

    Note: This application only requires `read-only` access.

4.  Create a `twitter4j.properties` file in the root of this directory, and add from Twitter:

        oauth.consumerKey=*********************
        oauth.consumerSecret=******************************************
        oauth.accessToken=**************************************************
        oauth.accessTokenSecret=******************************************

## Running the application

### 1) Package the application

Use the standard method:

    sbt assembly

Note: Spark and Hadoop are provided by the cluster manager at runtime. These dependencies have therefor been *excluded* from
the fat JAR by scoping them to `provided` configuration (see `build.sbt`).

### 2) Submit to Spark

Submit the application to the Spark cluster.

    $SPARK_HOME/bin/spark-submit \
      --master "local[*]" \
      --deploy-mode client \
      --class com.wagerfield.spark.twitter.Application \
      target/scala-2.11/spark-twitter-example-assembly-1.0.jar

These arguments indicate, in respective order:

-   The cluster to submit to (`--master`).
    We're running the application locally, creating a worker thread for each logical core on the machine (as indicated by `[*]`).

-   Whether to run on the worker nodes or locally (`--deploy-mode`).
    We specify a `client` deployment mode to have the application run in-process, thus allowing us to interact with the shell.
    The alternative deployment mode, `cluster`, is not available for `local` masters.

-   The main class of the application.

-   The previously assembled fat JAR.

Note: You will receive *warnings* regarding Spark not replicating to any peers. This is because the input dstreams that receive data over
the network (from Twitter in this case) attempt to persist data to two nodes by default. Otherwise if the executor fails, the block of
data it was processing will get lost. However, when running in `--deploy-mode client` there's only one worker, so its impossible to persist
to other peers.
