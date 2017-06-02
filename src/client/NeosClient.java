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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.SwingConstants;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.AsyncCallback;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class NeosClient implements ActionListener {
	static String[] args;
	public JFrame mainFrame;
	JPanel solverPanel;
	JFrame adminFrame;
	NeosXMLParser solverParser;
	JPanel mainPanel;

	JPanel connectionPane;
	JTextField hostTextField;
	JTextField portTextField;
	
	JButton connectButton;
	Color mouseOverColor, normalColor;

	JTextField usernameTextField;
	JTextField userPasswordTextField;
	
	JButton submitButton;
	JTextArea messagePane;

	JTextField jobNumberText;
	JTextField passwordText;

	JMenuBar menuBar;
	JMenu solverMenu;
	JMenu jobsMenu;

	public JobTabs tabs;
	HashMap tabindices;

	JDialog errorBox;

	Integer currentJob;
	String currentPassword;
	HashMap categories;

	CommunicationWrapper neos;
	boolean connected;

	public final Font menuFont = new Font("Helvetica", Font.BOLD, 14);
	public final Font textFont = new Font("Monospaced", Font.PLAIN, 12);

	public static void main(String[] arguments) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		args = arguments;
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
		NeosClient client = new NeosClient();
	}

	/***************/
	/* Constructor */
	/***************/
	NeosClient() {

		// Create and set up the window.
		mainFrame = new JFrame("Neos Submission Tool");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(new Dimension(180, 40));

		// Add the widgets.
		addWidgets();
		addMenus();

		// Display the window.
		mainFrame.pack();
		mainFrame.setVisible(true);
	}

	private JLabel label(String labelText) {
		return new JLabel(labelText, SwingConstants.RIGHT);
	}
	
	void addWidgets() {

		// Create and set up the panels.
		JPanel hostPane = new JPanel();
		hostPane.setLayout(new GridLayout(8, 3));
		hostTextField = new JTextField("neos-server.org");
		portTextField = new JTextField("3333");
		connectButton = new JButton("Connect to Server");
		connectButton.setActionCommand("Connect to Server");
		connectButton.addActionListener(this);
		mouseOverColor = new Color(16, 192, 32); 
		connectButton.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				normalColor = connectButton.getBackground();
				connectButton.setOpaque(true);
				connectButton.setBackground(mouseOverColor);
			}
			public void mouseExited(MouseEvent e) {
				connectButton.setOpaque(false);
				connectButton.setBackground(normalColor);
			}
		});
		hostPane.add(label("")); hostPane.add(new JLabel("Server", SwingConstants.CENTER)); hostPane.add(label(""));
		hostPane.add(label("Host:")); hostPane.add(hostTextField); hostPane.add(label(""));
		hostPane.add(label("Port:")); hostPane.add(portTextField); hostPane.add(label(""));
		hostPane.add(label("")); hostPane.add(connectButton); hostPane.add(label(""));
		
		hostPane.add(label("")); hostPane.add(label("")); hostPane.add(label(""));
		
		usernameTextField = new JTextField();
		userPasswordTextField = new JTextField();
		hostPane.add(label("")); hostPane.add(new JLabel("Authenticated Job Settings", SwingConstants.CENTER)); hostPane.add(label(""));
		hostPane.add(label("Registered Username (optional):")); hostPane.add(usernameTextField); hostPane.add(label(""));
		hostPane.add(label("User Password (optional):")); hostPane.add(userPasswordTextField); hostPane.add(label(""));
		
		messagePane = new JTextArea(25, 50);
		messagePane.setEditable(false);

		connectionPane = new JPanel();
		connectionPane.setLayout(new BoxLayout(connectionPane,
				BoxLayout.PAGE_AXIS));
		connectionPane.add(hostPane);
		connectionPane.add(messagePane);

		tabs = new JobTabs(this);
		// NOTE: this tab name is hardcoded in a test in JobTabs...
		tabs.addTab("Connection / Settings", connectionPane);

	}

	void addMenus() {
		/** Create Menubars ******************************/
		menuBar = new JMenuBar();
		mainFrame.setJMenuBar(menuBar);

		/* File menu */
		JMenu fileMenu = new JMenu("File");
		fileMenu.setFont(menuFont);
		// Note: this item has all code commented out in the listener..so not bothering to show the menu item
/*		JMenuItem save = new JMenuItem("Save Text");
		fileMenu.add(save);
		save.addActionListener(this);*/
		fileMenu.addSeparator();
		JMenuItem exit = new JMenuItem("Exit");
		fileMenu.add(exit);
		exit.addActionListener(this);
		menuBar.add(fileMenu);

		/* Options menu */
		jobsMenu = new JMenu("Jobs");
		jobsMenu.setFont(menuFont);
		JMenuItem queueMenu = new JMenuItem("Queue");
		jobsMenu.add(queueMenu);
		queueMenu.addActionListener(this);
		JMenuItem resultsMenu = new JMenuItem("Results");
		jobsMenu.add(resultsMenu);
		resultsMenu.addActionListener(this);
		JMenuItem killMenu = new JMenuItem("Kill");
		jobsMenu.add(killMenu);
		killMenu.addActionListener(this);
		jobsMenu.addActionListener(this);
		jobsMenu.setEnabled(false);
		menuBar.add(jobsMenu);

		/* Solvers menu */
		solverMenu = new JMenu("Solvers");
		solverMenu.setFont(menuFont);
		solverMenu.setEnabled(false);
		menuBar.add(solverMenu);

		/* Help Menu */
//		JMenu aboutMenu = new JMenu("Help");
//		aboutMenu.setFont(menuFont);
//		aboutMenu.add(new JMenuItem("Submit Client Help"));
//		aboutMenu.add(new JMenuItem("Server Help"));
//		aboutMenu.add(new JMenuItem("About"));
//		aboutMenu.add(new JMenuItem("Comments & Questions"));
//		aboutMenu.addActionListener(this);
//		menuBar.add(aboutMenu);

	}

	/************************************************/
	/** Error Dialog ********************************/
	/************************************************/

	public void errorDialog(String errorMessage) {
		int mRows = 5, mCols = 35;
		errorBox = new JDialog(mainFrame, "Error", true);
		JTextArea message = new JTextArea(errorMessage, mRows, mCols);
		JScrollPane messagePane = new JScrollPane(message);
		JButton OKButton = new JButton("OK");
		OKButton.setActionCommand("close dialog box");
		OKButton.addActionListener(this);
		errorBox.getContentPane().add("Center", messagePane);
		errorBox.getContentPane().add("South", OKButton);
		errorBox.pack();
		errorBox.show();
	}

	public void actionPerformed(ActionEvent event) {
		String theSource = event.getActionCommand();
		System.out.println(theSource);
		// Handle Menu actions
		if (event.getSource() instanceof JMenuItem) {
			if (theSource.compareTo("Save Text") == 0) {
				boolean completed = false;
				// completed = saveAs(note.getText());
				if (completed == false) {
					// flashFeedback("\n\tNo file saved \n");
				}
			} else if (theSource.compareTo("Exit") == 0) {
				mainFrame.dispose();
				System.exit(0);
			} else if (theSource.compareTo("Queue") == 0) {
				try {
					printQueue();
				} catch (XmlRpcException e) {
					errorDialog(e.getMessage());
				}
			} else if (theSource.compareTo("Results") == 0) {
				showAdminForm("getFinalResults");
			} else if (theSource.compareTo("Kill") == 0) {
				showAdminForm("killJob");
			} else if (theSource.compareTo("About") == 0) {
				// aboutUs("Optimization Technolgy Center,\nArgonne National Lab and\nNorthwestern University,\nDecember 2004");
			} else if (theSource.compareTo("Comments & Questions") == 0) {
				// aboutUs("\nMail questions or comments to\n<neos-comments@mcs.anl.gov>\n");
			} else if (theSource.compareTo("Server Help") == 0) {
				// pass
			} else if (theSource.compareTo("Submit Client Help") == 0) {
				// pass
			} else if (theSource.compareTo("Update Solvers") == 0) {
				try {
					createSolverMenu();
				} catch (Exception e) {
					errorDialog("Solver Menu Build failed.\n" + e.getMessage());
				}
			} else { // Picked a Solver (theSource is "cat:solver:input")
				try {
					String[] names = theSource.split(":");
					buildSolverInterface(names[0], names[1], names[2]);
				} catch (XmlRpcException e) {
					errorDialog(e.getMessage());
				} catch (NeosXMLException e) {
					errorDialog(e.getMessage());
				}
			}

		} else if (event.getSource() instanceof JButton) {
			if (theSource.compareTo("Connect to Server") == 0) {
				messagePane.setText("...Connecting...");
				messagePane.setCaretPosition(0);
				messagePane.update(messagePane.getGraphics()); // force update now...
				try {
					Thread.sleep(1);
				}
				catch( Exception e) {
				}
				try {
					String neosHost = hostTextField.getText();
					String neosPort = portTextField.getText();

					XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
					config.setServerURL(new URL("https://" + neosHost + ":"
							+ neosPort));
					XmlRpcClient client = new XmlRpcClient();
					client.setConfig(config);

					neos = new CommunicationWrapper(client, this);
					connected = true;
				} catch (IOException e) {
					messagePane.setText("Connection Failed.\n" + e.getMessage());
					messagePane.setCaretPosition(0);
					//errorDialog("Connection Failed.\n" + e.getMessage());
				}

				try {
					messagePane.setText((String) neos.execute("welcome",
							new Vector(), 5000));
					messagePane.setCaretPosition(0);
					createSolverMenu();
				} catch (XmlRpcException e) {
					messagePane.setText(e.getMessage());
					messagePane.setCaretPosition(0);
					//errorDialog(e.getMessage());
				}

			} else if (theSource.compareTo("close dialog box") == 0) {
				errorBox.dispose();

			} else if (theSource.compareTo("Submit") == 0) {
				String username = usernameTextField.getText();
				String password = userPasswordTextField.getText();

				if (username != null && username.length() <= 0) {
					username = null;
				}
				if (password != null && password.length() <= 0) {
					password = null;
				}
				
				try {
					submitToNeos(username, password);
				} catch (XmlRpcException e) {
					errorDialog(e.getMessage());
				}
			} else if (theSource.compareTo("killJob") == 0) {
				try {
					submitAdmin("killJob");
				} catch (XmlRpcException e) {
					errorDialog(e.getMessage());
				}
			} else if (theSource.compareTo("getFinalResults") == 0) {
				try {
					submitAdmin("getFinalResults");
				} catch (XmlRpcException e) {
					errorDialog(e.getMessage());
				}
			} else if (theSource.startsWith("close tab")) {
				tabs.removeTab(theSource.substring(10));
			}

		}
	}

	void printQueue() throws XmlRpcException {
		Vector params = new Vector();
		String queue = (String) neos.execute("printQueue", params, 5000);
		messagePane.setText(queue);
		tabs.showConnectionTab();
	}

	void createSolverMenu() throws XmlRpcException {
		// Destroy previous solver menu
		solverMenu.removeAll();
		Vector params = new Vector();

		categories = (HashMap) neos.execute("listCategories", params, 5000);

		Iterator values = categories.values().iterator();
		params.setSize(1);
		JMenuItem menuItem;
		String solverName;
		while (values.hasNext()) {
			String cat = (String) (values.next());
			// Ignore kestrel solvers
			if (cat.compareTo("kestrel") == 0)
				continue;
			JMenu catMenu = new JMenu(cat);
			//catMenu.setFont(menuFont);
			params.set(0, cat);
			Object[] results = (Object[]) neos.execute("listSolversInCategory",
					params, 5000);

			for (int i = 0; i < results.length; i++) {
				solverName = (String) results[i];
				menuItem = new JMenuItem(solverName);
				menuItem.setActionCommand(cat + ":" + solverName);
				menuItem.addActionListener(this);
			//	menuItem.setFont(menuFont);
				catMenu.add(menuItem);
			}
			solverMenu.add(catMenu);
		}
		solverMenu.setEnabled(true);
		jobsMenu.setEnabled(true);
	}

	public void buildSolverInterface(String category, String solver,
			String inputMethod) throws XmlRpcException, NeosXMLException {

		// Get the solver xml string
		Vector params = new Vector();
		params.add(category);
		params.add(solver);
		params.add(inputMethod);

		String xmlstring = (String) neos.execute("getXML", params, 5000);

		solverParser = new NeosXMLParser();
		solverParser.parse(xmlstring);

		JPanel solverTab = solverParser.formGUI(this);
		tabs.addTab(solver, solverTab);

	}

	void submitToNeos(String username, String password) throws XmlRpcException {
		String xmldoc = solverParser.writeDocument();
		Vector params = new Vector();
		params.add(xmldoc);
		String command = "submitJob";
		if (username == null || password == null) {
			params.add("JAVA user");
			params.add("JAVA Submission Tool");
		}
		else {
			command = "authenticatedSubmitJob";
			params.add(username);
			params.add(password);
		}
		
		Object[] results = (Object[]) neos.execute(command, params);

		currentJob = (Integer) results[0];
		currentPassword = (String) results[1];

		if (currentJob == 0) {
			tabs.addJob(currentJob);//"Submission Failed");
			tabs.setText(currentJob, "Authentication Failed: ");
			tabs.appendText(currentJob, currentPassword);
			return;
		}
		
		tabs.addJob(currentJob);
		tabs.setBusy(currentJob);
		tabs.setText(currentJob, "Job #" + currentJob.toString()
				+ "submitted to NEOS.\n");

		params.clear();
		
		String buffer = (String) neos.execute("printQueue", params);
		tabs.appendText(currentJob, buffer);

		ResultsReceiver receiver = new ResultsReceiver(this, currentJob,
				currentPassword);
		receiver.start();

	}

	/*
	 * Runs either killJob or getFinalResults
	 */
	public void showAdminForm(String adminJob) {
		adminFrame = new JFrame(adminJob);
		JPanel adminPane = new JPanel();

		jobNumberText = new JTextField(10);
		JLabel jobNumberLabel = new JLabel("Enter Job Number");
		jobNumberLabel.setLabelFor(jobNumberText);

		passwordText = new JTextField(10);
		JLabel passwordLabel = new JLabel("Enter Password");
		passwordLabel.setLabelFor(passwordText);

		adminPane.add(jobNumberLabel);
		adminPane.add(jobNumberText);
		adminPane.add(passwordLabel);
		adminPane.add(passwordText);

		JButton submitButton = new JButton(adminJob);
		submitButton.setActionCommand(adminJob);
		submitButton.addActionListener(this);
		adminPane.add(submitButton);

		adminFrame.getContentPane().add(adminPane);
		adminFrame.pack();
		adminFrame.setVisible(true);
	}

	public void submitAdmin(String adminJob) throws XmlRpcException {
		Vector params = new Vector();
		System.out.println(jobNumberText.getText());
		try {
			params.add(new Integer(jobNumberText.getText()));
		} catch (NumberFormatException e) {
			errorDialog(e.getMessage());
			return;
		}
		System.out.println((Integer) params.firstElement());
		params.add((Object) passwordText.getText());
		Object result = neos.execute(adminJob, params);
		tabs.addJob(adminJob);
		if (result instanceof String) {
			tabs.setText(new Integer(0), (String) result);
		} else if (result instanceof byte[]) {
			tabs.setText(new Integer(0), new String((byte[]) result));
		} else
			throw new XmlRpcException(0, "Unkown response from NEOS");
		adminFrame.dispose();
	}
}

