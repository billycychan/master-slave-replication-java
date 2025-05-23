package com.replication.node;

import com.replication.model.LogEntry;
import com.replication.model.LogEntry.OperationType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the master node in the replication system.
 * The master node is responsible for handling write operations and replicating
 * them to slave nodes.
 */
public class MasterNode extends AbstractNode {
    private final Set<SlaveNode> slaves;
    private final Map<Long, Set<String>> pendingReplications;
    private long nextLogId = 1;

    public MasterNode(String id) {
        super(id);
        this.slaves = ConcurrentHashMap.newKeySet();
        this.pendingReplications = new ConcurrentHashMap<>();
    }

    /**
     * Registers a slave node with this master.
     * @param slave the slave node to register
     */
    public void registerSlave(SlaveNode slave) {
        slaves.add(slave);
        System.out.println("Master " + id + " registered slave: " + slave.getId());
    }

    /**
     * Writes a key-value pair to the master and replicates it to the slaves.
     * @param key the key to write
     * @param value the value to write
     * @return true if the write was successful
     */
    public boolean write(String key, String value) {
        if (!up) {
            System.out.println("Master " + id + " is DOWN, cannot write");
            return false;
        }

        try {
            lock.writeLock().lock();
            
            // Create a new log entry for write operation
            LogEntry entry = new LogEntry(nextLogId++, key, value, OperationType.WRITE);
            
            // Apply to the master's data store first
            dataStore.put(key, value);
            log.add(entry);
            lastAppliedIndex = entry.getId();
            
            System.out.println("Master " + id + " wrote " + key + "=" + value + " (Log ID: " + entry.getId() + ")");
            
            // Track replication status
            pendingReplications.put(entry.getId(), ConcurrentHashMap.newKeySet());
            
            // Asynchronously replicate to slaves
            replicateToSlaves(entry);
            
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Deletes a key-value pair from the master and replicates the delete operation to the slaves.
     * @param key the key to delete
     * @return true if the delete was successful
     */
    @Override
    public boolean delete(String key) {
        if (!up) {
            System.out.println("Master " + id + " is DOWN, cannot delete");
            return false;
        }

        try {
            lock.writeLock().lock();
            
            // Check if the key exists before attempting to delete
            if (!dataStore.containsKey(key)) {
                System.out.println("Master " + id + " could not delete key '" + key + "' (not found)");
                return false;
            }
            
            // Create a new log entry for delete operation
            LogEntry entry = new LogEntry(nextLogId++, key, null, OperationType.DELETE);
            
            // Remove the key from the data store
            dataStore.remove(key);
            log.add(entry);
            lastAppliedIndex = entry.getId();
            
            System.out.println("Master " + id + " deleted key '" + key + "' (Log ID: " + entry.getId() + ")");
            
            // Track replication status
            pendingReplications.put(entry.getId(), ConcurrentHashMap.newKeySet());
            
            // Asynchronously replicate to slaves
            replicateToSlaves(entry);
            
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Replicates a log entry to all registered slave nodes asynchronously.
     * @param entry the log entry to replicate
     */
    private void replicateToSlaves(LogEntry entry) {
        for (SlaveNode slave : slaves) {
            CompletableFuture.runAsync(() -> {
                if (slave.isUp()) {
                    boolean success = slave.applyLogEntry(entry, lock);
                    if (success) {
                        // Track successful replication
                        Set<String> slaveSet = pendingReplications.get(entry.getId());
                        if (slaveSet != null) {
                            slaveSet.add(slave.getId());
                            System.out.println("Master " + id + " replicated log entry " + entry.getId() + 
                                    " to slave " + slave.getId());
                        }
                    } else {
                        slave.recoverSlave();
                    }
                } else {
                    System.out.println("Master " + id + " couldn't replicate to slave " + 
                            slave.getId() + " (DOWN)");
                }
            }, replicationExecutor);
        }
    }

    /**
     * Shuts down the replication executor service.
     */
    public void shutdown() {
        replicationExecutor.shutdown();
    }
}
