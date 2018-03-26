import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.border.BevelBorder;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Color;
import java.awt.Font;


public class GUI extends JFrame {
	
	class TableModel extends AbstractTableModel {
		private Node _host;
		
		public TableModel(Node host)
		{
			_host = host;
		}
		
		public int getRowCount()
		{
			return _host.peerCount();
		}
		
		public int getColumnCount()
		{
			return 1;
		}
		public Object getValueAt(int row, int column)
		{
			return _host.peerName(row);
		}
		
		public String getColumnName(int col) 
		{
		      return "Peers";
		}
	}

	private JPanel contentPane;
	private JTextField txtProductID;
	private JTextField txtPeerAddress;
	private JTextField txtHostAddress;
	private JTable tblPeers;
	
	private Node _host;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Node host = new Node();
		 Node host2 = new Node();
		 Node host3 = new Node();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI frame = new GUI(host, "Product Chain - Node");
					frame.setVisible(true);
					 GUI frame2 = new GUI(host2, "Product Chain - Node 2");
					 frame2.setVisible(true);
					 GUI frame3 = new GUI(host3, "Product Chain - Node 3");
					 frame3.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public GUI(Node host, String title) {
		_host = host;
		setTitle(title);
		
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 528, 455);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblStatusBar = new JLabel("");
		lblStatusBar.setFont(new Font("Arial", Font.PLAIN, 10));
		lblStatusBar.setForeground(Color.BLACK);
		lblStatusBar.setBounds(10, 406, 492, 16);
		lblStatusBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		contentPane.add(lblStatusBar);
		_host.setStatusBar(lblStatusBar);

		JEditorPane editProductDescription = new JEditorPane();
		editProductDescription.setBounds(10, 235, 286, 160);
		contentPane.add(editProductDescription);

		txtProductID = new JTextField();
		txtProductID.setBounds(10, 155, 492, 20);
		contentPane.add(txtProductID);
		txtProductID.setColumns(10);

