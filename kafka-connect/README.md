1. Start up ZooKeeper, Kafka, and Schema Registry.
    We’ll walk through each of the commands for starting up these services, but you should refer to the quick start 
    guide for a more detailed walkthrough.
    
    Start ZooKeeper:
    ```
    docker run -d \
        --net=host \
        --name=zookeeper \
        -e ZOOKEEPER_CLIENT_PORT=32181 \
        -e ZOOKEEPER_TICK_TIME=2000 \
        confluentinc/cp-zookeeper:3.3.0
    ```
    Start Kafka:
    ```
    docker run -d \
        --net=host \
        --name=kafka \
        -e KAFKA_ZOOKEEPER_CONNECT=localhost:32181 \
        -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:29092 \
        -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
        confluentinc/cp-kafka:3.3.0
    ```
    
    <b>Note</b>
    
    You’ll notice that we set the KAFKA_ADVERTISED_LISTENERS variable to localhost:29092. This will make Kafka 
    accessible from outside the container by advertising its location on the Docker host.
    
    We are also overriding `offsets.topic.replication.factor` to 1 at runtime, since there is only one active broker in 
    this example.
    
    ---
    
    Start the Schema Registry:
    ```
    docker run -d \
      --net=host \
      --name=schema-registry \
      -e SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL=localhost:32181 \
      -e SCHEMA_REGISTRY_HOST_NAME=localhost \
      -e SCHEMA_REGISTRY_LISTENERS=http://localhost:8081 \
      confluentinc/cp-schema-registry:3.3.0
    ```
    You can confirm that each of the services is up by checking the logs using the following command: 
    `docker logs <container_name>`. 
    For example, if we run docker logs kafka, we should see the following at the end of the log output:
    ```
    ....
    [2016-07-15 23:31:00,295] INFO [Kafka Server 1], started (kafka.server.KafkaServer)
    [2016-07-15 23:31:00,295] INFO [Kafka Server 1], started (kafka.server.KafkaServer)
    ...
    ...
    [2016-07-15 23:31:00,349] INFO [Controller 1]: New broker startup callback for 1 (kafka.controller.KafkaController)
    [2016-07-15 23:31:00,349] INFO [Controller 1]: New broker startup callback for 1 (kafka.controller.KafkaController)
    [2016-07-15 23:31:00,350] INFO [Controller-1-to-broker-1-send-thread], Starting  (kafka.controller.RequestSendThread)
    ...
    ```
2. Now let’s start up Kafka Connect. Connect stores config, status, and offsets of the connectors in Kafka topics. 
   We will create these topics now using the Kafka broker we created above.
    ```
    docker run \
      --net=host \
      --rm \
      confluentinc/cp-kafka:5.0.0 \
      kafka-topics --create --topic quickstart-json-offsets --partitions 1 \
      --replication-factor 1 --if-not-exists --zookeeper localhost:32181
   ```
   ```
   docker run \
     --net=host \
     --rm \
     confluentinc/cp-kafka:5.0.0 \
     kafka-topics --create --topic quickstart-json-config --partitions 1 --replication-factor 1 --if-not-exists --zookeeper localhost:32181
   ```
   ```
   docker run \
     --net=host \
     --rm \
     confluentinc/cp-kafka:5.0.0 \
     kafka-topics --create --topic quickstart-json-status --partitions 1 --replication-factor 1 --if-not-exists --zookeeper localhost:32181
   ```
   Before moving on, you can verify that the topics are created:
   ``` 
   docker run \
     --net=host \
     --rm \
     confluentinc/cp-kafka:5.0.0 \
     kafka-topics --describe --zookeeper localhost:32181
   ```
3. Download the latest MySQL JDBC driver and copy it to the jars folder. If you are running Docker Machine, you must SSH 
   into the VM to run these commands. You may have to run the command as root.

    First, create a folder named jars:
    
    ```
    mkdir -p /tmp/quickstart/jars
    ```
    
    Then download the JDBC driver:
    ```
    curl -k -SL "http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.37.tar.gz" | tar -xzf - -C /tmp/quickstart/jars --strip-components=1 mysql-connector-java-5.1.37/mysql-connector-java-5.1.37-bin.jar
    curl -k -SL "http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-8.0.11.tar.gz" | tar -xzf - -C /tmp/quickstart/jars --strip-components=1 mysql-connector-java-8.0.11/mysql-connector-java-8.0.11.jar
    ```

