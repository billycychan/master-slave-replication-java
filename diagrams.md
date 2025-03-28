# Master-Slave Replication System Diagrams


## Analysis Class Diagram

```mermaid
classDiagram
    %% Base class at the top
    class Node {
        id
        status
        dataStore
        log
        changeStatus()
        readData(key)
        deleteData(key)
        getDataStore()
        getLogInformation()
        processLogEntry(entry)
    }
    
    %% Specialized nodes side by side
    class Master {
        slaves
        pendingReplications
        registerSlave(slave)
        write(key, value)
        delete(key)
        replicateData(entry)
    }
    
    class Slave {
        master
        requestRecovery()
        recover()
    }
    
    %% Supporting entity
    class LogEntry {
        id
        key
        value
        timestamp
    }
    
    %% System container at the bottom
    class ReplicationSystem {
        master
        slaves
        write(key, value)
        read(key)
        delete(key)
        getAvailableSlave()
        monitorSystem()
    }
    
    %% Inheritance relationships
    Node <|-- Master
    Node <|-- Slave
    
    %% Master-Slave relationship
    Master "1" -- "many" Slave : manages
    
    %% LogEntry relationships
    Master -- LogEntry : creates
    
    %% Reference relationship
    Slave -- Master : connects to
    
    %% System composition relationships
    ReplicationSystem -- Master : contains
    ReplicationSystem -- Slave : contains
```

