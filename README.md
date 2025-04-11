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

```
Log #1: WRITE key='user1' value='John' (2025-03-29 19:45:12)
```

## Building and Running

### Requirements

- Java 17 or higher (tested with Java 23)
- Maven (for build automation)
- Docker (optional, for container-based execution)

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

### Interactive Mode

The system provides an interactive mode where you can manually:
- `write <key> <value>`: Create or update a key-value pair
- `read <key>`: Read the value for a specific key
- `delete <key>`: Delete a key-value pair
- `show`: Display all data in the store
- `logs`: View all replication log entries
- `status`: Check the status of all nodes (UP/DOWN)
- `exit`: Exit the application

### Sample Demonstration Session

```
Starting Master-Slave Replication System with Fault Tolerance

--- Interactive Mode ---
Commands: write <key> <value> | read <key> | delete <key> | show | logs | status | exit
> write user1 John
Write successful
> write user2 Jane
Write successful
> write user3 Bob
Write successful
> show

--- Current Data Store ---
user1 = John
user2 = Jane
user3 = Bob
> read user2
Jane
> delete user3
Delete successful
> show

--- Current Data Store ---
user1 = John
user2 = Jane
> status

--- Node Status ---
Master: UP
Slave-1: UP
Slave-2: UP
Slave-3: DOWN
> logs

--- Replication Log Entries ---
Log #1: WRITE key='user1' value='John' (Sat Mar 29 19:45:12 EDT 2025)
Log #2: WRITE key='user2' value='Jane' (Sat Mar 29 19:45:18 EDT 2025)
Log #3: WRITE key='user3' value='Bob' (Sat Mar 29 19:45:23 EDT 2025)
Log #4: DELETE key='user3' (Sat Mar 29 19:45:35 EDT 2025)
> exit
```

## Project Structure

```
master-slave-replication/
├── Dockerfile                      # Docker configuration for containerization
├── README.md                       # Project documentation
├── diagrams.md                     # System architecture diagrams
├── pom.xml                         # Maven project configuration
├── Test_Plan.md                    # Test plan documentation
├── Test_Report.md                  # Test report documentation
└── src/
    └── main/
        └── java/
            └── com/
                └── replication/
                    ├── Main.java                 # Main application entry point
                    ├── Test/                     # Unit test cases
                    │   ├── FaultToleranceTest.java
                    │   ├── MainTest.java
                    │   └── NodeTest.java
                    ├── model/                    # Data models
                    │   └── LogEntry.java         # Replication log entry model
                    ├── node/                     # Node implementations
                    │   ├── AbstractNode.java     # Common node functionality
                    │   ├── MasterNode.java       # Master node implementation
                    │   ├── Node.java             # Node interface
                    │   └── SlaveNode.java        # Slave node implementation
                    └── system/                   # System management
                        └── ReplicationSystem.java # Main replication system
```




## System Diagrams

Detailed design diagrams, including class diagrams, state charts, flowcharts, and sequence diagrams, are provided in a separate file:

[Click here to view full system diagrams](./diagrams.md)

These diagrams illustrate:

- Class relationships and system structure
- Master-slave replication sequence
- Fault-tolerant recovery flows
- Data flow across nodes and components
- State transitions during normal and failure operation



## Implementation Notes

- Uses only Java internal libraries as required
- Designed to be simple and modular
- Provides a clean API for interacting with the system
- Includes detailed logging for understanding system behavior


## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).



