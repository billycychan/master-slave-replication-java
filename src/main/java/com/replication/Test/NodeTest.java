package com.replication.Test;

import com.replication.model.LogEntry;
import com.replication.node.MasterNode;
import com.replication.node.SlaveNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

public class NodeTest {

    private MasterNode master;
    private SlaveNode slave1;
    private SlaveNode slave2;

    @Before
    public void setup() {
        master = new MasterNode("test-master");
        slave1 = new SlaveNode("test-slave-1", master);
        slave2 = new SlaveNode("test-slave-2", master);
    }

    @After
    public void tearDown() {
        if (master != null) {
            master.shutdown();
        }
    }

    @Test
    public void testMasterNodeBasicOperations() {
        // Test master node identification
        assertEquals("test-master", master.getId());
        assertTrue(master.isUp());

        // Test basic write operation
        assertTrue(master.write("master-key", "master-value"));

        // Test master node can read its own data
        assertEquals("master-value", master.read("master-key"));
    }

    @Test
    public void testSlaveNodeBasicOperations() throws InterruptedException {
        // Test slave node identification
        assertEquals("test-slave-1", slave1.getId());
        assertTrue(slave1.isUp());

        // Write to master and check replication to slave
        assertTrue(master.write("key-for-slave", "value-for-slave"));
        TimeUnit.SECONDS.sleep(1); // Wait for replication

        // Slave should have received the data
        assertEquals("value-for-slave", slave1.read("key-for-slave"));
    }

    @Test
    public void testSlaveNodeFailureAndRecovery() throws InterruptedException {
        // Initial write to master
        assertTrue(master.write("key1", "value1"));
        TimeUnit.SECONDS.sleep(1);

        // Verify slave has the data
        assertEquals("value1", slave1.read("key1"));

        // Simulate slave failure
        slave1.goDown();
        assertFalse(slave1.isUp());
        assertNull(slave1.read("key1")); // Should return null when down

        // Write more data while slave is down
        assertTrue(master.write("key2", "value2"));
        assertTrue(master.write("key3", "value3"));

        // Bring slave back up
        slave1.goUp(); // This should trigger recovery
        TimeUnit.SECONDS.sleep(2); // Wait for recovery

        // Verify slave has caught up with all data
        assertTrue(slave1.isUp());
        assertEquals("value1", slave1.read("key1"));
        assertEquals("value2", slave1.read("key2"));
        assertEquals("value3", slave1.read("key3"));
    }

    @Test
    public void testMasterNodeFailure() {
        // Write data while master is up
        assertTrue(master.write("pre-failure", "value"));

        // Simulate master failure
        master.goDown();
        assertFalse(master.isUp());

        // Write should fail when master is down
        assertFalse(master.write("post-failure", "value"));

        // Bring master back up
        master.goUp();
        assertTrue(master.isUp());

        // Write should succeed again
        assertTrue(master.write("post-recovery", "value"));
    }

    @Test
    public void testLogEntryReplication() throws InterruptedException {
        // Write several entries to create log entries
        for (int i = 0; i < 5; i++) {
            master.write("log-key-" + i, "log-value-" + i);
        }

        TimeUnit.SECONDS.sleep(1);

        // Get log entries from master after index 0
        List<LogEntry> masterLogEntries = master.getLogEntriesAfter(0);
        assertNotNull(masterLogEntries);
        assertEquals(5, masterLogEntries.size());

        // Get log entries from slave after index 0
        List<LogEntry> slaveLogEntries = slave1.getLogEntriesAfter(0);
        assertNotNull(slaveLogEntries);
        assertEquals(5, slaveLogEntries.size());

        // Compare log entries between master and slave
        for (int i = 0; i < 5; i++) {
            LogEntry masterEntry = masterLogEntries.get(i);
            LogEntry slaveEntry = slaveLogEntries.get(i);

            assertEquals(masterEntry.getId(), slaveEntry.getId());
            assertEquals(masterEntry.getKey(), slaveEntry.getKey());
            assertEquals(masterEntry.getValue(), slaveEntry.getValue());
        }
    }

    @Test
    public void testMultipleSlaveNodesConsistency() throws InterruptedException {
        // Write data to master
        master.write("shared-key", "shared-value");
        TimeUnit.SECONDS.sleep(1);

        // Both slaves should have the same data
        assertEquals("shared-value", slave1.read("shared-key"));
        assertEquals("shared-value", slave2.read("shared-key"));

        // Get data stores from both slaves
        Map<String, String> store1 = slave1.getDataStore();
        Map<String, String> store2 = slave2.getDataStore();

        // Data stores should be identical
        assertEquals(store1.size(), store2.size());
        for (String key : store1.keySet()) {
            assertEquals(store1.get(key), store2.get(key));
        }
    }

    @Test
    public void testLastLogIndexTracking() throws InterruptedException {
        // Initial state
        assertEquals(0, master.getLastLogIndex());
        assertEquals(0, slave1.getLastLogIndex());

        // Write entries
        master.write("index-key-1", "value1");
        TimeUnit.MILLISECONDS.sleep(500);
        assertEquals(1, master.getLastLogIndex());

        master.write("index-key-2", "value2");
        TimeUnit.MILLISECONDS.sleep(500);
        assertEquals(2, master.getLastLogIndex());

        // Wait for replication
        TimeUnit.SECONDS.sleep(1);

        // Slave should have the same log index
        assertEquals(2, slave1.getLastLogIndex());
    }
}