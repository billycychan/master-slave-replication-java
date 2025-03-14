package com.replication.node;

import com.replication.model.LogEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract base class for nodes in the replication system.
 * Provides common functionality for both master and slave nodes.
 */
public abstract class AbstractNode implements Node {
    protected final String id;
    protected boolean up = true;
    protected final Map<String, String> dataStore;
    protected final List<LogEntry> log;
    protected final ReadWriteLock lock;
    protected long lastAppliedIndex = 0;

    public AbstractNode(String id) {
        this.id = id;
        this.dataStore = new ConcurrentHashMap<>();
        this.log = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isUp() {
        return up;
    }

    @Override
    public void goDown() {
        System.out.println("Node " + id + " going DOWN");
        up = false;
    }

    @Override
    public void goUp() {
        System.out.println("Node " + id + " coming UP");
        up = true;
    }

    @Override
    public String read(String key) {
        if (!up) {
            System.out.println("Node " + id + " is DOWN, cannot read");
            return null;
        }
        
        try {
            lock.readLock().lock();
            return dataStore.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<String, String> getDataStore() {
        if (!up) {
            System.out.println("Node " + id + " is DOWN, cannot get data store");
            return null;
        }
        
        try {
            lock.readLock().lock();
            return new HashMap<>(dataStore);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long getLastLogIndex() {
        if (!up) {
            return -1;
        }
        return lastAppliedIndex;
    }

    @Override
    public boolean applyLogEntry(LogEntry entry) {
        if (!up) {
            System.out.println("Node " + id + " is DOWN, cannot apply log entry");
            return false;
        }
        
        try {
            lock.writeLock().lock();
            
            // Check if this log entry is the next in sequence
            if (entry.getId() != lastAppliedIndex + 1) {
                System.out.println("Node " + id + " received out-of-order log entry: " + entry.getId() + 
                        ", expected: " + (lastAppliedIndex + 1));
                return false;
            }
            
            // Apply the log entry to the data store
            dataStore.put(entry.getKey(), entry.getValue());
            
            // Add to log and update index
            log.add(entry);
            lastAppliedIndex = entry.getId();
            
            System.out.println("Node " + id + " applied log entry: " + entry);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<LogEntry> getLogEntriesAfter(long afterIndex) {
        if (!up) {
            System.out.println("Node " + id + " is DOWN, cannot get log entries");
            return Collections.emptyList();
        }
        
        List<LogEntry> entries = new ArrayList<>();
        try {
            lock.readLock().lock();
            for (LogEntry entry : log) {
                if (entry.getId() > afterIndex) {
                    entries.add(entry);
                }
            }
            return entries;
        } finally {
            lock.readLock().unlock();
        }
    }
}
