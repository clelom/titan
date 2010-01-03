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

/*
 * Serial communication structure on the SensorButton
 *
 * @author Christoph Walser
 */

module SensorbuttonSerialP {
  provides interface StdControl;
  provides interface Msp430UartConfigure;
  uses interface Resource;
}
implementation {
  
  msp430_uart_config_t msp430_uart_sensorbutton_config = { 
              ubr: UBR_1MHZ_115200, 
              umctl: UMCTL_1MHZ_115200, 
              ssel: 0x02, 
              pena: 0, 
              pev: 0, 
              spb: 0, 
              clen: 1, 
              listen: 0, 
              mm: 0, 
              ckpl: 0, 
              urxse: 0, 
              urxeie: 1, 
              urxwie: 0};

  command error_t StdControl.start(){
    return call Resource.immediateRequest();
  }
  command error_t StdControl.stop(){
    call Resource.release();
    return SUCCESS;
  }
  event void Resource.granted(){}

  async command msp430_uart_config_t* Msp430UartConfigure.getConfig() {
    return &msp430_uart_sensorbutton_config;
  }
  
}
