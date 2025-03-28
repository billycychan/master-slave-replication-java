package com.replication;

import com.replication.model.LogEntry;
import com.replication.system.ReplicationSystem;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Main class to demonstrate the replication system.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Master-Slave Replication System with Fault Tolerance");
        
        // Create a replication system with 3 slaves
        ReplicationSystem system = new ReplicationSystem(3);
        
        // Start the failure simulator with moderate probabilities
        // 10% chance of failure, 30% chance of recovery per 5 seconds
        system.startFailureSimulator(0.1, 0.3, 5);
        
        // // Demo the system
        // try {
        //     demoSystem(system);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // } finally {
        //     // Shutdown the system properly
        //     system.shutdown();
        // }
        interactiveMode(system);
    }

    // TODO: 2025/3/18 add unit test 
    private static void demoSystem(ReplicationSystem system) throws InterruptedException {
        // Initialize with some data
        System.out.println("\n--- Initializing data ---");
        system.write("key1", "value1");
        system.write("key2", "value2");
        system.write("key3", "value3");
        TimeUnit.SECONDS.sleep(2); // Wait for replication
        
        // Read from slaves
        System.out.println("\n--- Reading data from slaves ---");
        for (int i = 0; i < 5; i++) {
            String key = "key" + ((i % 3) + 1);
            String value = system.read(key);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        
        // Show all data
        System.out.println("\n--- Current data store ---");
        Map<String, String> dataStore = system.getDataStore();
        if (dataStore != null) {
            dataStore.forEach((k, v) -> System.out.println(k + " = " + v));
        }
        
        // Add more data
        System.out.println("\n--- Adding more data ---");
        system.write("key4", "value4");
        system.write("key5", "value5");
        TimeUnit.SECONDS.sleep(2); // Wait for replication
        
        // Read again
        System.out.println("\n--- Reading new data ---");
        for (int i = 0; i < 5; i++) {
            String key = "key" + ((i % 5) + 1);
            String value = system.read(key);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        
        // Update existing data
        System.out.println("\n--- Updating existing data ---");
        system.write("key1", "updated-value1");
        system.write("key3", "updated-value3");
        TimeUnit.SECONDS.sleep(2); // Wait for replication
        
        // Read after update
        System.out.println("\n--- Reading after updates ---");
        for (int i = 0; i < 5; i++) {
            String key = "key" + ((i % 5) + 1);
            String value = system.read(key);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        
        // Demonstrate delete operation
        System.out.println("\n--- Demonstrating delete operation ---");
        system.delete("key2");
        system.delete("key4");
        TimeUnit.SECONDS.sleep(2); // Wait for replication
        
        // Read after delete
        System.out.println("\n--- Reading after deletes ---");
        for (int i = 0; i < 5; i++) {
            String key = "key" + ((i % 5) + 1);
            String value = system.read(key);
            System.out.println("Key: " + key + ", Value: " + (value != null ? value : "<deleted>"));
            TimeUnit.MILLISECONDS.sleep(500);
        }
        
        // Demonstrate failures and recovery
        System.out.println("\n--- Demonstrating failures and recovery (wait 30 seconds) ---");
        System.out.println("    Watch as nodes go down and come back up!");
        TimeUnit.SECONDS.sleep(30);
        
        // Show final state
        System.out.println("\n--- Final data store state ---");
        dataStore = system.getDataStore();
        if (dataStore != null) {
            dataStore.forEach((k, v) -> System.out.println(k + " = " + v));
        }
        
        System.out.println("\nDemo completed!");
    }
    
    private static void interactiveMode(ReplicationSystem system) {
        Scanner scanner = new Scanner(System.in);
        String input;
        
        System.out.println("\n--- Interactive Mode ---");
        System.out.println("Commands: write <key> <value> | read <key> | delete <key> | show | logs | status | exit");
        
        while (true) {
            System.out.print("> ");
            input = scanner.nextLine().trim();
            
            if (input.equals("exit")) {
                break;
            } else if (input.equals("show")) {
                Map<String, String> dataStore = system.getDataStore();
                if (dataStore != null) {
                    System.out.println("\n--- Current Data Store ---");
                    if (dataStore.isEmpty()) {
                        System.out.println("(empty)");
                    } else {
                        dataStore.forEach((k, v) -> System.out.println(k + " = " + v));
                    }
                }
            } else if (input.equals("logs")) {
                List<LogEntry> logs = system.getLogs();
                System.out.println("\n--- Replication Log Entries ---");
                if (logs.isEmpty()) {
                    System.out.println("(no log entries)");
                } else {
                    for (LogEntry entry : logs) {
                        String operationStr = entry.isDelete() ? "DELETE" : "WRITE";
                        System.out.println("Log #" + entry.getId() + ": " + operationStr + 
                                " key='" + entry.getKey() + "'" + 
                                (entry.isDelete() ? "" : " value='" + entry.getValue() + "'") + 
                                " (" + new Date(entry.getTimestamp()) + ")");
                    }
                }
            } else if (input.equals("status")) {
                Map<String, Boolean> nodeStatus = system.getNodesStatus();
                System.out.println("\n--- Node Status ---");
                nodeStatus.forEach((nodeId, isUp) -> 
                        System.out.println(nodeId + ": " + (isUp ? "UP" : "DOWN")));
            } else if (input.startsWith("read ")) {
                String key = input.substring(5).trim();
                String value = system.read(key);
                if (value == null) {
                    System.out.println("Key not found or all slaves are down");
                }
            } else if (input.startsWith("delete ")) {
                String key = input.substring(7).trim();
                boolean success = system.delete(key);
                if (success) {
                    System.out.println("Delete successful");
                } else {
                    System.out.println("Delete failed (key not found or master down)");
                }
            } else if (input.startsWith("write ")) {
                String[] parts = input.substring(6).trim().split("\\s+", 2);
                if (parts.length >= 2) {
                    boolean success = system.write(parts[0], parts[1]);
                    if (success) {
                        System.out.println("Write successful");
                    } else {
                        System.out.println("Write failed (master down?)");
                    }
                } else {
                    System.out.println("Usage: write <key> <value>");
                }
            } else {
                System.out.println("Unknown command. Use write, read, delete, show, logs, status, or exit");
            }
        }

        scanner.close();
        system.shutdown();
    }
}
