/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * LayoutSettingsPanel.java
 *
 * Created on 2009-2-16, 14:34:57
 */

package seco.gui.layout;

import edu.umd.cs.piccolo.util.PBounds;
import seco.ThisNiche;
import seco.gui.GUIHelper;
import seco.gui.PSwingNode;
import seco.gui.PiccoloCanvas;
import seco.gui.TopFrame;
import seco.gui.VisualAttribs;
import seco.things.CellGroupMember;

/**
 *
 * @author Administrator
 */
public class LayoutSettingsPanel extends javax.swing.JPanel {

    private PSwingNode node;
    //private DRect oldRect;
    //private RefPoint oldRefP;
    private boolean pinned;
    
    /** Creates new form LayoutSettingsPanel */
    public LayoutSettingsPanel() {
        initComponents();
    }
    
    public LayoutSettingsPanel(PSwingNode node) {
        this();
        this.node = node;
        LayoutHandler vh = GUIHelper.getLayoutHandler(node);
        if(vh != null)
        {
          populate(vh.getBounds(), vh.getRefPoint());
          pinned = true;
          butPin.setEnabled(false);
        }
        else
        {
          populate(node.getFullBounds());
          butUnpin.setEnabled(false);
          butChange.setEnabled(false);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        spinX = new javax.swing.JSpinner();
        checkX = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        spinY = new javax.swing.JSpinner();
        checkY = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        spinW = new javax.swing.JSpinner();
        checkW = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        spinH = new javax.swing.JSpinner();
        checkH = new javax.swing.JCheckBox();
        comboRefP = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        butPin = new javax.swing.JButton();
        butUnpin = new javax.swing.JButton();
        butChange = new javax.swing.JButton();

        jLabel1.setText("Top Left X:");

        checkX.setText("%");

        jLabel2.setText("Top Left Y:");

        checkY.setText("%");

        jLabel3.setText("Width:");

        checkW.setText("%");

        jLabel4.setText("Height:");

        checkH.setText("%");

        comboRefP.setModel(new javax.swing.DefaultComboBoxModel(
            new Object[] {RefPoint.TOP_LEFT,
                RefPoint.TOP_RIGHT, RefPoint.BOTTOM_LEFT,
                RefPoint.BOTTOM_RIGHT }));

    jLabel5.setText("Ref. Point:");

    butPin.setText("Pin");
    butPin.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            butPinActionPerformed(evt);
        }
    });

    butUnpin.setText("Unpin");
    butUnpin.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            butUnpinActionPerformed(evt);
        }
    });

    butChange.setText("Apply");
    butChange.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            butChangeActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addGroup(layout.createSequentialGroup()
                    .addGap(57, 57, 57)
                    .addComponent(comboRefP, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel5)
                            .addComponent(jLabel2))
                        .addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jLabel3))
                        .addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jLabel4)))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(spinX, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(spinY, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(spinW, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(spinH, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(checkX)
                        .addComponent(checkY)
                        .addComponent(checkW)
                        .addComponent(checkH))))
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(butUnpin)
                .addComponent(butPin, javax.swing.GroupLayout.DEFAULT_SIZE, 71, Short.MAX_VALUE)
                .addComponent(butChange))
            .addContainerGap())
    );

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {butChange, butPin, butUnpin});

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {checkH, checkW, checkX, checkY});

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinH, spinW, spinX, spinY});

    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(comboRefP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel5)
                .addComponent(butPin))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(jLabel1)
                .addComponent(spinX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(checkX)
                .addComponent(butUnpin))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(jLabel2)
                .addComponent(spinY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(checkY)
                .addComponent(butChange))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(jLabel3)
                .addComponent(spinW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(checkW))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(jLabel4)
                .addComponent(spinH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(checkH))
            .addContainerGap(13, Short.MAX_VALUE))
    );

    layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {butChange, butPin, butUnpin});

    }// </editor-fold>//GEN-END:initComponents

    private void butPinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butPinActionPerformed
        butPin.setEnabled(false);
        butUnpin.setEnabled(true);
        butChange.setEnabled(true);
        PiccoloCanvas canvas = TopFrame.getInstance().getCanvas();
        canvas.getNodeLayer().removeChild(node);
        CellGroupMember m = ThisNiche.hg.get(node.getHandle());
        m.setAttribute(VisualAttribs.layoutHandler, 
                new DefaultLayoutHandler(getDRect(), getRefPoint()));
        canvas.getCamera().addChild(node);
        canvas.relayout();
        pinned = true;
    }//GEN-LAST:event_butPinActionPerformed

    private void butUnpinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butUnpinActionPerformed
        butPin.setEnabled(!false);
        butUnpin.setEnabled(!true);
        butChange.setEnabled(!true);
        PiccoloCanvas canvas = TopFrame.getInstance().getCanvas();
        PBounds b = node.getFullBounds();
        canvas.getCamera().removeChild(node);
        CellGroupMember m = ThisNiche.hg.get(node.getHandle());
        m.getAttributes().remove(VisualAttribs.layoutHandler);
        m.setAttribute(VisualAttribs.rect, b);
        canvas.getNodeLayer().addChild(node);
        canvas.relayout();
        pinned = false;
    }//GEN-LAST:event_butUnpinActionPerformed

    private void butChangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butChangeActionPerformed
       if(pinned)
       {
           LayoutHandler vh = GUIHelper.getLayoutHandler(node);
           //oldRect = vh.getBounds();
           //oldRefP = vh.getRefPoint(); 
           vh.setBounds(getDRect());
           vh.setRefPoint(getRefPoint());
           TopFrame.getInstance().getCanvas().relayout();
       }
}//GEN-LAST:event_butChangeActionPerformed


    public DRect getDRect()
    {
        return new DRect(
                new DValue(((Number) spinX.getValue()).doubleValue(), checkX.isSelected()),
                new DValue(((Number) spinY.getValue()).doubleValue(), checkY.isSelected()),
                new DValue(((Number) spinW.getValue()).doubleValue(), checkW.isSelected()),
                new DValue(((Number) spinH.getValue()).doubleValue(), checkH.isSelected()));
    }
    
    public RefPoint getRefPoint()
    {
        return (RefPoint) comboRefP.getSelectedItem();
    }
    
    void populate(DRect r, RefPoint p)
    {
        comboRefP.setSelectedItem(p);
        spinX.setValue(r.x.getValue()); checkX.setSelected(r.x.isPercent());
        spinY.setValue(r.y.getValue()); checkY.setSelected(r.y.isPercent());
        spinW.setValue(r.width.getValue()); checkW.setSelected(r.width.isPercent());
        spinH.setValue(r.height.getValue()); checkH.setSelected(r.height.isPercent());
    }
    
    void populate(PBounds r)
    {
        spinX.setValue(r.x); 
        spinY.setValue(r.y); 
        spinW.setValue(r.width); 
        spinH.setValue(r.height);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton butChange;
    private javax.swing.JButton butPin;
    private javax.swing.JButton butUnpin;
    private javax.swing.JCheckBox checkH;
    private javax.swing.JCheckBox checkW;
    private javax.swing.JCheckBox checkX;
    private javax.swing.JCheckBox checkY;
    private javax.swing.JComboBox comboRefP;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JSpinner spinH;
    private javax.swing.JSpinner spinW;
    private javax.swing.JSpinner spinX;
    private javax.swing.JSpinner spinY;
    // End of variables declaration//GEN-END:variables

}
