package com.replication.Test;

import com.replication.node.SlaveNode;
import com.replication.system.ReplicationSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class FaultToleranceTest {

    private ReplicationSystem system;

    @Before
    public void setup() {
        // Create a system with 5 slaves for more realistic fault tolerance testing
        system = new ReplicationSystem(5);
    }

    @After
    public void tearDown() {
        if (system != null) {
            system.shutdown();
        }
    }

    @Test
    public void testSystemRecoveryAfterHighFailureRate() throws InterruptedException {
        // Initial data
        Map<String, String> initialData = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String key = "init-key-" + i;
            String value = "init-value-" + i;
            initialData.put(key, value);
            system.write(key, value);
        }

        // Wait for initial replication
        TimeUnit.SECONDS.sleep(2);

        // Start failure simulator with high failure probability
        system.startFailureSimulator(0.7, 0.3, 1);

        // Write more data during high failure periods
        for (int i = 0; i < 15; i++) {
            String key = "failure-key-" + i;
            String value = "failure-value-" + i;
            system.write(key, value);
            TimeUnit.MILLISECONDS.sleep(200);
        }

        // Stop failure simulation and wait for system to stabilize
        // (this is a bit of a hack since there's no direct way to stop the simulator)
        system.startFailureSimulator(0.0, 1.0, 1); // Set failure rate to 0, recovery to 100%
        TimeUnit.SECONDS.sleep(5);

        // Check data consistency for initial data
        for (String key : initialData.keySet()) {
            assertEquals(initialData.get(key), system.read(key));
        }

        // Some of the data written during failure should be present
        int recoveredCount = 0;
        for (int i = 0; i < 15; i++) {
            String key = "failure-key-" + i;
            if (system.read(key) != null) {
                recoveredCount++;
            }
        }

        // We should have recovered a significant amount of the data
        assertTrue("Too few entries recovered: " + recoveredCount, recoveredCount > 5);

        //get all data from every slave
        List<SlaveNode> allUpSlaveNodes = system.getAllUpSlaveNodes();
        for (SlaveNode slaveNode : allUpSlaveNodes) {
            for (int i = 0; i < 15; i++) {
                assertEquals(slaveNode.getDataStore().get("failure-key-" + i), "failure-value-" + i);
            }
        }
    }

    @Test
    public void testConsecutiveReadsWithFailures() throws InterruptedException {
        // Setup data
        for (int i = 0; i < 5; i++) {
            system.write("read-key-" + i, "read-value-" + i);
        }
        TimeUnit.SECONDS.sleep(1);

        // Start failure simulator with moderate failure/recovery
        system.startFailureSimulator(0.4, 0.4, 1);

        // Perform multiple consecutive reads
        int successfulReads = 0;
        for (int i = 0; i < 20; i++) {
            String key = "read-key-" + (i % 5);
            String value = system.read(key);
            if (value != null && value.equals("read-value-" + (i % 5))) {
                successfulReads++;
            }
            TimeUnit.MILLISECONDS.sleep(300);
        }

        // Even with failures, we should get some successful reads
        assertTrue("Too few successful reads: " + successfulReads, successfulReads > 5);
    }

    @Test
    public void testContinuousWriteDuringFailures() throws InterruptedException {
        // Start failure simulator
        system.startFailureSimulator(0.3, 0.3, 1);

        // Perform continuous writes
        int successfulWrites = 0;
        for (int i = 0; i < 30; i++) {
            boolean success = system.write("continuous-key-" + i, "continuous-value-" + i);
            if (success) {
                successfulWrites++;
            }
            TimeUnit.MILLISECONDS.sleep(200);
        }

        // Master should never fail, so all writes should succeed
        assertEquals(30, successfulWrites);

        // Wait for recovery
        system.startFailureSimulator(0.0, 1.0, 1);
        TimeUnit.SECONDS.sleep(5);

        // Check data store
        Map<String, String> dataStore = system.getDataStore();
        assertNotNull(dataStore);

        // Verify random sample of writes persisted
        for (int i = 0; i < 5; i++) {
            int randomIndex = (int)(Math.random() * 30);
            String expectedValue = "continuous-value-" + randomIndex;
            String actualValue = system.read("continuous-key-" + randomIndex);
            assertEquals("Failed for index " + randomIndex, expectedValue, actualValue);
        }
    }

    @Test
    public void testSlaveRecovery() throws InterruptedException {
        // Write initial data
        for (int i = 0; i < 5; i++) {
            system.write("recovery-key-" + i, "recovery-value-" + i);
        }

        // Wait for replication
        TimeUnit.SECONDS.sleep(2);

        // Simulate all slaves failing then recovering
        // First force high failure rate
        system.startFailureSimulator(1.0, 0.0, 1);
        TimeUnit.SECONDS.sleep(3); // Time for all slaves to fail

        // Now force recovery
        system.startFailureSimulator(0.0, 1.0, 1);
        TimeUnit.SECONDS.sleep(5); // Time for recovery

        // After recovery, read should succeed for all initial data
        for (int i = 0; i < 5; i++) {
            String key = "recovery-key-" + i;
            String expected = "recovery-value-" + i;
            assertEquals(expected, system.read(key));
        }
    }

    @Test
    public void testRapidWriteReadCycles() throws InterruptedException {
        // Start failure simulator with moderate failure rate
        system.startFailureSimulator(0.2, 0.4, 1);

        // Perform rapid write-read cycles
        for (int i = 0; i < 20; i++) {
            String key = "cycle-key-" + i;
            String value = "cycle-value-" + i;

            // Write
            assertTrue(system.write(key, value));

            // Small delay to allow potential replication
            TimeUnit.MILLISECONDS.sleep(100);

            // Try to read (may or may not succeed depending on if any slaves are up)
            system.read(key);
        }

        // Wait for system to stabilize
        system.startFailureSimulator(0.0, 1.0, 1);
        TimeUnit.SECONDS.sleep(5);

        // Verify data integrity after stabilization
        for (int i = 0; i < 20; i++) {
            String key = "cycle-key-" + i;
            String expected = "cycle-value-" + i;
            assertEquals(expected, system.read(key));
        }
    }
}