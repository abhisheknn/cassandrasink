package com.micro.common;

import java.util.HashMap;
import java.util.Map;

public class Constants {

	public static final String KAFKA_BROKER = "KAFKA_BROKER";
	public static final String CREATE_TABLE_TOPIC = "create-table";
	public static final String CASSANDRA_HOST = System.getenv("CASSANDRA_HOST");
	public static final String CASSANDRA_PORT = System.getenv("CASSANDRA_PORT");
	public static final String DOCKERX_KEYSPACE = System.getenv("DOCKERX_KEYSPACE") ;
	public static final String COSUMERTOPICCONFIG = System.getenv("COSUMERTOPICCONFIG");
	public static final String TABLE = "table_name";
	public static final String TOPIC = "topic";
	public static final String STREAM_TO_TABLE = "stream_to_table";
	public static final Map<String, String> STREAM_TO_TABLE_CONFIG=  getStreamToTableConfig();
	private static Map<String, String> getStreamToTableConfig() {
		 Map<String, String> map =new HashMap<>();
		 map.put("uid", "uuid PRIMARY KEY");
		 map.put("topic", "text");
		 map.put("table_name", "text");
		return map;
	}
	

}
