/*
 *****************************************************************************
 * Copyright by ams AG                                                       *
 * All rights are reserved.                                                  *
 *                                                                           *
 * IMPORTANT - PLEASE READ CAREFULLY BEFORE COPYING, INSTALLING OR USING     *
 * THE SOFTWARE.                                                             *
 *                                                                           *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS       *
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT         *
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS         *
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT  *
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,     *
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT          *
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     *
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY     *
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT       *
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE     *
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.      *
 *****************************************************************************
 */
 /* @file AS39513.java
 *
 *  @author Florian Hofer (florian.hofer@ams.com)
 *
 *  @brief Backend Functionality for AS39513 Application
 *
 */
package com.ams.as39513;

import android.nfc.tech.NfcV;
import android.util.Log;

import java.io.IOException;

public class AS39513 {
	public static class SystemInformation {
		public byte[] uid;
		boolean dsfidAvailable;
		public byte dsfid;
		boolean afiAvailable;
		public byte afi;
		boolean memorySizeAvailable;
		public int blockSize;
		public int numBlocks;
		boolean icReferenceAvailable;
		public byte icReference;
	}
	
	enum PasswordLevel {
		NOT_ALLOWED,
		SYSTEM,
		APPLICATION,
		MEASUREMENT
	}

	public static class LogLimits {
		public char txhilim;
		public char thilim;
		public char tlolim;
		public char txlolim;
	}

	public static class LimitCounter {
		public char txhicnt;
		public char thicnt;
		public char tlocnt;
		public char txlocnt;
	}

	public static class CalibrationData {
		public char btype;
		public char bcksel;
		public char TV1SET;
		public char TV1TRIM;
		public char TV2SET;
		public char TV2TRIM;
		public char TSMOFF;
		public char TSV1G;
		public char EV1SET;
		public char EV1TRIM;
		public char EV2SET;
		public char EV2TRIM;
		public char EXMOFF;
		public char EXV1G;
		public char EXGAIN;
	}
	
	public static class LogMode {
		public int logint;
		public char strmd;
		public char batchk;
		public char logmd;
		public char tsmeas;
		public char exmeas;
	}
	
	public static class MeasurementStatus {
		public boolean active;
		public char errors;
		public char memrep;
		public int meascnt;
		public int measptr;
	}
		
	/** ISO15693 mandatory inventory command command code. */
	static final byte INVENTORY_CC = (byte) 0x01;
	/** ISO15693 mandatory stay quiet command command code. */
	static final byte STAY_QUIET_CC = (byte) 0x02;
	/** ISO15693 optional read single block command command code. */
	static final byte READ_SINGLE_BLOCK_CC = (byte) 0x20;
	/** ISO15693 optional write single block command command code. */
	static final byte WRITE_SINGLE_BLOCK_CC = (byte) 0x21;
	/** ISO15693 optional lock block command command code. */
	static final byte LOCK_BLOCK_CC = (byte) 0x22;
	/** ISO15693 optional read multiple blocks command command code. */
	static final byte READ_MULTIPLE_BLOCKS_CC = (byte) 0x23;
	/** ISO15693 optional write multiple blocks command command code. */
	static final byte WRITE_MULTIPLE_BLOCKS_CC = (byte) 0x24;
	/** ISO15693 optional select command command code. */
	static final byte SELECT_CC = (byte) 0x25;
	/** ISO15693 optional reset to ready command command code. */
	static final byte RESET_TO_READY = (byte) 0x26;
	/** ISO15693 optional write AFI command command code. */
	static final byte WRITE_AFI_CC = (byte) 0x27;
	/** ISO15693 optional lock AFI command command code. */
	static final byte LOCK_AFI_CC = (byte) 0x28;
	/** ISO15693 optional write DSFID command command code. */
	static final byte WRITE_DSFID_CC = (byte) 0x29;
	/** ISO15693 optional lock DSFID command command code. */
	static final byte LOCK_DSFID_CC = (byte) 0x2A;
	/** ISO15693 optional get system information command command code. */
	static final byte GET_SYSTEM_INFORMATION_CC = (byte) 0x2B;
	/** ISO15693 optional get multiple block security status command command code. */ 
	static final byte GET_MULTIPLE_BLOCK_SECURITY_STATUS_CC = (byte) 0x2C;

	/** AS39513 commands */
	static final byte CUSTOM_CMD_SET_ACCESS						= (byte) 0xA0;
	static final byte CUSTOM_CMD_SET_PASSWORD					= (byte) 0xA1;
	static final byte CUSTOM_CMD_READ_SYSTEM_BLOCK				= (byte) 0xA2;
	static final byte CUSTOM_CMD_WRITE_SYSTEM_BLOCK				= (byte) 0xA3;
	static final byte CUSTOM_CMD_READ_MULTIPLE_SYSTEM_BLOCKS	= (byte) 0xA4;
	static final byte CUSTOM_CMD_LOCK_ALL_BLOCKS				= (byte) 0xA6;
	static final byte CUSTOM_CMD_SET_MODE						= (byte) 0xA8;
	static final byte CUSTOM_CMD_DO_MEASUREMENT					= (byte) 0xA9;
	static final byte CUSTOM_CMD_GET_LOG_STATUS					= (byte) 0xAA;

