package com.micro.cassandratable;

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
import com.micro.connector.CassandraConnector;
import com.micro.constant.AppConstants.ReplicationStrategy;
import com.micro.kafka.KafkaConsumer;

@Component
public class CassandraTable {

	private Gson gson = new Gson();
	Type mapType = new TypeToken<Map<String, Cassandra.Configuration>>() {
	}.getType();
	Type cassandraConfType = new TypeToken<Cassandra.Configuration>() {
	}.getType();

	@Autowired
	CassandraConnector cassandraConnector;

	@PostConstruct
	public void create() {
		Properties config = new Properties();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv(Constants.KAFKA_BROKER));
		config.put(ConsumerConfig.GROUP_ID_CONFIG, "create-table");
		KafkaConsumer.build().withConfig(config).withTopic(Constants.CREATE_TABLE_TOPIC).withProcessor(() -> {
			ConsumerRecords<String, String> records = KafkaConsumer.builder.getConsumer().poll(100);
			for (ConsumerRecord<String, String> record : records) {
				String key = record.key();
				String value = record.value();
				
				Map<String, Cassandra.Configuration> configuration = gson.fromJson(value, mapType);
				Cassandra.Configuration columConfiguration = configuration.get(Cassandra.CONFIGURATION_TYPE.TABLE.toString());
				Cassandra.Configuration typeConfiguration = configuration.get(Cassandra.CONFIGURATION_TYPE.TYPE.toString());

				
				try {
					if (null != typeConfiguration) {
						Cassandra.createKeySpace(cassandraConnector.getSession(), typeConfiguration.getKeySpace(),
								ReplicationStrategy.SimpleStrategy, 1);
						
						Cassandra.createType(cassandraConnector.getSession(), columConfiguration.getKeySpace(),
								typeConfiguration.getName(), typeConfiguration.getConf());
					} else if (null != columConfiguration) {
						Cassandra.createKeySpace(cassandraConnector.getSession(), columConfiguration.getKeySpace(),
								ReplicationStrategy.SimpleStrategy, 1);
						Cassandra.createTable(cassandraConnector.getSession(), columConfiguration.getKeySpace(),
								columConfiguration.getName(), columConfiguration.getConf());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return true;
		}).consume();
	}
}
