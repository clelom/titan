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
 * Classifier for Sensorbutton
 * provides  knn classification
 * @author Andreas Bubenhofer
 * 
 */





module KnnClassifier {
   provides interface Classifier;
}
 
implementation {

#include "knn2.c"
uint16_t classification;
uint16_t* buffer;
task void calculation();

  command void Classifier.startClassifier( uint16_t* features)
  {
     buffer = features;
	 post calculation();
  }

  task void calculation(){
     classification = classify_knn( buffer );
     signal Classifier.classifierDone( classification );
  }
  
  default event void Classifier.classifierDone( uint8_t classno)
  {}
   
   

   

	
}
