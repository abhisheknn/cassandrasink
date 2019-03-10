package com.micro.connector;

import org.springframework.stereotype.Component;

import com.micro.common.Constants;


@Component
public class CassandraConnector extends com.micro.cassandra.CassandraConnector {
	CassandraConnector(){
		super.connect(Constants.CASSANDRA_HOST, Integer.parseInt(Constants.CASSANDRA_PORT));	
	}
}
