# Master-Slave Replication System with Fault Tolerance

This project implements a Master-Slave replication pattern with fault tolerance features inspired by the Raft consensus algorithm. The system runs a master node and three slave nodes within a single Java application, which can be containerized using Docker.

## System Architecture

The system consists of the following components:

1. **Node Interface**: Defines the common operations for both master and slave nodes.
2. **AbstractNode**: Base implementation of a node with common functionality.
3. **MasterNode**: Handles write operations and manages replication to slaves.
4. **SlaveNode**: Receives updates from the master and handles read operations.
5. **LogEntry**: Represents an entry in the replication log.
6. **ReplicationSystem**: Manages the entire cluster and provides a simple API.

## Features

- **Asynchronous Replication**: Write and delete operations are applied to the master immediately and then asynchronously replicated to slaves.
- **Fault Tolerance**: The system can handle node failures and recoveries.
- **Read-Write Separation**: Reads are distributed across slaves, while writes and deletes go to the master.
- **Log-Based Recovery**: When a slave comes back up, it recovers its state using the master's log.
- **Failure Simulation**: The system includes a failure simulator to demonstrate fault tolerance.
- **CRUD Operations**: Support for Create (write), Read, Update (write), and Delete operations.

## Data Structure

The system uses Java's `Map<String, String>` as the underlying data structure for storing key-value pairs.

## Log Entries

Each log entry includes:
- Log ID (monotonically increasing)
- Key and value of the data being operated on
- Operation type (WRITE or DELETE)
- Timestamp

## Building and Running

### Using Maven

```bash
mvn clean package
java -jar target/master-slave-replication-1.0-SNAPSHOT.jar
```

### Using Docker

```bash
docker build -t master-slave-replication .
docker run master-slave-replication
```

## How It Works

1. Write and delete operations are sent to the master node.
2. The master creates a log entry with the appropriate operation type and applies it to its local data store.
3. The log entry is asynchronously replicated to all slave nodes.
4. Read operations are randomly distributed across available slave nodes.
5. When a node fails, it's marked as down and excluded from operations.
6. When a node recovers, it requests missing log entries from the master and applies them according to their operation type.

## Fault Tolerance

The system implements fault tolerance through:
- **Asynchronous Replication**: Writes continue even if some slaves are down.
- **Log-Based Recovery**: Failed nodes can recover by requesting missing log entries.
- **Node Status Tracking**: The system keeps track of which nodes are up or down.

## Demonstration

The `Main` class includes a demonstration that:
1. Initializes the system with sample data
2. Performs reads, writes, and deletes
3. Simulates node failures and recoveries
4. Shows the final state of the data store

The system also provides an interactive mode where you can manually:
- Write data: `write <key> <value>`
- Read data: `read <key>`
- Delete data: `delete <key>`
- Show all data: `show`
- Exit: `exit`

## Implementation Notes

- Uses only Java internal libraries as required
- Designed to be simple and modular
- Provides a clean API for interacting with the system
- Includes detailed logging for understanding system behavior
