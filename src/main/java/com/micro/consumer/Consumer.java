package com.micro.consumer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.micro.cassandra.Cassandra;
import com.micro.common.Constants;
import com.micro.connector.CassandraConnector;
import com.micro.constant.AppConstants.ReplicationStrategy;
import com.micro.kafkaconsumer.KafkaConsumer;

@Component
public class Consumer {

	private Gson gson = new Gson();
	Type mapType = new TypeToken<Map<String, Object>>() {
	}.getType();

	@Autowired
	CassandraConnector cassandraConnector;

	@Autowired
	KafkaConsumer kafkaConsumer;
	
	@PostConstruct
	public void create() {
		Properties config = new Properties();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv(Constants.KAFKA_BROKER));
		
		String[] topics = Constants.TOPICS.split(",");
		for (String topic : topics) {
			config.put(ConsumerConfig.GROUP_ID_CONFIG, topic);
			config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
			kafkaConsumer
			.build()
			.withConfig(config)
			.withTopic(topic)
			.withProcessor(() -> {
				ConsumerRecords<String, String> records;
				 records = kafkaConsumer.builder.getConsumer().poll(100); 
				 for (ConsumerRecord<String, String> record : records) {
					String key = record.key();
					String[] tableInfo =kafkaConsumer.builder.getTopic().split("\\.");
					String keySpace = tableInfo[0];
					String table = tableInfo[1];
					String value = record.value();
					value=value.replace("'", "");
					try {
						Cassandra.insertJSON(cassandraConnector.getSession(), keySpace, table, value);
						kafkaConsumer.builder.getConsumer().commitSync();
					} catch (JsonSyntaxException e) {
						e.printStackTrace();
				  }catch(Exception e) {e.printStackTrace();}
				}
			}).consume();
		}
	}
}