class ResultsReceiver extends Thread {
	Integer jobNumber;
	String password;
	NeosClient parent;

	public ResultsReceiver(NeosClient p, Integer job, String pass) {
		this.parent = p;
		this.jobNumber = job;
		this.password = pass;
	}

	public void run() {
		Vector params = new Vector();

		params.add(this.jobNumber);
		params.add(this.password);

		Vector offsetParams = new Vector();
		offsetParams.add(this.jobNumber);
		offsetParams.add(this.password);
		int offset = 0;
		offsetParams.add(new Integer(offset));
		String status;

		try {
			status = (String) parent.neos.execute("getJobStatus", params);
			while (status.compareTo("Done") != 0) {
				Object[] retval = (Object[]) parent.neos.execute(
						"getIntermediateResults", offsetParams);
				offsetParams.set(2, (Integer) retval[1]);
				Object output = (Object) retval[0];
				String newText;
				if (output instanceof String)
					newText = (String) output;
				else
					newText = new String((byte[]) output);
				parent.tabs.appendText(jobNumber, newText);
				System.out.println("getting Job Status");
				status = (String) parent.neos.execute("getJobStatus", params);
			}
			System.out.println("getting Final results.");
			Object retval = parent.neos.execute("getFinalResults", params);
			parent.tabs.setDone(jobNumber);
			if (retval instanceof String)
				parent.tabs.setText(jobNumber, (String) retval);
			else if (retval instanceof byte[])
				parent.tabs.setText(jobNumber, new String((byte[]) retval));
		} catch (XmlRpcException e) {
			parent.errorDialog(e.getMessage());
		}
	}
}

