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

import java.io.FileNotFoundException;
import org.jgap.*;
import org.jgap.impl.DefaultConfiguration;

import titan.ConfigReader;
import titancommon.execution.TaskNetwork;
import titancommon.services.NodeInfo;
import titancommon.services.ServiceDirectory;
import titancommon.compiler.Compiler;

/**
 * THIS IS JUST SOME FIRST TRIAL. NOTHING RUNNABLE IMPLEMENTED YET!
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */

public class EvolutionCompiler implements Compiler {

    Configuration conf = new DefaultConfiguration();
    ServiceDirectory m_ServiceDirectory;
    
    private static final int POPULATION_SIZE = 500;
    private static final int ITERATIONS      = 150;
    
    public EvolutionCompiler(ServiceDirectory sd) {
        m_ServiceDirectory = sd;
    }
    
    public TaskNetwork compileNetwork(TaskNetwork templateNetwork) {

        // check inputs
        if ( m_ServiceDirectory == null ) return null;
        if ( templateNetwork == null ) return null;
        if ( templateNetwork.m_Nodes.size() != 1 ) return null;
        if ( ((TaskNetwork.NodeConfiguration)(templateNetwork.m_Nodes.get(0))).address != -1) return null;
        
        // Get the task network to distribute
        TaskNetwork.NodeConfiguration tnet = (TaskNetwork.NodeConfiguration)(templateNetwork.m_Nodes.get(0));
        
        if (tnet.tasks.length == 0) return null;

        // initialize JGAP library - follow the example on
        // http://jgap.sourceforge.net/doc/tutorial.html
        try {
            
            // set configuration
            Configuration.reset();
            EvolutionFitnessFunction eff = new EvolutionFitnessFunction(m_ServiceDirectory, templateNetwork); 
            conf.setFitnessFunction(eff);

            // construct genes - all possible locations of a task
            Gene[] sampleGenes = new Gene[tnet.tasks.length];
            double searchSpaceSize = 1;
            for(int i=0; i<tnet.tasks.length; i++) {

                int [] allnodes;
                // check whether this is a fixed task
                if (tnet.tasks[i].getAttribute("nodeID") != null) {
                    if (m_ServiceDirectory.hasTask( Integer.parseInt(tnet.tasks[i].getAttribute("nodeID")), tnet.tasks[i].getID()) == false ) {
                        return null;
                    }
                    
                    allnodes = new int[1];
                    allnodes[0] = Integer.parseInt(tnet.tasks[i].getAttribute("nodeID"));
                    
                    //System.out.println("Task "+tnet.tasks[i].getID()+" fixed to node " + tnet.tasks[i].getAttribute("nodeID"));
                }  else {
                
                    NodeInfo[] nis = m_ServiceDirectory.queryTask( tnet.tasks[i].getID() );
    
                    // task must be available somewhere
                    if (nis== null) {
                        return null;
                    }
    
                    //System.out.print("Task "+tnet.tasks[i].getID()+" available on nodes:");
                    allnodes = new int[nis.length];
                    for(int j=0;j<nis.length;j++) {
                        allnodes[j] = nis[j].address;
                        //System.out.print(" "+allnodes[j]);
                    }
                    //System.out.println("");
                }
                
                sampleGenes[i] = new EvolutionNodeGene(conf,allnodes);
                searchSpaceSize *= allnodes.length;
            }
            
            Chromosome sampleChromosome = new Chromosome( conf, sampleGenes );
            
            conf.setSampleChromosome(sampleChromosome );
            conf.setPopulationSize(POPULATION_SIZE);
            
            Genotype population = Genotype.randomInitialGenotype(conf);
            
            String strEvaluations = "";
            for(int i=0; i<ITERATIONS; i++) {
                population.evolve();

                IChromosome bestSolution = population.getFittestChromosome();
                double dResult = eff.evaluate(bestSolution);
                if (dResult > 100000) {
                    dResult = eff.evaluate(bestSolution);
                }
                strEvaluations += " "+(100/eff.evaluate(bestSolution));
            }
            System.out.println("Compiled " + eff.getEvaluations() + " of " + searchSpaceSize + " possible configurations during " + ITERATIONS + " iterations. Best values:" + strEvaluations);

            IChromosome bestSolution = population.getFittestChromosome();
            
            //eff.printChromosome(bestSolution);
            //System.out.println( eff.evaluate(bestSolution) );
            
            return eff.getTaskNetwork(bestSolution);
            

        } catch (Exception e) {
            // Catch any exceptions during compilation
            e.printStackTrace();
        }

        return null;
    }






    public TaskNetwork updateNetwork(TaskNetwork templateNetwork, TaskNetwork currentNetwork) {
        // TODO Work with updates - figure out what went wrong and reevalute network. Here it would be interesting to keep the last population and insert it again.
        return null;
    }
   
    
    

    
    /////////////////////////////////////////////////////////////////////////
    // Test code
    /**
     * @param args
     */
    public static void main(String[] args) {
        // read the test configuration file
        ConfigReader cfgFile;
        try {
            cfgFile = new ConfigReader("titan/test_configreader.txt");
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Could not open configuration file.");
            return;
        }
        
        // get the resulting task network
        TaskNetwork tn = cfgFile.getTaskNetwork();
        
        // open up the service directory
        ServiceDirectory sd = new ServiceDirectory(null);
        int[] testtasks = { 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17 };
//      sd.updateNodeInfo(0, 1, testtasks);
        sd.updateNodeInfo(1, 1, testtasks);
//      sd.updateNodeInfo(2, 1, testtasks);
        sd.updateNodeInfo(3, 1, testtasks);
        sd.updateNodeInfo(4, 1, testtasks);
        sd.updateNodeInfo(5, 1, testtasks);
        sd.updateNodeInfo(6, 1, testtasks);
        sd.updateNodeInfo(7, 1, testtasks);
        sd.updateNodeInfo(8, 1, testtasks);
        
        // compile the network
        try {
            Compiler cmp = new EvolutionCompiler(sd);
            TaskNetwork tno = cmp.compileNetwork(tn);
            if ( tno == null ) {
                System.err.println("ERROR: Compilation failed!");
                return;
            }
            System.out.println("Compiled network onto "+tno.m_Nodes.size()+" motes:\n");
            
            // print configuration
            tno.printTaskNetwork();
            
            // test sending
            tno.configureNetwork(null);

        } catch( Exception e ) {
            System.err.println("ERROR: Compilation failed: reason\""+e.getMessage()+"\"");
        }
        
        
    }

}
