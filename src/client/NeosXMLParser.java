/*
 * Copyright (c) 2017 NEOS-Server
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package client;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class NeosXMLParser {
	Document doc;
	public String solver;
	public String category;
	public String inputMethod;
	public String abs;
	public InputItem[] inputItems;
	private JTextField emailField;

	/* For testing only */
	public static void main(String[] arguments) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	public static void createAndShowGUI() {
		// Make sure we have nice window decorations.
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		FileReader fr = null;
		BufferedReader xmlreader = null;
		String xmlstring = new String();
		String buf;
		try {
			fr = new FileReader("../Test/blmvm.xml");
			xmlreader = new BufferedReader(fr);
			while ((buf = xmlreader.readLine()) != null)
				xmlstring += buf + "\n";
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		System.out.print(xmlstring);
	}

	public String getSolver() {
		return solver;
	}

	public String getAbstract() {
		return abs;
	}

	public String getCategory() {
		return category;
	}

	public String getInputMethod() {
		return inputMethod;
	}

	public void parse(String s) throws NeosXMLException {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			doc = docFactory.newDocumentBuilder().parse(
					new ByteArrayInputStream(s.getBytes()));
		} catch (IOException e) {
			throw new NeosXMLException(e.getMessage());
		} catch (SAXException e) {
			throw new NeosXMLException(e.getMessage());
		} catch (ParserConfigurationException e) {
			throw new NeosXMLException(e.getMessage());
		}

		NodeList nodelist = doc.getElementsByTagName("neos:solver");
		if (nodelist.getLength() > 0)
			solver = NeosXMLParser.getText(nodelist.item(0));
		else {
			throw new NeosXMLException("No neos:solver tag in Solver XML");
		}

		nodelist = doc.getElementsByTagName("neos:category");
		if (nodelist.getLength() > 0)
			category = NeosXMLParser.getText(nodelist.item(0));
		else {
			throw new NeosXMLException("No neos:category tag in Solver XML");
		}

		nodelist = doc.getElementsByTagName("neos:inputMethod");
		if (nodelist.getLength() > 0)
			inputMethod = NeosXMLParser.getText(nodelist.item(0));
		else {
			throw new NeosXMLException("No neos:inputMethod tag in Solver XML");
		}

		nodelist = doc.getElementsByTagName("neos:abstract");
		if (nodelist.getLength() > 0)
			abs = NeosXMLParser.getText(nodelist.item(0));
		else {
			abs = "";
		}

		nodelist = doc.getElementsByTagName("neos:input");
		InputItemFactory factory = new InputItemFactory();
		if (nodelist.getLength() > 0) {
			inputItems = new InputItem[nodelist.getLength()];
			for (int i = 0; i < nodelist.getLength(); i++) {
				inputItems[i] = factory.create(nodelist.item(i));
			}
		} else
			throw new NeosXMLException("No neos:input tags in Solver XML");

	}

	public JPanel formGUI(ActionListener submitHandler) {
		JPanel solverPane = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0; // vertical placement of panels
		Font f = new Font("Arial", Font.BOLD, 12);
		
		for (int i = 0; i < inputItems.length; i++) {
			JPanel outerPanel = new JPanel(new GridBagLayout());
			TitledBorder title = BorderFactory.createTitledBorder(inputItems[i]
					.getPrompt());
			title.setTitleFont(f);
			outerPanel.setBorder(title);
			GridBagConstraints oc = new GridBagConstraints();
			outerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

			oc.gridx = 0; // vertical placement of panels
			oc.anchor = GridBagConstraints.LINE_START;

			// add help panel if necessary
			if (inputItems[i].getHelp().length() > 0) {
				JEditorPane helpText = new JEditorPane();
				JScrollPane helpScroll = new JScrollPane(helpText);
				helpScroll.setBorder(null);
				helpText.setContentType("text/html");
				helpText.setText("<html>" + inputItems[i].getHelp() + "</html>");
				
				helpText.setCaretPosition(0);

				helpText.setBackground((new JLabel()).getBackground());
				helpText.setEditable(false);
				helpText.setPreferredSize(new Dimension(30, 100));
				oc.insets = new Insets(5,5,5,5);
				oc.weightx = 1.0f;
				oc.weighty = 1.0f;
				oc.fill = GridBagConstraints.HORIZONTAL;
				outerPanel.add(helpScroll, oc);
			}

			oc.weighty = 0.0f;
			oc.fill = GridBagConstraints.HORIZONTAL;
			oc.weightx = 0.0f;
			outerPanel.add(inputItems[i].createPanel(), oc);

			solverPane.add(outerPanel, c);
		}

		// Email section
		JPanel outerPanel = new JPanel(new GridBagLayout());
		TitledBorder title = BorderFactory.createTitledBorder("Email");
		title.setTitleFont(f);
		outerPanel.setBorder(title);
		GridBagConstraints oc = new GridBagConstraints();
		outerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		oc.gridx = 0; // vertical placement of panels
		oc.anchor = GridBagConstraints.LINE_START;

		JEditorPane helpText = new JEditorPane();
		JScrollPane helpScroll = new JScrollPane(helpText);
		helpScroll.setBorder(null);
		helpText.setContentType("text/html");
		helpText.setText("<html>An email address is required for CPLEX submissions.</html>");
		
		helpText.setCaretPosition(0);

		helpText.setBackground((new JLabel()).getBackground());
		helpText.setEditable(false);
		helpText.setPreferredSize(new Dimension(2, 16));
		oc.insets = new Insets(5,5,5,5);
		oc.weightx = 1.0f;
		oc.weighty = 1.0f;
		oc.fill = GridBagConstraints.HORIZONTAL;
		outerPanel.add(helpScroll, oc);

		oc.weighty = 0.0f;
		oc.fill = GridBagConstraints.HORIZONTAL;
		oc.weightx = 0.0f;
		JPanel emailPanel = new JPanel();
		emailPanel.setPreferredSize(new Dimension(2, 32));
		emailField = new JTextField(25);
		emailPanel.add(emailField);
		outerPanel.add(emailPanel, oc);
		solverPane.add(outerPanel, c);

		// Finally....
		JButton submitButton = new JButton("submit to NEOS");
		submitButton.setActionCommand("Submit");
		submitButton.addActionListener(submitHandler);
		c.fill = 0;
		c.ipady = 10;
		solverPane.add(submitButton, c);

		return solverPane;
	}

	public InputItem[] getInputItems() {
		return inputItems;
	}

	public String writeDocument() {
		String submission = new String();
		String header = "<document>\n<category>" + getCategory()
				+ "</category>\n<solver>" + getSolver()
				+ "</solver>\n<inputMethod>" + getInputMethod()
				+ "</inputMethod>\n";
		String footer = "</document>";

		for (int i = 0; i < inputItems.length; i++) {
			submission += inputItems[i].writeItem();
		}
		if (emailField.getText().length() > 0) {
			submission += "<email>" + emailField.getText() + "</email>";
		}
		System.out.println(header + submission + footer);
		return header + submission + footer;
	}

	public static String getText(Node node) {
		String retval = "";
		if (node == null)
			return "";

		if ((node.getNodeType() == node.TEXT_NODE)
				|| (node.getNodeType() == node.CDATA_SECTION_NODE)
				|| (node.getNodeType() == node.ATTRIBUTE_NODE))
			return node.getNodeValue();
		if (node.hasChildNodes()) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
				retval += getText(children.item(i));
		}
		return retval;
	}
}