	static final byte AS39513_MODE_IDLE  	= 0;
	static final byte AS39513_MODE_WAIT  	= 1;
	static final byte AS39513_MODE_ACTIVE	= 2;
	static final byte AS39513_MODE_NONE  	= 3;

	static final byte AS39513_MEASUREMENT_TEMP 	    = 0;
	static final byte AS39513_MEASUREMENT_BATTERY  	= 1;
	static final byte AS39513_MEASUREMENT_SENSOR	= 2;
	static final byte AS39513_MEASUREMENT_RESERVED 	= 3;

	static final String TAG = "AS39513";
	protected NfcV nfcv;

	private boolean calib_values_temp = false;
	private boolean data_rate_low = true;
	char TV1TRIM_local;
	char TV1SET_local;
	char TV2TRIM_local;
	char TV2SET_local;
	char TSMOFF_local;
	char TSV1G_local;
	
	public AS39513(NfcV nfcv, boolean data_rate_low) {
		super();
		this.nfcv = nfcv;
		this.data_rate_low = data_rate_low;
	}
	
	/**
	 * ISO15693 read single block command.
	 * 
	 * @param blockNumber Block number of the block to read.
	 * @return Content of tag memory block \a blockNumber.
	 * @throws IOException
	 */
	public byte[] readSingleBlock(int blockNumber) throws IOException {
		if ((blockNumber < 0) || (blockNumber > 255))
			throw new IllegalArgumentException("block number must be within 0-255");
		byte[] parameter = new byte[1];
		parameter[0] = (byte) (blockNumber & 0xFF);
		byte result[] = transceive(READ_SINGLE_BLOCK_CC, parameter);
		return result;
	}
	
	/**
	 * ISO15693 read multiple blocks command.
	 * 
	 * @param firstBlock First block to read.
	 * @param numBlocks Number of blocks to read.
	 * @return 
	 * @throws IOException
	 */
	public byte[] readMultipleBlocks(char firstBlock, char numBlocks) throws IOException {
		if ((firstBlock < 0) || (firstBlock > 255))
			throw new IllegalArgumentException("Start block must be within 0-255");
		if (numBlocks < 0)
			throw new IllegalArgumentException("Number of blocks to read must be within 0-255");
		if ((numBlocks + firstBlock) > 255)
			throw new IllegalArgumentException("Read length exceeds last block");
		
		byte parameter[] = new byte[2];
		parameter[0] = (byte) (firstBlock & 0xFF);
		parameter[1] = (byte) ((numBlocks-1) & 0xFF);
		byte result[] = transceive(READ_MULTIPLE_BLOCKS_CC, parameter);
		return result;
	}

	/**
	 * ISO15693 get system information (optional) command.
	 *
	 * @return
	 * @throws IOException
	 */
	public SystemInformation getSystemInformation() throws IOException {
		byte result[] = transceive(GET_SYSTEM_INFORMATION_CC);
		SystemInformation systemInformation = new SystemInformation();
		systemInformation.uid = new byte[8];
		System.arraycopy(result, 2, systemInformation.uid, 0, 8);

		systemInformation.dsfidAvailable = true;
		systemInformation.dsfid = result[10];
		systemInformation.afi = result[11];
		systemInformation.blockSize = ((int) result[12]) & 0x1F;
		systemInformation.numBlocks = ((int) result[13]) & 0xFF;
		systemInformation.icReferenceAvailable = true;
		systemInformation.icReference = result[14];
		
		return systemInformation;
	}

	/**
	 * AS39513 setPassword command
	 *
	 * @param passwordLevel Application,Measurement,System level
	 * @param password 4 byte password
	 * @throws IOException
	 */
	public void customCmdSetPassword(PasswordLevel passwordLevel, byte[] password) throws IOException {
		byte command[] = new byte[5];
		switch (passwordLevel)
		{
			case SYSTEM: command[0] = 0x01; break;
			case APPLICATION: command[0] = 0x02; break;
			case MEASUREMENT: command[0] = 0x03; break;
			case NOT_ALLOWED: command[0] = 0x00; break;
			default:
				Log.e(TAG,String.format("Unkown passwordLevel argument value: " + passwordLevel));
				throw new IllegalArgumentException("Unkown passwordLevel argument value: " + passwordLevel);
		}
		
		if (password.length < 4) {
			throw new IllegalArgumentException("Password byte array too short");
		}
		System.arraycopy(password, 0, command, 1, 4);
		
		transceive(CUSTOM_CMD_SET_PASSWORD);
	}

