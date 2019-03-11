package com.micro.kafkaconsumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class KafkaConsumer extends com.micro.kafka.KafkaConsumer {

}