class CommunicationWrapper implements AsyncCallback {
	boolean connected;
	boolean waiting;
	Object response;
	NeosClient parent;
	XmlRpcClient server;
	Throwable exception;
	Date startTime;
	Stack stack;
	final static int sleepInterval = 100; // milliseconds

	public CommunicationWrapper(XmlRpcClient s, NeosClient p) {
		server = s;
		parent = p;
		connected = false;
	}

	public Object execute(String method, Vector params) throws XmlRpcException {
		try {
			return server.execute(method, params);
		} catch (Exception e) {
			throw new XmlRpcException(0, e.getMessage());
		}
	}

	public Object execute(String method, Vector params, long timeout)
			throws XmlRpcException {
		if (timeout < 0) {
			timeout = 1000000000L; // ~300 hrs
		}
		waiting = true;
		Cursor oldCursor = parent.mainFrame.getContentPane().getCursor();
		parent.mainFrame.getContentPane().setCursor(
				new Cursor(Cursor.WAIT_CURSOR));
		response = null;
		exception = null;
		startTime = new Date();
		server.executeAsync(method, params, this);
		while (waiting
				&& (new Date().getTime() - startTime.getTime() < timeout)) {
			try {
				Thread.sleep(sleepInterval);
			} catch (InterruptedException e) {
			}
		}
		parent.mainFrame.getContentPane().setCursor(oldCursor);
		if (exception != null)
			throw new XmlRpcException(0, exception.getMessage());
		if (response == null) {
			throw new XmlRpcException(0, "Error communicating with server.");
		}
		return response;
	}

