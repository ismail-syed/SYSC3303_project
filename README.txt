========================================================================

TEAM: 3000000

Group Members:

- Kunall Banerjee (#100978717)
- Mohamed Zalat (#100968390)
- Shasthra Ranasinghe (#100867803)
- Ismail Syed (#100923110)
- Aritra Sengupta (#100921432)

========================================================================

SET UP INSTRUCTIONS

1) Open Eclipse
2) Select File -> Import then General -> Existing Projects into Workspace 
3) For the root directory select the submitted folder "SYSC3303_Iteration2"
3) Run TFTPClient.java
	 i) Type DEFAULT to chose the directory the program is running in or 
	    type in a path to a directory (the directory can be changed by typing "cd").
	    The directory chosen in this step will be where files transferred from the server
	    to the client will be stored.
	ii) Choose to keep Verbose mode on or off by typing "Y" or "N"
       iii) To use test mode type "TEST" to use normal mode type "NORMAL"
	iv) Chose Read or Write request by typing "R" or "W"
	 v) Type file name of a file existing in the directory chosen in step i)
	
4) Run TFTPSim.java
5) Run TFTPServer.java
        i) Type DEFAULT to chose the directory the program is running in or 
	   type in a path to a directory (the directory can be changed by typing "cd"). 
	   The directory chosen in this step will be where files transferred from the client
	   to the server will be stored.
       ii) Choose to keep Verbose mode on or off by typing "Y" or "N"
6) To close the server or the client type "QUIT"

Note: Files can NOT be overwritten on the server and client

========================================================================

RESPONSIBILITIES

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
ITERATION 1
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Kunall Banerjee & Mohamed Zalat
- Client side implementation
- Client is able to read and write to files
- Added verbose and quite mode
- Added test and normal mode
- Client also responds to data and acknowledgement packets appropriately

Shasthra Ranasinghe
- Server side implementation
- Writing the DataPacket, Reader and Write classes
- Implementing steady-state file transfer from the server
- Implementing threading on the server
- Modifying the Error Simulator to work with DATA and ACK packets with a multithreaded server

Aritra Sengupta
- Writing the Exceptions, FileIO and TFTPPackets packages
- Implementing steady-state file transfer from the server
- Implementing threading on the server
- Git integration

Ismail Syed
- Project management and coordination
- Git & Github repository setup, integration and management
- UML class diagram
- UCM diagrams
- Readme.txt creation and maintenance

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
ITERATION 2
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


Kunall Banerjee & Mohamed Zalat
- Server side implementation
- Created the whole class structure for the ErrorPacket class
- Exceptions are caught gracefully & translated to proper
  TFTP error codes
- Fixed issues raised in feedback obtained for ITERATION 1


Shasthra Ranasinghe
-

Aritra Sengupta
-

Ismail Syed
-

========================================================================

FILES
│   README.txt
│
├───Images
│       Iteration2_UML_Class_diagrams.png
│       Timing_Diagram_Error_1.png
│       Timing_Diagram_Error_2.png
│       Timing_Diagram_Error_3.png
│       Timing_Diagram_Error_6.png
│       UCM_read_file_transfer.png
│       UCM_write_file_transfer.png
│
└───SYSC3303_Iteration2
    │   .classpath
    │   .project
    │
    ├───Client
    │    Client_0.txt
    │    Client_1000.txt
    │    Client_250.txt
    │    Client_512.txt
    │    Client_90000.txt
    │
    ├───Server
    │    Server_0.txt
    │    Server_1000.txt
    │    Server_250.txt
    │    Server_512.txt
    │    Server_90000.txt
    │
    └───src
        ├───Exceptions
        │       InvalidBlockNumberException.java
        │       MalformedPacketException.java
        │       PacketOverflowException.java
        │
        ├───FileIO
        │       TFTPReader.java
        │       TFTPWriter.java
        │
        ├───TFTP
        │       TFTPClient.java
        │       TFTPServer.java
        │       TFTPServerTransferThread.java
        │       TFTPSim.java
        │
        └───TFTPPackets
                ACKPacket.java
                DataPacket.java
                ErrorPacket.java
                RRQPacket.java
                RRQWRQPacketCommon.java
                TFTPPacket.java
                WRQPacket.java

========================================================================

TEST INSTRUCTIONS

(Note: The numbers in the files below is how big the files are in bytes)

1) Complete Set up instruction above.
2) When making a read request the files available are:
- Server_0.txt
- Server_250.txt
- Server_512.txt
- Server_1000.txt
- Server_90000.txt
3) When making a write request the files available are:
- Client_0.txt
- Client_250.txt
- Client_512.txt
- Client_1000.txt
- Client_90000.txt

========================================================================

CREDITS

Assignment 1 sample solution from the Course Materials Page

========================================================================
