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
 * Defines the message structures to be used for the RSSI tests.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */


#ifndef TEST_RSSI_H
#define TEST_RSSI_H


enum {
  AM_WVN_RSSI_MEASUREMENT=1,
  AM_WVN_COLLECT=2,
  AM_SERIALMSG =3
  };

enum{AM_WVN_RSSIMSG=AM_SERIALMSG};

typedef nx_struct WVN_RssiMsg {
  nx_uint16_t sender;
  nx_uint16_t receiver;
  nx_uint16_t counter;
  nx_int8_t power_level;
  nx_int8_t rssi_value;
} WVN_RssiMsg;


#endif /* TEST_RSSI_H */
