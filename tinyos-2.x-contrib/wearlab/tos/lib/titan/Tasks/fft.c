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
 * fft.c
 *
 * Original code:
 * @author Mathias Staeger   <staeger@ife.ee.ethz.ch>
 *
 * Adaptation for Titan/TinyOS
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * Performs an FFT. This is used by TitanTaskFFT
 *
 */

#include "scaling.c"

#define FFTSCALE 14               // maximum can be 14 since SineWav and wr, wi defined as int (-32768...32767)
 
#define SWAP(a,b) tempr=(a);(a)=(b);(b)=tempr
 
 /***************************************************************************************
 * LOOKUP TABLE for SineWave
 * Sei FFTSIZE die Länge der Komplexen FFT (d.h. input-array ist 2*FFTSIZE gross)
 * Für FFT wären nur die hälfte der Punkte notwendig, aber für conv2real braucht es alle.
 * Sinus Table = round(2^SCALE * sin(2*pi*k/(2*FFTSIZE)));
 * k = [0,1,2,..,(3/4*(2*FFTSIZE))-1];
 **************************************************************************************/
const int16_t SineWave256[384]={0,201,402,603,804,1005,1205,1406,1606,1806,2006,2205,2404,2603,2801,2999,3196,3393,3590,3786,3981,4176,4370,4563,4756,4948,5139,5330,5520,5708,5897,6084,6270,6455,6639,6823,7005,7186,7366,7545,7723,7900,8076,8250,8423,8595,8765,8935,9102,9269,9434,9598,9760,9921,10080,10238,10394,10549,10702,10853,11003,11151,11297,11442,11585,11727,11866,12004,12140,12274,12406,12537,12665,12792,12916,13039,13160,13279,13395,13510,13623,13733,13842,13949,14053,14155,14256,14354,14449,14543,14635,14724,14811,14896,14978,15059,15137,15213,15286,15357,15426,15493,15557,15619,15679,15736,15791,15843,15893,15941,15986,16029,16069,16107,16143,16176,16207,16235,16261,16284,16305,16324,16340,16353,16364,16373,16379,16383,16384,16383,16379,16373,16364,16353,16340,16324,16305,16284,16261,16235,16207,16176,16143,16107,16069,16029,15986,15941,15893,15843,15791,15736,15679,15619,15557,15493,15426,15357,15286,15213,15137,15059,14978,14896,14811,14724,14635,14543,14449,14354,14256,14155,14053,13949,13842,13733,13623,13510,13395,13279,13160,13039,12916,12792,12665,12537,12406,12274,12140,12004,11866,11727,11585,11442,11297,11151,11003,10853,10702,10549,10394,10238,10080,9921,9760,9598,9434,9269,9102,8935,8765,8595,8423,8250,8076,7900,7723,7545,7366,7186,7005,6823,6639,6455,6270,6084,5897,5708,5520,5330,5139,4948,4756,4563,4370,4176,3981,3786,3590,3393,3196,2999,2801,2603,2404,2205,2006,1806,1606,1406,1205,1005,804,603,402,201,0,-201,-402,-603,-804,-1005,-1205,-1406,-1606,-1806,-2006,-2205,-2404,-2603,-2801,-2999,-3196,-3393,-3590,-3786,-3981,-4176,-4370,-4563,-4756,-4948,-5139,-5330,-5520,-5708,-5897,-6084,-6270,-6455,-6639,-6823,-7005,-7186,-7366,-7545,-7723,-7900,-8076,-8250,-8423,-8595,-8765,-8935,-9102,-9269,-9434,-9598,-9760,-9921,-10080,-10238,-10394,-10549,-10702,-10853,-11003,-11151,-11297,-11442,-11585,-11727,-11866,-12004,-12140,-12274,-12406,-12537,-12665,-12792,-12916,-13039,-13160,-13279,-13395,-13510,-13623,-13733,-13842,-13949,-14053,-14155,-14256,-14354,-14449,-14543,-14635,-14724,-14811,-14896,-14978,-15059,-15137,-15213,-15286,-15357,-15426,-15493,-15557,-15619,-15679,-15736,-15791,-15843,-15893,-15941,-15986,-16029,-16069,-16107,-16143,-16176,-16207,-16235,-16261,-16284,-16305,-16324,-16340,-16353,-16364,-16373,-16379,-16383};   // 256-FFT (complex), SCALE = 14
const int16_t SineWave128[192]={0,402,804,1205,1606,2006,2404,2801,3196,3590,3981,4370,4756,5139,5520,5897,6270,6639,7005,7366,7723,8076,8423,8765,9102,9434,9760,10080,10394,10702,11003,11297,11585,11866,12140,12406,12665,12916,13160,13395,13623,13842,14053,14256,14449,14635,14811,14978,15137,15286,15426,15557,15679,15791,15893,15986,16069,16143,16207,16261,16305,16340,16364,16379,16384,16379,16364,16340,16305,16261,16207,16143,16069,15986,15893,15791,15679,15557,15426,15286,15137,14978,14811,14635,14449,14256,14053,13842,13623,13395,13160,12916,12665,12406,12140,11866,11585,11297,11003,10702,10394,10080,9760,9434,9102,8765,8423,8076,7723,7366,7005,6639,6270,5897,5520,5139,4756,4370,3981,3590,3196,2801,2404,2006,1606,1205,804,402,0,-402,-804,-1205,-1606,-2006,-2404,-2801,-3196,-3590,-3981,-4370,-4756,-5139,-5520,-5897,-6270,-6639,-7005,-7366,-7723,-8076,-8423,-8765,-9102,-9434,-9760,-10080,-10394,-10702,-11003,-11297,-11585,-11866,-12140,-12406,-12665,-12916,-13160,-13395,-13623,-13842,-14053,-14256,-14449,-14635,-14811,-14978,-15137,-15286,-15426,-15557,-15679,-15791,-15893,-15986,-16069,-16143,-16207,-16261,-16305,-16340,-16364,-16379}; // 128-FFT (complex), SCALE=14
const int16_t SineWave64[96]={0,804,1606,2404,3196,3981,4756,5520,6270,7005,7723,8423,9102,9760,10394,11003,11585,12140,12665,13160,13623,14053,14449,14811,15137,15426,15679,15893,16069,16207,16305,16364,16384,16364,16305,16207,16069,15893,15679,15426,15137,14811,14449,14053,13623,13160,12665,12140,11585,11003,10394,9760,9102,8423,7723,7005,6270,5520,4756,3981,3196,2404,1606,804,0,-804,-1606,-2404,-3196,-3981,-4756,-5520,-6270,-7005,-7723,-8423,-9102,-9760,-10394,-11003,-11585,-12140,-12665,-13160,-13623,-14053,-14449,-14811,-15137,-15426,-15679,-15893,-16069,-16207,-16305,-16364};  // 64-FFT (complex), SCALE=14
const int16_t SineWave32[48]={0,1606,3196,4756,6270,7723,9102,10394,11585,12665,13623,14449,15137,15679,16069,16305,16384,16305,16069,15679,15137,14449,13623,12665,11585,10394,9102,7723,6270,4756,3196,1606,0,-1606,-3196,-4756,-6270,-7723,-9102,-10394,-11585,-12665,-13623,-14449,-15137,-15679,-16069,-16305};  // 32-FFT (complex), SCALE=14
const int16_t SineWave16[24]={0,3196,6270,9102,11585,13623,15137,16069,16384,16069,15137,13623,11585,9102,6270,3196,0,-3196,-6270,-9102,-11585,-13623,-15137,-16069}; // 16-FFT (complex), SCALE = 14
const int16_t SineWave8[12]={0,6270,11585,15137,16384,15137,11585,6270,0,-6270,-11585,-15137}; // 8-FFT (complex), SCALE=14
const int16_t SineWave4[6]={0,11585,16384,11585,0,-11585};   // 4-FFT (complex), SCALE = 14
const int16_t SineWave2[3]={0,16384,0};                      // 2-FFT (complex), SCALE = 14

