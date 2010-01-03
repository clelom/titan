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
 *
 * @author Andreas Bubenhofer
 * ========================================================================
 */
 


configuration SerialComC
{
  provides interface SBSend;
  provides interface Resource;
}


implementation
{
  components HplMsp430Usart0C;
  components new Msp430Usart0C() as ResourceCmdC;
  
  components SerialCom;
  
  Resource = SerialCom.Resource;
  SBSend = SerialCom.SBSend;
  
  SerialCom.ResourceCmd -> ResourceCmdC;
  SerialCom.UartControl -> HplMsp430Usart0C;
    }