## State Charts
```mermaid
stateDiagram-v2
    direction LR
    [*] --> SystemInitializing: System Created
    SystemInitializing --> SystemRunning: Initialization Complete
    SystemRunning --> SystemShuttingDown: shutdown() called
    SystemShuttingDown --> [*]: Resources Released
    
    state SystemRunning {
        direction LR
        [*] --> AllSlavesUp
        
        AllSlavesUp --> SomeSlavesDown: Slave Failure(s)
        SomeSlavesDown --> AllSlavesUp: All Slaves Recovered
        
        state AllSlavesUp {
            direction LR
            
            state MasterNodeState {
                direction TB
                [*] --> Master_Idle
                Master_Idle --> Master_Processing_Write: write() called
                Master_Idle --> Master_Processing_Delete: delete() called
                Master_Processing_Write --> Master_Idle: Write Replication Complete
                Master_Processing_Delete --> Master_Idle: Delete Replication Complete
                
                state Master_Processing_Write {
                    [*] --> Master_CreatingWriteLogEntry
                    Master_CreatingWriteLogEntry --> Master_UpdatingLocalStore
                    Master_UpdatingLocalStore --> Master_Replicating
                    Master_Replicating --> [*]
                }
                
                state Master_Processing_Delete {
                    [*] --> Master_CreatingDeleteLogEntry
                    Master_CreatingDeleteLogEntry --> Master_RemovingFromLocalStore
                    Master_RemovingFromLocalStore --> Master_Replicating
                    Master_Replicating --> [*]
                }
            }
            
            state AllSlaveNodesState {
                direction TB
                state "SlaveNodeStates" as SlaveCompound {
                    [*] --> Slaves_Idle
                    
                    Slaves_Idle --> Slaves_Reading: read()
                    Slaves_Reading --> Slaves_Idle
                    
                    Slaves_Idle --> Slaves_ReceivingLogEntry: applyLogEntry()
                    Slaves_ReceivingLogEntry --> Slaves_ApplyingLogEntry
                    Slaves_ApplyingLogEntry --> Slaves_CheckingOperationType
                    Slaves_CheckingOperationType --> Slaves_StoringData: Write Operation
                    Slaves_CheckingOperationType --> Slaves_DeletingData: Delete Operation
                    Slaves_StoringData --> Slaves_Idle
                    Slaves_DeletingData --> Slaves_Idle
                }
            }
            
            state ReplicationSystemState {
                direction TB
                [*] --> System_Idle
                System_Idle --> System_Write: write() requested
                System_Idle --> System_Delete: delete() requested
                System_Idle --> System_Read: read() requested
                System_Read --> System_Idle
                System_Write --> System_Idle
                System_Delete --> System_Idle
            }
            
            MasterNodeState --> AllSlaveNodesState: Replication
            ReplicationSystemState --> MasterNodeState: write()
            ReplicationSystemState --> AllSlaveNodesState: read()
        }
        
        state SomeSlavesDown {
            direction LR
            
            state "Master State" as MasterWithSlavesDown {
                direction TB
                [*] --> MasterLimitedOps
                MasterLimitedOps --> MasterWrite: write() called
                MasterLimitedOps --> MasterDelete: delete() called
                MasterWrite --> MasterReplicatePartial
                MasterDelete --> MasterReplicatePartial
                MasterReplicatePartial --> MasterLimitedOps
            }
            
            state "Slave Nodes" as SlaveNodesWithFailures {
                direction TB
                [*] --> ActiveSlaves
                ActiveSlaves --> FailedSlaves: Failure
                FailedSlaves --> SlaveRecovering: goUp()
                SlaveRecovering --> ActiveSlaves: Recovery Complete
                
                state SlaveRecovering {
                    [*] --> RequestingRecovery
                    RequestingRecovery --> FetchingMissingLogs
                    FetchingMissingLogs --> ApplyingMissingLogs
                    ApplyingMissingLogs --> [*]
                }
            }
            
            state "System State" as SystemWithFailures {
                direction TB
                [*] --> LimitedOperations
                LimitedOperations --> FailoverReads: read()
                FailoverReads --> LimitedOperations
                LimitedOperations --> PartialWrite: write()
                LimitedOperations --> PartialDelete: delete()
                PartialWrite --> LimitedOperations
                PartialDelete --> LimitedOperations
            }
            
            MasterWithSlavesDown --> SlaveNodesWithFailures: Partial Replication
            SystemWithFailures --> MasterWithSlavesDown: write()
            SystemWithFailures --> SlaveNodesWithFailures: read()
        }
    }
    
    note right of SystemRunning
        Master node is always UP
        Only slaves can fail
    end note
```

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
        +boolean delete(String key)
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
        +boolean delete(String key)
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
        -OperationType operationType
        -long timestamp
        +LogEntry(long id, String key, String value)
        +LogEntry(long id, String key, String value, OperationType operationType)
        +getters()
        +boolean isDelete()
    }
    
    class OperationType {
        <<enumeration>>
        WRITE
        DELETE
    }

    class ReplicationSystem {
        -MasterNode master
        -List~SlaveNode~ slaves
        -Random random
        -ScheduledExecutorService scheduler
        +boolean write(String key, String value)
        +boolean delete(String key)
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
    LogEntry -- OperationType : uses
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
    
    Note over MasterNode: Create LogEntry with ID and WRITE operation type
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

## Delete Flow

```mermaid
sequenceDiagram
    participant Client
    participant ReplicationSystem
    participant MasterNode
    participant SlaveNode1
    participant SlaveNode2
    participant SlaveNode3

    Client->>ReplicationSystem: delete(key)
    ReplicationSystem->>MasterNode: delete(key)
    
    Note over MasterNode: Check if key exists
    alt Key exists
        Note over MasterNode: Create LogEntry with ID and DELETE operation type
        MasterNode->>MasterNode: Remove from local dataStore
        MasterNode->>MasterNode: Add to log
        
        par Asynchronous Replication
            MasterNode-->>SlaveNode1: applyLogEntry(entry, lock)
            Note over SlaveNode1: If up, apply delete with lock
            SlaveNode1-->>MasterNode: Replication result
            alt Replication Failed
                MasterNode-->>SlaveNode1: recoverSlave()
            end

            MasterNode-->>SlaveNode2: applyLogEntry(entry, lock)
            Note over SlaveNode2: If up, apply delete with lock
            SlaveNode2-->>MasterNode: Replication result
            alt Replication Failed
                MasterNode-->>SlaveNode2: recoverSlave()
            end

            MasterNode-->>SlaveNode3: applyLogEntry(entry, lock)
            Note over SlaveNode3: If up, apply delete with lock
            SlaveNode3-->>MasterNode: Replication result
            alt Replication Failed
                MasterNode-->>SlaveNode3: recoverSlave()
            end
        end
        
        MasterNode->>ReplicationSystem: Return success
        ReplicationSystem->>Client: Return true
    else Key does not exist
        MasterNode->>ReplicationSystem: Return failure
        ReplicationSystem->>Client: Return false
    end
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
        SomeSlavesDown --> AllNodesUp: Slave recovery
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