class InputItemFactory {

	public InputItem create(Node node) throws NeosXMLException {

		NamedNodeMap map = node.getAttributes();
		if (map == null)
			throw new NeosXMLException("Could not get attr. in Solver XML\n"
					+ node.toString());

		String type = NeosXMLParser.getText(map.getNamedItem("TYPE"));
		if (type.compareTo("") == 0)
			throw new NeosXMLException("Missing 'TYPE' attr. in input\n"
					+ map.toString() + "\n" + map.getNamedItem("TYPE") + "\n"
					+ node.toString());
		System.out.println(type);
		if (type.compareTo("file") == 0)
			return new FileItem(node);
		else if (type.compareTo("textfield") == 0)
			return new TextFieldItem(node);
		else if (type.compareTo("textarea") == 0)
			return new TextAreaItem(node);
		else if (type.compareTo("radio") == 0)
			return new RadioItem(node);
		else if (type.compareTo("checkbox") == 0)
			return new CheckBoxItem(node);
		throw new NeosXMLException("unknown input type " + node.toString());
	}
}

abstract class InputItem {
	protected String help;
	protected String token;
	protected String prompt;
	protected Node node;

	public InputItem(Node n) throws NeosXMLException {
		node = n;
		NodeList nodelist;
		nodelist = ((Element) n).getElementsByTagName("neos:token");
		if (nodelist.getLength() > 0)
			token = NeosXMLParser.getText(nodelist.item(0));
		else
			throw new NeosXMLException("missing neos:token tag in Solver XML");

		nodelist = ((Element) n).getElementsByTagName("neos:help");
		if (nodelist.getLength() > 0)
			help = NeosXMLParser.getText(nodelist.item(0));
		else
			help = "";

		nodelist = ((Element) n).getElementsByTagName("neos:prompt");
		if (nodelist.getLength() > 0)
			prompt = NeosXMLParser.getText(nodelist.item(0));
		else { // For backward compatibility
			nodelist = ((Element) n).getElementsByTagName("neos:text");
			if (nodelist.getLength() > 0)
				prompt = NeosXMLParser.getText(nodelist.item(0));
			else
				throw new NeosXMLException(
						"missing neos:prompt tag in Solver XML");
		}
	}

	public abstract JPanel createPanel();

	public abstract String writeItem();

