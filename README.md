Makefile

    "make" compiles and setups build/.

    "make run_integration" runs a integration test file for development purposes. It also resets the rmiregistry.

    "make run_metaserver" runs a MetaServer instance with default configs and resets the rmiregistry if needed.

    "make run_storageserver" launches a new StorageServer with default configs.

    "make run_client" runs a self-contained Client instance which acts as a shell program.

Client Shell

    The Client has the following commands available:

        - pwd shows the current directory path.
        - ls shows all of the files/directories in the current directory.
        - cd DIR changes the current directory to DIR.
        - mv FILE1 FILE2 renames FILE1 into FILE2 deleting the former if it exists already.
        - mv FILE DIR sends FILE into DIR.
        - rm FILE deletes FILE.
        - open FILE uses the program specified in the configuration file to open the file FILE.
        - mkdir DIR creates the directory DIR.
        - rmdir removes DIR if it is empty.
        - touch FILE creates an empty file FILE.
        - upload FILE DIR sends a local file FILE to the file system's directory DIR.
        - download FILE DIR sends a remote file FILE to the local directory DIR.
        - exit/quit terminates the client process.
        - reload updates the configuration file.
