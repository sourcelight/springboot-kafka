package dev.rbruno.spring.kafka;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KafkaListener(id = "multiGroup", topics = "multitype")
public class MultiTypeKafkaListener {
    
    @PostConstruct
    void init(){
        log.info("Inside MultiTypeKafkaListener post construct");
    }

    @KafkaHandler
    public void handleGreeting(Greeting greeting) {
        log.info("FromMulti Greeting received: " + greeting);
    }

    @KafkaHandler
    public void handleF(Farewell farewell) {
        log.info("FromMulti Farewell received: " + farewell);
    }

    @KafkaHandler(isDefault = true)
    public void unknown(Object object) {
        log.info("FromMulti Unkown type received: " + object);
    }

}
