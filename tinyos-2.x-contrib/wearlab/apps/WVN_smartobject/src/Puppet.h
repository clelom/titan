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
 * PuppetApp
 * 
 * This is a test application for the puppet. It will check how the reading works
 * 
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */
 
 
#ifndef PUPPET_H
#define PUPPET_H

#define MAX_NUM_SMART_OBJECTS 10

enum{ AM_PUPPETMSG = 13, AM_IDMSG = 15 };

typedef nx_struct IDMsg {
  nx_uint8_t uiIdentifier;
  nx_uint8_t strName[16];
  nx_uint8_t strVerb[10];
} IDMsg;

typedef nx_struct PuppetMsg {
  nx_uint8_t uiIdentifier;
  nx_uint8_t strMessage[26];
} PuppetMsg;

// probability distribution
#define DMAX 10
#define DMIN -10
#define PMAX 200 /* that's 78% of 255*/

#endif /* PUPPET_H */