4. Start a connect worker with Json support.
    ```
    docker run -d \
      --name=kafka-connect-json \
      --net=host \
      -e CONNECT_BOOTSTRAP_SERVERS=localhost:29092 \
      -e CONNECT_REST_PORT=28083 \
      -e CONNECT_GROUP_ID="quickstart-json" \
      -e CONNECT_CONFIG_STORAGE_TOPIC="quickstart-json-config" \
      -e CONNECT_OFFSET_STORAGE_TOPIC="quickstart-json-offsets" \
      -e CONNECT_STATUS_STORAGE_TOPIC="quickstart-json-status" \
      -e CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR=1 \
      -e CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR=1 \
      -e CONNECT_STATUS_STORAGE_REPLICATION_FACTOR=1 \
      -e CONNECT_KEY_CONVERTER="org.apache.kafka.connect.json.JsonConverter" \
      -e CONNECT_VALUE_CONVERTER="org.apache.kafka.connect.json.JsonConverter" \
      -e CONNECT_KEY_CONVERTER_SCHEMA_REGISTRY_URL="http://localhost:8081" \
      -e CONNECT_VALUE_CONVERTER_SCHEMA_REGISTRY_URL="http://localhost:8081" \
      -e CONNECT_INTERNAL_KEY_CONVERTER="org.apache.kafka.connect.json.JsonConverter" \
      -e CONNECT_INTERNAL_VALUE_CONVERTER="org.apache.kafka.connect.json.JsonConverter" \
      -e CONNECT_REST_ADVERTISED_HOST_NAME="localhost" \
      -e CONNECT_LOG4J_ROOT_LOGLEVEL=DEBUG \
      -e CONNECT_PLUGIN_PATH=/usr/share/java,/etc/kafka-connect/jars \
      --mount src=/tmp/quickstart/jars,target=/etc/kafka-connect/jars,type=bind \
      --mount src=/tmp/quickstart/file,target=/tmp/quickstart,type=bind \
      confluentinc/cp-kafka-connect:latest
    ```
    ```
    docker exec -it kafka-connect-json bash
    cd /etc/kafka-connect/jars/
    curl -k -SL "http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-8.0.11.tar.gz" | tar -xzf - -C . --strip-components=1 mysql-connector-java-8.0.11/mysql-connector-java-8.0.11.jar
    exit
    docker stop kafka-connect-json
    docker start kafka-connect-json
    ```
5.  Make sure that the connect worker is healthy.
    ```
    docker logs kafka-connect-json | grep started
    ```
    You should see the following output in your terminal window:
    ```
    [2016-08-25 19:18:38,517] INFO Kafka Connect started (org.apache.kafka.connect.runtime.Connect)
    [2016-08-25 19:18:38,557] INFO Herder started (org.apache.kafka.connect.runtime.distributed.DistributedHerder)
    ```
6. Launch a MYSQL database.
    
    First, launch the database container
    ```
    docker run -d \
      --name=quickstart-mysql \
      --net=host \
      -e MYSQL_ROOT_PASSWORD=confluent \
      -e MYSQL_USER=confluent \
      -e MYSQL_PASSWORD=confluent \
      -e MYSQL_DATABASE=connect_test \
      mysql
    ```
    Next, Create databases and tables. You’ll need to exec into the Docker container to create the databases.
    ```
    docker exec -it quickstart-mysql bash
    ```
    On the bash prompt, create a MySQL shell. Wait for sometimes if you get error.
    ```
    mysql -u confluent -pconfluent
    ```
    Now, execute the following SQL statements:
    ```
    CREATE DATABASE IF NOT EXISTS connect_test;
    USE connect_test;
    
    DROP TABLE IF EXISTS test;
    
    
    CREATE TABLE IF NOT EXISTS test (
      id serial NOT NULL PRIMARY KEY,
      name varchar(100),
      email varchar(200),
      department varchar(200),
      modified timestamp default CURRENT_TIMESTAMP NOT NULL,
      INDEX `modified_index` (`modified`)
    );
    
    INSERT INTO test (name, email, department) VALUES ('alice', 'alice@abc.com', 'engineering');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    INSERT INTO test (name, email, department) VALUES ('bob', 'bob@abc.com', 'sales');
    exit;
    ```
    
    Finally, exit the container shell by typing `exit`.