	public String getPrompt() {
		return prompt;
	}

	public String getHelp() {
		return help;
	}

	public String getToken() {
		return token;
	}

	public String writeNode(String token, String value) {
		if (value.compareTo("") != 0)
			return "<" + token + "><![CDATA[" + value + "\n]]></" + token
					+ ">\n";
		else
			return "";
	}
}

class FileItem extends InputItem implements ActionListener {
	static File lastDirectory = null;
	JPanel bottomPane;
	JTextField fileText;
	JButton browseButton;

	public FileItem(Node n) throws NeosXMLException {
		super(n);
	}

	public JPanel createPanel() {
		JPanel panel = new JPanel();
		
		fileText = new JTextField(25);
		browseButton = new JButton("Browse");
		browseButton.addActionListener(this);

		bottomPane = new JPanel();
		panel.setAlignmentX(0.0f);
		bottomPane.add(fileText);
		bottomPane.add(browseButton);

		// panel.add(helpText);
		panel.add(bottomPane);
		return panel;
	}

	public String writeItem() {
		if (fileText.getText().length() > 0) {
			try {
				String fileContents = "", buf;

				FileReader fr = new FileReader(fileText.getText());

				BufferedReader xmlreader = new BufferedReader(fr);
				Boolean first = true;
				while ((buf = xmlreader.readLine()) != null) {
					if (first == false) fileContents += "\n";
					fileContents += buf;
					first = false;
				}

				return writeNode(token, fileContents);
			} catch (IOException e) {
				System.out.println("Error reading file");
				return "";
			}
		} else
			return "";
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();

		if (fileText.getText().length() > 0)
			chooser.setCurrentDirectory(new File(fileText.getText()));
		else
			chooser.setCurrentDirectory(lastDirectory);

		int returnVal = chooser.showOpenDialog(browseButton);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			lastDirectory = chooser.getSelectedFile();
			fileText.setText(chooser.getSelectedFile().getAbsolutePath());
			System.out.println("You chose to open this file: "
					+ chooser.getSelectedFile().getName());
		}
	}

}

class TextAreaItem extends InputItem {
	JTextArea textArea;
	JTextArea helpText;

	public TextAreaItem(Node n) throws NeosXMLException {
		super(n);
	}

	public JPanel createPanel() {
		JPanel panel = new JPanel();
		textArea = new JTextArea(5, 30);
		panel.add(textArea);
		return panel;
	}

	public String writeItem() {
		return writeNode(token, textArea.getText());
	}
}

class TextFieldItem extends InputItem {
	JTextField textField;

	public TextFieldItem(Node n) throws NeosXMLException {
		super(n);
	}

	public JPanel createPanel() {
		JPanel panel = new JPanel();
		textField = new JTextField(25);
		panel.add(textField);
		return panel;
	}

	public String writeItem() {
		return writeNode(token, textField.getText());
	}
}

class CheckBoxItem extends InputItem {
	JCheckBox checkBox;
	String value;

	public CheckBoxItem(Node n) throws NeosXMLException {
		super(n);
	}

	public JPanel createPanel() {
		checkBox = new JCheckBox(getPrompt());
		checkBox.setSelected(false);
		JPanel panel = new JPanel();
		panel.add(checkBox);
		return panel;
	}

	public String writeItem() {
		value = "";
		if (checkBox.isSelected())
			value = "yes";

		return writeNode(token, value);
	}
}

class RadioItem extends InputItem {
	ButtonGroup buttonGroup;

	public RadioItem(Node n) throws NeosXMLException {
		super(n);
	}

	public JPanel createPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		buttonGroup = new ButtonGroup();
		NodeList nodelist = ((Element) node)
				.getElementsByTagName("neos:option");
		if (nodelist.getLength() == 0)
			return null;
		String value;
		String defaultString;
		String option;
		boolean chooseme = false;
		JRadioButton[] radio = new JRadioButton[nodelist.getLength()];

		for (int i = 0; i < nodelist.getLength(); i++) {
			option = NeosXMLParser.getText(nodelist.item(i));
			NamedNodeMap attribs = nodelist.item(i).getAttributes();
			if (attribs.getLength() >= 0) {
				value = NeosXMLParser.getText(attribs.getNamedItem("value"));
				defaultString = NeosXMLParser.getText(attribs
						.getNamedItem("default"));

				if ((defaultString != null)
						&& (defaultString.toLowerCase().compareTo("true") != 0))
					chooseme = true;
				radio[i] = new JRadioButton(option, chooseme);
				radio[i].setActionCommand(value);
				radio[i].setAlignmentX(Component.LEFT_ALIGNMENT);
				buttonGroup.add(radio[i]);
				panel.add(radio[i]);
			}
		}
		return panel;
	}

	public String writeItem() {
		return writeNode(token, buttonGroup.getSelection().getActionCommand());
	}

}

class NeosXMLException extends Exception {
	public NeosXMLException(String s) {
		super(s);
	}
}
