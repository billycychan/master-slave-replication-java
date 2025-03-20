# Master-Slave Replication System Diagrams

## System Architecture

```mermaid
classDiagram
    class Node {
        <<interface>>
        +String getId()
        +boolean isUp()
        +void goDown()
        +void goUp()
        +String read(String key)
        +Map~String, String~ getDataStore()
        +long getLastLogIndex()
        +boolean applyLogEntry(LogEntry entry, ReadWriteLock lock)
        +List~LogEntry~ getLogEntriesAfter(long afterIndex)
    }

    class AbstractNode {
        #String id
        #boolean up
        #Map~String, String~ dataStore
        #List~LogEntry~ log
        #ReadWriteLock lock
        #long lastAppliedIndex
        #ExecutorService replicationExecutor
        +AbstractNode(String id)
    }

    class MasterNode {
        -Set~SlaveNode~ slaves
        -Map~Long, Set~String~~ pendingReplications
        -long nextLogId
        +void registerSlave(SlaveNode slave)
        +boolean write(String key, String value)
        -void replicateToSlaves(LogEntry entry)
        +void shutdown()
    }

    class SlaveNode {
        -MasterNode master
        +void requestRecovery()
        +void recoverSlave()
    }

    class LogEntry {
        -long id
        -String key
        -String value
        -long timestamp
        +LogEntry(long id, String key, String value)
        +getters()
    }

    class ReplicationSystem {
        -MasterNode master
        -List~SlaveNode~ slaves
        -Random random
        -ScheduledExecutorService scheduler
        +boolean write(String key, String value)
        +String read(String key)
        -SlaveNode getRandomUpSlave()
        +void startFailureSimulator(double failureProbability, double recoveryProbability, int checkIntervalSeconds)
        -void simulateFailureAndRecovery(double failureProbability, double recoveryProbability)
        +void shutdown()
    }

    Node <|.. AbstractNode
    AbstractNode <|-- MasterNode
    AbstractNode <|-- SlaveNode
    MasterNode "1" -- "many" SlaveNode : manages
    MasterNode -- LogEntry : creates
    SlaveNode -- MasterNode : references
    ReplicationSystem -- MasterNode : contains
    ReplicationSystem -- SlaveNode : contains
```

## Write Flow

```mermaid
sequenceDiagram
    participant Client
    participant ReplicationSystem
    participant MasterNode
    participant SlaveNode1
    participant SlaveNode2
    participant SlaveNode3

    Client->>ReplicationSystem: write(key, value)
    ReplicationSystem->>MasterNode: write(key, value)
    
    Note over MasterNode: Create LogEntry with ID
    MasterNode->>MasterNode: Apply to local dataStore
    MasterNode->>MasterNode: Add to log
    
    par Asynchronous Replication
        MasterNode-->>SlaveNode1: applyLogEntry(entry, lock)
        Note over SlaveNode1: If up, apply entry with lock
        SlaveNode1-->>MasterNode: Replication result
        alt Replication Failed
            MasterNode-->>SlaveNode1: recoverSlave()
        end

        MasterNode-->>SlaveNode2: applyLogEntry(entry, lock)
        Note over SlaveNode2: If up, apply entry with lock
        SlaveNode2-->>MasterNode: Replication result
        alt Replication Failed
            MasterNode-->>SlaveNode2: recoverSlave()
        end

        MasterNode-->>SlaveNode3: applyLogEntry(entry, lock)
        Note over SlaveNode3: If up, apply entry with lock
        SlaveNode3-->>MasterNode: Replication result
        alt Replication Failed
            MasterNode-->>SlaveNode3: recoverSlave()
        end
    end
    
    MasterNode->>ReplicationSystem: Return success
    ReplicationSystem->>Client: Return result
```

## Read Flow

```mermaid
sequenceDiagram
    participant Client
    participant ReplicationSystem
    participant SlaveNode as Selected SlaveNode

    Client->>ReplicationSystem: read(key)
    
    Note over ReplicationSystem: Select random up slave
    ReplicationSystem->>SlaveNode: read(key)
    
    alt Slave is UP
        SlaveNode->>SlaveNode: Read from dataStore
        SlaveNode->>ReplicationSystem: Return value
        ReplicationSystem->>Client: Return value
    else All slaves are DOWN
        ReplicationSystem->>Client: Return null/error
    end
```

## Node Recovery Process

```mermaid
sequenceDiagram
    participant FailureSimulator
    participant SlaveNode
    participant MasterNode

    Note over SlaveNode: Node was DOWN
    FailureSimulator->>SlaveNode: goUp()
    SlaveNode->>SlaveNode: Set state to UP
    SlaveNode->>SlaveNode: recoverSlave()
    
    Note over SlaveNode: Using replicationExecutor
    SlaveNode->>SlaveNode: Get lastLogIndex
    SlaveNode->>MasterNode: Get log entries after lastLogIndex
    
    loop For each missing LogEntry
        SlaveNode->>SlaveNode: applyLogEntry(entry, lock)
        Note over SlaveNode: Thread-safe application with lock
        SlaveNode->>SlaveNode: Apply to dataStore
        SlaveNode->>SlaveNode: Update lastAppliedIndex
    end
    
    Note over SlaveNode: Node fully recovered
```

## System State Diagram

```mermaid
stateDiagram-v2
    [*] --> SystemRunning
    
    state SystemRunning {
        [*] --> AllNodesUp
        
        AllNodesUp --> SomeSlavesDown: Slave failure
        AllNodesUp --> MasterDown: Master failure
        
        SomeSlavesDown --> AllNodesUp: Slave recovery
        SomeSlavesDown --> MasterDown: Master failure
        
        MasterDown --> AllNodesUp: Master recovery
        MasterDown --> MasterAndSlavesDown: Slave failure
        
        MasterAndSlavesDown --> MasterDown: Slave recovery
        MasterAndSlavesDown --> SomeSlavesDown: Master recovery
    }
    
    SystemRunning --> [*]: shutdown()
```

## Data Flow Overview

```mermaid
flowchart TD
    Client[Client] --> RS[ReplicationSystem]
    
    subgraph Replication Cluster
        RS --> M[MasterNode]
        RS --> S1[SlaveNode 1]
        RS --> S2[SlaveNode 2] 
        RS --> S3[SlaveNode 3]
        
        M -- "write()" --> M_DS[(Master DataStore)]
        M -- "applyLogEntry(entry, lock)" --> S1
        M -- "applyLogEntry(entry, lock)" --> S2
        M -- "applyLogEntry(entry, lock)" --> S3
        
        S1 -- "read()" --> S1_DS[(Slave1 DataStore)]
        S2 -- "read()" --> S2_DS[(Slave2 DataStore)]
        S3 -- "read()" --> S3_DS[(Slave3 DataStore)]
        
        S1 -- "requestRecovery()" --> M
        S2 -- "requestRecovery()" --> M
        S3 -- "requestRecovery()" --> M
    end
    
    FS[Failure Simulator] -.-> M
    FS -.-> S1
    FS -.-> S2
    FS -.-> S3
    
    classDef master fill:#f96,stroke:#333,stroke-width:2px;
    classDef slave fill:#9cf,stroke:#333,stroke-width:2px;
    classDef system fill:#fc9,stroke:#333,stroke-width:2px;
    classDef datastore fill:#fcf,stroke:#333,stroke-width:1px;
    classDef simulator fill:#cfc,stroke:#333,stroke-width:1px;
    
    class M master;
    class S1,S2,S3 slave;
    class RS system;
    class M_DS,S1_DS,S2_DS,S3_DS datastore;
    class FS simulator;
```
