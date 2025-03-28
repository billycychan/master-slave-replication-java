package com.replication.node;

import com.replication.model.LogEntry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of a slave node in the replication system.
 * Slave nodes receive and apply log entries from the master,
 * and handle read operations.
 */
public class SlaveNode extends AbstractNode {
    private final MasterNode master;

    public SlaveNode(String id, MasterNode master) {
        super(id);
        this.master = master;
        // Register with the master
        master.registerSlave(this);
    }

    /**
     * Requests recovery from the master node.
     * This is called when a slave node comes back up after being down.
     */
    public void requestRecovery() {
        if (!up) {
            System.out.println("Slave " + id + " is DOWN, cannot request recovery");
            return;
        }

        System.out.println("Slave " + id + " requesting recovery from master");
        recoverSlave();
    }

    @Override
    public void goUp() {
        super.goUp();
        // When coming back up, request recovery from the master
        requestRecovery();
    }

    /**
     * Recovers a slave node by sending it all missing log entries.
     */
    public void recoverSlave() {
        if (!up || !this.isUp()) {
            System.out.println("Master or Slave " + this.getId() + " is DOWN, cannot recover");
            return;
        }

        System.out.println("Master starting recovery for slave " + this.getId());

        CompletableFuture.runAsync(() -> {
            long slaveLastIndex = this.getLastLogIndex();
            List<LogEntry> missingEntries = master.getLogEntriesAfter(slaveLastIndex);

            System.out.println("Master sending " + missingEntries.size() +
                    " log entries to slave " + this.getId());

            for (LogEntry entry : missingEntries) {
                this.applyLogEntry(entry, master.lock);
            }

            System.out.println("Master completed recovery for slave " +
                    this.getId() + " up to log index " + lastAppliedIndex);
        }, replicationExecutor);
    }
}
