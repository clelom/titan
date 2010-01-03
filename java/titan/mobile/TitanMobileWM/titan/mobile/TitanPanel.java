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

/*
 * TitanPanel.java
 *  Panel for Titan Command
 * Created on 28. Juli 2008, 19:10
 */

package titan.mobile;

import java.awt.FileDialog;
import java.io.File;

/**
 * 
 * @author Mirco Rossi <mrossi@ife.ee.ethz.ch>
 */
public class TitanPanel extends java.awt.Panel {

    FileDialog fd;

    
    /** Creates new form TitanPanel */
    public TitanPanel() {
        initComponents();
        fd= new FileDialog(TitanMobile.singleton.getGUI(),"Select Config File",FileDialog.LOAD);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    StatusLabel = new java.awt.Label();
    LoadCommand = new java.awt.Button();
    ServicesCommand = new java.awt.Button();
    CommandText = new java.awt.TextField();
    label1 = new java.awt.Label();
    label2 = new java.awt.Label();
    SendCommand = new java.awt.Button();
    HelpCommand = new java.awt.Button();
    LedtestCommand = new java.awt.Button();
    ClearCommand = new java.awt.Button();
    FarkleCommand = new java.awt.Button();
    NodeStopCommand = new java.awt.Button();
    NodeStartCommand = new java.awt.Button();
    LoadTestCommand = new java.awt.Button();
    RouterCommand = new java.awt.Button();
    StressCommand = new java.awt.Button();

    LoadCommand.setLabel("Load");
    LoadCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        LoadCommandActionPerformed(evt);
      }
    });

    ServicesCommand.setLabel("Services");
    ServicesCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        ServicesCommandActionPerformed(evt);
      }
    });

    CommandText.setText("titan start");

    label1.setText("Default Commands:");

    label2.setText("Custom Command:");

    SendCommand.setLabel("Send");
    SendCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        SendCommandActionPerformed(evt);
      }
    });

    HelpCommand.setActionCommand("Help");
    HelpCommand.setLabel("Help");
    HelpCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        HelpCommandActionPerformed(evt);
      }
    });

    LedtestCommand.setLabel("LEDtest");
    LedtestCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        LedtestCommandActionPerformed(evt);
      }
    });

    ClearCommand.setActionCommand("Clear");
    ClearCommand.setLabel("Clear");
    ClearCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        ClearCommandActionPerformed(evt);
      }
    });

    FarkleCommand.setActionCommand("Dice");
    FarkleCommand.setLabel("Dice");
    FarkleCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        FarkleCommandActionPerformed(evt);
      }
    });

    NodeStopCommand.setActionCommand("NStop");
    NodeStopCommand.setLabel("NStop");
    NodeStopCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        NodeStopCommandActionPerformed(evt);
      }
    });

    NodeStartCommand.setActionCommand("NStart");
    NodeStartCommand.setLabel("NStart");
    NodeStartCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        NodeStartCommandActionPerformed(evt);
      }
    });

    LoadTestCommand.setActionCommand("LoadT");
    LoadTestCommand.setLabel("LoadT");
    LoadTestCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        LoadTestCommandActionPerformed(evt);
      }
    });

    RouterCommand.setActionCommand("Router");
    RouterCommand.setLabel("Router");
    RouterCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        RouterCommandActionPerformed(evt);
      }
    });

    StressCommand.setActionCommand("Stress");
    StressCommand.setLabel("Stress");
    StressCommand.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        StressCommandActionPerformed(evt);
      }
    });

    org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(layout.createSequentialGroup()
        .addContainerGap()
        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
          .add(layout.createSequentialGroup()
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
              .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                  .add(label1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                  .add(layout.createSequentialGroup()
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                      .add(layout.createSequentialGroup()
                        .add(LedtestCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(ServicesCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                      .add(layout.createSequentialGroup()
                        .add(LoadCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(ClearCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                      .add(FarkleCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                      .add(HelpCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .add(18, 18, 18)
                .add(StatusLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 14, Short.MAX_VALUE))
              .add(label2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
          .add(layout.createSequentialGroup()
            .add(CommandText, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(SendCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .add(12, 12, 12))
          .add(layout.createSequentialGroup()
            .add(NodeStartCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(NodeStopCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(LoadTestCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(42, Short.MAX_VALUE))
          .add(layout.createSequentialGroup()
            .add(RouterCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(StressCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .add(112, 112, 112))))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(layout.createSequentialGroup()
        .addContainerGap()
        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
          .add(StatusLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
          .add(label1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
          .add(LedtestCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
          .add(ServicesCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
          .add(FarkleCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
          .add(LoadCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
          .add(ClearCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
          .add(HelpCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
          .add(layout.createSequentialGroup()
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
              .add(NodeStopCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
              .add(layout.createSequentialGroup()
                .add(NodeStartCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                  .add(StressCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                  .add(RouterCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(label2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
          .add(LoadTestCommand, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
          .add(SendCommand, 0, 0, Short.MAX_VALUE)
          .add(CommandText, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

private void HelpCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HelpCommandActionPerformed
    sendCommand("titan help");
}//GEN-LAST:event_HelpCommandActionPerformed

public void sendCommand(String s){
    System.out.println("COMMAND: "+ s);
    TitanMobile.execTitanCommand(s);    
}


private void SendCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SendCommandActionPerformed
sendCommand(CommandText.getText());
}//GEN-LAST:event_SendCommandActionPerformed

private void ServicesCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ServicesCommandActionPerformed
sendCommand("titan services");
}//GEN-LAST:event_ServicesCommandActionPerformed

private void LoadCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadCommandActionPerformed
   new OpenFileFrame(this).setVisible(true);
   //fd.show();
   //if(fd.getFile()!=null)
   //    sendCommand("titan load \""+ fd.getDirectory()+fd.getFile()+"\"");

}//GEN-LAST:event_LoadCommandActionPerformed

private void LedtestCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedtestCommandActionPerformed
    sendCommand("titan ledtest all");
}//GEN-LAST:event_LedtestCommandActionPerformed

private void ClearCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearCommandActionPerformed
    sendCommand("titan clear all");
}//GEN-LAST:event_ClearCommandActionPerformed

private void FarkleCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FarkleCommandActionPerformed
sendCommand("titan app dice");
}//GEN-LAST:event_FarkleCommandActionPerformed

private void NodeStopCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NodeStopCommandActionPerformed
  sendCommand("titan node stop");
}//GEN-LAST:event_NodeStopCommandActionPerformed

private void NodeStartCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NodeStartCommandActionPerformed
  sendCommand("titan node start");
}//GEN-LAST:event_NodeStartCommandActionPerformed

private void LoadTestCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadTestCommandActionPerformed
  sendCommand("titan load cfg/test.txt");
}//GEN-LAST:event_LoadTestCommandActionPerformed

private void RouterCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RouterCommandActionPerformed
  sendCommand("titan router 1");
}//GEN-LAST:event_RouterCommandActionPerformed

private void StressCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StressCommandActionPerformed
  sendCommand("titan stress 1 0 2 30");
}//GEN-LAST:event_StressCommandActionPerformed

    public void setText(String s1){
        StatusLabel.setText(s1);   
    }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private java.awt.Button ClearCommand;
  private java.awt.TextField CommandText;
  private java.awt.Button FarkleCommand;
  private java.awt.Button HelpCommand;
  private java.awt.Button LedtestCommand;
  private java.awt.Button LoadCommand;
  private java.awt.Button LoadTestCommand;
  private java.awt.Button NodeStartCommand;
  private java.awt.Button NodeStopCommand;
  private java.awt.Button RouterCommand;
  private java.awt.Button SendCommand;
  private java.awt.Button ServicesCommand;
  private java.awt.Label StatusLabel;
  private java.awt.Button StressCommand;
  private java.awt.Label label1;
  private java.awt.Label label2;
  // End of variables declaration//GEN-END:variables

}