package com.tarento.commenthub.transactional.cassandrautils;

import java.util.List;
import java.util.Map;

/**
 * @author Mahesh RV
 * @author Ruksana
 * Interface defining Cassandra operations for querying records.
 */

public interface CassandraOperation {

    /**
     * Retrieves records from Cassandra based on specified properties and key.
     *
     * @param keyspaceName The name of the keyspace containing the table.
     * @param tableName    The name of the table from which to retrieve records.
     * @param propertyMap  A map representing properties to filter records.
     * @param fields       A list of fields to include in the retrieved records.
     * @param key          The key used for retrieving records (e.g., partition key).
     * @return A list of maps representing the retrieved records.
     */
    List<Map<String, Object>> getRecordsByPropertiesByKey(String keyspaceName, String tableName,
                                                          Map<String, Object> propertyMap, List<String> fields, String key);

    /**
     * Inserts a record into Cassandra.
     *
     * @param keyspaceName The name of the keyspace containing the table.
     * @param tableName    The name of the table into which to insert the record.
     * @param request      A map representing the record to insert.
     * @return An object representing the result of the insertion operation.
     */
    public Object insertRecord(String keyspaceName, String tableName, Map<String, Object> request);

    public List<Map<String, Object>> getRecordsByPropertiesWithoutFiltering(String keyspaceName, String tableName,
        Map<String, Object> propertyMap, List<String> fields, Integer limit);

    /**
     * Method to update the record on basis of composite primary key.
     *
     * @param keyspaceName     Keyspace name
     * @param tableName        Table name
     * @param updateAttributes Column map to be used in set clause of update query
     * @param compositeKey     Column map for composite primary key
     * @return Response consisting of update query status
     */
    Map<String, Object> updateRecordByCompositeKey(String keyspaceName, String tableName, Map<String, Object> updateAttributes,
        Map<String, Object> compositeKey);
}
