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
	i) Choose a network error type
       ii) choose a the type of packet and the block number if applicable
      iii) choose delay if applicaple 
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
- Client side implementation
- Error checking on the client-side
- Error simulator modifications
- Fixed issues and bugs from ITERATION 1

Aritra Sengupta
- Project management and coordination
- Git management
- UML class diagram
- Timing diagrams for error codes 01, 02, 03 and 06
- Updated UCMs for READ and WRITE file transfers

Ismail Syed
- Client side implementation
- Fixed issues and bugs from ITERATION 1
- Minor bug fixes

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
ITERATION 3
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Kunall Banerjee & Ismail Syed
- Error Simulator implementation
- Refactoring of the entire simulator
- Thread creation implementation
- Designed an implemented network error simulating logic

Shasthra Ranasinghe
- Project management and coordination
- UML class diagram
- Timing diagrams for all possible cases
- Git management

Aritra Sengupta
- Server side implementation
- Refactored server code
- Modified the server so it drops duplicate packets
- Modified the server to resend DATA packets if it does not receive an ACK from the client

Mohamed Zalat
- Client side implementation
- Added resending of data packets and request to client
- Added droping for duped acks to client
- Added responding to duped and old data packets with ack while not writing to client
- Added proper file handling to client
- Debugged Sim to point out errors

========================================================================

FILES
│   README.txt
│
├───Images
|	Iteration1_UML_Class_diagrams.png
│       Iteration2_UML_Class_diagrams.png
|	Iteration3_UML_Class_diagrams.png
│       Timing_Diagram_Error_1.png
│       Timing_Diagram_Error_2.png
│       Timing_Diagram_Error_3.png
│       Timing_Diagram_Error_6.png
│       UCM_read_file_transfer.png
│       UCM_write_file_transfer.png
|	Timing_Diagram_Lost_WRQ.png
|	Timing_Diagram_Lost_RRQ.png
|	Timing_Diagram_Duplicate_WRQ.png
|	Timing_Diagram_Duplicate_WRQ.png
|	Timing_Diagram_Delay_WRQ.png
|	Timing_Diagram_Delay_WRQ.png
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
	|	ErrorSimDelayThread.java
        │       TFTPClient.java
	|	TFTPErrorSimMode.java
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

TEST INSTRUCTIONS (NETWORK ERRORS)

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
4) Choose Normal mode by typing 0
	i) The simulator will not wait for packets from client
       ii) Any transfer(read or write) will be completed without any network errors
       		- I/O Erros will be handled here
5) Restart The sim and enter 1 for packet loss
	i) Choose DATA, ACK, WRQ, or RRQ packet to loose
		- Choosing DATA or ACK will prompt you to enter the packet number. this is the block number to be lost
		- Choosing WRQ or RRQ doe not prompt anything further
       ii) The Sim will loose the specified packet but the file would still transfer correctly
       		- Use fc on command line to compare files to see if the application handled the error
6) Restart the sim and enter 2 for packet delay
	i) Choose DATA, ACK, WRQ, or RRQ packet to delay
		- Choosing DATA or ACK will prompt you to enter the packet number. this is the block number to be delay
		- Choosing WRQ or RRQ doe not prompt for a packet number
		- After choosing the packet number or not, you will need to specify a delay length in milliseconds
			This is how long the sim will hold the packet specified before sending it to its destination
       ii) The sim will delay the specified packet and any duplicated will be discarded
       		- The file should be transfered coreectly
		- Use fc on command line to compare files to see if the application handled the error
7) Restart the sim and enter 3 for packet duplication
	i) Choose DATA, ACK, WRQ, or RRQ packet to duplicate
		- The duplicate prompts will work very much the same way as delay, 
			so refer to the above point on how to set up the tests
       ii) The sim will duplicate the specified packet and that packet will be discarded
       		- The file should be transfered coreectly
		- Use fc on command line to compare files to see if the application handled the error
8) The tests above work for both READ and WRITE and can be repeated for both
9) The test files provided will help you perform these tests for all cases:
	- less than 512 bytes
	- exactly 512 bytes
	- greater than 512 bytes

========================================================================

CREDITS

Assignment 1 sample solution from the Course Materials Page

========================================================================
