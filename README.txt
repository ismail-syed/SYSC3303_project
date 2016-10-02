========================================================================
TEAM: 3000000

Group Members: 
- Kunall Banerjee (#100978717)
- Mohamed Zalat (#100968390)
- Shasthra Ranasinghe (#100867803)
- Ismail Syed (#100923110)
- Aritra Sengupta  (#100921432)

========================================================================
RESPONSIBILITIES: 

Kunall Banerjee:
- Client side implementation

Mohamed Zalat:
- Client side implementation

Shasthra Ranasinghe:
- Server side implementation
- Writing the DataPacket, Reader and Write classes
- Implementing steady-state file transfer from the server
- Implementing threading on the server
- Modifying the simulator to work with DATA and ACK packets

Aritra Sengupta:
- Writing the Exceptions, FileIO and TFTPPackets packages
- Implementing steady-state file transfer from the server
- Implementing threading on the server
- Git integration

Ismail Syed:
- Project management and coordination
- Git & Github repository setup, integration and management
- UML class diagram
- UCM diagrams
- Readme.txt creation and maintenance

========================================================================
FILES:

Exceptions:
- InvalidBluckNumberException.java
- MalformedPacketException.java
- PacketOverflowException.java

FileIO: 
- TFTPReader.java
- TFTPWriter.java

TFTP:
- TFTPClient.java
- TFTPServer.java
- TFTPServerTransferThread.java
- TFTPSim.java

TFTPPackets:
- ACKPacket.java
- DataPacket.java
- RRQPacket.java
- RRQWRQPacketCommon.java
- TFTPPacket.java
- WRQPacket.java

- Iteration1_UML_Class_diagrams.png
- UCM_read_file_transfer.png
- UCM_write_file_tansfer.png

========================================================================
SET UP INSTRUCTIONS: 

1) Create a blank Java eclipse project.
2) Copy the submitted ./src directory files into the ./src directory of the newly created eclipse project.
3) In the root path of the new Eclipse project, create a Client and Server Folder. These folders will be used for the 'DEFAULT' mode.
4) Copy the "Client_#" and "Server_#" test files from the submitted test_files folder into the Client and Server directories created in Step 3 respectively.

========================================================================
TEST INSTRUCTIONS: 

1) Complete Set up instruction above.
2) Run TFTPServer and follow the console instructions.
3) Run FTFPSim.java.
4) Run TFTPClient.java and follow the instructions.
5) When making a read request the files available are:
- Server_250.txt
- Server_513.txt
- Server_1000.txt
- Server_90000.txt
6) When making a write request the files available are:
- Client_250.txt
- Client_513.txt
- Client_1000.txt
- Client_90000.txt

========================================================================
Credits:
- Assignment 1 Sample Solution from the Course Materials Page
========================================================================
