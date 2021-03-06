/*
 * This file is part of the Scriba source distribution. This is free, open-source 
 * software. For full licensing information, please see the LicensingInformation file
 * at the root level of the distribution.
 *
 * Copyright (c) 2006-2007 Kobrix Software, Inc.
 */
package seco.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.hypergraphdb.HGHandle;

import seco.ThisNiche;
import seco.gui.GUIHelper;
import seco.notebook.NotebookDocument;
import seco.notebook.NotebookUI;
import seco.notebook.XMLConstants;
import seco.notebook.style.FontEx;
import seco.notebook.style.NBStyle;
import seco.notebook.style.StyleAttribs;
import seco.notebook.style.StyleType;
import seco.things.CellGroup;
import seco.things.CellUtils;
import seco.util.GUIUtil;

public class CellPropsDialog extends SecoDialog
{
	private static final long serialVersionUID = 1L;
	protected boolean succeeded = false;
    protected NotebookDocument doc;
    protected JEditorPane previewPane;
    protected HGHandle bookH;
    protected NotebookDocument sample_doc;
    protected StyleType style_type;
    protected NBStyle style;

    public CellPropsDialog(final Frame parent, NotebookDocument doc, StyleType style_type)
    {
        super(parent, "Properties for: " + style_type.getDescription(), true);
        if(parent == null) setIconImage(GUIHelper.LOGO_IMAGE);
        this.doc = doc;
        this.style_type = style_type;
        style = doc.getStyle(style_type);

        JPanel pp = new JPanel();
        pp.setBorder(new EmptyBorder(10, 10, 5, 10));
        JPanel pa = new JPanel(new BorderLayout(5, 5));
        Border ba = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED),
                "Appearance");
        pa.setBorder(new CompoundBorder(ba, new EmptyBorder(0, 5, 5, 5)));
        JPanel pb = new JPanel(new GridLayout(4, 1, 5, 5));
        JButton bt = new JButton("Background");
        bt.setMnemonic('b');
        bt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                Color bgColor = JColorChooser.showDialog(CellPropsDialog.this,
								                         "Background",
								                         (Color) style.getDefaultValue(StyleAttribs.BG_COLOR));
                if (bgColor == null) return;
                style.put(StyleAttribs.BG_COLOR, bgColor);
                showColors();
            }
        });
        pb.add(bt);
        bt = new JButton("Font");
        bt.setMnemonic('t');
        bt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                FontDialog dlg = new FontDialog(parent, 
                                               (FontEx) style.getDefaultValue(StyleAttribs.FONT), 
                                               (Color) style.getDefaultValue(StyleAttribs.FG_COLOR));
                GUIUtil.centerOnScreen(dlg);                
                dlg.setVisible(true);
                if (dlg.succeeded)
                {
                    style.put(StyleAttribs.FONT, dlg.getFont());
                    style.put(StyleAttribs.FG_COLOR, dlg.getFontColor());
                    showColors();
                }
            }
        });
        pb.add(bt);
        bt = new JButton("Border");
        bt.setMnemonic('v');

        bt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                Color bgColor = JColorChooser.showDialog(CellPropsDialog.this,
                        "Border", (Color) style
                                .getDefaultValue(StyleAttribs.BORDER_COLOR));
                if (bgColor == null) return;
                style.put(StyleAttribs.BORDER_COLOR, bgColor);
                showColors();
            }
        });
        pb.add(bt);
        pa.add(pb, BorderLayout.WEST);
        initSampleBook();
        previewPane = new NotebookUI(bookH);
        previewPane.setBackground(Color.white);
        previewPane.setEditable(false);
        previewPane.setBorder(new CompoundBorder(new BevelBorder(
                BevelBorder.LOWERED), new EmptyBorder(10, 10, 10, 10)));
        showColors();
        pa.add(previewPane, BorderLayout.CENTER);
        pp.add(pa);
        bt = new JButton("Save");
        bt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                saveData();
                dispose();
            }
        });
        pp.add(bt);
        bt = new JButton("Cancel");
        bt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        pp.add(bt);
        getContentPane().add(pp, BorderLayout.CENTER);
        //setPreferredSize(new Dimension(600, 430));
        //pack();
        // setResizable(false);
        setLocationRelativeTo(parent);
    }

    public boolean succeeded()
    {
        return succeeded;
    }

    protected void saveData()
    {
        doc.addStyle(style);
        succeeded = true;
    }

    private void initSampleBook()
    {
        if (bookH != null) return;
        bookH = CellUtils.createGroupHandle();
        CellGroup book = (CellGroup) ThisNiche.graph.get(bookH);
        HGHandle cellH = CellUtils.makeCellH("s = \"Simple Input Cell\"", "beanshell");
        book.insert(0, cellH);
        book.insert(1, CellUtils.createOutputCellH(cellH, "Simple Output Cell", null, false));
        cellH = CellUtils.makeCellH("s = \"Another Input Cell\"", "beanshell");
        book.insert(2, cellH);
        book.insert(3, CellUtils.createOutputCellH(cellH, "Simple Error Cell", null, true));
        sample_doc = new NotebookDocument(bookH);
        @SuppressWarnings("unchecked")
		Map<StyleType, NBStyle> map = (Map<StyleType, NBStyle>) doc.getBook().getAttribute(XMLConstants.CELL_STYLE);
        if (map != null) for (NBStyle s : map.values())
        {
//        	s.put(StyleAttribs.BG_COLOR, StyleAttribs.BG_COLOR.getDefVal());
            sample_doc.addStyle(s);
        }
        sample_doc.init();
    }

    protected void showColors()
    {
        sample_doc.addStyle(style);
        previewPane.setDocument(sample_doc);
    }

}