	/**
	 * AS39513 setAccess command
	 *
	 * @param passwordLevel Application,Measurement,System level
	 * @param password 4 byte password
	 * @throws IOException
	 */
	public void customCmdSetAccess(PasswordLevel passwordLevel, byte[] password) throws IOException {
		byte command[] = new byte[5];
		switch (passwordLevel)
		{
			case SYSTEM: command[0] = 0x01; break;
			case APPLICATION: command[0] = 0x02; break;
			case MEASUREMENT: command[0] = 0x03; break;
			case NOT_ALLOWED: command[0] = 0x00; break;
			default:
				Log.e(TAG,String.format("Unkown passwordLevel argument value: " + passwordLevel));
				throw new IllegalArgumentException("Unkown passwordLevel argument value: " + passwordLevel);
		}
		
		if (password.length < 4) {
			throw new IllegalArgumentException("Password byte array too short");
		}
		System.arraycopy(password, 0, command, 1, 4);
		
		transceive(CUSTOM_CMD_SET_ACCESS);
	}

	/**
	 * SL13A setLogMode command
	 *
	 * @param logMode Class including all log mode values
	 * @throws IOException
	 */
	public void setLogMode(LogMode logMode) throws IOException {
		// log interval
		customCmdWriteSystemBlock((char)0x02,logMode.logint<<16,(long)0xFFFF0000);

		// storage rule
		writeSystemAddress((long) 0x40E, (char) (logMode.strmd << 6), (char) 0x40);

		// battery check
		// logging form
		// SL13A: either internal or external sensor measurement
		// AS39513: can measure internal/external or both
		customCmdWriteSystemBlock((char) 0x0E, (logMode.batchk << 16) | (logMode.tsmeas << 24) | (logMode.exmeas << 26) | (logMode.logmd << 28), 0x35010000);
	}

	/**
	 * SL13A setLogLimits command
	 *
	 * @param logLimits Class which includes all log limits
	 * @throws IOException
	 */
	public void setLogLimits(LogLimits logLimits) throws IOException {
		// extreme lower limit
		// lower limit
		// upper limit
		customCmdWriteSystemBlock((char)0x04,(logLimits.txhilim)|(logLimits.txlolim<<8)|(logLimits.thilim<<16)|(logLimits.tlolim<<24));
	}

	/**
	 * SL13A getMeasurementSetup command
	 *
	 * @param startTime Logging start time
	 * @param logLimits Logging limits
	 * @param logMode Logging mode
	 * @param delayTime Delay time before first logging
	 * @throws IOException
	 */
	public void getMeasurementSetup(Integer startTime, LogLimits logLimits, LogMode logMode, Integer delayTime) throws IOException {
		// start time
		// |  MSB  | ...   |       |       |       |       |   ... |  LSB  |
		// |x x x x|x x x x|x x x x|x x x x|x x x x|x x x x|x x x x|x x x x|
		// |    year   | month |   day   |   hour  |    min    |    sec    |
		// |x x x x x x|x x x x|x x x x x|x x x x x|x x x x x x|x x x x x x|
		long stime = customCmdReadSystemBlock((char)0x00);
		startTime = Integer.valueOf((int) stime);

		// logging interval
		// logging delay after start
		long data = customCmdReadSystemBlock((char)0x02);
		logMode.logint = (int)(data>>16);
		delayTime = (int)(data&0x0FFF);

		// extreme upper limit
		// extreme lower limit
		// upper limit
		// lower limit
		data = customCmdReadSystemBlock((char)0x04);
		logLimits.txhilim = (char)(data&0xFF);
		logLimits.txlolim = (char)((data>>8)&0xFF);
		logLimits.thilim  = (char)((data>>16)&0xFF);
		logLimits.tlolim  = (char)((data>>24)&0xFF);

		// storage rule
		char temp8bit = readSystemAddress((long)0x40E);
		logMode.strmd = (char)((temp8bit>>6)&0x01);

		// battery check
		// logging form
		// SL13A: either internal or external sensor measurement
		// AS39513: can measure internal/external or both
		data = customCmdReadSystemBlock((char)0x0E);
		logMode.tsmeas = (char)((data>>24)&0x01);
		logMode.exmeas = (char)((data>>26)&0x01);
		logMode.logmd  = (char)((data>>28)&0x03);
		logMode.batchk = (char)((data>>16)&0x01);
	}

