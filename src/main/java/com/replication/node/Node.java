package com.replication.node;

import com.replication.model.LogEntry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Interface representing a node in the replication system.
 * Both master and slave nodes implement this interface.
 */
public interface Node {
    /**
     * Gets the unique ID of this node.
     * @return the node ID
     */
    String getId();
    
    /**
     * Gets the current state of this node (UP or DOWN).
     * @return true if the node is up, false otherwise
     */
    boolean isUp();
    
    /**
     * Brings the node down to simulate failure.
     */
    void goDown();
    
    /**
     * Brings the node back up after a failure.
     */
    void goUp();
    
    /**
     * Reads a value from the node's data store.
     * @param key the key to read
     * @return the value, or null if not found
     */
    String read(String key);
    
    /**
     * Deletes a key-value pair from the node's data store.
     * @param key the key to delete
     * @return true if the key was found and deleted, false otherwise
     */
    boolean delete(String key);
    
    /**
     * Gets a copy of the entire data store.
     * @return a copy of the data store
     */
    Map<String, String> getDataStore();
    
    /**
     * Gets the last log index that this node has processed.
     * @return the last log index
     */
    long getLastLogIndex();
    
    /**
     * Applies a log entry to this node.
     * @param entry the log entry to apply
     * @return true if applied successfully
     */
    boolean applyLogEntry(LogEntry entry, ReadWriteLock lock);
    
    /**
     * Gets all log entries after the specified index.
     * @param afterIndex the index after which to get log entries
     * @return a list of log entries
     */
    List<LogEntry> getLogEntriesAfter(long afterIndex);
}
