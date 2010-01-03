package titancommon.node.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import titancommon.node.DataPacket;
import titancommon.node.TitanTask;
import titancommon.tasks.TitanFileWriter;

/**
 * Implements a Titan service which writes incoming Titan datapackges to a file.
 * 
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 */
public class ETitanFileWriter extends TitanFileWriter implements ExecutableTitanTask {

    private static int fwCounter = 0;   // counts number of instances
    private BufferedWriter fout;
    private TitanTask tTask;

    /**
     * Takes incoming Titan packages on port 0 and writes their content to the
     * file. Each line has the following format:
     *      <timestamp>    <data>    <data>   <data>    ...
     *
     * @todo At the moment, only packets with data arrays of type short are
     * supported. The question is, if anythin else can happen?
     *
     * @param port  Number of incoming port. Has to be 0.
     * @param data  A titan data package.
     */
    public void inDataHandler(int port, DataPacket data) {

        // We want our data only on one port
        if (port != 0) {
            System.err.println("[EFileWriter] Data on wrong port!");
            return;
        }

        try {

            // Write timestamp
            fout.write(data.getTimestamp() + "\t");


            // FIXME: There's a tab too much at the line end. -> Tradeoff between
            // nice files and performance loss of an additional while?
            for (int i = 0; i < data.sdata.length; i++) {
                fout.write(data.sdata[i] + "\t");
            }
            fout.newLine();

        } catch (IOException e) {
            System.err.println("[EFileWriter] Error while writing file: " + e.getMessage());
        }
    }

    /**
     * Creates a file using the content of the variable 'filename' as filename.
     * 
     * @return  true in case of success, false else.
     * @throws IOException
     */
    private boolean createFile() throws IOException {
        File file = new File(filename);
        boolean ok = file.createNewFile();
        return ok;
    }

    /**
     * Initializes the TitanFileWriter, i.e. creates the file to write in.
     */
    public void init() {

        // We have now one filewriter more.
        fwCounter = fwCounter + 1;

        String basefilename = filename;   // temporarily store the original filename
        int localcounter = 0;             // used if several attempts to create a file are needed.


        try {

            // Let's try to create a file. If the first attempt fails, we add
            // this object's number to the filename and if that doesen't do the
            // trick because a file called e.g. fw2-test.txt does already exist
            // as well, we start adding numbers: fw2_1-test.txt, etc.
            while (!createFile()) {

                System.err.println("[EFileWriter] File '" + filename + "' does already exist!");

                if (localcounter == 0) {
                    // If it's the first failure, we add the instance count of this object.
                    filename = "fw" + fwCounter + "-" + basefilename;
                } else {
                    // Else, we add the number of the local counter as well.
                    filename = "fw" + fwCounter + "_" + localcounter + "-" + basefilename;
                }

                System.err.println("[EFileWriter] Using '" + filename + "' instead.");

                localcounter = localcounter + 1;
            }

            // Open an output stream
            fout = new BufferedWriter(new FileWriter(filename));

        } catch (IOException e) {
            System.err.println("[EFileWriter] Unable to create/open file:  " + e.getMessage());
        }


        /*
         * Attach the thread which shall close the file properly to the
         * Runtime.addShutdownHook.
         */
        Runtime.getRuntime().addShutdownHook(new FileFlushThread());
    }

    /**
     * receiving configuration here... i.e. the filename
     * @param param parameters, i.e. filename
     * @return true, if configuration is accepted
     */
    public boolean setExecParameters(short[] param) {


        /* convert the short array back to char array */
        char[] filename_c = new char[param.length];
        for (int i = 0; i < param.length; i++) {
            filename_c[i] = (char) param[i];
        }

        this.filename = new String(filename_c);
        //System.out.println("Filename in setExecParameters is ... " + filename);
        return true;
    }

    public void setTitanTask(TitanTask tsk) {
        tTask = tsk;
    }

    /**
     * This implements a thread whose only purpose is to properly close the open
     * file and ensure minimal to no data losses at unexpected shutdowns.
     *
     * It is kind of a workaround which is necessary because the finalize() method
     * was never called.
     */
    private class FileFlushThread extends Thread {

        public void run() {
            try {
                System.out.println("[ETitanFileWriter] Closing file...");
                fout.flush();
                fout.close();
                System.out.println("[ETitanFileWriter] Done!");
            } catch (IOException e) {
                System.err.println("[ETitanFileWriter] Could *not* close file: " + e.getMessage());
            }
        }
    }
}
