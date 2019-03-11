package com.micro.cassandratable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
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
import com.micro.cassandra.Cassandra.Configuration;
import com.micro.common.Constants;
import com.micro.connector.CassandraConnector;
import com.micro.constant.AppConstants.ReplicationStrategy;
import com.micro.kafkaconsumer.KafkaConsumer;

@Component
public class CassandraTable {

	private Gson gson = new Gson();
	Type mapType = new TypeToken<Map<String, Object>>() {
	}.getType();
	Type cassandraConfType = new TypeToken<Cassandra.Configuration>() {
	}.getType();
	Type listCassandraConfType = new TypeToken<List<Cassandra.Configuration>>() {
	}.getType();

	@Autowired
	CassandraConnector cassandraConnector;

	@Autowired
	KafkaConsumer kafkaConsumer;
	
	@PostConstruct
	public void create() {
		Properties config = new Properties();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv(Constants.KAFKA_BROKER));
		config.put(ConsumerConfig.GROUP_ID_CONFIG, "create-table");
		kafkaConsumer.build().withConfig(config).withTopic(Constants.CREATE_TABLE_TOPIC).withProcessor(() -> {
			ConsumerRecords<String, String> records = kafkaConsumer.builder.getConsumer().poll(100);
			for (ConsumerRecord<String, String> record : records) {
				String key = record.key();
				String value = record.value();
				
				Map<String, Object> configuration = gson.fromJson(value, mapType);
				Cassandra.Configuration columConfiguration = gson.fromJson( gson.toJson(configuration.get(Cassandra.CONFIGURATION_TYPE.TABLE.toString())),cassandraConfType);
				List<Cassandra.Configuration> typeConfigurations = gson.fromJson( gson.toJson(configuration.get(Cassandra.CONFIGURATION_TYPE.TYPE.toString())),listCassandraConfType);

				
				try {
					if (null != typeConfigurations) {
					for(Cassandra.Configuration typeConfiguration:typeConfigurations) {
						Cassandra.createKeySpace(cassandraConnector.getSession(), typeConfiguration.getKeySpace(),
								ReplicationStrategy.SimpleStrategy, 1);
						
						Cassandra.createType(cassandraConnector.getSession(), columConfiguration.getKeySpace(),
								typeConfiguration.getName(), typeConfiguration.getConf());
					} 
					}
					if (null != columConfiguration) {
						Cassandra.createKeySpace(cassandraConnector.getSession(), columConfiguration.getKeySpace(),
								ReplicationStrategy.SimpleStrategy, 1);
						Cassandra.createTable(cassandraConnector.getSession(), columConfiguration.getKeySpace(),
								columConfiguration.getName(), columConfiguration.getConf());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}).consume();
	}
}
