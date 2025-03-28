package com.replication.system;

import com.replication.node.MasterNode;
import com.replication.node.SlaveNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manager class for the entire replication system.
 * It manages master and slave nodes, and provides a simple API
 * for interacting with the replication system.
 */
public class ReplicationSystem {
    private final MasterNode master;
    private final List<SlaveNode> slaves;
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler;

    /**
     * Creates a new replication system with a master and the specified number of slaves.
     * @param numSlaves the number of slave nodes to create
     */
    public ReplicationSystem(int numSlaves) {
        // Create master node
        this.master = new MasterNode("master");
        this.slaves = new ArrayList<>();
        this.scheduler = new ScheduledThreadPoolExecutor(2);

        // Create slave nodes
        for (int i = 0; i < numSlaves; i++) {
            SlaveNode slave = new SlaveNode("slave-" + i, master);
            slaves.add(slave);
        }

        System.out.println("Replication system initialized with 1 master and " + numSlaves + " slaves");
    }

    /**
     * Writes a key-value pair to the master.
     * @param key the key to write
     * @param value the value to write
     * @return true if the write was successful
     */
    public boolean write(String key, String value) {
        return master.write(key, value);
    }
    
    /**
     * Deletes a key-value pair from the master.
     * @param key the key to delete
     * @return true if the delete was successful
     */
    public boolean delete(String key) {
        return master.delete(key);
    }

    /**
     * Reads a value from a random slave node.
     * If the chosen slave is down, tries another slave.
     * @param key the key to read
     * @return the value, or null if not found or all slaves are down
     */
    public String read(String key) {
        // Try to get a working slave
        SlaveNode slave = getRandomUpSlave();
        if (slave == null) {
            System.out.println("All slaves are DOWN, cannot read");
            return null;
        }
        
        String value = slave.read(key);
        System.out.println("Read " + key + "=" + value + " from " + slave.getId());
        return value;
    }

    /**
     * Gets a random slave that is up (not failed).
     * @return a random up slave, or null if all slaves are down
     */
    private SlaveNode getRandomUpSlave() {
        List<SlaveNode> upSlaves = new ArrayList<>();
        for (SlaveNode slave : slaves) {
            if (slave.isUp()) {
                upSlaves.add(slave);
            }
        }
        
        if (upSlaves.isEmpty()) {
            return null;
        }
        
        return upSlaves.get(random.nextInt(upSlaves.size()));
    }

    /**
     * Gets the data store of a random slave that is up.
     * @return the data store, or null if all slaves are down
     */
    public Map<String, String> getDataStore() {
        SlaveNode slave = getRandomUpSlave();
        if (slave == null) {
            System.out.println("All slaves are DOWN, cannot get data store");
            return null;
        }
        
        return slave.getDataStore();
    }

    /**
     * Starts the failure simulator, which will randomly bring nodes down and up.
     * @param failureProbability the probability of a node failing in each check
     * @param recoveryProbability the probability of a failed node recovering in each check
     * @param checkIntervalSeconds the interval between checks in seconds
     */
    public void startFailureSimulator(double failureProbability, double recoveryProbability, int checkIntervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            simulateFailureAndRecovery(failureProbability, recoveryProbability);
        }, checkIntervalSeconds, checkIntervalSeconds, TimeUnit.SECONDS);
        
        System.out.println("Started failure simulator with check interval " + 
                checkIntervalSeconds + " seconds");
    }

    /**
     * Simulates node failures and recoveries.
     * @param failureProbability the probability of a node failing
     * @param recoveryProbability the probability of a failed node recovering
     */
    private void simulateFailureAndRecovery(double failureProbability, double recoveryProbability) {
        for (SlaveNode slave : slaves) {
            if (slave.isUp() && random.nextDouble() < failureProbability) {
                slave.goDown();
            } else if (!slave.isUp() && random.nextDouble() < recoveryProbability) {
                slave.goUp();
            }
        }
    }

    /**
     * Gets all log entries from the master node.
     * @return a list of all log entries from the master
     */
    public List<com.replication.model.LogEntry> getLogs() {
        if (!master.isUp()) {
            System.out.println("Master is DOWN, cannot get logs");
            return Collections.emptyList();
        }
        
        return master.getLogEntriesAfter(0); // Get all logs from the beginning
    }
    
    /**
     * Gets the status of all nodes in the system.
     * @return a map of node IDs to their status (UP/DOWN)
     */
    public Map<String, Boolean> getNodesStatus() {
        Map<String, Boolean> status = new HashMap<>();
        
        // Add master status
        status.put(master.getId(), master.isUp());
        
        // Add slave statuses
        for (SlaveNode slave : slaves) {
            status.put(slave.getId(), slave.isUp());
        }
        
        return status;
    }
    
    /**
     * Shuts down the replication system.
     */
    public void shutdown() {
        scheduler.shutdown();
        master.shutdown();
        System.out.println("Replication system shut down");
    }
}
