package com.replication.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a log entry in the replication log.
 * Each entry contains information about a write or delete operation.
 */
public class LogEntry implements Serializable {
    /**
     * Enum representing the type of operation in the log entry.
     */
    public enum OperationType {
        WRITE,
        DELETE
    }
    
    private final long id;
    private final String key;
    private final String value;
    private final long timestamp;
    private final OperationType operationType;

    /**
     * Creates a new log entry for a write operation.
     * @param id the log entry ID
     * @param key the key being written
     * @param value the value being written
     */
    public LogEntry(long id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
        this.operationType = OperationType.WRITE;
    }
    
    /**
     * Creates a new log entry with a specified operation type.
     * @param id the log entry ID
     * @param key the key being operated on
     * @param value the value (for write operations, null for delete operations)
     * @param operationType the type of operation
     */
    public LogEntry(long id, String key, String value, OperationType operationType) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
        this.operationType = operationType;
    }

    public long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the operation type of this log entry.
     * @return the operation type
     */
    public OperationType getOperationType() {
        return operationType;
    }
    
    /**
     * Checks if this log entry is a delete operation.
     * @return true if this is a delete operation, false otherwise
     */
    public boolean isDelete() {
        return operationType == OperationType.DELETE;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", operationType=" + operationType +
                ", timestamp=" + timestamp +
                '}';
    }
}
