/*
    This file is part of Titan.

    Titan is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Titan is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Titan. If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * TitanComm.h
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * Packet structure definitions.
 *
 *
 */

#ifndef TITANCOMM_H
#define TITANCOMM_H

#include "Titan.h"
#include "TitanTaskUIDs.h"

#define TITAN_CFGMSG_RETRIES 6
#define TITAN_CFGMSG_RETRYDELAY 50
#define TITAN_DATAMSG_RETRIES 3
#define TITAN_DATAMSG_RETRYDELAY 50

//#if (TOSH_DATA_LENGTH < TITAN_PACKET_SIZE)
//  #error TitanComm does not support packets bigger than the active message payload!
//#endif

////////////////////////////////////////////////////////////////////////////////
// This is for version 1
enum { TITANCOMM_VERSION=1, };

// message types (lower 4 bits of TitanCommConfigMsg.type)
// ATTENTION: This must be consistent with TitanCommand.java
enum{
	 TITANCOMM_CONFIG	    = 0,
   TITANCOMM_CFGTASK    = 1,
   TITANCOMM_CFGCONN    = 2,
   TITANCOMM_CFGSUCC    = 3,
   TITANCOMM_DISCOVER   = 4,
   TITANCOMM_DISC_REP   = 5,
   TITANCOMM_FORWARD    = 6,
   TITANCOMM_DATAMSG    = 7,
   TITANCOMM_ERROR      = 8,
   TITANCOMM_CFGSTART   = 9,
   TITANCOMM_CACHE_START=10,
   TITANCOMM_CACHE_STORE=11,
};

typedef nx_struct TitanCommDataMsg {
//	nx_uint8_t  destNodeH; /* If this is a uint16_t the struct size is 30 Byte! */
//	nx_uint8_t  destNodeL;
	nx_uint8_t  destPort;  /* port of the local communication module */
	nx_uint8_t  dataLength;
	nx_uint8_t  data[0];
} TitanCommDataMsg;

#define TCC_DATALENGTH (TOSH_DATA_LENGTH - sizeof(uint8_t) )

////////////////////////////////////////////////////////////////////////////////
// Configuration message structure
//
// IMPORTANT: do not use the array descriptors, as the compiler does not arrange 
//            the values right!

#if TitanTaskUID != uint16_t
  #warning TitanTaskUID is not uint16_t
#endif

typedef nx_struct TCCTaskDesc {
	nx_uint8_t  taskIDH; // most significant byte
	nx_uint8_t  taskIDL; // least significant byte
	nx_uint8_t  runID;
	nx_uint8_t  configLength; // size of configData structure
	nx_uint8_t  configData[0];
} TCCTaskDesc;

typedef nx_struct TCCConnection {
	nx_uint8_t sourceRunID;
	nx_uint8_t sourcePortID;
	nx_uint8_t destRunID;
	nx_uint8_t destPortID;
} TCCConnection;

typedef nx_struct TCCTaskMsg {
  nx_uint8_t configID : 4;
  nx_uint8_t numTasks : 4; 
  /*	nx_uint8_t configIDNumTasks; */
	TCCTaskDesc tasks[0];
} __attribute__ ((packed)) TCCTaskMsg;

typedef nx_struct TCCConnMsg {
  nx_uint8_t configID : 4;
  nx_uint8_t numConns : 4; 
  /*	nx_uint8_t configIDNumConns; */
	TCCConnection connections[0];
} __attribute__ ((packed)) TCCConnMsg;


typedef nx_struct  TCCConfigMsg {
  nx_uint8_t delayedReconfig : 1; // immediately reconfigure after complete configuration data
	nx_uint8_t numTasks : 7;
	nx_uint8_t numConnections;
	nx_uint8_t masterIDH;
	nx_uint8_t masterIDL;
	TCCTaskMsg taskMsg[0];
} __attribute__ ((packed)) TCCConfigMsg;

typedef nx_struct TCCCfgStartMsg {
  nx_uint8_t masterIDH;
  nx_uint8_t masterIDL;
  nx_uint8_t configID;
} TCCCfgStartMsg;





////////////////////////////////////////////////////////////////////////////////
// Sending back ACK to signal configuration success

typedef nx_struct TCCfgSuccess {
	nx_uint8_t configID;
	nx_uint8_t nodeIDH;
	nx_uint8_t nodeIDL;
} TCCfgSuccess;

////////////////////////////////////////////////////////////////////////////////
// Service discovery

enum { TCD_SCOPE_ALL, TCD_SCOPE_TASKS, TCD_SCOPE_ENVIRONMENT, };

typedef nx_struct TCDiscover {
	nx_uint8_t sourceIDH;
	nx_uint8_t sourceIDL;
	nx_uint8_t scope;
} TCDiscover;

typedef nx_struct TCDiscoverRep {
	nx_uint8_t nodeIDH;
	nx_uint8_t nodeIDL;
	nx_uint8_t numTasks;
 nx_uint16_t tasks[0];	
}TCDiscoverRep;

////////////////////////////////////////////////////////////////////////////////
// Forwarding

typedef nx_struct TCForwardMsg {
	nx_uint8_t  hops;
	nx_uint8_t  length;
	nx_uint16_t nodeIDs[0];
} TCForwardMsg;

////////////////////////////////////////////////////////////////////////////////
// Service discovery

enum {AM_TCCMSG = AM_TITANCOMMCONFIGMSG}; // for MIG - together with content member

typedef nx_struct TCCMsg {
	nx_uint8_t verType;
//	nx_uint8_t content[0]; // uncomment this for MIG generation
//	nx_union msg { TCCConfigMsg config; TCCTaskMsg tasks; TCCConnMsg conns; 
//              TCDiscover disc; TCCfgSuccess succ; TCForwardMsg fwd; } msg[0];
} TCCMsg;

////////////////////////////////////////////////////////////////////////////////
// Error report

typedef nx_struct TCErrorMsg {
  nx_uint16_t nodeID;
  nx_uint8_t  configID;
  nx_uint8_t  errSource;
  nx_uint8_t  errType;
} TCErrorMsg;

////////////////////////////////////////////////////////////////////////////////
// Internal configuration structures

enum { TC_MAX_CONNECTIONS=10 };

typedef struct TCConnMap {
	uint8_t nodeIDH;
	uint8_t nodeIDL;
	uint8_t portID;
} TCConnMap;

//#define SERIALTEST
//#ifdef SERIALTEST

enum { AM_SERIALMSG=66 };

typedef nx_struct SerialMsg {
  nx_uint8_t  length;
  nx_uint16_t address;
  nx_uint8_t  rssi;
  nx_uint8_t  data[100];
} SerialMsg;

//#endif

#endif /*TITANCOMM_H*/
