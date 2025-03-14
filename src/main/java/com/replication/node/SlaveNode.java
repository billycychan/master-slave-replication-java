package com.replication.node;

import com.replication.model.LogEntry;

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
        master.recoverSlave(this);
    }

    @Override
    public void goUp() {
        super.goUp();
        // When coming back up, request recovery from the master
        requestRecovery();
    }
}
