package com.micro;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.micro.cassandra.Cassandra;
import com.micro.common.Constants;
import com.micro.constant.AppConstants.ReplicationStrategy;
import com.micro.kafka.KafkaConsumer;

@Component
public class CassandraTable {

	private Gson gson = new Gson();
	Type mapType = new TypeToken<Map<String, Object>>() {
	}.getType();
	
	@Autowired
	CassandraConnector cassandraConnector;
	
	@PostConstruct
	public void create() {
		Properties config= new Properties();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv(Constants.KAFKA_BROKER));
		config.put(ConsumerConfig.GROUP_ID_CONFIG, "create-table");
		Cassandra.createKeySpace(cassandraConnector.getSession(), Constants.DOCKERX_KEYSPACE, ReplicationStrategy.SimpleStrategy, 1);
		KafkaConsumer
		.build()
		.withConfig(config)
		.withTopic(Constants.CREATE_TABLE_TOPIC)	
		.withProcessor(()->{
			ConsumerRecords<String, String> records= KafkaConsumer.builder.getConsumer().poll(100);
			for(ConsumerRecord<String, String> record:records) {
				String key= record.key();
				String value= record.value();
				Map<String, String> columConfiguration= gson.fromJson(value, mapType);
				try {
				Cassandra.createTable(cassandraConnector.getSession(), Constants.DOCKERX_KEYSPACE, key, columConfiguration);
				}catch(Exception e) {
					e.printStackTrace();
				}
				}
			return true;
		}).consume();
	}
}
