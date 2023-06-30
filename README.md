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

Test consumers:  
* listenWithHeaders
* listenToPartition
* listenToPartitionAndOffset
* listenWithFilter
* listenGroupFoo
* listenGroupBar


Particular consumers:  

greetingKafkaListenerContainerFactory
* Json objects: Greeting => ConsumerFactory<String, Greeting>
    * StringDeserializer and JsonDeserializer(value)





In KafkaApplication class we have the MessageProducer and the MessageConsumer
Multi-Method Listeners


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


TESTING:

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
Consumer lag is simply the delta between the consumer's last committed offset and the producer's end offset in the log(topic)

Simulator

Monitoring Consumer Lag via Actuator Endpoint

Configuring Metrics Using Micrometer

reading  via scripts in nera real time

#### References
https://www.baeldung.com/spring-kafka  
https://docs.spring.io/spring-kafka/api/org/springframework/kafka/support/KafkaHeaders.html
https://logback.qos.ch/manual/configuration.html#autoScan