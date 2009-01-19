/*
 * This file is part of the Scriba source distribution. This is free, open-source 
 * software. For full licensing information, please see the LicensingInformation file
 * at the root level of the distribution.
 *
 * Copyright (c) 2006-2007 Kobrix Software, Inc.
 */
package seco.notebook.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import seco.gui.AppForm;


public class FindDialog extends JDialog  
{
	public static final char[] WORD_SEPARATORS = {' ', '\t', '\n',
		'\r', '\f', '.', ',', ':', '-', '(', ')', '[', ']', '{',
		'}', '<', '>', '/', '|', '\\', '\'', '\"'};

	private JEditorPane owner;
	protected JTabbedPane tabbedPane;
	protected JTextField txtFind1;
	protected JTextField txtFind2;
	protected Document docFind;
	protected Document docReplace;
	protected ButtonModel modelWord;
	protected ButtonModel modelCase;
	protected ButtonModel modelUp;
	protected ButtonModel modelDown;

	protected int searchIndex = -1;
	protected boolean searchUp = false;
	protected String	searchData;
	
	public FindDialog(final JEditorPane owner, int index)
	{
		super(GUIUtilities.getFrame(owner), "Find and Replace", false);
		this.owner = owner;
		tabbedPane = new JTabbedPane();
		// "Find" panel
		JPanel p1 = new JPanel(new BorderLayout());
		JPanel pc1 = new JPanel(new BorderLayout());
		JPanel pf = new JPanel();
		pf.setLayout(new DialogLayout2(20, 5));
		pf.setBorder(new EmptyBorder(8, 5, 8, 0));
		pf.add(new JLabel("Find what:"));
		txtFind1 = new JTextField();
		docFind = txtFind1.getDocument();
		pf.add(txtFind1);
		pc1.add(pf, BorderLayout.CENTER);
		JPanel po = new JPanel(new GridLayout(2, 2, 8, 2));
		po.setBorder(new TitledBorder(new EtchedBorder(), "Options"));
		JCheckBox chkWord = new JCheckBox("Whole words only");
		chkWord.setMnemonic('w');
		modelWord = chkWord.getModel();
		po.add(chkWord);
		ButtonGroup bg = new ButtonGroup();
		JRadioButton rdUp = new JRadioButton("Search up");
		rdUp.setMnemonic('u');
		modelUp = rdUp.getModel();
		bg.add(rdUp);
		po.add(rdUp);
		JCheckBox chkCase = new JCheckBox("Match case");
		chkCase.setMnemonic('c');
		modelCase = chkCase.getModel();
		po.add(chkCase);
		JRadioButton rdDown = new JRadioButton("Search down", true);
		rdDown.setMnemonic('d');
		modelDown = rdDown.getModel();
		bg.add(rdDown);
		po.add(rdDown);
		pc1.add(po, BorderLayout.SOUTH);
		p1.add(pc1, BorderLayout.CENTER);
		JPanel p01 = new JPanel(new FlowLayout());
		JPanel p = new JPanel(new GridLayout(2, 1, 2, 8));
		ActionListener findAction = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				findNext(false, true);
			}
		};
		JButton btFind = new JButton("Find Next");
		btFind.addActionListener(findAction);
		btFind.setMnemonic('f');
		p.add(btFind);
		ActionListener closeAction = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
			}
		};
		JButton btClose = new JButton("Close");
		btClose.addActionListener(closeAction);
		btClose.setDefaultCapable(true);
		p.add(btClose);
		p01.add(p);
		p1.add(p01, BorderLayout.EAST);
		tabbedPane.addTab("Find", p1);
		// "Replace" panel
		JPanel p2 = new JPanel(new BorderLayout());
		JPanel pc2 = new JPanel(new BorderLayout());
		JPanel pc = new JPanel();
		pc.setLayout(new DialogLayout2(20, 5));
		pc.setBorder(new EmptyBorder(8, 5, 8, 0));
		pc.add(new JLabel("Find what:"));
		txtFind2 = new JTextField();
		txtFind2.setDocument(docFind);
		pc.add(txtFind2);
		pc.add(new JLabel("Replace:"));
		JTextField txtReplace = new JTextField();
		docReplace = txtReplace.getDocument();
		pc.add(txtReplace);
		pc2.add(pc, BorderLayout.CENTER);
		po = new JPanel(new GridLayout(2, 2, 8, 2));
		po.setBorder(new TitledBorder(new EtchedBorder(), "Options"));
		chkWord = new JCheckBox("Whole words only");
		chkWord.setMnemonic('w');
		chkWord.setModel(modelWord);
		po.add(chkWord);
		bg = new ButtonGroup();
		rdUp = new JRadioButton("Search up");
		rdUp.setMnemonic('u');
		rdUp.setModel(modelUp);
		bg.add(rdUp);
		po.add(rdUp);
		chkCase = new JCheckBox("Match case");
		chkCase.setMnemonic('c');
		chkCase.setModel(modelCase);
		po.add(chkCase);
		rdDown = new JRadioButton("Search down", true);
		rdDown.setMnemonic('d');
		rdDown.setModel(modelDown);
		bg.add(rdDown);
		po.add(rdDown);
		pc2.add(po, BorderLayout.SOUTH);
		p2.add(pc2, BorderLayout.CENTER);
		JPanel p02 = new JPanel(new FlowLayout());
		p = new JPanel(new GridLayout(3, 1, 2, 8));
		ActionListener replaceAction = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				findNext(true, true);
			}
		};
		JButton btReplace = new JButton("Replace");
		btReplace.addActionListener(replaceAction);
		btReplace.setMnemonic('r');
		p.add(btReplace);
		ActionListener replaceAllAction = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				int counter = 0;
				while (true)
				{
					int result = findNext(true, false);
					if (result < 0) // error
						return;
					else if (result == 0) // no more
						break;
					counter++;
				}
				JOptionPane
						.showMessageDialog(owner, counter
								+ " replacement(s) have been done",
								"Scriba",
								JOptionPane.INFORMATION_MESSAGE);
			}
		};
		JButton btReplaceAll = new JButton("Replace All");
		btReplaceAll.addActionListener(replaceAllAction);
		btReplaceAll.setMnemonic('a');
		p.add(btReplaceAll);
		btClose = new JButton("Close");
		btClose.addActionListener(closeAction);
		btClose.setDefaultCapable(true);
		p.add(btClose);
		p02.add(p);
		p2.add(p02, BorderLayout.EAST);
		// Make button columns the same size
		p01.setPreferredSize(p02.getPreferredSize());
		tabbedPane.addTab("Replace", p2);
		tabbedPane.setSelectedIndex(index);
		JPanel pp = new JPanel(new BorderLayout());
		pp.setBorder(new EmptyBorder(5, 5, 5, 5));
		pp.add(tabbedPane, BorderLayout.CENTER);
		getContentPane().add(pp, BorderLayout.CENTER);
		pack();
		setResizable(false);
		setLocationRelativeTo(owner);
		WindowListener flst = new WindowAdapter() {
			public void windowActivated(WindowEvent e)
			{
				searchIndex = -1;
			}

			public void windowDeactivated(WindowEvent e)
			{
				searchData = null;
			}
		};
		addWindowListener(flst);
	}

	public void setSelectedIndex(int index)
	{
		tabbedPane.setSelectedIndex(index);
		setVisible(true);
		searchIndex = -1;
	}

	public int findNext(boolean doReplace, boolean showWarnings)
	{
		JEditorPane monitor = owner;
		int pos = monitor.getCaretPosition();
		if (modelUp.isSelected() != searchUp)
		{
			searchUp = modelUp.isSelected();
			searchIndex = -1;
		}
		if (searchIndex == -1)
		{
			try
			{
				Document doc = owner.getDocument();
				if (searchUp)
					searchData = doc.getText(0, pos);
				else
					searchData = doc.getText(pos, doc.getLength() - pos);
				searchIndex = pos;
			}
			catch (BadLocationException ex)
			{
				warning(ex.toString());
				return -1;
			}
		}
		String key = "";
		try
		{
			key = docFind.getText(0, docFind.getLength());
		}
		catch (BadLocationException ex)
		{
		}
		if (key.length() == 0)
		{
			warning("Please enter the target to search");
			return -1;
		}
		if (!modelCase.isSelected())
		{
			searchData = searchData.toLowerCase();
			key = key.toLowerCase();
		}
		if (modelWord.isSelected())
		{
			for (int k = 0; k < WORD_SEPARATORS.length; k++)
			{
				if (key.indexOf(WORD_SEPARATORS[k]) >= 0)
				{
					warning("The text target contains an illegal "
							+ "character \'" + WORD_SEPARATORS[k] + "\'");
					return -1;
				}
			}
		}
		String replacement = "";
		if (doReplace)
		{
			try
			{
				replacement = docReplace.getText(0, docReplace.getLength());
			}
			catch (BadLocationException ex)
			{
			}
		}
		int xStart = -1;
		int xFinish = -1;
		while (true)
		{
			if (searchUp)
				xStart = searchData.lastIndexOf(key, pos - 1);
			else
				xStart = searchData.indexOf(key, pos - searchIndex);
			if (xStart < 0)
			{
				if (showWarnings) warning("Text not found");
				return 0;
			}
			xFinish = xStart + key.length();
			if (modelWord.isSelected())
			{
				boolean s1 = xStart > 0;
				boolean b1 = s1
						&& !isSeparator(searchData.charAt(xStart - 1));
				boolean s2 = xFinish < searchData.length();
				boolean b2 = s2
						&& !isSeparator(searchData.charAt(xFinish));
				if (b1 || b2) // Not a whole word
				{
					if (searchUp && s1) // Can continue up
					{
						pos = xStart;
						continue;
					}
					if (!searchUp && s2) // Can continue down
					{
						pos = xFinish + 1;
						continue;
					}
					// Found, but not a whole word, and we cannot continue
					if (showWarnings) warning("Text not found");
					return 0;
				}
			}
			break;
		}
		if (!searchUp)
		{
			xStart += searchIndex;
			xFinish += searchIndex;
		}
		if (doReplace)
		{
			setSelection(xStart, xFinish, searchUp);
			monitor.replaceSelection(replacement);
			setSelection(xStart, xStart + replacement.length(),
					searchUp);
			searchIndex = -1;
		} else
			setSelection(xStart, xFinish, searchUp);
		return 1;
	}
	
	private void setSelection(int xStart, int xFinish, boolean moveUp) {
		if (moveUp) {
			owner.setCaretPosition(xFinish);
			owner.moveCaretPosition(xStart);
		}
		else
			owner.select(xStart, xFinish);
	}

	protected void warning(String message)
	{
		JOptionPane.showMessageDialog(owner, message,"Scriba",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static boolean isSeparator(char ch) {
		for (int k=0; k<WORD_SEPARATORS.length; k++)
			if (ch == WORD_SEPARATORS[k])
				return true;
		return false;
	}
}
