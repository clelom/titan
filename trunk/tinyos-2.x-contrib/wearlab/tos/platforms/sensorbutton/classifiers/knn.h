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

/*****************************************************************************
 * Semester Thesis WSPack 6 2006
 * ETH Zürich, Inst. for Electronics
 *
 * Autor:               Felix Arnold (farnold@ee.ethz.ch)
 * Target:              MSP430F1611 Processor
 * Build Environment:   IAR Embedded Workbench V3.3A/W32
 * File:                knn.h

 * KNN (K-Nearest-Neighbor Classifier)
 *****************************************************************************/


#ifndef _KNN_H_
#define _KNN_H_


#define KNN_K   5
#define KNN_NUMOFPOINTS 1148+164 
#define KNN_NUMOFCLASSES 8
#define KNN_NUMOFFEATURES CALC_NUMOFFEATURES
#define KNN_NUMOFSENSORS CALC_NUMOFSENSORS

//extern const uint16_t knn_datapoints[KNN_NUMOFPOINTS][KNN_NUMOFFEATURES*KNN_NUMOFSENSORS+1];
extern uint16_t insert_best(uint16_t best_distances[], uint16_t best_datapoints[],uint16_t distance,uint16_t no);
extern  uint16_t classify_knn(uint16_t features[]);


#endif //_KNN_H_
