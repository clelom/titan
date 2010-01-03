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
 * TitanC.nc
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * This is the main component file for the Tiny Task Network (Titan).
 *
 *
 */
 
#include "TitanInternal.h"
 
 configuration TitanC {
	provides interface Init;
 	uses interface Leds;
	uses interface Boot;
 	
 }
 
 implementation {
 
 	// core component instantiation
 	components TitanM, TitanMemoryM, TitanCommM, TitanConfigM, TitanSchedulerM, TitanConfigCacheM;

  ////////////////////////////////////////////////////////////////////////////
	// RF bindings

	components new AMSenderC(AM_TITANCOMMCONFIGMSG)   as ConfigSender;
	components new AMReceiverC(AM_TITANCOMMCONFIGMSG) as ConfigReceiver;
  components new AMSnooperC(AM_TITANCOMMCONFIGMSG)  as ConfigSnooper;
	components new AMSenderC(AM_TITANCOMMDATAMSG)     as DataSender;
	components new AMSenderC(AM_TITANCOMMDATAMSG)     as DataForwarder;
	components new AMReceiverC(AM_TITANCOMMDATAMSG)   as DataReceiver;
	components new AMSnooperC(AM_TITANCOMMDATAMSG)    as DataSnooper;
	
  ////////////////////////////////////////////////////////////////////////////
  // Platform specific parts

#ifdef TITAN_COMM_CHANNEL
#ifndef TOSSIM
  components CC2420ControlC,TitanConfigPlatformM;
  TitanConfigPlatformM.CC2420Config -> CC2420ControlC;
  Boot = TitanConfigPlatformM.Boot;
#endif
#endif

#if defined(SWITCHMODULE)
  components SensorPowerC;
#endif
 

#ifndef TOSSIM
  components CC2420ActiveMessageC as ActiveMessageC;
  components SerialActiveMessageC, TitanSerialM;
  components RandomC;
  TitanSerialM.Boot = Boot;
  TitanSerialM.Control        -> SerialActiveMessageC;
  TitanSerialM.SerialReceive  -> SerialActiveMessageC.Receive[AM_SERIALMSG];
  TitanSerialM.SerialSend     -> SerialActiveMessageC.AMSend[AM_SERIALMSG];
  TitanSerialM.SerialPacket   -> SerialActiveMessageC.Packet;
  TitanSerialM.SerialAMPacket -> SerialActiveMessageC.AMPacket;
  TitanSerialM.PacketLink     -> ActiveMessageC.PacketLink;
  TitanSerialM.Random -> RandomC.Random;
  TitanCommM.Random   -> RandomC.Random;
  TitanConfigM.Random -> RandomC.Random;
  
  TitanConfigM.SerialPacket-> SerialActiveMessageC;
  TitanCommM.SerialPacket  -> SerialActiveMessageC;
  TitanConfigM.PacketLink  -> ActiveMessageC.PacketLink;
  TitanCommM.PacketLink    -> ActiveMessageC.PacketLink;
  TitanCommM.CC2420Packet  -> ActiveMessageC;
  TitanConfigM.CC2420Packet-> ActiveMessageC;
#else
  // Simulation version - uses injected AM packets to receive data over the 
  // serial port: this is a hack
	components ActiveMessageC;
  components TitanSerialM;
  components new AMReceiverC(AM_SERIALMSG) as SerialReceiver;
  components new AMSenderC(AM_SERIALMSG) as SerialSender;
  TitanSerialM.Boot        =  Boot;
  TitanSerialM.Control     -> ActiveMessageC;
  TitanSerialM.SerialReceive  -> SerialReceiver.Receive;
  TitanSerialM.SerialSend     -> SerialSender.AMSend;
  TitanSerialM.SerialPacket   -> ActiveMessageC.Packet;
  TitanSerialM.SerialAMPacket -> ActiveMessageC.AMPacket;

  TitanConfigM.SerialPacket  -> ActiveMessageC.Packet;
  TitanCommM.SerialPacket    -> ActiveMessageC.Packet;
#endif
  
  ////////////////////////////////////////////////////////////////////////////
  // serial communication

  TitanSerialM.RFControl    -> ActiveMessageC;
  TitanSerialM.RFSendCfg    -> ConfigSender;
  TitanSerialM.RFSendData   -> DataForwarder;
  TitanSerialM.RFAMPacket   -> ConfigSender;
  TitanSerialM.RFPacket     -> ConfigSender;
  
  TitanConfigM.TitanSerialBufferSend    -> TitanSerialM.CFG_BufferSend;
  TitanConfigM.LocalReceive  -> TitanSerialM.LocalCfgForward;
  TitanCommM.LocalReceive    -> TitanSerialM.LocalDataForward;

  //TitanCommM.SerialSend    -> TitanSerialM.AMSend; /*TitanSerialM;*/
  TitanCommM.TitanSerialBufferSend    -> TitanSerialM.DAT_BufferSend;

  ////////////////////////////////////////////////////////////////////////////
  // configuration communication

  TitanConfigM.AMControl     -> ActiveMessageC;
  TitanConfigM.AMSend        -> ConfigSender;
  TitanConfigM.AMPacket      -> ConfigSender;
  TitanConfigM.Packet        -> ConfigSender;
  TitanConfigM.Receive       -> ConfigReceiver;
  TitanConfigM.Snoop         -> ConfigSnooper;

  ////////////////////////////////////////////////////////////////////////////
  // data communication

  TitanCommM.AMControl     -> ActiveMessageC;
  TitanCommM.AMSend        -> DataSender;
  TitanCommM.AMPacket      -> DataSender;
  TitanCommM.Packet        -> DataSender;
  TitanCommM.Receive       -> DataReceiver;
  TitanCommM.Snoop         -> DataSnooper;
  
  ////////////////////////////////////////////////////////////////////////////
  // LED bindings
  
  TitanCommM.Leds   = Leds;
  TitanSerialM.Leds = Leds;
 	TitanM.Leds       = Leds;
  TitanConfigM.Leds = Leds;

  ////////////////////////////////////////////////////////////////////////////
  // Init bindings
	
  Init = TitanM;
  Init = TitanConfigM;

  TitanConfigM.Boot = Boot;

  ////////////////////////////////////////////////////////////////////////////
  // Module interconnnections
	
  TitanConfigM.TitanConfigCache -> TitanConfigCacheM;
 	TitanConfigM.TitanConfigure   -> TitanM;

 	TitanM.TitanMemory   -> TitanMemoryM;
	TitanM.TitanSchedule -> TitanSchedulerM;
 	TitanCommM.Titan -> TitanM.Titan[unique("Titan")];
  
  ////////////////////////////////////////////////////////////////////////////
  // Begin Task bindings - (un-)comment any line to include/exclude a task
  components TitanTaskSimpleWriter,new TimerMilliC() as TimerTSW; 	TitanTaskSimpleWriter ->TitanM.Titan[unique("Titan")]; TitanTaskSimpleWriter.Timer -> TimerTSW;
  components TitanTaskSink;          TitanTaskSink         -> TitanM.Titan[unique("Titan")];
  components TitanTaskDuplicator;    TitanTaskDuplicator   -> TitanM.Titan[unique("Titan")];
  components TitanTaskMerge;         TitanTaskMerge        -> TitanM.Titan[unique("Titan")];
  components TitanTaskLEDs;          TitanTaskLEDs         -> TitanM.Titan[unique("Titan")]; TitanTaskLEDs.Leds = Leds;
  components TitanTaskMagnitude;     TitanTaskMagnitude    -> TitanM.Titan[unique("Titan")];

  components TitanTaskSum;           TitanTaskSum          -> TitanM.Titan[unique("Titan")];
  components TitanTaskAverage;       TitanTaskAverage      -> TitanM.Titan[unique("Titan")];
  components TitanTaskVariance;      TitanTaskVariance     -> TitanM.Titan[unique("Titan")];
  components TitanTaskMin;           TitanTaskMin          -> TitanM.Titan[unique("Titan")];
  components TitanTaskMax;           TitanTaskMax          -> TitanM.Titan[unique("Titan")];
  components TitanTaskMean;          TitanTaskMean         -> TitanM.Titan[unique("Titan")];
  components TitanTaskZeroCross;     TitanTaskZeroCross    -> TitanM.Titan[unique("Titan")];
  components TitanTaskNumAvg;        TitanTaskNumAvg       -> TitanM.Titan[unique("Titan")];
  components TitanTaskCovariance;    TitanTaskCovariance   -> TitanM.Titan[unique("Titan")];

//  components TitanTaskFFT;           TitanTaskFFT          -> TitanM.Titan[unique("Titan")];
//  components TitanTaskFBandEnergy;   TitanTaskFBandEnergy  -> TitanM.Titan[unique("Titan")];

  components TitanTaskTransDetect;   TitanTaskTransDetect  -> TitanM.Titan[unique("Titan")];
  components TitanTaskThreshold;     TitanTaskThreshold    -> TitanM.Titan[unique("Titan")];
  components TitanTaskSynchronizer;  TitanTaskSynchronizer -> TitanM.Titan[unique("Titan")];

  components TitanTaskKNN;           TitanTaskKNN          -> TitanM.Titan[unique("Titan")];
  components TitanTaskSimilaritySearch; TitanTaskSimilaritySearch -> TitanM.Titan[unique("Titan")];
  components TitanTaskDecisionTree;  TitanTaskDecisionTree -> TitanM.Titan[unique("Titan")];
  components TitanTaskFuzzy;         TitanTaskFuzzy        -> TitanM.Titan[unique("Titan")];
  
  // device dependant tasks
  #if defined(VOICEMODULE) || defined(SWITCHMODULE) || defined(SENSORBUTTON) || defined(TELOSB) || defined(TOSSIM)
    components TitanTaskAccelerometerC; TitanTaskAccelerometerC -> TitanM.Titan[unique("Titan")]; 
  #endif
  
  #if defined(SWITCHMODULE)
    // Resource sharing between voice task and pressure sensor does not really work - must be solved before using it!
    components TitanTaskLightSensor, new LightC() as LightSensor, new TimerMilliC() as TimerLight; TitanTaskLightSensor -> TitanM.Titan[unique("Titan")]; TitanTaskLightSensor.Light -> LightSensor.Read; TitanTaskLightSensor.PS_LIGHT -> SensorPowerC.PS_LIGHT; TitanTaskLightSensor.Timer -> TimerLight; TitanTaskLightSensor.Leds = Leds;
	
	// CL:CAUTION: USING THE PRESSURE SENSOR OR THE SWITCH MAY HARM WIRELESS COMMUNICATION!!!
//    components TitanTaskPressureSensor, new PressureC() as PressureSensor, new TimerMilliC() as TimerPressure; TitanTaskPressureSensor -> TitanM.Titan[unique("Titan")]; TitanTaskPressureSensor.PressInit -> PressureSensor.Init; TitanTaskPressureSensor.Press -> PressureSensor.Pressure; TitanTaskPressureSensor.Temp -> PressureSensor.Temperature; TitanTaskPressureSensor.PS_PRESS -> SensorPowerC.PS_PRESS; TitanTaskPressureSensor.Timer -> TimerPressure; TitanTaskPressureSensor.Leds = Leds;
//    components TitanTaskSwitch, SwitchC as Switch; TitanTaskSwitch -> TitanM.Titan[unique("Titan")]; TitanTaskSwitch.Switch -> Switch;
  #endif
    
  #ifdef VOICEMODULE
    components TitanTaskVoice, new VoiceSynthC() as Voice; TitanTaskVoice -> TitanM.Titan[unique("Titan")]; TitanTaskVoice.VStamp -> Voice; components new TimerMilliC() as TimerVoice; TitanTaskVoice.Timer -> TimerVoice; TitanTaskVoice.Leds = Leds;
  #endif
  
  // End Task bindings
  ////////////////////////////////////////////////////////////////////////////

 }
