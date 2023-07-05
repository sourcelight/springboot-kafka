## Spring Kafka

This module contains different examples about Spring with Kafka  
We could create topics manually as shown below,   
but this time we're going to create our topic programmatically using the AdminClient interface

`$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic rbruno`<br>
`$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 5 --topic partitioned`<br>
`$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic filtered`<br>
`$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic greeting`<br>



## Notes on the covered examples

### BASIC:

* Start docker-compose from the folder src/main/java/dev/rbruno/spring/kafka/
* For offset explorer 2.3.1 point zookeeper on port 22181

With the introduction of AdminClient in Kafka, we can now create topics programmatically.  


In KafkaApplication class we have the MessageProducer and the MessageConsumer  
with multiple methods for the producers and the consumers(@KafkaListener)


## Key Points:
### Producer
* KafkaProducerConfig:
* producerFactory(with all specific parameters)
* KafkaTemplate(which simply wraps a producerFactory instance and provides convenience methods for sending messages to Kafka topics)

Particular producers:
* Json objects: Greeting => ProducerFactory<String, Greeting>
  * StringSerializer and JsonSerializer(value)

* Different Objects in the same topic => ProducerFactory<String, Object>
  * multiTypeProducerFactory: configProps.put(JsonSerializer.TYPE_MAPPINGS
  


### Consumer
* we need to configure a ConsumerFactory and a KafkaListenerContainerFactory
* now it's possible to use @KafkaListener once we've declared also: @EnableKafka in the configuration class
* Note that "listenToPartitionAndOffset" listen from  the designed offset, but consumes always the current inserted offset even if "designed offset" > "actual offset"
* Note the use of "ConcurrentKafkaListenerContainerFactory", it's thread-safe and manages multiple threads(setConcurrency(num threads < partition number))
* While the "KafkaMessageListenerContainer" manages only one thread for all partitions and multiple topics


Test consumers:  
* 2 consumers on the same topic
  * listenGroupFoo
  * listenGroupBar
* listenWithHeaders
* listenToPartition
* listenToPartitionAndOffset
* listenWithFilter



Particular consumers:  

greetingKafkaListenerContainerFactory
* Json objects: Greeting => ConsumerFactory<String, Greeting>
    * StringDeserializer and JsonDeserializer(value)

* MultiTypeKafkaListener on a whole class (not on the method )
  * use of a Kafka @KafkaHandler for each type of object
  * Note with the "isDefault" attribute to true you can manage everything and tune later

Important Note: If ack=all & min.insync.replicas = Number of partitions  
In case one broker is down => The producer get stuck waiting for the "min.insync.replicas" never reached  
Conclusion => better having "min.insync.replicas" < num.brokers  
"acks" is a client producer configuration  
"min.insync.replicas" is a config on the broker  



### KAFKA STREAMS:

Kafka Streams is a client-side library built on top of Apache Kafka
It enables the processing of an unbounded stream of events in a declarative manner.
Some real-life examples of streaming data could be sensor data, stock market event streams, and system logs  
We'll build a simple word-count streaming application.  
**Kafka Streams provides a duality between Kafka topics and relational database tables.**  
It enables us to do operations like joins, grouping, aggregation, and filtering of one or more streaming events.  
**PROCESSOR TOPOLOGY** is the BLUEPRINT of Kafka Stream operations on one or more event streams.
The processor topology can be considered as a DIRECTED ACYCLIC GRAPH. In this graph, nodes are categorized into SOURCE, PROCESSOR, and SINK nodes.
Alongside the core processing, the state of the stream is saved periodically using checkpoints for fault tolerance and resilience.


**Steps**
* Here, we've used th e@EnableKafkaStreams annotation to autoconfigure the required components
* We create an "KafkaStreamsConfiguration" Bean 
* Spring Boot uses the above configuration and creates a KafkaStreams client to manage our application lifecycle
* We build the Topology processor to keep a count of the words from input messages
* **Notice the analogy**:
  * "KafkaTemplate" for the producer and "KStream" for Kafka Streams to create the input stream 
  * "KTable" to create the output stream.
* We've used the high-level DSL to define the transformations
  * Create a KStream from the input topic using the specified key and value SerDes.
  * Create a KTable by transforming, splitting, grouping, and then counting the data.
  * Materialize the result to an output stream.

Save the output to a kafka topic and in a kafka store(Materialized.as("counts")) 

We Create a Rest application (WordCountRestService)
* We read the data from the kafka store via Rest API
* We insert the data into the kafkaProducer via Rest API


### Test KafkaStreams

#### Junit test with WordCountProcessorUnitTest
It leverages on the "TopologyTestDriver"  
and**eliminates the need to have a broker running and still verify the pipeline behavior**  

* test: try to remove the transforming(lowercase) and put a capital letter in the input data




#### Integration test with KafkaStreamsApplicationLiveTest
We use a TestContainer to make and end-to-end test  
* We make post messages on the API(using dynamic port)
* We test the results directly on the topic(by a blocking queue)
* We test the results by the Rest API




### TESTING
In general, when writing clean integration tests, we shouldn't depend on external services that we might not be able to control or might suddenly stop working  

Inside the "embedded" folder  
Let' create a simple producer-consumer application

#### Testing Using Embedded Kafka(In memory Kafka Broker), no need to run docker  
IN-MEMORY KAFKA BROKER  
* We use an in-memory Kafka instance to run our tests against
* Our dependency contains the EmbeddedKafkaBroker class
* @EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
* the above annotation:  inject an instance of an EmbeddedKafkaBroker into our tests.

#### Test Containers (It requires Docker installed and running)  
TESTING KAFKA WITH TESTCONTAINERS
* Small differences between a real external service vs an embedded in-memory
* We'll instance a Kafka Broker inside a Docker Container
* "@ClassRule" manages the whole lifecycle container
* With "KafkaTestContainersConfiguration" we manage the dynamic configuration created by the "kafkatestcontainer"  
that prevents port clashes.

  

### Monitoring the CONSUMER LAG:
Kafka consumer group lag is a key performance indicator of any Kafka-based event-driven system  
Consumer lag is simply the delta between the consumer's last committed offset and the producer's end offset in the log(topic)  
* We'll build an analyzer application to monitor Kafka consumer lag.  
* To inspect the offset values of a consumer group, we'll need the administrative Kafka client  
**Compute the difference offsets (producer vs consumer group id) for each topic partition**  
* We created a getConsumerGrpOffsets
* We created a getProducerOffsets  
**We need to automate everything**
* We created a LagAnalyzerService with a @Scheudled annotation
**Test Class**
* We use an embedded kafka broker(not a test container)
* We create a producer, a consumer, and we start to consume(poll every second)
* Three test cases
  * AllProducedMessagesAreConsumed_thenLagBecomesZero
  * MessageNotConsumed_thenLagIsEqualToProducedMessage
  * MessageConsumedLessThanProduced_thenLagIsNonZero

* Note producer produces a batch of 100 messages at time
* Note we consume at a rate of 1 sec(sleep) and default max.poll.records is 500 
  * For other considerations take into account also "max.partition.fetch.bytes, which is 1MB by default."

* Additional: check on the AllProducedMessagesAreConsumed_thenLagBecomesZero test:   
  about poll on the consumer: props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,"1000");

