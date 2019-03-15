package com.micro.consumer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.micro.cassandra.Cassandra;
import com.micro.common.Constants;
import com.micro.connector.CassandraConnector;
import com.micro.constant.AppConstants.ReplicationStrategy;
import com.micro.kafka.KafkaConsumer;

@Component
public class Consumer {

	private Gson gson = new Gson();
	Type mapType = new TypeToken<Map<String, Object>>() {
	}.getType();
	Type listType = new TypeToken<List<Map<String, String>>>() {
	}.getType();

	@Autowired
	CassandraConnector cassandraConnector;
	
	
	@PostConstruct
	public void create() {
		Properties config = new Properties();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv(Constants.KAFKA_BROKER));
		//[{"topic":"dockerx.container_details","table_name":"dockerx.container_details"},{"topic":"dockerx.container_to_mount","table_name":"dockerx.container_to_mount"},{"topic":"dockerx.network_details","table_name":"dockerx.network_details"}]
		List<Map<String, String>>  consumerTopicConfigs = gson.fromJson(Constants.COSUMERTOPICCONFIG,listType);
		Cassandra.createTable(cassandraConnector.getSession(), Constants.DOCKERX_KEYSPACE,Constants.STREAM_TO_TABLE, Constants.STREAM_TO_TABLE_CONFIG);
		for (Map<String, String> consumerTopicConfig : consumerTopicConfigs) {
			consumerTopicConfig.put("uid",""+UUID.randomUUID());
			Cassandra.insertJSON(cassandraConnector.getSession(), Constants.DOCKERX_KEYSPACE, Constants.STREAM_TO_TABLE, gson.toJson(consumerTopicConfig));
			String tableInfo =consumerTopicConfig.get(Constants.TABLE);
			String keySpace = tableInfo.split("\\.")[0];
			String table = tableInfo.split("\\.")[1];	
			String topic=consumerTopicConfig.get(Constants.TOPIC);
			config.put(ConsumerConfig.GROUP_ID_CONFIG,tableInfo );
			config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
			
			KafkaConsumer kafkaConsumer= new KafkaConsumer(); 
			kafkaConsumer
			.build()
			.withConfig(config)
			.withTopic(topic)
			.withProcessor(() -> {
				ConsumerRecords<String, String> records;
				 records = kafkaConsumer.builder.getConsumer().poll(100); 
				 for (ConsumerRecord<String, String> record : records) {
					String key = record.key();
					String value = record.value();
					value=value.replace("'", "");                  // Cannot insert "'" as part of JSON
					try {
						Cassandra.insertJSON(cassandraConnector.getSession(), keySpace, table, value);
						kafkaConsumer.builder.getConsumer().commitSync();
					} catch (JsonSyntaxException e) {
						e.printStackTrace();
				  }catch(InvalidQueryException e) {
					  e.printStackTrace();
					  kafkaConsumer.builder.getConsumer().commitSync();
				  }
					catch(Exception e) {e.printStackTrace();}
				}
			}).consume();
		}
	}
}
