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

import java.io.Serializable;
import org.jgap.*;

/**
 * EvolutionNodeGene
 *
 * Represents a task graph distribution onto the network.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */

public class EvolutionNodeGene extends BaseGene implements Gene, Serializable {

    private static final long serialVersionUID = 1L;
    private int[] m_nodes;
    private int m_curNodeIndex;
    
    public EvolutionNodeGene(Configuration conf, int[] nodes) throws InvalidConfigurationException {
        super(conf);
        m_nodes = nodes;
        m_curNodeIndex = 0;
    }
    
    protected Object getInternalValue() {
        if ( m_curNodeIndex < 0 || m_nodes.length <= m_curNodeIndex ) {
            System.err.println("node index invalid: "+m_curNodeIndex);
        }
        return m_nodes[m_curNodeIndex];
    }

    protected Gene newGeneInternal() {
        try {
            return new EvolutionNodeGene(this.getConfiguration(),m_nodes);
        } catch (InvalidConfigurationException e) {
            // Don't work with infeasible configurations
            e.printStackTrace();
            return null;
        }
    }

    public void setAllele(Object arg) {
        Integer nodeAddr = (Integer)arg;
        for (int i=0;i<m_nodes.length;i++) {
            if (m_nodes[i] == nodeAddr) {
                m_curNodeIndex = i;
                return;
            }
        }
        // not found? -> don't change
    }

    public String getPersistentRepresentation()
            throws UnsupportedOperationException {
        return m_nodes[m_curNodeIndex]+"";
    }

    public void setValueFromPersistentRepresentation(String arg0)
            throws UnsupportedOperationException,
            UnsupportedRepresentationException {
        try {
            setAllele(Integer.parseInt(arg0));
        } catch(NumberFormatException nfe) {
            throw new UnsupportedRepresentationException("Unknown node");
        }

    }

    public void setToRandomValue(RandomGenerator rg) {
        if ( m_nodes.length > 1 ) {
            m_curNodeIndex = rg.nextInt(m_nodes.length-1);
        } else {
            m_curNodeIndex = 0;
        }

    }

    public void applyMutation(int index, double percentage) {
        
/*        if ( Math.abs(percentage) > 0.2) {
            m_curNodeIndex += (percentage>0)? 1:-1;
            if(m_curNodeIndex < 0 ) m_curNodeIndex=m_nodes.length-1;
            else if(m_curNodeIndex>=m_nodes.length) m_curNodeIndex=0;
        }
*/        
        // percentage is between -1 and 1
        m_curNodeIndex = (int)(m_curNodeIndex+4*(percentage+1)*m_nodes.length)%m_nodes.length;
    }

    public int compareTo(Object arg) {
        if ( arg instanceof EvolutionNodeGene ) {
            return (((EvolutionNodeGene)arg).getInternalValue() == getInternalValue())? 0 : 1;
        } else return -1;
    }

}
