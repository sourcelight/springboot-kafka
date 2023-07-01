package dev.rbruno.spring.kafka;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
@Slf4j
@SpringBootApplication
public class KafkaApplication {

    public static void main(String[] args) throws Exception {

        ConfigurableApplicationContext context = SpringApplication.run(KafkaApplication.class, args);

        MessageProducer producer = context.getBean(MessageProducer.class);
        MessageListener listener = context.getBean(MessageListener.class);
        /*
         * Sending a Hello World message to topic 'rbruno'. 
         * Must be received by both listeners with group foo
         * and bar with containerFactory fooKafkaListenerContainerFactory
         * and barKafkaListenerContainerFactory respectively.
         * It will also be received by the listener with
         * headersKafkaListenerContainerFactory as container factory.
         */
        producer.sendMessage("Hello, World!");
        listener.latch.await(10, TimeUnit.SECONDS);

        /*
         * Sending message to a topic with 5 partitions,
         * each message to a different partition. But as per
         * listener configuration, only the messages from
         * partition 0 and 3 will be consumed.
         */
        for (int i = 0; i < 5; i++) {
            producer.sendMessageToPartition("Hello To Partitioned Topic!", i);
        }
        listener.partitionLatch.await(10, TimeUnit.SECONDS);

        /*
         * Sending message to 'filtered' topic. As per listener
         * configuration,  all messages with char sequence
         * 'World' will be discarded.
         */
        producer.sendMessageToFiltered("Hello rbruno!");
        producer.sendMessageToFiltered("Hello World!");
        listener.filterLatch.await(10, TimeUnit.SECONDS);

        /*
         * Sending message to 'greeting' topic. This will send
         * and received a java object with the help of
         * greetingKafkaListenerContainerFactory.
         */
        producer.sendGreetingMessage(new Greeting("Greetings", "World!"));
        listener.greetingLatch.await(10, TimeUnit.SECONDS);


        producer.sendMultiType(new Greeting("Greetings", "World!"));
        producer.sendMultiType(new Farewell("Farewell", 25));
        producer.sendMultiType(new GreetingNotMapped("Hello", "Riccardo","Bruno"));
        producer.sendMultiType( "Simple string message");
        listener.multiTypeLatch.await(10, TimeUnit.SECONDS);


        context.close();
    }

    @Bean
    public MessageProducer messageProducer() {
        return new MessageProducer();
    }

    @Bean
    public MessageListener messageListener() {
        return new MessageListener();
    }

    public static class MessageProducer {

        @Autowired
        private KafkaTemplate<String, String> kafkaTemplate;

        @Autowired
        private KafkaTemplate<String, Greeting> greetingKafkaTemplate;

        @Autowired
        private KafkaTemplate<String, Object> multiTypeKafkaTemplate;

        @Value(value = "${message.topic.name}")
        private String topicName;

        @Value(value = "${partitioned.topic.name}")
        private String partitionedTopicName;

        @Value(value = "${filtered.topic.name}")
        private String filteredTopicName;

        @Value(value = "${greeting.topic.name}")
        private String greetingTopicName;

        @Value(value = "${multi.type.topic.name}")
        private String multiTypeTopicName;

        public void sendMessage(String message) {

            //illustrate blocking case adding get() at the end of send method(on CompletableFuture), the thread will wait
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topicName, message).whenComplete((res,ex) ->{
                if (ex == null) {
                    log.info("Sent message=[" + message + "] with offset=[" + res.getRecordMetadata()
                            .offset() + "]");
                } else {
                    log.info("Unable to send message=[" + message + "] due to : " + ex.getMessage());
                }
            });

        }

        public void sendMessageToPartition(String message, int partition) {
            kafkaTemplate.send(partitionedTopicName, partition, null, message);
        }

        public void sendMessageToFiltered(String message) {
            kafkaTemplate.send(filteredTopicName, message);
        }

        public void sendGreetingMessage(Greeting greeting) {
            greetingKafkaTemplate.send(greetingTopicName, greeting);
        }


        public void sendMultiType(Object object) {
            multiTypeKafkaTemplate.send(multiTypeTopicName, object);
        }


    }

    public static class MessageListener {

        private CountDownLatch latch = new CountDownLatch(3);

        private CountDownLatch partitionLatch = new CountDownLatch(2);

        private CountDownLatch filterLatch = new CountDownLatch(2);

        private CountDownLatch greetingLatch = new CountDownLatch(1);

        private CountDownLatch multiTypeLatch = new CountDownLatch(4);

        @KafkaListener(topics = "${message.topic.name}", groupId = "foo", containerFactory = "fooKafkaListenerContainerFactory")
        public void listenGroupFoo(String message) {
            log.info("Received Message in group 'foo': " + message);
            latch.countDown();
        }

        @KafkaListener(topics = "${message.topic.name}", groupId = "bar", containerFactory = "barKafkaListenerContainerFactory")
        public void listenGroupBar(String message) {
            log.info("Received Message in group 'bar': " + message);
            latch.countDown();
        }



        @KafkaListener(topics = "${message.topic.name}", containerFactory = "headersKafkaListenerContainerFactory")
        public void listenWithHeaders(@Payload String message, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,@Header(KafkaHeaders.OFFSET) String offset) {
            log.info("Received All Headers  producer: " + message + " from partition: " + partition+" topic: "+topic +" offset: "+offset );
            latch.countDown();
        }

        @KafkaListener(topicPartitions = @TopicPartition(topic = "${partitioned.topic.name}", partitions = { "0", "3" }), containerFactory = "partitionsKafkaListenerContainerFactory")
        public void listenToPartition(@Payload String message, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
            log.info("Received Partitioned Message: " + message + " from partition: " + partition);
            this.partitionLatch.countDown();
        }

        @KafkaListener(topicPartitions = @TopicPartition(topic = "${partitioned.topic.name}", partitionOffsets = {
                @PartitionOffset(partition = "0", initialOffset = "40")}), containerFactory = "partitionsKafkaListenerContainerFactory")
        public void listenToPartitionAndOffset(@Payload String message, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,@ Header(KafkaHeaders.OFFSET) String offset) {
            log.info("Received PartitionedOffset Message: " + message + " from partition: " + partition + " offset: " + offset);
            this.partitionLatch.countDown();
        }

        @KafkaListener(topics = "${filtered.topic.name}", containerFactory = "filterKafkaListenerContainerFactory")
        public void listenWithFilter(String message) {
            log.info("ReceivedFilter Message in filtered listener: " + message);
            this.filterLatch.countDown();
        }

        @KafkaListener(topics = "${greeting.topic.name}", containerFactory = "greetingKafkaListenerContainerFactory")
        public void greetingListener(Greeting greeting) {
            log.info("Received greeting message: " + greeting);
            this.greetingLatch.countDown();
        }



    }

}