	/**
	 * SL13A setInternalCalibration command
	 *
	 * @param calibrationData Class containing all calibration values
	 * @throws IOException
	 */
	public void setInternalCalibrationData(CalibrationData calibrationData) throws IOException
	{
		// SL13A
		// reg05.lowPOR = sp_LowPor->value();	// Calibration for 1.5V battery
		// reg05.highPOR = sp_HighPor->value();	// Calibration for 3.0V battery
		// AS39513
		// btype = select battery / battery detection
		// bcksel = select calibration value for all battery types
		//
		// btype = 0b000 (Battery Voltage 1.5V), 0b001 (Battery Voltage 3.0V), 0b010 (Battery type determined at power-up),
		// 0b011 (Battery type determined after initialization), 0b1xx (Battery type determined using sensor circuits)
		writeSystemAddress((long)0x43A,(char)((calibrationData.btype<<1)|(calibrationData.bcksel<<4)),(char)0x3E);

		// SL13A
		// reg05.ADCOffset =sp_ADCOffset->value();
		// AS39513
		// Calibration is done via 6 registers: xV1TRIM, xV1SET, xV2TRIM, xV2SET, xxMOFF, xxV1G, (xxGAIN, only for external sensor)
		customCmdWriteSystemBlock((char) 0x0C, (calibrationData.TV1TRIM) | (calibrationData.TV1SET << 5) | (calibrationData.TV2TRIM << 8) | (calibrationData.TV2SET << 13) | (calibrationData.TSMOFF << 16) | (calibrationData.TSV1G << 24), 0x017FFFFF);
	}

	/**
	 * SL13A setExternalCalibrationData command
	 *
	 * @param calibrationData Class containing all calibration values
	 * @throws IOException
	 */
	public void setExternalCalibrationData(CalibrationData calibrationData) throws IOException {
		// SL13A
		// itsAS39513Com->coolLogSetExternalCalibration(customCmdError, itsFlagByte, uid, le_ExternalCalVal->text().toULong(0, 16));
		// AS39513
		// Calibration is done via 6 registers: xV1TRIM, xV1SET, xV2TRIM, xV2SET, xxMOFF, xxV1G, (xxGAIN, only for external sensor)
		customCmdWriteSystemBlock((char)0x0D,(calibrationData.EV1TRIM)|(calibrationData.EV1SET<<5)|(calibrationData.EV2TRIM<<8)|(calibrationData.EV2SET<<13)|(calibrationData.EXMOFF<<16)|(calibrationData.EXV1G<<24)|(calibrationData.EXGAIN<<25),0x077FFFFF);
	}

	/**
	 * SL13A getLogState command
	 *
	 * @param measurementStatus Class containing measurement status values
	 * @param limitCounter Class containing limit counter values
	 * @throws IOException
	 */
	public void getLogState(MeasurementStatus measurementStatus, LimitCounter limitCounter) throws IOException {
		// mem replacements
		// measurement count
		long data = customCmdReadSystemBlock((char)0x03);
		measurementStatus.meascnt = (int)(data&0xFFFF);
		measurementStatus.memrep = (char)((data>>16)&0x3F);

		// measurement pointer
		// active/passive state
		// errors/events
		data = customCmdReadSystemBlock((char)0x0F);
		measurementStatus.active = (((data>>15)&0x01)==0x01 ? true : false);
		measurementStatus.errors = (char)((data>>8)&0x7F);
		measurementStatus.measptr = (int)((data>>16)&0x0FFF);

		// extreme low limit count
		// low limit count
		// upper limit count
		// extreme upper limit count
		data = customCmdReadSystemBlock((char)0x05);
		limitCounter.txhicnt = (char)(data&0xFF);
		limitCounter.txlocnt = (char)((data>>8)&0xFF);
		limitCounter.thicnt  = (char)((data>>16)&0xFF);
		limitCounter.tlocnt  = (char)((data>>24)&0xFF);
	}

	/**
	 * AS39513 readMultipleSystemBlocks command
	 *
	 * @param address First block address
	 * @param numBlocks Number of blocks to read
	 * @return
	 * @throws IOException
	 */
	public byte[] customCmdReadMultipleSystemBlocks (char address, char numBlocks) throws IOException
	{
		byte[] parameter = new byte[2];
		parameter[0] = (byte) (address & 0xFF);
		parameter[1] = (byte) (numBlocks & 0xFF);
		byte[] resp = transceive(CUSTOM_CMD_READ_MULTIPLE_SYSTEM_BLOCKS, parameter);

		return resp;
	}

