# Flow

File Scan -> Synchronizer -> Storage -> Remote Index File & Remote File Storage

# Components

* Analyzer:

    Analyzes local folders - watch directories trees for  files changes and
    do full folder scan, firing LocalFile event.

* Sync Arbiter:

    Consumes the LocalFile events, updates local file metadata information
    (file path, md5, last modification, size), and decide whether to fire FileTransfer
    events.

* Storage:

    Support for specific storage backend - do the actual file
    transfer. Fires TransferStatus events.

* NBus:

    Message bus used for communication between the modules
    above. Takes care of coroutine initialization and so on.

* Application:

    Provide entry point for implement Use Cases operations.
    Consume and execute AppCommands.

* CLI/GUI:

    On top of the REST API, provides access to configuration setup and operation
    status.

* Data Structures:

    conf:
        stored as json file
        - general
        - synchronizations:
          - <sync name>
            local: <local path - c:\documents>
            storage: <remote path - smb://server/folder >
                    
    index_format:
        file_path:checksum:size:timestamp
        
        index files are stored under the '.nsync/' folder and are named as
        '<uuid>.index'
        

## Open Question:
• Sanitization: Removing old files
• File transfer / storage optmization
• File recovery mechanism