import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MontrealApparel extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonExit;
    private JButton viewYourOrdersButton;
    private JTextField employeeIDTextField;
    private JPanel eidPanel;
    private JButton submitEidButton;
    private JCheckBox thisMonthCheckBox;
    private JCheckBox allTimeCheckBox;
    private JTextPane outputPane;

    private MontrealApparel() throws SQLException{
        setSize(1280,720);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());
        buttonExit.addActionListener(e -> onCancel());
        viewYourOrdersButton.addActionListener(e -> onViewYourOrders());
        submitEidButton.addActionListener(e -> {
            try {
                onSubmitEid();
            } catch (SQLException e1) {
                int code = e1.getErrorCode();
                String state = e1.getSQLState();
                outputPane.setText("ERROR!!" + "\n" + "CODE: " + code + " STATE: " + state);
            }
        });
        thisMonthCheckBox.addActionListener(e -> onSelectThisMonth());
        allTimeCheckBox.addActionListener(e -> onSelectAllTime());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onSelectAllTime() {
        if (thisMonthCheckBox.isSelected()) {
            thisMonthCheckBox.setSelected(false);
        }
    }

    private void onSelectThisMonth() {
        if (allTimeCheckBox.isSelected()) {
            allTimeCheckBox.setSelected(false);
        }
    }

    private void onSubmitEid() throws SQLException{
        Statement s = MakeConnection();
        /* If employee wants to query orders of this month, we order by handle date so that he knows what has to be done first */
        if (thisMonthCheckBox.isSelected()) {
            // Clear out the previous text in the output pane
            outputPane.setText("");
            // Get current year and month and append a "-01" at the end of that so that it's the beginning of this month
            DateFormat df = new SimpleDateFormat("yyyy-MM");
            Date dt = new Date();
            String thisMonth = df.format(dt) + "-01";
            String eid = employeeIDTextField.getText();
            // MAKE SURE YOU ADD A ' BEFORE AND AFTER SENDING IN A DATE TO QUERY!
            String condition = "ORDER.HANDLER = " + eid + " AND ORDER.HANDLEDATE > '" + thisMonth + "'";
            ResultSet rs = SendStandardQuery(s, "DISTINCT ORDERID, HANDLEDATE", "EMPLOYEE, ORDER", condition, "HANDLEDATE");
            while(rs.next()) {
                int orderID = rs.getInt(1);
                String handleDate = rs.getDate(2).toString();
                outputPane.setText(outputPane.getText() +
                                    "Order ID: " + orderID + "\n" +
                                    "Handling deadline: " + handleDate + "\n" +
                                    "##############" + "\n");
            }
            /* If employee wants to query orders of all time, we order by ORDERID */
        } else if (allTimeCheckBox.isSelected()) {
            outputPane.setText("");
            String eid = employeeIDTextField.getText();
            String condition = "ORDER.HANDLER = " + eid;
            ResultSet rs = SendStandardQuery(s, "DISTINCT ORDERID, HANDLEDATE", "EMPLOYEE, ORDER", condition, "ORDERID");
            while(rs.next()) {
                int orderID = rs.getInt(1);
                String handleDate = rs.getDate(2).toString();
                outputPane.setText(outputPane.getText() +
                        "Order id: " + orderID + "\n" +
                        "Handling deadline: " + handleDate + "\n" +
                        "##############" + "\n");
            }
        }
    }

    private void onViewYourOrders() {
        eidPanel.setVisible(true);
    }

    private void onOK() {

    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    private Statement MakeConnection() throws SQLException{
        String url = "jdbc:db2://comp421.cs.mcgill.ca:50000/cs421";
        Connection con = DriverManager.getConnection(url, "CS421G42", "cashMeOutside420");
        return con.createStatement();
    }

    public static void main(String[] args) throws SQLException
    {
        // CONNECTING TO DB
        try {
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
        } catch (Exception classNotFoundException) {
            System.out.println("Class not found!");
        }
        String url = "jdbc:db2://comp421.cs.mcgill.ca:50000/cs421";
        Connection con = DriverManager.getConnection(url, "CS421G42", "cashMeOutside420");
        Statement statement = con.createStatement();

        MontrealApparel dialog = new MontrealApparel();
        dialog.setVisible(true);

        // EXIT PROGRAM
        statement.close();
        con.close();
        System.exit(0);
    }

    private ResultSet SendStandardQuery(Statement statement, String select, String table, String condition, String ordering) {
        int sqlCode;
        String sqlState;
        try {
            return statement.executeQuery("SELECT " + select + " FROM " + table + " WHERE " + condition + " ORDER BY " + ordering);

        } catch (SQLException e) {
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            outputPane.setText("ERROR!!" + "\n" + "CODE: " + sqlCode + " STATE: " + sqlState);
        }
        return null;
    }
}
