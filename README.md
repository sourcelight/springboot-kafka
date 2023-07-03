## Spring Kafka

This module contains different examples about Spring with Kafka


### Intro

This is a simple Spring Boot app to demonstrate sending and receiving of messages in Kafka using spring-kafka.

As Kafka topics are not created automatically by default, this application requires that you create the following topics manually.

`$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic rbruno`<br>
`$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 5 --topic partitioned`<br>
`$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic filtered`<br>
`$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic greeting`<br>

When the application runs successfully, following output is logged on to console (along with spring logs):

#### Message received from the 'rbruno' topic by the basic listeners with groups foo and bar
>Received Message in group 'foo': Hello, World!<br>
Received Message in group 'bar': Hello, World!

#### Message received from the 'rbruno' topic, with the partition info
>Received Message: Hello, World! from partition: 0

#### Message received from the 'partitioned' topic, only from specific partitions
>Received Message: Hello To Partioned Topic! from partition: 0<br>
Received Message: Hello To Partioned Topic! from partition: 3

#### Message received from the 'filtered' topic after filtering
>Received Message in filtered listener: Hello rbruno!

#### Message (Serialized Java Object) received from the 'greeting' topic
>Received greeting message: Greetings, World!!

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


### TESTING:

a)IN-MEMORY KAFKA BROKER

@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })

First, we'll start by looking at how to use and configure an EMBEDDED INSTANCE OF KAFKA.

Then we'll see how we can make use of the popular framework TESTCONTAINERS from our tests.


A Word on Testing

In general, when writing clean integration tests, we shouldn't depend on external services that we might not be able to control or might suddenly stop working

b)TESTING KAFKA WITH TESTCONTAINERS

Reasons: small differences between a real external service vs an embedded in-memory instance




### KAFA STREAMS:
Kafka Streams is a client-side library built on top of Apache Kafka
It enables the processing of an unbounded stream of events in a declarative manner.
Some real-life examples of streaming data could be sensor data, stock market event streams, and system logs
We'll build a simple word-count streaming application.
Kafka Streams provides a duality between Kafka topics and relational database tables.
It enables us to do operations like joins, grouping, aggregation, and filtering of one or more streaming events.
PROCESSOR TOPOLOGY is the BLUEPRINT of Kafka Stream operations on one or more event streams.
The processor topology can be considered as a DIRECTED ACYCLIC GRAPH. In this graph, nodes are categorized into SOURCE, PROCESSOR, and SINK nodes.
Alongside the core processing, the state of the stream is saved periodically using checkpoints for fault tolerance and resilience.

Create a topolgy processor
Notice "KafkaTemplate" for the producer and "KStream" for Kafka Streams to create the input stream and "KTable" to reate the output stream.

Save the output to a kafka store

Create a Rest application reading from the store


### TESTING
Inside the "embedded" folder  
Let' create a simple producer-consumer application

#### Testing Using Embedded Kafka(In memory Kafka Broker), no need to run docker
* We use an in-memory Kafka instance to run our tests against
* Our dependency contains the EmbeddedKafkaBroker class
* @EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
* the above annotation:  inject an instance of an EmbeddedKafkaBroker into our tests.

#### Test Containers (It requires Docker installed and running)
* Small differences between a real external service vs an embedded in-memory
* We'll instance a Kafka Broker inside a Docker Container
* "@ClassRule" manages the whole lifecycle container
* With "KafkaTestContainersConfiguration" we manage the dynamic configuration created by the "kafkatestcontainer"  
that prevents port clashes.



	UNIT TEST
		TopologyTestDriver => to test kafka streams
	
	TEST INTEGRATION
		Test containers and Rest Template

RestTempalte to post data and reads data from the kafka store

Conclusions:
In this example, we've seen how to create a simple event-driven application to process messages with Kafka Streams and Spring Boot.
After a brief overview of core streaming concepts, we looked at the configuration and creation of a Streams topology.
Then, we saw how to integrate this with the REST functionality provided by Spring Boot.
Finally, we covered some approaches for effectively testing and verifying our topology and application behavior.


### CONSUMER LAG:
Kafka consumer group lag is a key performance indicator of any Kafka-based event-driven system  
Consumer lag is simply the delta between the consumer's last committed offset and the producer's end offset in the log(topic)  
We'll build an analyzer application to monitor Kafka consumer lag.  
To inspect the offset values of a consumer group, we'll need the administrative Kafka client  
*We compute the differences offset (producer vs consumer group id) for each topic partition*  




Simulator

Monitoring Consumer Lag via Actuator Endpoint

Configuring Metrics Using Micrometer

reading  via scripts in near real time

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