	/**
	 * SL13A getCalibrationData command
	 *
	 * @param calibrationData Class containing all calibration data
	 * @throws IOException
	 */
	public void getCalibrationData(CalibrationData calibrationData) throws IOException
	{
		// SL13A
		// le_LowPor_Display->setText(QString("%1").arg(reg05.lowPOR));		// Calibration for 1.5V battery
		// le_HighPor_Display->setText(QString("%1").arg(reg05.highPOR));	// Calibration for 3.0V battery
		// AS39513
		// btype = select battery / battery detection
		// bcksel = select calibration value for all battery types
		//
		// btype = 0b000 (Battery Voltage 1.5V), 0b001 (Battery Voltage 3.0V), 0b010 (Battery type determined at power-up),
		// 0b011 (Battery type determined after initialization), 0b1xx (Battery type determined using sensor curcuits)
		char temp8bit = readSystemAddress((long)0x43A);
		calibrationData.btype = (char)((temp8bit>>1)&0x07);
		calibrationData.bcksel = (char)((temp8bit>>4)&0x03);

		// SL13A
		// le_ADCOffset_Display->setText(QString("%1").arg(reg05.ADCOffset));
		// AS39513
		// Calibration is done via 6 registers: xV1TRIM, xV1SET, xV2TRIM, xV2SET, xxMOFF, xxV1G, (xxGAIN, only for external sensor)
		long data = customCmdReadSystemBlock((char)0x0C);
		calibrationData.TV1TRIM = (char)(data&0x1F);
		calibrationData.TV1SET  = (char)((data>>5)&0x07);
		calibrationData.TV2TRIM = (char)((data>>8)&0x1F);
		calibrationData.TV2SET  = (char)((data>>13)&0x07);
		calibrationData.TSMOFF  = (char)((data>>16)&0x7F);
		calibrationData.TSV1G   = (char)((data>>24)&0x01);
		TV1TRIM_local = (char)(data&0x1F);
		TV1SET_local  = (char)((data>>5)&0x07);
		TV2TRIM_local = (char)((data>>8)&0x1F);
		TV2SET_local  = (char)((data>>13)&0x07);
		TSMOFF_local  = (char)((data>>16)&0x7F);
		TSV1G_local   = (char)((data>>24)&0x01);
		calib_values_temp = true;

		// SL13A
		// le_BVtoCE_Display->setText(QString("%1").arg(reg05.battVoltageToCE));
		// AS39513
		// does not include the feature "Enable battery voltage switch to CE pin"

		// SL13A
		// le_ExternalCalValDisplay->setText(QString("%1").arg(extCalibrVal, 8, 16, QLatin1Char('0')));
		// AS39513
		// Calibration is done via 6 registers: xV1TRIM, xV1SET, xV2TRIM, xV2SET, xxMOFF, xxV1G, (xxGAIN, only for external sensor)
		data = customCmdReadSystemBlock((char)0x0D);
		calibrationData.EV1TRIM = (char)(data&0x1F);
		calibrationData.EV1SET  = (char)((data>>5)&0x07);
		calibrationData.EV2TRIM = (char)((data>>8)&0x1F);
		calibrationData.EV2SET  = (char)((data>>13)&0x07);
		calibrationData.EXMOFF  = (char)((data>>16)&0x7F);
		calibrationData.EXV1G   = (char)((data>>24)&0x01);
		calibrationData.EXGAIN  = (char)((data>>25)&0x03);
	}

	/**
	 * AS39513 readSystemBlock command
	 *
	 * @param block_address
	 * @return
	 * @throws IOException
	 */
	private long customCmdReadSystemBlock(char block_address) throws IOException
	{
		byte[] parameter = new byte[1];
		parameter[0] = (byte) (block_address & 0xFF);
		byte[] resp = transceive(CUSTOM_CMD_READ_SYSTEM_BLOCK, parameter);
		if(resp[0]==0x00)
		{
			long block = (resp[4] << 24) & 0xFF000000;
			block |= (resp[3] << 16) & 0x00FF0000;
			block |= (resp[2] << 8) & 0x0000FF00;
			block |= (resp[1]) & 0x000000FF;
			Log.v(TAG, "ReadSystemBlock: tx=" + toHexString(parameter) + ", rx=" + toHexString(resp));
			return block;
		}
		else
		{
			long block = 0x00000000;
			Log.w(TAG, "ReadSystemBlock: tx=" + toHexString(parameter) + ", rx=" + toHexString(resp));
			return block;
		}
	}

	/**
	 * AS39513 writeSystemBlock command
	 *
	 * @param block_address
	 * @param block_data
	 * @throws IOException
	 */
	private void customCmdWriteSystemBlock(char block_address, long block_data, long mask) throws IOException
	{
		byte[] parameter = new byte[5];
		long data = block_data;
		if(mask != 0xFFFFFFFF)
		{
			data = customCmdReadSystemBlock(block_address);
			data &= (~mask);
			data |= (block_data&mask);
		}
		parameter[0] = (byte) ((block_address) &0xFF);
		parameter[1] = (byte) ((data)    &0xFF);
		parameter[2] = (byte) ((data>>8) &0xFF);
		parameter[3] = (byte) ((data>>16)&0xFF);
		parameter[4] = (byte) ((data>>24)&0xFF);

		transceive(CUSTOM_CMD_WRITE_SYSTEM_BLOCK, parameter);
	}
	private void customCmdWriteSystemBlock(char block_address, long block_data) throws IOException
	{
		byte[] parameter = new byte[5];
		parameter[0] = (byte) ((block_address) &0xFF);
		parameter[1] = (byte) ((block_data)    &0xFF);
		parameter[2] = (byte) ((block_data>>8) &0xFF);
		parameter[3] = (byte) ((block_data>>16)&0xFF);
		parameter[4] = (byte) ((block_data>>24)&0xFF);

		byte[] resp = transceive(CUSTOM_CMD_WRITE_SYSTEM_BLOCK, parameter);
	}

