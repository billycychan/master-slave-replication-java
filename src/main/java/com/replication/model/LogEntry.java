package com.replication.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a log entry in the replication log.
 * Each entry contains information about a write operation.
 */
public class LogEntry implements Serializable {
    private final long id;
    private final String key;
    private final String value;
    private final long timestamp;

    public LogEntry(long id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
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

    @Override
    public String toString() {
        return "LogEntry{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