		JButton btnSubmitTransaction = new JButton("Submit Transaction");
		btnSubmitTransaction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_host.generateTransaction( editProductDescription.getText() );
			}
		});
		btnSubmitTransaction.setBounds(306, 235, 196, 20);
		btnSubmitTransaction.setEnabled(false);
		contentPane.add(btnSubmitTransaction);
		
		JButton btnMineBlock = new JButton("Mine Block");
		btnMineBlock.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_host.mineBlock();
			}
		});
		btnMineBlock.setBounds(306, 266, 196, 23);
		contentPane.add(btnMineBlock);
		
		JButton btnCreateProduct = new JButton("Create Product");
		btnCreateProduct.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_host.createProduct();
				txtProductID.setText(_host.productID());
				editProductDescription.setText("");
				btnSubmitTransaction.setEnabled(true);
			}
		});
		btnCreateProduct.setBounds(306, 186, 196, 23);
		contentPane.add(btnCreateProduct);
		
		txtPeerAddress = new JTextField();
		txtPeerAddress.setBounds(107, 70, 99, 20);
		contentPane.add(txtPeerAddress);
		txtPeerAddress.setColumns(10);
		
		JLabel lblHostAddress = new JLabel("Host Address");
		lblHostAddress.setBounds(10, 11, 87, 14);
		contentPane.add(lblHostAddress);
		
		txtHostAddress = new JTextField();
		txtHostAddress.setEditable(false);
		txtHostAddress.setBounds(107, 8, 99, 20);
		contentPane.add(txtHostAddress);
		txtHostAddress.setColumns(10);
		txtHostAddress.setText(_host.hostAddress());
		
		JLabel lblPeerAddress = new JLabel("Peer Address");
		lblPeerAddress.setBounds(10, 73, 87, 14);
		contentPane.add(lblPeerAddress);
		
		JLabel lblProductId = new JLabel("Product ID");
		lblProductId.setBounds(10, 140, 87, 14);
		contentPane.add(lblProductId);
		
		JButton btnPrevProductDescription = new JButton("<");
		btnPrevProductDescription.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String description = _host.previousProductDescription();
				if(description != null)
				{
					editProductDescription.setText(description);
				}
			}
		});
		btnPrevProductDescription.setBounds(10, 186, 41, 23);
		btnPrevProductDescription.setEnabled(false);
		contentPane.add(btnPrevProductDescription);
		
		JButton btnNextProductDescription = new JButton(">");
		btnNextProductDescription.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String description = _host.nextProductDescription();
				if(description != null)
				{
					editProductDescription.setText(description);
				}
			}
		});
		btnNextProductDescription.setBounds(255, 186, 41, 23);
		btnNextProductDescription.setEnabled(false);
		contentPane.add(btnNextProductDescription);

		JLabel lblProductDescription = new JLabel("Product Description");
		lblProductDescription.setBounds(10, 220, 286, 14);
		contentPane.add(lblProductDescription);
		
		JButton btnShowProductDescription = new JButton("V   Product Description   V");
		btnShowProductDescription.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String productID = txtProductID.getText();
				editProductDescription.setText(_host.latestProductDescription(productID));
				if( productID.length() == 0 || !productID.equals(_host.productID()) )
				{
					btnSubmitTransaction.setEnabled(false);
				}
				else
				{
					btnSubmitTransaction.setEnabled(true);
				}
				
				if(productID.length() != 0)
				{
					btnPrevProductDescription.setEnabled(true);
					btnNextProductDescription.setEnabled(true);
				}
			}
		});
		btnShowProductDescription.setBounds(61, 186, 184, 23);
		contentPane.add(btnShowProductDescription);
		
		JLabel lblHostPort = new JLabel("Host Port");
		lblHostPort.setBounds(10, 36, 87, 14);
		contentPane.add(lblHostPort);
		
		JLabel lblPeerPort = new JLabel("Peer Port");
		lblPeerPort.setBounds(10, 97, 87, 14);
		contentPane.add(lblPeerPort);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(306, 11, 196, 132);
		contentPane.add(scrollPane);
		
		tblPeers = new JTable();
		scrollPane.setViewportView(tblPeers);
		tblPeers.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		TableModel peerTableModel = new TableModel(_host);
		tblPeers.setModel( peerTableModel );
		_host.setPeerTable(peerTableModel);
		
		JSpinner spinHostPort = new JSpinner();
		spinHostPort.setModel(new SpinnerNumberModel(10000, 0, 65535, 1));
		spinHostPort.setBounds(107, 33, 99, 20);
		contentPane.add(spinHostPort);
		
		JSpinner spinPeerPort = new JSpinner();
		spinPeerPort.setModel(new SpinnerNumberModel(10000, 0, 65535, 1));
		spinPeerPort.setBounds(107, 94, 99, 20);
		contentPane.add(spinPeerPort);
		tblPeers.getColumnModel().getColumn(0).setResizable(false);

		JButton btnAddPeer = new JButton(">>");
		btnAddPeer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_host.addPeer(txtPeerAddress.getText(), (int)spinPeerPort.getValue());
			}
		});
		btnAddPeer.setBounds(231, 78, 49, 23);
		contentPane.add(btnAddPeer);
		
		JButton btnStart = new JButton("Start");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				_host.startListening( (int)spinHostPort.getValue() );
				btnStart.setEnabled(false);
			}
		});
		btnStart.setBounds(218, 11, 78, 39);
		contentPane.add(btnStart);
		
		JButton btnPrintBlockchainData = new JButton("Print Blockchain Data");
		btnPrintBlockchainData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				_host.printBlockchainData();
			}
		});
		btnPrintBlockchainData.setBounds(306, 372, 196, 23);
		contentPane.add(btnPrintBlockchainData);
		
		JButton btnPrintTransactionPoolData = new JButton("Print Transaction Pool Data");
		btnPrintTransactionPoolData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_host.printTransactionPoolData();
			}
		});
		btnPrintTransactionPoolData.setBounds(306, 338, 196, 23);
		contentPane.add(btnPrintTransactionPoolData);
		
	}
}
