package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.TemplateTask;

/**
 *
 * 0) have you implemented a TemplateTask.java in titancommon/tasks? if not, go there and do it (copy from TemplateTask.java there for example)
 * 1) implement all following methods
 *
 * you'll need to change the class name of this task according to your task name
 * don't forget that the filename and the class name must match!
 *
 * then you'll need to change the import, and the "extends" at this class header
 *
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class ETemplateTask extends TemplateTask implements ExecutableTitanTask {

    private TitanTask tTask;

    /**
     * don't need to change that
     * @param tsk
     */
    public void setTitanTask(TitanTask tsk) {
        tTask = tsk;
    }

    /**
     * your configuration gets set here
     * each line from TemplateTask's getConfigBytes method will be inserted here separately,
     * but you have no clue about the order of this calls... it might be possible, that getConfigBytes[0] is the first, then ..[1] and so on, but who know?
     *
     * this method must play together with the TemplateTask's getConfigBytes!
     *
     * @param param one line with a configuration
     * @return true if the config was set successfully
     */
    public boolean setExecParameters(short[] param) {
        return true;
    }

    /**
     * this initializes your task... whatevery you need for the initialization goes here
     */
    public void init() {
    }

    /**
     * this method is called for each incoming packet
     * you can get the portnumber in 'port', and the data with data.short[]
     * please pay attention, whether the data in data.short[] are really shorts, or if they are actually only bytes
     *
     * to send out another packet, use:
     * DataPacket outputpaket = new DataPacket(your_data_short_array);
     * tTask.send(outputport, outputpaket);
     *
     * again, have in mind that the following task might be written to accept only char-values in the short-array (i.e. only continuing with
     * the lower bytes of each element), or possibly it interprets the whole short-range of each element
     *
     * TODO: it would be a damn good idea to implement the DataPacket in a clean manner, with a getShortArray and a getCharArray (and
     * appopriate setters) to have an abstraction on exactly that problem.
     * As far as I know, this problems comes because some hardware (the tMoteSky for example) can only communicate with char-values, while
     * on the laptop, shorts are okay.
     *
     * TODO: there is also a little problem, when communicating between two virtual nodes... some data does not get sent (via TCP) which need further investigating
     *
     * TODO: the DataPacket has setter and getter for timestamp... but this timestamp is not sent over TCP... you can only use timestamps
     * when the whole DataPacket stays on the same virtual node all time.
     * and don't forget to set the timestamp again, if you send out a new packet, if they're needed later (for exmample, for the TitanFileWriter)
     *
     *
     * @param port the port number, at which the packet arrived
     * @param data the packet itself
     */
    public void inDataHandler(int port, DataPacket data) {
    }
}