	// public void handleError(java.lang.Exception e, java.net.URL url,
	// java.lang.String method) {
	// System.out.println("Error occured");
	// if (waiting) {
	// waiting = false;
	// exception = e;
	// response = null;
	// }
	// }
	//
	// public void handleResult(java.lang.Object result, java.net.URL url,
	// java.lang.String method) {
	// System.out.println("Normal execution");
	// if (waiting) {
	// waiting = false;
	// response = result;
	// }
	// }

	@Override
	public void handleError(XmlRpcRequest arg0, Throwable error) {

		System.out.println("Error occured");
		if (waiting) {
			waiting = false;
			exception = error;
			response = null;
		}
	}

	@Override
	public void handleResult(XmlRpcRequest arg0, Object result) {

		System.out.println("Normal execution");
		if (waiting) {
			waiting = false;
			response = result;
		}
	}

}

class JobTabs {
	NeosClient parent;
	JTabbedPane tabPane;
	Hashtable outputPanes; // indexed by jobnumber
	Hashtable oldCursors; // indexed by jobnumber

	public JobTabs(NeosClient p) {
		parent = p;
		tabPane = new JTabbedPane();
		parent.mainFrame.getContentPane().add(tabPane);
		outputPanes = new Hashtable();
		oldCursors = new Hashtable();
	}

