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
  * TitanTaskAccelerometer.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  * @author Andreas Breitenmoser
  *
  * The accelerometer samples an accelerometer residing on an extension board.
  */


configuration TitanTaskAccelerometerC
{
	uses interface Titan;
}
implementation
{

#if defined(VOICEMODULE) || defined(SWITCHMODULE)
  components TSOAccelerometerPPSM as Task;
  components new AccelerationC();
  components SensorPowerC;
  components new TimerMilliC() as Timer;
  Task.Read -> AccelerationC.Read;
  Task.PS_ACC -> SensorPowerC.PS_ACC;
#elif defined(TELOSB)
  components TSOAccelerometerPPSM as Task;
  components new AccelerationC();
  components new TimerMilliC() as Timer;
  Task.Read -> AccelerationC.Read;
#elif defined(SENSORBUTTON)
  components TAAccelerometerM as Task;
  components new ACCSensorC();
  components PowerSwitchC;
  components new TimerMilliC() as Timer;
  Task.Read  -> ACCSensorC;
  Task.ACC   -> PowerSwitchC.ACC;
#elif defined(TOSSIM)
  components TossimAccelerometerP as Task;
  components new TimerMilliC() as Timer;
#endif

  Task.Titan = Titan;
  Task.Timer -> Timer;
}
