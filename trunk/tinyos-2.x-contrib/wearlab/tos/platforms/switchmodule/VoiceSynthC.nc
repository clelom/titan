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

/************************************************************************************
* Actuator Driver
*
* @author Andreas Breitenmoser
*
* Wrapper for the V-Stamp.
* It's a common naming layer atop a HIL. 
*
* Created: 16.12.2007
* Last modified: 11.01.2008
*
************************************************************************************/

generic configuration VoiceSynthC()
{
  provides
  {
	interface VStamp;
	// interface Init;
	// interface AMSend as TransmitData;
	// interface Receive as ReceiveData;
  }
}
implementation
{
  components new VStampC();
  
  VStamp = VStampC;
  // Init = VStampC.Init;
  // TransmitData = VStampC.Send;
  // ReceiveData = VStampC.Receive;
}
