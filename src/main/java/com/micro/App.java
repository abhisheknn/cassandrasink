package com.micro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.micro.cassandratable.CassandraTable;
import com.micro.consumer.Consumer;


@SpringBootApplication
public class App {

	@Autowired
	CassandraTable cassandraTable;
	
	@Autowired
	Consumer consumer;
	
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