	/**
	 * Internal function to write to one of the 4 addresses included in a block
	 *
	 * @param address
	 * @param data
	 * @throws IOException
	 */
	private void writeSystemAddress(long address, char data) throws IOException
	{
		writeSystemAddress(address, data, (char) 0xFF);
	}
	private void writeSystemAddress(long address, char data, char mask) throws IOException
	{
		// e.g. address = 0x406 --> block 01, byte 2
		char block_address = (char)((address-0x400)/4);
		char byte_address = (char)((address-0x400)%4);

		// read out whole data block (4 bytes)
		long block_data = customCmdReadSystemBlock(block_address);

		// clear particular data byte
		switch(byte_address)
		{
			case 0: block_data &= (0xFFFFFF00 | (~mask)); break;
			case 1: block_data &= (0xFFFF00FF | ((~mask) << 8)); break;
			case 2: block_data &= (0xFF00FFFF | ((~mask) << 16)); break;
			case 3: block_data &= (0x00FFFFFF | ((~mask) << 24)); break;
			default: break;
		}
		// overwrite data byte
		block_data |= ((data&mask) << (byte_address*8));

		// write out whole data block (4 bytes)
		customCmdWriteSystemBlock(block_address, block_data);
	}

	/**
	 * Internal function to read from one of the four addresses included in a block
	 *
	 * @param address
	 * @return
	 * @throws IOException
	 */
	private char readSystemAddress(long address) throws IOException
	{
		// e.g. address = 0x406 --> block 01, byte 2
		char block_address = (char)((address-0x400)/4);
		char byte_address = (char)((address-0x400)%4);

		// read out whole data block (4 bytes)
		long block_data = customCmdReadSystemBlock(block_address);

		char data_temp = (char)(block_data >> (byte_address*8));
		return data_temp;
	}

	/**
	 * SL13A getBatteryLevel command
	 *
	 * @return
	 * @throws IOException
	 */
	public int getBatteryLevel() throws IOException {
		int data = customCmdDoMeasurement((char) AS39513_MEASUREMENT_BATTERY);
		return data;
	}

	/**
	 * AS39513 doMeasurement command
	 *
	 * @param meas Measurement type
	 * @return
	 * @throws IOException
	 */
	private int customCmdDoMeasurement (char meas) throws IOException
	{
		byte[] parameter = new byte[1];
		parameter[0] = (byte)(meas);
		byte[] resp = transceive(CUSTOM_CMD_DO_MEASUREMENT, parameter);

		if(resp[0]==0x00)
		{
			int block = ((resp[1] << 8) & 0xFF00) | ((resp[2]) & 0xFF);
			Log.v(TAG, "DoMeasurement: tx=" + toHexString(parameter) + ", rx=" + toHexString(resp));
			return block;
		}
		else
		{
			Log.w(TAG, "DoMeasurement: tx=" + toHexString(parameter) + ", rx=" + toHexString(resp));
			return resp[1]&0xFF;
		}
	}

	/**
	 * AS39513 getLogStatus command
	 *
	 * @return
	 * @throws IOException
	 */
	private char customCmdGetLogStatus () throws IOException
	{
		byte[] parameter = new byte[0];
		byte[] resp = transceive(CUSTOM_CMD_GET_LOG_STATUS, parameter);

		if(resp[0]==0x00)
		{
			Log.v(TAG, "GetLogStatus: tx=" + toHexString(parameter) + ", rx=" + toHexString(resp));
			return (char)resp[1];
		}
		else
		{
			Log.w(TAG, "GetLogStatus: tx=" + toHexString(parameter) + ", rx=" + toHexString(resp));
			return 0x00;
		}
	}

	/**
	 * AS39513 lockAllBlocks command
	 *
	 * @throws IOException
	 */
	private void customCmdLockAllBlocks () throws IOException
	{
		byte[] parameter = new byte[0];
		transceive(CUSTOM_CMD_LOCK_ALL_BLOCKS, parameter);
	}

	/**
	 * SL13A getTemperature command
	 *
	 * @return
	 * @throws IOException
	 */
	public int getTemperature() throws IOException {
		int data = customCmdDoMeasurement((char) AS39513_MEASUREMENT_TEMP);
		return data;
	}

	/**
	 * SL13A initialize command
	 *
	 * @param appblks Number of user blocks
	 * @param logdel Logging delay
	 * @param lkall Single use/secure flag
	 * @return
	 * @throws IOException
	 */
	public boolean initialize(char appblks, int logdel, char lkall) throws IOException{
		// number of four byte memory blocks
		writeSystemAddress((long)0x406,appblks);

		// number of seconds before first logging event
		customCmdWriteSystemBlock((char)0x02,logdel,(long)0x00000FFF);

		// single use/secure flag
		if(lkall==0x01)
		{
			customCmdLockAllBlocks();
		}
		return true;
	}

