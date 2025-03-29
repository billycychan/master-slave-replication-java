package com.replication.Test;

import com.replication.system.ReplicationSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertFalse;

public class MainTest {

    private ReplicationSystem system;

    @Before
    public void setup() {
        // Create a new system with 3 slaves for each test
        system = new ReplicationSystem(3);
    }

    @After
    public void tearDown() {
        // Clean up after each test
        if (system != null) {
            system.shutdown();
        }
    }

    @Test
    public void testBasicWriteAndRead() throws InterruptedException {
        // Basic write/read functionality
        assertTrue(system.write("key1", "value1"));
        TimeUnit.SECONDS.sleep(1); // Wait for replication
        assertEquals("value1", system.read("key1"));
    }

    @Test
    public void testMultipleWritesAndReads() throws InterruptedException {
        // Write multiple entries
        assertTrue(system.write("key1", "value1"));
        assertTrue(system.write("key2", "value2"));
        assertTrue(system.write("key3", "value3"));

        // Wait for replication
        TimeUnit.SECONDS.sleep(1);

        // Read all entries
        assertEquals("value1", system.read("key1"));
        assertEquals("value2", system.read("key2"));
        assertEquals("value3", system.read("key3"));

        // Verify non-existent key returns null
        assertNull(system.read("nonexistent"));
    }

    @Test
    public void testUpdateExistingKey() throws InterruptedException {
        // Write initial value
        assertTrue(system.write("key1", "initial"));
        TimeUnit.SECONDS.sleep(1);
        assertEquals("initial", system.read("key1"));

        // Update with new value
        assertTrue(system.write("key1", "updated"));
        TimeUnit.SECONDS.sleep(1);
        assertEquals("updated", system.read("key1"));
    }

    @Test
    public void testDataStoreConsistency() throws InterruptedException {
        // Write several entries
        assertTrue(system.write("key1", "value1"));
        assertTrue(system.write("key2", "value2"));
        TimeUnit.SECONDS.sleep(1);

        // Get the data store and check contents
        Map<String, String> dataStore = system.getDataStore();
        assertNotNull(dataStore);
        assertEquals("value1", dataStore.get("key1"));
        assertEquals("value2", dataStore.get("key2"));
        assertEquals(2, dataStore.size());
    }

    @Test
    public void testReplicationWithFailureSimulator() throws InterruptedException {
        // Start the failure simulator
        system.startFailureSimulator(0.5, 0.5, 1); // High failure/recovery rate, fast cycles

        // Perform multiple writes
        for (int i = 0; i < 10; i++) {
            assertTrue(system.write("key" + i, "value" + i));
            TimeUnit.MILLISECONDS.sleep(200);
        }

        // Wait for replication and recovery
        TimeUnit.SECONDS.sleep(5);

        // Verify at least some of the values are available (we can't guarantee all
        // since nodes might be down temporarily, but the system should maintain data)
        Map<String, String> finalState = system.getDataStore();
        assertNotNull(finalState);
        assertTrue(finalState.size() > 0);
    }

    @Test
    public void testEmptyDataStore() {
        // New system should have empty data store
        Map<String, String> dataStore = system.getDataStore();
        assertNotNull(dataStore);
        assertTrue(dataStore.isEmpty());
    }

    @Test
    public void testLargeNumberOfWrites() throws InterruptedException {
        // Perform 100 writes
        int numWrites = 100;
        for (int i = 0; i < numWrites; i++) {
            assertTrue(system.write("bulk-key" + i, "bulk-value" + i));
        }

        // Wait for replication
        TimeUnit.SECONDS.sleep(3);

        // Verify random sample of the writes
        for (int i = 0; i < 10; i++) {
            int randomIndex = (int)(Math.random() * numWrites);
            assertEquals("bulk-value" + randomIndex, system.read("bulk-key" + randomIndex));
        }
    }
}