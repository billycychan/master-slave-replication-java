package com.replication.Test;

import com.replication.system.ReplicationSystem;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;

public class MainTest {

    @Test
    public void test() throws InterruptedException {
        // Create a replication system with 3 slaves
        ReplicationSystem system = new ReplicationSystem(3);

        // Start the failure simulator with moderate probabilities
        // 10% chance of failure, 30% chance of recovery per 5 seconds
        system.startFailureSimulator(0.1, 0.3, 5);
        system.write("key1", "value1");
        TimeUnit.SECONDS.sleep(1);
        assertEquals(system.read("key1"),"value1");
        system.shutdown();
    }
}
