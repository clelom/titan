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

package titan.compiler;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

import titancommon.compiler.*;
import titancommon.execution.TaskNetwork;
import titancommon.services.ServiceDirectory;

/**
 * The fitness function implementation for the genetic algorithm used by the 
 * EvolutionCompiler.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */

public class EvolutionFitnessFunction extends FitnessFunction {

	// required for serialization
	private static final long serialVersionUID = 1L;
	
	protected ServiceDirectory m_ServiceDirectory;
    protected TaskNetwork m_TaskNetwork;
    protected TaskNetworkConverter m_TNC;
    protected NetworkEvaluator m_NetworkEvaluator;
    protected int m_iEvaluationCount = 0;
    
    EvolutionFitnessFunction(ServiceDirectory sd, TaskNetwork tn) {
        m_ServiceDirectory = sd;
        m_TaskNetwork = tn;
        m_TNC = new TaskNetworkConverter(sd);
        m_NetworkEvaluator = new NetworkEvaluator(sd);
    }
    
    /**
     * Returns the fitness of the chromosome, the higher, the better!
     */
    protected double evaluate(IChromosome tasknet) {
        
    	m_iEvaluationCount++;

        TaskNetwork tn = getTaskNetwork(tasknet);
        double dResult = m_NetworkEvaluator.evaluateNet(tn);
        
        if ( dResult > 10e100 ) {
        	//System.out.println("Too high value");
        	return 0;
        }

        //The GA algorithm searches the maximum value - invert the value
        return (dResult>=Double.MAX_VALUE)? 0 : 100/dResult;
    }
    
    public TaskNetwork getTaskNetwork(IChromosome tasknet) {

        // copy genes to task vector, ignoring fixed tasks
        int [] taskvector = new int[tasknet.getGenes().length];
        for ( int i=0; i<taskvector.length; i++ ) {
            taskvector[i] = (Integer)tasknet.getGene(i).getAllele();
        }
        
        try{

            return m_TNC.VectorToTaskNetwork(m_TaskNetwork,taskvector);
            

        } catch(Exception e) {
            // an error indicates there is something wrong with the 
            // vector or the task network
            System.err.println("ERROR: Gene configuration is wrong! ("+e.getMessage()+")");
            return null;
        }
        
    }
    
    public void printChromosome(IChromosome tasknet) {
    	System.out.print("Chromosome: [");
        for ( int i=0; i<tasknet.getGenes().length; i++ ) {
        	System.out.print(" "+(Integer)tasknet.getGene(i).getAllele());
        }
    	System.out.println(" ]");
    }
    
    /** Returns the number of evaluations performed
     */
    public int getEvaluations() {
    	return m_iEvaluationCount;
    }

}
