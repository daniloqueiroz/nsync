The metadata sub-system is responsible to store all
information regarding the Filesystem and its files.

For accomplish this goal it relies on the following
components: Fstab, Filesystem, NameSystem and MetadataServer.

# Fstab
Contains information about all FS on the system.
It stores the following data:

 * fsentry: fs_id, local folder path, remote folder path

## Metadata file
 * filesystems - contains control block for all fs

# Filesystem
Contains all the file metadata for a given FS.
It stores the following data:

 * index: file_id, relative path, inode position
 * inode: size, checksum, modificationTS, status

## Metadata files
 * <fs_id>.control - inode table
 * <fs_id>.inodes - contains the inode entries, one per line - fixed size

# NamingServer
Resolve identifier to resources.
This is the only component of the metadata sub-system that
is exposed by the kernel facade.

##  Identifier format
nsync:<fs_id>[:file_id]

## Operations
 * given a fs_id and relative path, returns an Identifier 
 * given a identifier, returns a resource

## Resources
 *  FSMeta: uri, local path, remote path
 *  FileInfo: uri, relative path, size, checksum, modificationTS, status

# MetadataServer
The metadata server is responsible by manipulating the metadata
structures (fstab and filesystems) directly.