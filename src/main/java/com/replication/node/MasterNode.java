package com.replication.node;

import com.replication.model.LogEntry;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of the master node in the replication system.
 * The master node is responsible for handling write operations and replicating
 * them to slave nodes.
 */
public class MasterNode extends AbstractNode {
    private final Set<SlaveNode> slaves;
    private final ExecutorService replicationExecutor;
    private final Map<Long, Set<String>> pendingReplications;
    private long nextLogId = 1;

    public MasterNode(String id) {
        super(id);
        this.slaves = ConcurrentHashMap.newKeySet();
        this.replicationExecutor = Executors.newFixedThreadPool(5);
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
            
            // Create a new log entry
            LogEntry entry = new LogEntry(nextLogId++, key, value);
            
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
     * Replicates a log entry to all registered slave nodes asynchronously.
     * @param entry the log entry to replicate
     */
    private void replicateToSlaves(LogEntry entry) {
        for (SlaveNode slave : slaves) {
            CompletableFuture.runAsync(() -> {
                if (slave.isUp()) {
                    boolean success = slave.applyLogEntry(entry);
                    if (success) {
                        // Track successful replication
                        Set<String> slaveSet = pendingReplications.get(entry.getId());
                        if (slaveSet != null) {
                            slaveSet.add(slave.getId());
                            System.out.println("Master " + id + " replicated log entry " + entry.getId() + 
                                    " to slave " + slave.getId());
                        }
                    }
                } else {
                    System.out.println("Master " + id + " couldn't replicate to slave " + 
                            slave.getId() + " (DOWN)");
                }
            }, replicationExecutor);
        }
    }

    /**
     * Recovers a slave node by sending it all missing log entries.
     * @param slave the slave node to recover
     */
    public void recoverSlave(SlaveNode slave) {
        if (!up || !slave.isUp()) {
            System.out.println("Master " + id + " or Slave " + slave.getId() + " is DOWN, cannot recover");
            return;
        }

        System.out.println("Master " + id + " starting recovery for slave " + slave.getId());
        
        CompletableFuture.runAsync(() -> {
            long slaveLastIndex = slave.getLastLogIndex();
            List<LogEntry> missingEntries = getLogEntriesAfter(slaveLastIndex);
            
            System.out.println("Master " + id + " sending " + missingEntries.size() + 
                    " log entries to slave " + slave.getId());
            
            for (LogEntry entry : missingEntries) {
                slave.applyLogEntry(entry);
            }
            
            System.out.println("Master " + id + " completed recovery for slave " + 
                    slave.getId() + " up to log index " + lastAppliedIndex);
        }, replicationExecutor);
    }

    /**
     * Shuts down the replication executor service.
     */
    public void shutdown() {
        replicationExecutor.shutdown();
    }
}
