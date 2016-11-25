package TFTP;

import java.io.IOException;
import java.util.Arrays;

import Exceptions.InvalidBlockNumberException;
import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;
import TFTPPackets.ACKPacket;
import TFTPPackets.DataPacket;
import TFTPPackets.ErrorPacket;
import TFTPPackets.RRQWRQPacketCommon;
import TFTPPackets.TFTPPacket;
import TFTPPackets.WRQPacket;
import TFTPPackets.ErrorPacket.ErrorCode;
import TFTPPackets.RRQWRQPacketCommon.Mode;

public class CorruptionTester {

	public static void main(String[] args) {
		TFTPPacket tftpPacketWRQ = new TFTPPacket();
		try {
			tftpPacketWRQ = new WRQPacket("file.txt", RRQWRQPacketCommon.Mode.OCTET);
		} catch (PacketOverflowException e) {
			e.printStackTrace();
		}

		TFTPPacket tftpPacketACK = new TFTPPacket();
		try {
			tftpPacketACK = new ACKPacket(3);
		} catch (InvalidBlockNumberException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TFTPPacket tftpPacketDATA = new TFTPPacket();
		String string = "ABCD";
		byte[] data = string.getBytes();
		try {
			tftpPacketDATA = new DataPacket(4, data);
		} catch (PacketOverflowException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedPacketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TFTPPacket tftpPacketERROR = new TFTPPacket();
		tftpPacketERROR = new ErrorPacket(ErrorCode.ACCESS_VIOLATION, "Access Violation");
		
		//WRQ/RRQ
		byte[] packet = tftpPacketWRQ.getByteArray();
		byte[] result;
		System.out.println("WRQ:                    " + TFTPPacket.toString(Arrays.copyOfRange(packet, 0,packet.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.INVALID_OPCODE);
		System.out.println("Invalid Opcode:         " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.RQ_MISSING_FILENAME);
		System.out.println("Missing Filename:       " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.RQ_MISSING_FIRST_ZERO);
		System.out.println("Missing First Zero:     " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.RQ_MISSING_MODE);
		System.out.println("Missing Mode:           " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.RQ_INVALID_MODE);
		System.out.println("Corrupted Mode:         " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.RQ_MISSING_SECOND_ZERO);
		System.out.println("Missing 2nd Zero:       " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.EXTRA_DATA_AT_END);
		System.out.println("Extra Data:             " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		System.out.println("\n");
		
		packet = tftpPacketDATA.getByteArray();
		System.out.println("DATA:                 " + TFTPPacket.toString(Arrays.copyOfRange(packet, 0,packet.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.INVALID_OPCODE);
		System.out.println("Invalid Opcode:       " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.DATA_OR_ACK_INVALID_BLOCK_NUMBER);
		System.out.println("Invalid Block Number: " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.DATA_OR_ACK_MISSING_BLOCK_NUMBER);
		System.out.println("Missing Block Number: " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.DATA_MISSING_DATA);
		System.out.println("Missing Data:         " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.EXTRA_DATA_AT_END);
		System.out.println("Extra Data:           " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		System.out.println("\n");
		
		packet = tftpPacketACK.getByteArray();
		System.out.println("ACK:                  " + TFTPPacket.toString(Arrays.copyOfRange(packet, 0,packet.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.INVALID_OPCODE);
		System.out.println("Invalid Opcode:       " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.DATA_OR_ACK_INVALID_BLOCK_NUMBER);
		System.out.println("Invalid Block Number: " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.DATA_OR_ACK_MISSING_BLOCK_NUMBER);
		System.out.println("Missing Block Number: " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.EXTRA_DATA_AT_END);
		System.out.println("Extra Data:           " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		System.out.println("\n");
		
		packet = tftpPacketERROR.getByteArray();
		System.out.println("ERROR:                 " + TFTPPacket.toString(Arrays.copyOfRange(packet, 0,packet.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.INVALID_OPCODE);
		System.out.println("Invalid Opcode:        " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.ERROR_INVALID_ERROR_CODE);
		System.out.println("Invalid Error Code:    " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.ERROR_MISSING_ERROR_CODE);
		System.out.println("Missing Error Code:    " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.ERROR_MISSING_ERROR_MESSAGE);
		System.out.println("Missing Error Message: " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.ERROR_MISSING_ZERO);
		System.out.println("Missing Zero:          " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		result = PacketCorrupter.corruptPacket(packet, ExtendedMenu.EXTRA_DATA_AT_END);
		System.out.println("Extra Data:            " + TFTPPacket.toString(Arrays.copyOfRange(result, 0,result.length)) + "\n");
		System.out.println("\n");
	}

}