/**************************************************************************
 * Function void scaleDown(long* value, unsigned int scale);
 **************************************************************************/
void scaleDown(long *value, unsigned int scale)
{
    long tempNeg;
    if (0x80000000 & value[0]){    // gleichbedeutend wie: if (value[0] < 0){ aber schneller...
        tempNeg = -value[0];
        tempNeg >>= scale;
        value[0] = -tempNeg;
    }
    else{
        value[0] >>= scale;
    }
}

/**************************************************************************
* Function fixpt32_fft(void);
*
* I don't use f***ing pointers since they don't seem to work in the usual way
*
* Replaces fft_data[1..2*FFTSIZE] by its discrete Fourier transform.
* fft_data is a complex array of length FFTSIZE (even samples real, odd samples
* imaginary part) or, equivalently, a real array of length 2*FFTSIZE.
* FFTSIZE must be an integer power of 2 (this is not checked for!).
* Input fft_data must be scaled by 2^SCALE
* FFTSIZE and LOG2_FFTSIZE need to be defined
***************************************************************************/
void fixpt32_micfft(uint8_t uiLogFFTSize, int32_t *fft_data)
{
    unsigned int mmax,m,j,istep,i;
    long tempr,tempi;
    int  wr, wi;

    unsigned int k    = uiLogFFTSize;
    unsigned int N2   = (1<<(k+1));	// N2=2*2^uiLogFFTSize = size(fft_data)
    unsigned int PI_2 = N2 >> 2;
    unsigned int FFTSIZE = (1<<uiLogFFTSize);

    const int16_t* SineWave = (uiLogFFTSize == 8) ? SineWave256 :
                              (uiLogFFTSize == 7) ? SineWave128 :
					                    (uiLogFFTSize == 6) ? SineWave64  :
					                    (uiLogFFTSize == 5) ? SineWave32  :
					                    (uiLogFFTSize == 4) ? SineWave16  :
					                    (uiLogFFTSize == 3) ? SineWave8   :
					                    (uiLogFFTSize == 2) ? SineWave4   :
					                    (uiLogFFTSize == 1) ? SineWave2   : 
					                                          NULL;
     if ( SineWave == NULL ) return;

    // --------------------------------------------
    // bit-reversal section
    // --------------------------------------------
    #define _FFT_BIT_REVERSE_
    #ifdef _FFT_BIT_REVERSE_
    j = 0;
    for(i=1; i<(FFTSIZE); ++i)
    {
	      m = (FFTSIZE);
        do {
  	    m >>= 1;
        } while(j+m > (FFTSIZE)-1);
        j = (j & (m-1)) + m;
        if(j <= i) continue;

            SWAP(fft_data[j<<1],fft_data[i<<1]); 		// Exchange the two complex numbers (real part)
            SWAP(fft_data[(j<<1)+1],fft_data[(i<<1)+1]);	// Exchange the two complex numbers (imag part)
    }
    #endif // of _FFT_BIT_REVERSE_
    // -------------------------------------------------


    // ---------------------------------------------------------
    // Here begins the Danielson-Lanczos section of the routine.
    // ---------------------------------------------------------
    mmax=2;
    while (N2 > mmax)		// Outer loop executed log2(FFTSIZE) times.
    {
			--k;
			istep=mmax << 1;	// 2*mmax
			for (m=0;m<mmax;m+=2)	// Here are the two nested inner loops.
			{
			    j = (m>>1)<<k;			// j = (m/2)*(2^k)
		      j <<= 1;				// j = j*2, since we need only half of the points
		      wr = SineWave[j+PI_2];
		      wi = -SineWave[j];
		
		      for (i=m;i<N2;i+=istep) // Butterflies
		      {
						j=i+mmax;
						// has to be scaled down, because Sinewave is already scaled
						// beta*W*fft_data[d]/beta
						scaleDown(&fft_data[j], FFTSCALE);
						scaleDown(&fft_data[j+1], FFTSCALE);

						tempr = fft_data[j]*wr   - fft_data[j+1]*wi;// real*wr - imag*wi
				    tempi = fft_data[j+1]*wr + fft_data[j]*wi;	// imag*wr + real*wi
						fft_data[j]   =  fft_data[i]-tempr;
						fft_data[j+1] =  fft_data[i+1]-tempi;
						fft_data[i]   += tempr;
						fft_data[i+1] += tempi;
					} // for: Butterflies
			} // for m<mmax
			mmax=istep;
  } // while N2 > mmax
  
}