	/**
	 * SL13A startLog command
	 *
	 * @param startTime
	 * @return
	 * @throws IOException
	 */
    public boolean startLog(int startTime) throws IOException{
		customCmdSetMode((char)AS39513_MODE_ACTIVE);

		// start time
		// |  MSB  | ...   |       |       |       |       |   ... |  LSB  |
		// |x x x x|x x x x|x x x x|x x x x|x x x x|x x x x|x x x x|x x x x|
		// |    year   | month |   day   |   hour  |    min    |    sec    |
		// |x x x x x x|x x x x|x x x x x|x x x x x|x x x x x x|x x x x x x|
		customCmdWriteSystemBlock((char) 0x00, (long) startTime);
		return true;
    }

	/**
	 * AS39513 setMode command
	 *
	 * @param mode
	 * @throws IOException
	 */
	private void customCmdSetMode (char mode) throws IOException
	{
		byte[] parameter = new byte[1];
		parameter[0] = (byte)(mode);

		transceive(CUSTOM_CMD_SET_MODE, parameter);
	}

	/**
	 * SL13A setPassive command
	 *
	 * @return
	 * @throws IOException
	 */
    public boolean setPassive() throws IOException{
		customCmdSetMode((char)AS39513_MODE_IDLE);
        return true;
    }

	/**
	 * Convert a temperature measurement result to degree celsius.
	 * 
	 * @param code Temperature measurement returned by getTemperature().
	 * @return Temperature measurement result converted to degree celsius.
	 */
	public double convertTemperatureCodeToCelsius(int code) throws IOException{
		if(!calib_values_temp)
		{
			long data = customCmdReadSystemBlock((char)0x0C);

			TV1TRIM_local = (char)(data&0x1F);
			TV1SET_local  = (char)((data>>5)&0x07);
			TV2TRIM_local = (char)((data>>8)&0x1F);
			TV2SET_local  = (char)((data>>13)&0x07);
			TSMOFF_local  = (char)((data>>16)&0x7F);
			TSV1G_local   = (char)((data>>24)&0x01);
			calib_values_temp = true;
		}

		double VSTEP = 50.0/1000.0;
		double V1 = VSTEP*(3.0+TV1SET_local+0.0125*TV1TRIM_local);
		double V2 = VSTEP*(5.0+TV2SET_local+0.0125*TV2TRIM_local);

		char GNDV1 = TSV1G_local;

		double VLOW = V2;
		double VRNG = (1-GNDV1)*V1;
		double VMUXO = 0.0;
		double AMUX = 1.0;

		double Vzero  = (VLOW-VMUXO)/AMUX;
		double Vscale = (VLOW-VRNG)/AMUX;

		double VSIG = ((code/1024.0)*Vscale)+Vzero;
		double T = (VSIG*593.0)-273.0;

		return T;
	}
	
	/**
	 * Convert a battery measurement result to voltage.
	 * 
	 * @param data Battery measurement returned by getBattery().
	 * @return Battery measurement result converted to voltage.
	 */
	public double convertBatteryCodeToVoltage(int data) throws IOException
	{
		double bat = 0.0;
		if(data == 0)
			return bat;

		char btype = readSystemAddress(0x43A);
		btype = (char)((btype>>1)&0x07);
		if(!calib_values_temp)
		{
			long temp32bit = customCmdReadSystemBlock((char)0x0C);
			TV1TRIM_local = (char)(temp32bit&0x1F);
			TV1SET_local  = (char)((temp32bit>>5)&0x07);
			TV2TRIM_local = (char)((temp32bit>>8)&0x1F);
			TV2SET_local  = (char)((temp32bit>>13)&0x07);
			TSMOFF_local  = (char)((temp32bit>>16)&0x7F);
			TSV1G_local   = (char)((temp32bit>>24)&0x01);
			calib_values_temp = true;
		}

		double VSTEP = 50.0/1000.0;
		double V2 = VSTEP*(5.0+1.0+0.0125*TV2TRIM_local);

		double VLOW = V2;
		double VRNG = 0.0;
		double VMUXO = 0.0;
		double AMUX = 1.0;

		double Vzero  = (VLOW-VMUXO)/AMUX;
		double Vscale = (VLOW-VRNG)/AMUX;

		double VSIG = ((data/1024.0)*Vscale)+Vzero;

		if(btype == 0x00)
		{
			// 1.5V battery assumed
			bat = (VSIG*2.688);
		}
		else if(btype == 0x01)
		{
			// 3.0V battery assumed
			bat = (VSIG*5.452);
		}
		else
		{
			// 3.0V battery assumed
			bat = (VSIG*5.452);
		}

		return bat;
	}
	
