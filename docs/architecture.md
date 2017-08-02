# Flow

File Scan -> Synchronizer -> Storage -> Remote Index File & Remote File Storage

# Components

* File Scanner:

    Do full folder scan and add files to synchronization queue
    Register Directories to watch and add files to synchronization queue
    Send full scans to sanitizer - so it can remove old files

* Synchronizer:

    Consumes the files from the files from synchronization queue
    Updates local database (record file, path, md5, last modification, size)
    Implements update heuristics

* Storage:

    Takes care of the persistence logic
    Creates/Updates the File Index
    Writes the file and the index to the File Storage

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