#### Simulator
* Prerequisites; set properties "monitor.producer.simulate", etc to true
* Classes inside "simulation" package
* Notes:
  * run docker compose 
  * The topic is crated by AdminClient in other classes
  * logback from DEBUG -> INFO
  * run LagAnalyzerApplication as SpringBoot application (checks every 5 secs)

**We produce at 1ms(for 30 secs) and consume at 10ms rate**
  * the lag increases and decreases to zero

So, lag will start building for the first 30 seconds, after which the producer stops producing, so lag will gradually decline to 0.


#### Monitoring Consumer Lag via Actuator Endpoint

* We set the dependencies for the actuator, prometheus and micrometer
* We enable in application properties the /actuator
* Enable JMX and create an instance of MeterRegistry
* Add the MeterRegistry instance to the ConsumerFactory

Reading  via scripts in near real time using property exposed by micrometer-prometheus
<pre>
while true
do
curl --silent -XGET http://localhost:8081/actuator/prometheus | \
awk '/kafka_consumer_fetch_manager_records_lag{/{print "Current lag is:",$2}';
sleep 5;
done
</pre>

Reading  via scripts in near real time using property exposed by custom EP with the Actuator
<pre>
while true
do
curl --silent -XGET http://localhost:8081/actuator/get-lag-consumer | \
awk '{print "Current lag is:",$1}';
sleep 2;
done
</pre>



### Conclusions:
In this example, we've seen how to create a simple event-driven application to process messages with Kafka Streams and Spring Boot.  
After a brief overview of core streaming concepts, we looked at the configuration and creation of a Streams topology.  
Then, we saw how to integrate this with the REST functionality provided by Spring Boot.  
Finally, we covered some approaches for effectively testing and verifying our topology and application behavior.  



#### References
Basic: https://www.baeldung.com/spring-kafka  
Kafka Headers: https://docs.spring.io/spring-kafka/api/org/springframework/kafka/support/KafkaHeaders.html  
LogBack: https://logback.qos.ch/manual/configuration.html#autoScan  
InSync: https://accu.org/journals/overload/28/159/kozlovski/  
Filtering maven dependencies: https://maven.apache.org/plugins/maven-dependency-plugin/examples/filtering-the-dependency-tree.html    
* mvn dependency:tree [groupId]:[artifactId]:[type]:[version]  
* Example: mvn dependency:tree -Dincludes=junit:junit:jar:4.13.2  
*Junit Testcontainers*
  * Junit Testcontainers integrations: https://java.testcontainers.org/test_framework_integration/junit_4/
  * Jupieter/Junit 5: https://java.testcontainers.org/test_framework_integration/junit_5/ 
* Blocking queue: https://www.youtube.com/watch?v=d3xb1Nj88pw
* àk,