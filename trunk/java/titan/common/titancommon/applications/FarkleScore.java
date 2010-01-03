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

package titancommon.applications;

/**
 * This class computes scores for the Farkle game and controls the game play.
 * 
 * It needs to receive the following events:
 * 
 * 1) When a dice is picked up it must detect whether it is allowed to be thrown.
 *    - it can't be thrown if it has been laid aside
 *    - at least one dice needs to be laid aside for each turn
 *    - triples must either all or none be left there
 *    
 *    pickUp() and startShake() are taking care about this.
 * 
 * 2) When the throw is completed, the maximum score available right now 
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public class FarkleScore {

   private static final int DICE = Farkle.DICE;
	private int [] m_diceValues = new int[DICE]; // the current values of the dice (check modifications)
	private boolean [] m_diceBlocked = new boolean[DICE];
	private boolean [] m_dicePickedUp = new boolean[DICE];
	private int [] m_diceScoring = new int[DICE]; // 0=not scoring, 1=scoring, >1 means triple set with corresponding number
	private int hotDiceScore; // if there has been a "hot dice" (all dice score), keep here the score from before
	private boolean bFirstThrow = true;
	
	/** used to indicate invalid dice pick ups. */
	public class PickUpException extends Throwable {
		private static final long serialVersionUID = 3058121028018775025L;

		private String m_strMessage;
		public PickUpException(String strMessage) {
			m_strMessage = strMessage;
		}
		
		public String toString() {
			return m_strMessage;
		}
	};
	
	/**
	 * Called when a dice has been picked up. Adds the dice to the current set of throwing dice
	 * @param dice dice number that has been picked up
	 * @return whether the set is complete and can be thrown
	 * @throws PickUpException In case any conditions are not right
	 */
	public boolean pickUp( int dice ) throws PickUpException {
		
		// check conditions
		if ( dice < 0 || dice >= m_diceValues.length ) throw new PickUpException("Invalid dice ID");
		if ( m_diceBlocked[dice] == true ) throw new PickUpException("Dice has been blocked - not allowed to be picked up!");
		
		m_dicePickedUp[dice] = true;
		
		// check for tripples - if a triple member has been picked up, remove tripple
		if ( m_diceScoring[dice] > 1 ) {
			int triple = m_diceScoring[dice];
			for (int i=0; i<m_diceScoring.length;i++) {
				if (m_diceScoring[i] == triple) {
					m_diceScoring[i] = (m_diceValues[i] == 1) ? 1 : // when removing triple, don't forget remaining scores
									   (m_diceValues[i] == 5) ? 1 :
										   						0;
				}
			}
		}
		
		// check whether all dice have been picked up
		boolean bAllPickedUp = true;
		for (int i=0;i<m_dicePickedUp.length; i++) {
			if ( m_dicePickedUp[i] == false && m_diceScoring[i] == 0) {
				bAllPickedUp = false;
				break;
			}
		}
		
		return bAllPickedUp;
	}
	
	public void placedDown( int dice ) {
		if (dice < 0 || dice >= m_diceValues.length ) return;
		m_dicePickedUp[dice] = false;
	}
	
	/**
	 * Checks whether all conditions are met to start shaking - at least one new dice must be blocked,
	 * and all non-scoring dice must be thrown. This function must be called to fix the dice which are 
	 * blocked at each turn.
	 * @throws PickUpException Exception in case some dice must be additionally picked up.
	 */
	public boolean [] startShake() throws PickUpException {
		
		boolean newDiceBlocked = false;
		
		// check through all dice picked up
		for ( int i=0; i < m_diceScoring.length; i++ ) {
			
			// check whether a non-scoring dice has been left, or whether a dice can be blocked
			if ( m_diceBlocked[i] == false && m_dicePickedUp[i] == false ) {
				if ( m_diceScoring[i] == 0) throw new PickUpException("Dice "+i+" must also be picked up!");
				m_diceBlocked[i] = true;
				newDiceBlocked = true;
			} else if ( m_diceBlocked[i] == true && m_dicePickedUp[i] == true ) {
				throw new PickUpException("Dice " + i + " can't be picked up!");
			}
		}
		
		if ( bFirstThrow ) {
			if ( newDiceBlocked == true ) throw new PickUpException("All dice need to be picked up for this throw");
		} else {
			if ( newDiceBlocked == false ) throw new PickUpException("No scoring dice has been left");
		}
		
		return m_diceBlocked;
	}
	
	/**
	 * Delivers the values of the lying dice. This computes the score of the current throw.
	 * @param dice Dice values received
	 * @return maximum score that can be got with those dice. Returns zero for Farkle
	 */
	public int throwDone( int [] dice) throws PickUpException {
		
		// check whether any blocked dice have been changed
		for (int i=0; i < m_diceValues.length; i++) {
			if ( m_diceBlocked[i] == true && m_diceValues[i] != dice[i] ) throw new PickUpException("Value change on blocked dice "+i+": this is cheating!");
		}
		
		m_diceValues = (int [])dice.clone();
		
		return getScore();
	}

	/**
	 * Computes the score of the last throw. 
	 * 
	 * WARNING: This turns the internal state into pickup mode!
	 * @return score after last throw.
	 */
	public int getScore() {
		int score = 0;
		
		// check for values
		int iSet = 2;
		m_diceScoring = new int[DICE];
		for (int i=0; i< m_diceValues.length; i++) { // go through all dice
			int found=1;
			if (m_diceScoring[i] == 0) { // not scoring yet
				// check how many other dice have the same value
				for (int j=i+1; j< m_diceValues.length; j++ ) {
					if ( m_diceValues[i] == m_diceValues[j] ) found++;
				}
				
				// check if there is a set (or two) 
				if (found>=3) {
					
					// mark the set
					for (int j=i; j<m_diceValues.length; j++ ) {
						if ( m_diceValues[i] == m_diceValues[j] ) {
							m_diceScoring[j] = iSet;
						}
					}
					iSet++;
					
					// add the score
					score += (m_diceValues[i] == 1)? 1000: m_diceValues[i]*100;
					if (found == 6) score += (m_diceValues[i] == 1)? 1000: m_diceValues[i]*100;
					
				} else {
					m_diceScoring[i] = (m_diceValues[i] == 1)? 1:
						               (m_diceValues[i] == 5)? 1:
						            	                       0;
					score += (m_diceValues[i] == 1)? 100:
			                 (m_diceValues[i] == 5)?  50:
    	                                               0;
				}
			} // if (m_diceScoring[i] == 0)
			
		}
		
		m_dicePickedUp = new boolean[DICE];

		// check for "hot dice"
		boolean hotDice = true;
		for (int i=0; i<m_diceScoring.length; i++) {
			if( m_diceScoring[i] == 0) {
				hotDice = false;
				break;
			}
		}
		if ( hotDice == true ) {
			hotDiceScore += score;
			bFirstThrow = true;
			m_diceBlocked = new boolean[DICE];
			return hotDiceScore;
		} else {
			bFirstThrow = false;
		}
		
		// check for Farkle - if any new dice are scoring, the score counts
		for (int i=0; i < m_diceScoring.length; i++ ) {
			if ( m_diceBlocked[i] == false && m_diceScoring[i] != 0 ) return (hotDiceScore + score);
		}
		
		return 0;
	}
	
	
	
	/* **********************************************************************/
	/*                               TEST CODE                              */
	/* **********************************************************************/
	
	public static void main(String [] args) {
	
		FarkleScore fs = new FarkleScore();
		
		int [] diceThrow = {1,1,1,1,1,1}; 
		
		try {
			System.out.println("Pickup returned: " + (fs.pickUp(0)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(1)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(2)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(3)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(4)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(5)? "true":"false"));
			fs.startShake();
			int iResult = fs.throwDone(diceThrow);
			System.out.println("The score is: " + iResult);
			System.out.println((iResult==2000)? "Result is ok!\n" : "ERROR: should be 2000\n");
			
			// this was a hot dice: ground points 2000
			
			diceThrow[0] = 5;
			diceThrow[1] = 2;
			diceThrow[2] = 5;
			diceThrow[3] = 2;
			diceThrow[4] = 2;
			diceThrow[5] = 5;
			System.out.println("Pickup returned: " + (fs.pickUp(0)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(1)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(2)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(3)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(4)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(5)? "true":"false"));
			fs.startShake();
			iResult = fs.throwDone(diceThrow);
			System.out.println("The score is: " + iResult);
			System.out.println((iResult==2700)? "Result is ok!\n" : "ERROR: should be 2700\n");
			
			// this was a hot dice: ground points 2700
			
			diceThrow[0] = 3;
			diceThrow[1] = 4;
			diceThrow[2] = 1;
			diceThrow[3] = 5;
			diceThrow[4] = 5;
			diceThrow[5] = 6;
			System.out.println("Pickup returned: " + (fs.pickUp(0)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(1)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(2)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(3)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(4)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(5)? "true":"false"));
			fs.startShake();
			iResult = fs.throwDone(diceThrow);
			System.out.println("The score is: " + iResult);
			System.out.println((iResult==2900)? "Result is ok!\n" : "ERROR: should be 2900\n");
			
			diceThrow[0] = 3;
			diceThrow[1] = 5;
			diceThrow[2] = 1; // must remain
			diceThrow[3] = 2;
			diceThrow[4] = 5;
			diceThrow[5] = 5;
			System.out.println("Pickup returned: " + (fs.pickUp(0)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(1)? "true":"false"));
			//System.out.println("Pickup returned: " + (fs.pickUp(2)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(3)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(4)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(5)? "true":"false"));
			fs.startShake();
			iResult = fs.throwDone(diceThrow);
			System.out.println("The score is: " + iResult + "");
			System.out.println((iResult==3300)? "Farkle - result is ok!\n" : "ERROR: should be 3300\n");
			
			diceThrow[0] = 3;
			diceThrow[1] = 6;
			diceThrow[2] = 1; // must remain
			diceThrow[3] = 1;
			diceThrow[4] = 5; // must remain
			diceThrow[5] = 6;
			System.out.println("Pickup returned: " + (fs.pickUp(0)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(1)? "true":"false"));
			//System.out.println("Pickup returned: " + (fs.pickUp(2)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(3)? "true":"false"));
			//System.out.println("Pickup returned: " + (fs.pickUp(4)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(5)? "true":"false"));
			fs.startShake();
			iResult = fs.throwDone(diceThrow);
			System.out.println("The score is: " + iResult + "");
			System.out.println((iResult==2950)? "Farkle - result is ok!\n" : "ERROR: should be 2950\n");
			
			diceThrow[0] = 3;
			diceThrow[1] = 6;
			diceThrow[2] = 1; // must remain
			diceThrow[3] = 1; // must remain
			diceThrow[4] = 5; // must remain
			diceThrow[5] = 6;
			System.out.println("Pickup returned: " + (fs.pickUp(0)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(1)? "true":"false"));
			//System.out.println("Pickup returned: " + (fs.pickUp(2)? "true":"false"));
			//System.out.println("Pickup returned: " + (fs.pickUp(3)? "true":"false"));
			//System.out.println("Pickup returned: " + (fs.pickUp(4)? "true":"false"));
			System.out.println("Pickup returned: " + (fs.pickUp(5)? "true":"false"));
			fs.startShake();
			iResult = fs.throwDone(diceThrow);
			System.out.println("The score is: " + iResult + "");
			System.out.println((iResult==0)? "Farkle - result is ok!\n" : "ERROR: should be 0\n");
			
		} catch (PickUpException e) {
			System.out.println(e);
			e.printStackTrace();
		}
		
		
	}
	
}