	/**
	 * Convert an ISO15693 or SL13A error code into an IOException.
	 * 
	 * @param errorCode Flags byte of the ISO15693 response
	 * @throws IOException
	 */
	protected void throwIso15693ErrorException(byte errorCode) throws IOException {
		switch (errorCode) {
		case 0x00: return;
		case 0x01: throw new IOException("Command not supported");
		case 0x02: throw new IOException("Command not recognized");
		case 0x03: throw new IOException("Option not supported");
		case 0x0F: throw new IOException("Unknown error");
		case 0x10: throw new IOException("Block not available");
		case 0x11: throw new IOException("Block already locked");
		case 0x12: throw new IOException("Block already locked");
		case (byte) 0xA0: throw new IOException("Incorrect password");
		case (byte) 0xA1: throw new IOException("Log parameter missing");
		case (byte) 0xA2: throw new IOException("Battery measurement error");
		case (byte) 0xA3: throw new IOException("Temperature measurement error");
		case (byte) 0xA5: throw new IOException("User data area error");
		case (byte) 0xA6: throw new IOException("EEPROM collision");
		default: throw new IOException(String.format("Unkown ISO15693 error: %02x", ((int) errorCode) & 0xFF));
		}
	}

    protected byte[] transceive(byte command) throws IOException {
		byte[] parameter = new byte[0];		
		return transceive(command, parameter);
	}

	protected byte[] transceive(byte command, byte[] parameter) throws IOException {
		/*
		// unaddressed
		byte[] nfcVCommand = new byte[2 + parameter.length];
		//nfcVCommand[0] = 0x02;	// high data-rate, 1 subcarrier
		nfcVCommand[0] = 0x00;		// low data-rate,  1 subcarrier
		//nfcVCommand[0] = 0x03;	// high data-rate, 2 subcarriers
		//nfcVCommand[0] = 0x01;	// low data-rate,  2 subcarriers
		nfcVCommand[1] = command;
		System.arraycopy(parameter, 0, nfcVCommand,2,parameter.length);
		*/

		byte[] nfcVCommand;
		int compare = command;
		if(compare<0)
			compare+=256;
		if(compare >= 0xA0)
		{
			// custom commands, add 0x36 (Manufacturer)

			nfcVCommand = new byte[11 + parameter.length];
			if(data_rate_low)
			{
				//nfcVCommand[0] = 0x22;	// high data-rate, 1 subcarrier
				nfcVCommand[0] = 0x20;      // low data-rate,  1 subcarrier
				//nfcVCommand[0] = 0x23;	// high data-rate, 2 subcarriers
				//nfcVCommand[0] = 0x21;	// low data-rate,  2 subcarriers
			}
			else
			{
				nfcVCommand[0] = 0x22;		// high data-rate, 1 subcarrier
				//nfcVCommand[0] = 0x20;    // low data-rate,  1 subcarrier
				//nfcVCommand[0] = 0x23;	// high data-rate, 2 subcarriers
				//nfcVCommand[0] = 0x21;	// low data-rate,  2 subcarriers
			}
			nfcVCommand[1] = command;
			nfcVCommand[2] = 0x36;
			System.arraycopy(nfcv.getTag().getId(), 0, nfcVCommand, 3, 8);
			System.arraycopy(parameter, 0, nfcVCommand, 11,parameter.length);
		}
		else
		{
			// standard ISO15693 commands

			nfcVCommand = new byte[10 + parameter.length];
			if(data_rate_low)
			{
				//nfcVCommand[0] = 0x22;	// high data-rate, 1 subcarrier
				nfcVCommand[0] = 0x20;      // low data-rate,  1 subcarrier
				//nfcVCommand[0] = 0x23;	// high data-rate, 2 subcarriers
				//nfcVCommand[0] = 0x21;	// low data-rate,  2 subcarriers
			}
			else
			{
				nfcVCommand[0] = 0x22;		// high data-rate, 1 subcarrier
				//nfcVCommand[0] = 0x20;    // low data-rate,  1 subcarrier
				//nfcVCommand[0] = 0x23;	// high data-rate, 2 subcarriers
				//nfcVCommand[0] = 0x21;	// low data-rate,  2 subcarriers
			}
			nfcVCommand[1] = command;
			System.arraycopy(nfcv.getTag().getId(), 0, nfcVCommand, 2, 8);
			System.arraycopy(parameter, 0, nfcVCommand, 10, parameter.length);
		}

		Log.v(TAG, "Transmitting: " + toHexString(nfcVCommand));
		byte result[] = nfcv.transceive(nfcVCommand);
		Log.v(TAG, "Received: " + toHexString(result));

		return result;
	}
	
	protected String toHexString(byte byteArray[]) {
		StringBuilder stringBuilder = new StringBuilder();
		for (byte value : byteArray) {
			stringBuilder.append(String.format("%02X", value));
		}
		
		return stringBuilder.toString();
	}
}
