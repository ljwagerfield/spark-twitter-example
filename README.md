# Spark Twitter Example

Spark example app that demonstrates, on a broad level, various aspects of Spark.

## Setup

These instructions have been tested against OS X 10.11.

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

### 2) Install Hadoop (and YARN)

We install Hadoop locally to provide a more 'production realistic' environment for executing our Spark application.

*Quick Hadoop refresher:* Hadoop can be simplified into 2 components:

-   HDFS, a distributed file system, containing 1x `NameNode` to store metadata for all files, and many `DataNode`s to store the
    actual data.

-   YARN, a resource manager which allocates containers where jobs can be run using data stored in HDFS.

1.  Choose an install location for Hadoop and CD to it. E.g.:

        cd ~/Applications

2.  Download Hadoop using the same version specified above:

        curl -O http://apache.mirrors.spacedump.net/hadoop/common/hadoop-2.7.0/hadoop-2.7.0.tar.gz
        tar xvf hadoop-2.7.0.tar.gz --gzip
        rm hadoop-2.7.0.tar.gz

3.  Set up the following environment variables (by saving to ``~/.zshrc` or equivalent):

        export HADOOP_PREFIX=/Users/lawrence/Applications/hadoop-2.7.0 # Change this to your directory!
        export HADOOP_HOME=$HADOOP_PREFIX
        export HADOOP_COMMON_HOME=$HADOOP_PREFIX
        export HADOOP_CONF_DIR=$HADOOP_PREFIX/etc/hadoop
        export HADOOP_HDFS_HOME=$HADOOP_PREFIX
        export HADOOP_MAPRED_HOME=$HADOOP_PREFIX
        export HADOOP_YARN_HOME=$HADOOP_PREFIX

4.  Configure Hadoop for standalone installation:

    1.  Replace the `<configuration>` element in file `$HADOOP_PREFIX/etc/hadoop/hdfs-site.xml` with:

            <configuration>
                <property>
                    <name>dfs.datanode.data.dir</name>
                    <value>file:///Users/lawrence/Applications/hadoop-2.7.0/hdfs/datanode</value>
                    <description>Comma separated list of paths on the local filesystem of a DataNode where it should store its blocks.</description>
                </property>

                <property>
                    <name>dfs.namenode.name.dir</name>
                    <value>file:///Users/lawrence/Applications/hadoop-2.7.0/hdfs/namenode</value>
                    <description>Path on the local filesystem where the NameNode stores the namespace and transaction logs persistently.</description>
                </property>
            </configuration>

        *Important:* change paths to match your own.

    2.  Replace the `<configuration>` element in file `$HADOOP_PREFIX/etc/hadoop/core-site.xml` with:

            <configuration>
                <property>
                    <name>fs.defaultFS</name>
                    <value>hdfs://localhost/</value>
                    <description>NameNode URI</description>
                </property>
            </configuration>

5.  Format the name node directory:

        $HADOOP_PREFIX/bin/hdfs namenode -format # In cluster environment, only on NAME NODE.

6.  Run the daemons. **Must be run after reboots too.**

        $HADOOP_PREFIX/sbin/hadoop-daemon.sh start namenode # In cluster environment, only on NAME NODE.
        $HADOOP_PREFIX/sbin/hadoop-daemon.sh start datanode # In cluster environment, all SLAVE NODES.
        $HADOOP_PREFIX/sbin/yarn-daemon.sh start nodemanager  # In cluster environment, all SLAVE NODES.
        $HADOOP_PREFIX/sbin/yarn-daemon.sh start resourcemanager  # In cluster environment, only on RESOURCEMANAGER NODE.

    Run `jps` and check the following services are running:

    -   NameNode

    -   DataNode

    -   NodeManager

    -   ResourceManager

    Troubleshoot by reading the logs if necessary (see below).

#### Testing Hadoop works

1.  Check if Hadoop works by running a shell command (such as `date`) across the cluster. The following command does
    exactly that, spawning `2` containers, thus producing 2 different (but similar) dates:

        $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.7.0.jar org.apache.hadoop.yarn.applications.distributedshell.Client --jar $HADOOP_PREFIX/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-2.7.0.jar --shell_command date --num_containers 2 --master_memory 1024

    Note: the command reuses the same JAR since the `Client` and `ApplicationMaster` classes are both defined within it.

2.  Verify console output ends with:

        INFO distributedshell.Client: Application completed successfully

3.  Find the application ID in the output (looks like `application_1453042926574_0001`).

4.  Read the outputs from each of the `2` shells via:

        grep "" $HADOOP_PREFIX/logs/userlogs/<APPLICATION ID>/**/stdout

    Important: replace `<APPLICATION ID>` with the ID you found in the output.

#### Troubleshooting via Hadoop logs

Check the logs: `$HADOOP_PREFIX/logs/<daemon with problems>.log`

To stop the daemons, use same procedure as above, but with `stop` instead of `start`....

    $HADOOP_PREFIX/sbin/hadoop-daemon.sh stop namenode
    $HADOOP_PREFIX/sbin/hadoop-daemon.sh stop datanode
    $HADOOP_PREFIX/sbin/yarn-daemon.sh stop nodemanager
    $HADOOP_PREFIX/sbin/yarn-daemon.sh stop resourcemanager

### 3) Get Twitter API Keys

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

#### Option A) Run in interactive mode (non-clustered)

    $SPARK_HOME/bin/spark-submit \
      --master "local[*]" \
      --deploy-mode client \
      --class com.wagerfield.spark.twitter.Application \
      target/scala-2.11/spark-twitter-example-assembly-1.0.jar

Note: You will receive *warnings* regarding Spark not replicating to any peers. This is because the input dstreams that receive data over
the network (from Twitter in this case) attempt to persist data to two nodes by default. Otherwise if the executor fails, the block of
data it was processing will get lost. However, when running in `--deploy-mode client` there's only one worker, so its impossible to persist
to other peers.

Note 2: The `[*]` indicates that a worker thread should be created for each logical core on the machine. A fixed number can be provided,
or the `[_]` can be omitted entirely for serial execution.

#### Option B) Run in clustered mode

    $SPARK_HOME/bin/spark-submit \
      --master "yarn" \
      --deploy-mode cluster \
      --class com.wagerfield.spark.twitter.Application \
      target/scala-2.11/spark-twitter-example-assembly-1.0.jar

Note: Yarn... something about just specifying 'yarn'.