/****************************************************
 * Function comp2real(long *data)
 * Input:  fft(data[1..2*FFTSIZE], FFTSIZE) of a
 *         real valued data array
 * Output: reconstructs first 0..FFTSIZE-1 components
 *         of fft(data[1..2*FFTSIZE], 2*FFTSIZE)
 ****************************************************/
void comp2real(uint8_t uiLogFFTSize, int32_t *fft_data)
{
  long   h1r, h1i, h2r, h2i, temp1,temp2;
  int  wr, wi;
  unsigned int i, i1, i2, i3, i4;
    unsigned int FFTSIZE = (1<<uiLogFFTSize);
    unsigned int k    = uiLogFFTSize;
    unsigned int N2   = (1<<(k+1));	// N2=2*2^uiLogFFTSize = size(fft_data)
    unsigned int PI_2 = N2 >> 2;

    const int16_t* SineWave = (uiLogFFTSize == 8) ? SineWave256 :
                              (uiLogFFTSize == 7) ? SineWave128 :
					                    (uiLogFFTSize == 6) ? SineWave64  :
					                    (uiLogFFTSize == 5) ? SineWave32  :
					                    (uiLogFFTSize == 4) ? SineWave16  :
					                    (uiLogFFTSize == 3) ? SineWave8   :
					                    (uiLogFFTSize == 2) ? SineWave4   :
					                    (uiLogFFTSize == 1) ? SineWave2   : 
					                                          NULL;
     if ( SineWave == NULL ) return;

  for (i=1;i<((FFTSIZE) >> 1);i++)		// Case i = 0 done separately below, iterate until N/4
  {
      i1 = (i<<1);				// 2*i
      i2 = i1 + 1;				// i1 + 1
      i3 = ((FFTSIZE)<<1) - i1;	                // 2*FFTSIZE - 2*i
      i4 = i3 + 1;				// i3 + 1

      h1r = fft_data[i1] + fft_data[i3];
      h1i = fft_data[i2] - fft_data[i4];
      h2r =  fft_data[i2] + fft_data[i4];
      h2i = -fft_data[i1] + fft_data[i3];

      wr = SineWave[i+PI_2];
      wi = -SineWave[i];

      // has to be scaled down, because Sinewave is already scaled
      // beta*W*fft_data[d]/beta
      scaleDown(&h2r, FFTSCALE);
      scaleDown(&h2i, FFTSCALE);

      temp1 = wr*h2r;
      temp2 = wi*h2i;
      fft_data[i1] = h1r + temp1 - temp2;
      fft_data[i3] = h1r - temp1 + temp2;
      temp1 = wr*h2i;
      temp2 = wi*h2r;
      fft_data[i2] = h1i + temp1 + temp2;
      fft_data[i4] = -h1i + temp1 + temp2;

      scaleDown(&fft_data[i1],1);                   // factor 0.5
      scaleDown(&fft_data[i2],1);                   // factor 0.5
      scaleDown(&fft_data[i3],1);                   // factor 0.5
      scaleDown(&fft_data[i4],1);                   // factor 0.5
  }
  // case i = 0
  fft_data[0] = fft_data[0] + fft_data[1];
  fft_data[1] = 0;					// we could also fill in F(N/2) here...

  // case i = N/4 = FFTSIZE/2
  i2 = 1+(FFTSIZE);
  fft_data[i2] = -fft_data[i2];
}


