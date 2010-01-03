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
 * scaling.c
 *
 * @author Mathias Staeger   <staeger@ife.ee.ethz.ch>
 *
 * Performs an FFT. This is used by TitanTaskFFT
 *
 */

#define uint16 uint16_t
#define uint32 uint32_t
#define int16 int16_t
#define int32 int32_t

/************************************************************************************
* Function uint16 MSB_int32_array(int32 *array, uint16 arraysize)
*
* Input:  pointer to int32 array (signed: from -2^31 .. +2^31 -1)
* Output: MSB of highest absolute value in the array
*         -> most significant bit is at position MSB (start with position 0)
*         -> absolute value of all numbers in the array is smaller than 2^(MSB+1) - 1
*************************************************************************************/
uint16 MSB_int32_array(int32 *array, uint16 arraysize)
{
    uint16 i;
    int32 temp, max=0;

    // first: find the "maximum" value
    // the statement "max |= temp" doesn't actually find the maximum
    // but the MSB of max will be at the same place as the real maximum would be
    for (i=0; i<arraysize; i++)
    {
        temp = array[i];
        if (0x80000000 & temp){      // if temp < 0
            temp = -temp;
        }
        max |= temp;
    }

    // second: find MSB of max
    i = 30;
    temp = 0x40000000;      // highest position for MSB (= 2^i)
    while (i > 0){
        if (temp & max){
            return i;
        }
        temp >>= 1;
        i--;
    }
    return 0;
}


/*************************************************************************************
* Function uint16 MSB_uint32_array(uint32 *array, uint16 arraysize)
*
* Input:  pointer to uint32 array (from 0 .. +2^32 -1)
* Output: MSB of highest absolute value in the array
*         -> most significant bit is at position MSB (start with position 0)
*         -> absolute value of all numbers in the array is smaller than 2^(MSB+1) - 1
**************************************************************************************/
uint16 MSB_uint32_array(uint32 *array, uint16 arraysize)
{
    uint16 i;
    uint32 temp, max=0;

    // first: find the "maximum" value
    // the statement "max |= temp" doesn't actually find the maximum
    // but the MSB of max will be at the same place as the real maximum would be

    for (i=0; i<arraysize; i++)
    {
        max |= array[i];
    }

    // second: find MSB of max
    i = 31;
    temp = 0x80000000;      // highest position for MSB (= 2^i)
    while (i > 0){
        if (temp & max){
            return i;
        }
        temp >>= 1;
        i--;
    }
    return 0;
}



/*****************************************************************************************
* Function int16 adapt_int32_array(int32 *array, uint16 arraysize, uint16 maxMSB)
*
* Input:  *array:      pointer to int32 array (signed: from -2^31 .. +2^31 -1)
*         arraysize:   length of array
*         maxMSBinput: position of maximal allowed MSB (start with position 0)
* Ouput:  scaleFactor: input array was scaled by 2^|scaleFactor|
*                      scale DOWN if scaleFactor < 0
*                      scale UP   if scaleFactor > 0
*****************************************************************************************/
int16 adapt_int32_array(int32 *array, uint16 arraysize, uint16 maxMSB)
{
      uint16 i, msb;
      int16 scaleFactor = 0;
      int32 temp;

      msb = MSB_int32_array(array,arraysize);
      scaleFactor = maxMSB - msb;

      if(scaleFactor < 0)       // scale DOWN
      {
          scaleFactor = -scaleFactor;
          for (i=0; i<arraysize; i++) {
              if (array[i] < 0){
                  temp = -array[i];
                  temp >>= scaleFactor;
                  array[i] = -temp;
              } else {
                  array[i] >>= scaleFactor;
              }
          }
          scaleFactor = -scaleFactor;
      } else if (scaleFactor > 0) // scale UP
      {
          for (i=0; i<arraysize; i++) {
              if (array[i] < 0){
                  temp = -array[i];
                  temp <<= scaleFactor;
                  array[i] = -temp;
              } else {
                  array[i] <<= scaleFactor;
              }
          }
      }
      return scaleFactor;
}


/*********************************************************************************************
* Function int16 adapt_uint32_array(uint32 *array, uint16 arraysize, uint16 maxMSB)
*
* Input:  *array:      pointer to uint32 array (from 0 .. +2^32 -1)
*         arraysize:   length of array
*         maxMSBinput: position of maximal allowed MSB (start with position 0)
* Ouput:  scaleFactor: input array was scaled by 2^|scaleFactor|
*                      scale DOWN if scaleFactor < 0
*                      scale UP   if scaleFactor > 0
*********************************************************************************************/
int16 adapt_uint32_array(uint32 *array, uint16 arraysize, uint16 maxMSB)
{
      uint16 i, msb;
      int16 scaleFactor = 0;

      msb = MSB_uint32_array(array,arraysize);
      scaleFactor = maxMSB - msb;

      if(scaleFactor < 0)       // scale DOWN
      {
          scaleFactor = -scaleFactor;
          for (i=0; i<arraysize; i++)
          {
              array[i] >>= scaleFactor;
          }
          scaleFactor = -scaleFactor;
      } else if (scaleFactor > 0) // scale UP
      {
          for (i=0; i<arraysize; i++)
          {
              array[i] <<= scaleFactor;
          }
      }
      return scaleFactor;
}
