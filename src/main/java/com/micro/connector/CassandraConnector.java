package com.micro.connector;

import org.springframework.stereotype.Component;

import com.micro.common.Constants;


@Component
public class CassandraConnector extends com.micro.cassandra.CassandraConnector {
	CassandraConnector(){
		String[] nodes=Constants.CASSANDRA_HOST.split(",");
		super.connect(nodes, Integer.parseInt(Constants.CASSANDRA_PORT));	
	}
}