	public void showConnectionTab() {
		tabPane.setSelectedIndex(0);
	}
	
	public void addTab(String name, JComponent c) {
		JPanel panel = new JPanel(new BorderLayout());
		if (name.compareTo("Connection / Settings") != 0) {
			JButton closeMe = new JButton("Close Tab");
			closeMe.setActionCommand("close tab " + name);
			closeMe.addActionListener(parent);
			panel.add(closeMe, BorderLayout.PAGE_START);
		}
		JScrollPane sp = new JScrollPane(c);
		panel.add(sp);
		tabPane.add(name, panel);
		tabPane.setSelectedComponent(panel);
	}

	public void addJob(Integer jobNumber) {
		JPanel panel = new JPanel(new BorderLayout());
		JButton closeMe = new JButton("Close Tab");
		closeMe.setActionCommand("close tab job #" + jobNumber.toString());
		closeMe.addActionListener(parent);

		JTextArea textArea = new JTextArea(30, 50);
		textArea.setEditable(false);
		outputPanes.put(jobNumber, textArea);
		panel.add(closeMe, BorderLayout.PAGE_START);
		JScrollPane scroller = new JScrollPane(textArea);
		panel.add(scroller, BorderLayout.CENTER);
		tabPane.add("job #" + jobNumber.toString(), panel);
		tabPane.setSelectedComponent(panel);
	}

	public void addJob(String s) {

	}

	public void setText(Integer jobNumber, String msg) {
		JTextArea textArea = (JTextArea) outputPanes.get(jobNumber);
		textArea.setText(msg);
	}

	public void appendText(Integer jobNumber, String msg) {
		JTextArea textArea = (JTextArea) outputPanes.get(jobNumber);
		textArea.setText(textArea.getText() + msg);
	}

	public void setBusy(Integer jobNumber) {
		JTextArea textArea = (JTextArea) outputPanes.get(jobNumber);
		oldCursors.put(jobNumber, textArea.getCursor());
		textArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
	}

	public void setDone(Integer jobNumber) {
		JTextArea textArea = (JTextArea) outputPanes.get(jobNumber);
		textArea.setCursor((Cursor) oldCursors.get(jobNumber));
	}

	public void removeTab(String tabName) {
		int tabIndex = tabPane.indexOfTab(tabName);
		if (tabIndex >= 0)
			tabPane.remove(tabIndex);
	}

	public void saveTab(String tabName) {

	}
}
