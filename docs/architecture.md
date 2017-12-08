# Flow

## WebUI/CLI

WebUI/CLI -(http)-> REST API <--> Kernel Facade -(nbus)-> [NSync Servers]

## NSync Servers

Analyzer -(nbus)-> Sync -(nbus)-> Storage

# Components
* CLI/GUI:

    On top of the REST API, provides access to configuration setup and operation
    status.

* REST API:

    Provides access to Application resources through a HTTP RESTful API

* Kernel Facade:

    Provide entry point for implement Use Cases operations.
    Execute operations asynchronously and provide access to Kernel
    readonly data structures (metadata & metrics)

* Analyzer:

    Analyzes local folders - watch directories trees for  files changes and
    do full folder scan, firing LocalFile event.

* Sync:

    Consumes the LocalFile events, updates local file metadata information
    (file path, md5, last modification, size), and decide whether to fire FileTransfer
    events.

* Storage:

    Support for specific storage backend - do the actual file
    transfer. Fires TransferStatus events.

* [Metadata](metadata.md):

    Keeps metadata information about the Filesystems and files within the system.
    Provides a readonly access and can be modified through asynchronous signals 

* NBus:

    Message bus used for communication between the modules
    above. Takes care of coroutine initialization and so on.


## Open Question:
• Sanitization: Removing old files
• File transfer / storage optmization
• File recovery mechanism