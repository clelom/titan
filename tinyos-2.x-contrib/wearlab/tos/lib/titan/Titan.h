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
 * Titan.h
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * Packet structure definitions.
 *
 *
 */

#ifndef TITAN_H
#define TITAN_H

#include "message.h"
#include "TitanTaskUIDs.h"

/* ************************************************************************** */
/* Configuration 																															*/

/* Titan Active Message Identifier */
enum {
	 AM_TITANCOMMDATAMSG = 42,
	 AM_TITANCOMMCONFIGMSG =    13
};

#define TITANDEBUG
#ifdef TITANDEBUG
	#define TITAN_TEST_PATTERN 0x55
#endif

/* Titan Configuration Constants */
#define TITAN_TASK_NUM    uniqueCount("Titan")
/* see TitanCommData struct */
#define TITAN_PACKET_SIZE 30
// using maximum packet size  #define TITAN_PACKET_SIZE  (TOSH_DATA_LENGTH - sizeof(uint16_t) - 3*sizeof(uint8_t))

/* ************************************************************************** */

typedef enum TitanType { TT_INT8, TT_UINT8, TT_INT16, TT_UINT16, TT_INT32, TT_UINT32, TT_BUF16} TitanType;
#define TT_INT16_MAX  32767
#define TT_INT16_MIN -32767
#define TT_UINT16_MAX 0x7FFF

/* Packet definition */
typedef struct TitanPacket {
	uint8_t   length; // size of data that has been used
	TitanType type;   // data type
	uint8_t data[ TITAN_PACKET_SIZE ];
	#ifdef TITANDEBUG	
	char cTestPattern;
	#endif
} TitanPacket;


/* ************************************************************************** */
/* Configuration structure 																									  */
#define TITAN_MAX_CONFIG_LENGTH (TOSH_DATA_LENGTH - 5*sizeof(uint16_t)) /*~=22*/

typedef struct TitanTaskConfig {
	uint8_t  taskID;
	TitanTaskUID taskType;
	uint8_t  configLength;
	uint8_t  configData[TITAN_MAX_CONFIG_LENGTH];
	
	// to be filled in by the task
	uint8_t inPorts;
	uint8_t outPorts;
	void*   pTaskData;
} TitanTaskConfig;

/* ************************************************************************** */
/* Error messages, which tasks can send using Titan.issueError()              */
/* Additional custom error codes are also allowed                             */
	#define ERROR_NO_MEMORY          1  /* Could not allocate enough memory */
	#define ERROR_INPUT              2  /* Task without input receives input */
	#define ERROR_MULT_INST          3  /* Some tasks can only be instantiated once */
	#define ERROR_CONFIG             4  /* Configuration format is wrong */
	#define ERROR_SEGMENT_SIZE       5
	#define ERROR_PACKET             6  /* A wrong packet has been received */
	#define ERROR_RECEIVED_SOMETHING 7  /* Packet received at inexistent port */
	#define ERROR_TYPE				       8  /* Packet contains data of invalid type */
  #define ERROR_NOT_IMPLEMENTED    9
  #define ERROR_OUT_FIFO_FULL     10  /* No packet could be obtained on getNextPacket */
  #define ERROR_IN_FIFO_EMPTY     11
  #define ERROR_NO_CONTEXT        12  /* Task did not get context when started */
  #define ERROR_OUTBUF_FULL       13  /* Called by the COMM task to indicate congestion */
  #define ERROR_NO_CACHE_ENTRY    14  /* issued if no cache entry exists with the indicated configuration */
  #define ERROR_CACHE             15  /* Cache could not store the configuration message */

 #endif // TITAN_H
