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

/************************************************************************
* Sensor Power Control
*
* @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
*
* Dummy module to set multiple ports at once. Problem: need to define 
*  combine function for fanout.
*
* Created: 02.06.2008
* Last modified: 02.06.2008
*
************************************************************************/

#include "hardware.h"

module SensorPowerP
{
  provides interface GeneralIO as PS_PRESS;
  uses interface GeneralIO as Port17;
  uses interface GeneralIO as Port20;
  uses interface GeneralIO as Port51;
  uses interface GeneralIO as Port52;
  uses interface GeneralIO as Port53;
}
implementation
{


  async command void PS_PRESS.set() {
    call Port17.set();
    call Port20.set();
    call Port51.set();
    call Port52.set();
    call Port53.set();
  }
  
  async command void PS_PRESS.clr() {
    call Port17.clr();
    call Port20.clr();
    call Port51.clr();
    call Port52.clr();
    call Port53.clr();
  }
  
  async command void PS_PRESS.toggle() {
    call Port17.toggle();
    call Port20.toggle();
    call Port51.toggle();
    call Port52.toggle();
    call Port53.toggle();
  }
  
  async command void PS_PRESS.makeInput() {
    call Port17.makeInput();
    call Port20.makeInput();
    call Port51.makeInput();
    call Port52.makeInput();
    call Port53.makeInput();
  }
  
  async command void PS_PRESS.makeOutput() {
    call Port17.makeOutput();
    call Port20.makeOutput();
    call Port51.makeOutput();
    call Port52.makeOutput();
    call Port53.makeOutput();
  }

  async command bool PS_PRESS.get() {
    call Port17.get();
    call Port20.get();
    call Port51.get();
    call Port52.get();
    call Port53.get();
  }
  

  async command bool PS_PRESS.isInput() {
    bool bResult = TRUE;
    bResult &= call Port17.isInput();
    bResult &= call Port20.isInput();
    bResult &= call Port51.isInput();
    bResult &= call Port52.isInput();
    bResult &= call Port53.isInput();
    return bResult;
  }
  
  async command bool PS_PRESS.isOutput() {
    bool bResult = TRUE;
    bResult &= call Port17.isOutput();
    bResult &= call Port20.isOutput();
    bResult &= call Port51.isOutput();
    bResult &= call Port52.isOutput();
    bResult &= call Port53.isOutput();
    return bResult;
  }

}