/*************************************************************************
* Function void compabs(void)
* Input:  data[1..2*FFTSIZE]
*         complex array with even samples real, odd samples imaginary part
* Output: abs(data)^2 with data2[1...FFTSIZE] containing the result
*         data2[i]=data_real[i]^2 + data_imag[i]^2
* NOTE:   Scaling: if input 2^16 -> output = 0.5*(2*(2^16)^2) = 2^32
*         therefore output is alway scaled by 0.5
**************************************************************************/

uint32_t compabs16(uint16_t arg1, uint16_t arg2)
{
	return arg1*arg1 + arg2*arg2;
}

void compabs(uint8_t uiLogFFTSize, uint16_t* pOut, int32_t *fft_data)
{
    unsigned int FFTSIZE = (1<<uiLogFFTSize);
    unsigned int i, i1;
    unsigned int temp1, temp2;

    for (i = 0; i < FFTSIZE; i++)
    {
        // fft_data[i] = fft_data[i1]*fft_data[i1] + fft_data[i1+1]*fft_data[i1+1]
        // is a bad way, since it uses 32bit multiplication
        // Additionaly the unsigned long output offers one bit more for the following calculations
        // alternative:

      	i1 = i<<1;	  // 2*i
        if (0x80000000 & fft_data[i1]) {    // gleichbedeutend wie: if (fft_data[i1] < 0)
	    temp1 = (unsigned int)-fft_data[i1];
            // // in case overflow occured (if fft_data[i1] == -65536)
            // // MSB_int32_array() prevents this from happening...
            //if (temp1 == 0) {
            //    temp1 = 0xFFFF;
            //}
        } else {
            temp1 = (unsigned int)fft_data[i1];
        }


        i1++;
        if (0x80000000 & fft_data[i1]) {    // gleichbedeutend wie: if (fft_data[i1] < 0)
	    temp2 = (unsigned int)-fft_data[i1];
             // // in case overflow occured (if fft_data[i1] == -65536)
             // // MSB_int32_array() should prevent this from happening, therefore we comment this out...
             //if (temp2 == 0) {
             //    temp2 = 0xFFFF;
             //}
       } else {
            temp2 = (unsigned int)fft_data[i1];
        }

				pOut[i]  = (compabs16(temp1, temp2)>>16); //(unsigned long)(temp1*temp1 + temp2*temp2);

        // do something with SUMEXT... to scale the result...
        //          - if SUMEXT = 1 then Result = 2^32 + fft_data2[i]
        //          - if SUMEXT = 0 then Result = fft_data2[i]
/*//cl        fft_data2[i] >>= 1;          // factor fft_data2[i] = 0.5*fft_data2[i]
        if (SUMEXT == 1)         // then:  fft_data2[i] = 0.5*fft_data2[i] + 0.5*2^32
        {
            fft_data2[i] = fft_data2[i] + 0x80000000;
        }
*/
    }
}
