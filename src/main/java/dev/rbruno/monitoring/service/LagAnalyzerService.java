package dev.rbruno.monitoring.service;

import dev.rbruno.monitoring.util.MonitoringUtil;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;


@Service
public class LagAnalyzerService {


    private Long lag;

    public Long getLag() {
        return lag;
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(LagAnalyzerService.class);

    private final AdminClient adminClient;
    private final KafkaConsumer<String, String> consumer;

    @Autowired
    public LagAnalyzerService(@Value("${monitor.kafka.bootstrap.config}") String bootstrapServerConfig) {
        adminClient = getAdminClient(bootstrapServerConfig);
        consumer = getKafkaConsumer(bootstrapServerConfig);
    }

    /**
     * this method returns the lagd for each partition of a specific consumer-group
     *
     * @param groupId
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Map<TopicPartition, Long> analyzeLag(String groupId)
            throws ExecutionException, InterruptedException {
        Map<TopicPartition, Long> consumerGrpOffsets = getConsumerGrpOffsets(groupId);
        Map<TopicPartition, Long> producerOffsets = getProducerOffsets(consumerGrpOffsets);
        Map<TopicPartition, Long> lags = computeLags(consumerGrpOffsets, producerOffsets);
        for (Map.Entry<TopicPartition, Long> lagEntry : lags.entrySet()) {
            String topic = lagEntry.getKey()
                    .topic();
            int partition = lagEntry.getKey()
                    .partition();
            this.lag = lagEntry.getValue();
            LOGGER.info("Time={} | Lag for topic = {}, partition = {}, groupId = {} is {}",
                    MonitoringUtil.time(),
                    topic,
                    partition,
                    groupId, this.lag);
        }
        return lags;
    }

    /**
     * This method returns the current offset for each topic partition
     * where a consumer with a specific group id is acting
     *
     * @param groupId
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Map<TopicPartition, Long> getConsumerGrpOffsets(String groupId)
            throws ExecutionException, InterruptedException {
        ListConsumerGroupOffsetsResult info = adminClient.listConsumerGroupOffsets(groupId);
        Map<TopicPartition, OffsetAndMetadata> metadataMap = info
                .partitionsToOffsetAndMetadata()
                .get();
        Map<TopicPartition, Long> groupOffset = new HashMap<>();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : metadataMap.entrySet()) {
            TopicPartition key = entry.getKey();
            OffsetAndMetadata metadata = entry.getValue();
            groupOffset.putIfAbsent(new TopicPartition(key.topic(), key.partition()), metadata.offset());
        }
        return groupOffset;
    }

    /**
     * This method returns the end offset for each topic partition
     * where a consumer with a specific consumerGrpOffset is acting
     *
     * @param consumerGrpOffset
     * @return
     */
    private Map<TopicPartition, Long> getProducerOffsets(Map<TopicPartition, Long> consumerGrpOffset) {
        List<TopicPartition> topicPartitions = new LinkedList<>();
        for (Map.Entry<TopicPartition, Long> entry : consumerGrpOffset.entrySet()) {
            TopicPartition key = entry.getKey();
            topicPartitions.add(new TopicPartition(key.topic(), key.partition()));
        }
        return consumer.endOffsets(topicPartitions);
    }

    /**
     * This method returns the lag for each partition
     * elaborated by a consumer group
     *
     * @param consumerGrpOffsets
     * @param producerOffsets
     * @return
     */
    public Map<TopicPartition, Long> computeLags(
            Map<TopicPartition, Long> consumerGrpOffsets,
            Map<TopicPartition, Long> producerOffsets) {
        Map<TopicPartition, Long> lags = new HashMap<>();
        for (Map.Entry<TopicPartition, Long> entry : consumerGrpOffsets.entrySet()) {
            Long producerOffset = producerOffsets.get(entry.getKey());
            Long consumerOffset = consumerGrpOffsets.get(entry.getKey());
            long lag = Math.abs(Math.max(0, producerOffset) - Math.max(0, consumerOffset));
            lags.putIfAbsent(entry.getKey(), lag);
        }
        return lags;
    }

    private AdminClient getAdminClient(String bootstrapServerConfig) {
        Properties config = new Properties();
        config.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServerConfig);
        return AdminClient.create(config);
    }

    private KafkaConsumer<String, String> getKafkaConsumer(
            String bootstrapServerConfig) {
        Properties properties = new Properties();
        properties.setProperty(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServerConfig);
        properties.setProperty(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        return new KafkaConsumer<>(properties);
    }
}