7. Create our JDBC Source connector using the Connect REST API. (You’ll need to have curl installed)
    
    Set the CONNECT_HOST. If you are running this on Docker Machine, then the hostname will be docker-machine ip 
    `<your docker machine name>`.
    ```
    export CONNECT_HOST=localhost
    ```
    Create the JDBC Source connector.
    ```
    curl -X POST \
      -H "Content-Type: application/json" \
      --data '{ "name": "quickstart-jdbc-source", "config": { "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector", "tasks.max": 1, "connection.url": "jdbc:mysql://127.0.0.1:3306/connect_test?user=confluent&password=confluent", "mode": "incrementing", "incrementing.column.name": "id", "timestamp.column.name": "modified", "topic.prefix": "quickstart-jdbc-", "poll.interval.ms": 30000 } }' \
      http://$CONNECT_HOST:28083/connectors
    ```
    The output of this command should be similar to the message shown below:
    ```
    {"name":"quickstart-jdbc-source","config":{"connector.class":"io.confluent.connect.jdbc.JdbcSourceConnector","tasks.max":"1","connection.url":"jdbc:mysql://127.0.0.1:3306/connect_test?user=root&password=confluent","mode":"incrementing","incrementing.column.name":"id","timestamp.column.name":"modified","topic.prefix":"quickstart-jdbc-","poll.interval.ms":"1000","name":"quickstart-jdbc-source"},"tasks":[]}
    ```
    Check the status of the connector using curl as follows:
    ```
    curl -s -X GET http://$CONNECT_HOST:28083/connectors/quickstart-jdbc-source/status
    ```
    You should see the following:
    ```
    {"name":"quickstart-jdbc-source","connector":{"state":"RUNNING","worker_id":"localhost:28083"},"tasks":[{"state":"RUNNING","id":0,"worker_id":"localhost:28083"}]}
    ```
    The JDBC sink create intermediate topics for storing data. We should see a quickstart-jdbc-test topic.
    ```
    docker run \
       --net=host \
       --rm \
       confluentinc/cp-kafka:5.0.0 \
       kafka-topics --describe --zookeeper localhost:32181
    ```
   
    Now run the java application
8. You will now launch a File Sink to read from this topic and write to an output file.
    ```
    curl -X POST -H "Content-Type: application/json" \
      --data '{"name": "quickstart-json-file-sink", "config": {"connector.class":"org.apache.kafka.connect.file.FileStreamSinkConnector", "tasks.max":"1", "topics":"quickstart-jdbc-test", "file": "/tmp/quickstart/jdbc-output.txt"}}' \
      http://$CONNECT_HOST:28083/connectors
    ```
    You should see the following in the output.
    ```
    {"name":"quickstart-json-file-sink","config":{"connector.class":"org.apache.kafka.connect.file.FileStreamSinkConnector","tasks.max":"1","topics":"quickstart-jdbc-test","file":"/tmp/quickstart/jdbc-output.txt","name":"quickstart-json-file-sink"},"tasks":[]}
    ```
    Check the status of the connector by running the following curl command:
    ```
    curl -s -X GET http://$CONNECT_HOST:28083/connectors/quickstart-json-file-sink/status
    ```
    You should get the response shown below:
    ```
    {"name":"quickstart-json-file-sink","connector":{"state":"RUNNING","worker_id":"localhost:28083"},"tasks":[{"state":"RUNNING","id":0,"worker_id":"localhost:28083"}]}
    ```
    Now check the file to see if the data is present. You will need to SSH into the VM if you are running Docker Machine.
    ```
    cat /tmp/quickstart/file/jdbc-output.txt | wc -l
    ```
    You should see `10` as the output.
    
    Because of https://issues.apache.org/jira/browse/KAFKA-4070, you will not see the actual data in the file.

9. Once you’re done, cleaning up is simple. You can simply run 
   `docker rm -f $(docker ps -a -q) && docker rmi $(docker images -a -q)` to delete all the 
   containers we created in the steps above. Because we allowed Kafka and ZooKeeper to store data on their respective 
   containers, there are no additional volumes to clean up. If you also want to remove the Docker machine you used, you 
   can do so using `docker-machine rm <machine-name>`.