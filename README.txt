========================================================================
TEAM: 3000000

Group Members: 
- Kunall Banerjee (#100978717)
- Mohamed Zalat (#100968390)
- Shasthra Ranasinghe (#100867803)
- Ismail Syed (#100923110)
- Aritra Sengupta (#100921432)

========================================================================
RESPONSIBILITIES: 

Kunall Banerjee & Mohamed Zalat:
-

Shasthra Ranasinghe:
- 

Aritra Sengupta:
- 

Ismail Syed:
- 

========================================================================
FILES:
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
SET UP INSTRUCTIONS: 

1) Open Eclipse
2) Select File->Import then General->Existing Projects into Workspace 
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

========================================================================
TEST INSTRUCTIONS: 
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
Credits:
- Assignment 1 Sample Solution from the Course Materials Page
========================================================================
