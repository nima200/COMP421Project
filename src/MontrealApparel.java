import java.text.ParseException;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MontrealApparel extends JDialog {
    private JPanel contentPane;
    private JButton buttonExit;
    private JButton viewYourOrdersButton;
    private JTextField employeeIDTextField;
    private JPanel eidPanel;
    private JButton submitEidButton;
    private JCheckBox thisMonthCheckBox;
    private JCheckBox allTimeCheckBox;
    private JTextPane outputPane;
    private JButton searchClothesButton;
    private JPanel searchClothesPanel;
    private JTextField clothModelNameTextField;
    private JComboBox colorDropDown;
    private JButton submitClothButton;
    private JButton employeeLoginButton;
    private JPanel loginPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton createEmployeeButton;
    private JPanel createEmployeePanel;
    private JTextField newPasswordField;
    private JTextField newEmploymentDateField;
    private JTextField newENameField;
    private JTextField newSalaryField;
    private JPanel changeOrderPanel;
    private JTextField orderIDField;
    private JButton searchOrderButton;
    private JButton viewCardInfoButton;
    private JButton refundOrderButton;
    private JButton modifyOrderButton;
    private String currentEmployee;

    private MontrealApparel() throws SQLException{
        setSize(1280,720);
        setContentPane(contentPane);
        setModal(true);

        /* ACTION LISTENERS FOR BUTTONS TO RESPOND */
        buttonExit.addActionListener(e -> onCancel());
        viewYourOrdersButton.addActionListener(e -> onViewYourOrders());
        submitEidButton.addActionListener(e -> onSubmitEid());
        thisMonthCheckBox.addActionListener(e -> onSelectThisMonth());
        allTimeCheckBox.addActionListener(e -> onSelectAllTime());
        // When 'Enter' is pressed on the cloth model name search field, we query for the colors of that unit
        clothModelNameTextField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    QueryUnitColors(clothModelNameTextField.getText());
                }
            }
        });
        passwordField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onLogin();
                }
            }
        });
        usernameField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onLogin();
                }
            }
        });
        newPasswordField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onCreateEmployeeRequest();
                }
            }
        });
        orderIDField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onSearchOrder();
                }
            }
        });
        searchClothesButton.addActionListener(e -> onSearchClothes());
        submitClothButton.addActionListener(e -> onSubmitClothSearch());
        employeeLoginButton.addActionListener(e -> onEmployeeLogin());
        loginButton.addActionListener(e -> onLogin());
        createEmployeeButton.addActionListener(e -> onCreateEmployeeRequest());
        searchOrderButton.addActionListener(e -> onSearchOrder());
        refundOrderButton.addActionListener(e -> onRefundOrder());
        modifyOrderButton.addActionListener(e -> onModifyOrder());
        viewCardInfoButton.addActionListener(e -> onViewCardInfo());
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE key pressed
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    /* ##########################################
     * #   METHODS TRIGGERED ON BUTTON CLICKS   #
     * ########################################## */
    private void onViewCardInfo() {
        String oid = (orderIDField.getText().length() > 0) ? orderIDField.getText() : "";
        if (oid.length() == 0) {
            outputPane.setText("Please enter a valid order ID!");
        } else {
            try {
                Statement s = MakeConnection();
                ResultSet rs = s.executeQuery("SELECT MODELNAME, SIZE, COLOR, QUANTITY FROM CARTDETAILS, ORDER WHERE CARTDETAILS.CARTID = ORDER.CARTID AND ORDERID = " + oid);
                outputPane.setText(outputPane.getText() + "\n\n" + "########################" + "\n" +
                        "CART DETAILS: " + "\n########################\n\n");
                while (rs.next()) {
                    String mname = rs.getString(1);
                    String size = rs.getString(2);
                    String color = rs.getString(3);
                    String quantity = rs.getString(4);
                    outputPane.setText(outputPane.getText() + "Model Name: " + mname +
                            "\nModel Size: " + size + "\nModel Color: " + color + "\nQuantity: " + quantity +
                            "\n-------------\n");
                }
                outputPane.setText(outputPane.getText() + "\n########################\n");
            } catch (SQLException e) {
                outputPane.setText("Connection failed!");
            }
        }
    }

    private void onModifyOrder() {
        eidPanel.setVisible(false);
        changeOrderPanel.setVisible(true);
        loginPanel.setVisible(false);
        searchClothesPanel.setVisible(false);
    }

    private void onRefundOrder() {
        try {
            Statement s = MakeConnection();
            ResultSet rs = s.executeQuery("SELECT EMPLOYEE.EID, EMPLOYEE.ENAME FROM EMPLOYEE WHERE EID = " + currentEmployee);
            String eid = "";
            String ename = "";
            String oid = "";
            while (rs.next()) {
                eid = rs.getString(1);
                ename = rs.getString(2);
                oid = orderIDField.getText();
                if (!checkIfPurchase(oid)) {
                    outputPane.setText("ORDER WAS ALREADY REFUNDED!\n##############################");
                    onSearchOrder();
                    return;
                }
            }
            s.executeUpdate("UPDATE ORDER " +
                    "SET ORDERTYPE = 'REFUNDED ON " + getToday() + " BY: " + ename + ", ID: " + eid + "' " +
                    "WHERE ORDERID = " + oid);
            String currentText = outputPane.getText();
            outputPane.setText("ORDER WAS REFUNDED!\n##############################\n\n" + currentText);
        } catch (SQLException e) {
            int code = e.getErrorCode();
            String state = e.getSQLState();
            System.out.println("CODE: " + code + " STATE: " + state);
            outputPane.setText("Connection failed!");
        }
    }

    private void onSearchOrder() {
        try {
            Statement s = MakeConnection();
                String orderid = orderIDField.getText();
                if (orderid.length() == 0) {
                    outputPane.setText("Please enter an order ID!");
                } else if (!checkIfOrderValid(orderid)) {
                    outputPane.setText("Order was not found! Please try again.");
                } else {
                    viewCardInfoButton.setVisible(true);
                    refundOrderButton.setVisible(true);
                    ResultSet rs = s.executeQuery("SELECT * FROM ORDER WHERE ORDERID = " + orderid);
                    while (rs.next()) {
                        String oid = rs.getString(1);
                        String otype = rs.getString(2);
                        String oPaymentMethod = rs.getString(3);
                        String oFinalAmount = rs.getString(4);
                        String oCustomerEmail = rs.getString(5);
                        String oHandleDate = rs.getString(6);
                        String oHandler = rs.getString(7);
                        String oShippingID = rs.getString(8);
                        String oBillingID = rs.getString(9);
                        String oCartID = rs.getString(10);
                        String oTrackingNumber = rs.getString(11);
                        if (outputPane.getText().equals("ORDER WAS ALREADY REFUNDED!\n##############################")) {
                            outputPane.setText(outputPane.getText() + "\n" + "ORDER DETAILS: \n--------------------------------------\n" +
                                    "Order ID: " + oid + "\n" +
                                    "Order Type: " + otype + "\n" +
                                    "Order Payment Method: " + oPaymentMethod + "\n" +
                                    "Order Final Amount: " + oFinalAmount + "\n" +
                                    "Order Customer Email: " + oCustomerEmail + "\n" +
                                    "Order Handle Date: " + oHandleDate + "\n" +
                                    "Order Handler: " + oHandler + "\n" +
                                    "Order Shipping ID: " + oShippingID + "\n" +
                                    "Order Billing ID: " + oBillingID + "\n" +
                                    "Order Card ID: " + oCartID + "\n" +
                                    "Order Shipping Tracking Number: " + oTrackingNumber + "\n" +
                                    "--------------------------------------");
                        } else {
                            outputPane.setText("ORDER DETAILS: \n--------------------------------------\n" +
                                    "Order ID: " + oid + "\n" +
                                    "Order Type: " + otype + "\n" +
                                    "Order Payment Method: " + oPaymentMethod + "\n" +
                                    "Order Final Amount: " + oFinalAmount + "\n" +
                                    "Order Customer Email: " + oCustomerEmail + "\n" +
                                    "Order Handle Date: " + oHandleDate + "\n" +
                                    "Order Handler: " + oHandler + "\n" +
                                    "Order Shipping ID: " + oShippingID + "\n" +
                                    "Order Billing ID: " + oBillingID + "\n" +
                                    "Order Card ID: " + oCartID + "\n" +
                                    "Order Shipping Tracking Number: " + oTrackingNumber + "\n" +
                                    "--------------------------------------");
                        }
                    }
                }
        } catch (SQLException e) {
            outputPane.setText("Connection Failed!");
        }
    }

    private void onSubmitClothSearch() {
        String color = colorDropDown.getSelectedItem().toString();
        String modelName = clothModelNameTextField.getText();
        try {
            Statement s = MakeConnection();
            ResultSet rs = s.executeQuery("SELECT DISTINCT CLOTHINGMODEL.MODELNAME, CLOTHINGUNIT.COLOR, CLOTHINGUNIT.RESTOCKDATE, UNITSTOCKING.QUANTITYAVAILABLE " +
                    "FROM CLOTHINGUNIT, CLOTHINGMODEL, UNITSTOCKING " +
                    "WHERE CLOTHINGMODEL.MODELNAME = CLOTHINGUNIT.MODELNAME AND " +
                    "CLOTHINGMODEL.MODELNAME = UNITSTOCKING.MODELNAME AND " +
                    "CLOTHINGUNIT.COLOR = UNITSTOCKING.COLOR AND " +
                    "CLOTHINGUNIT.COLOR = '" + color + "' AND " +
                    "CLOTHINGMODEL.MODELNAME = '" + modelName +
                    "' GROUP BY CLOTHINGMODEL.MODELNAME, CLOTHINGUNIT.COLOR, RESTOCKDATE, QUANTITYAVAILABLE");
            while (rs.next()) {
                String mname = rs.getString(1);
                String col = rs.getString(2);
                String restockDate = rs.getString(3);
                String qAvail = rs.getString(4);
                outputPane.setText(outputPane.getText() +
                                    "Model Name: " + mname + "\n" +
                                    "Color: " + col + "\n" +
                                    "Restock Date: " + restockDate + "\n" +
                                    "Quantity Available: " + qAvail + "\n" +
                                    "##############################" + "\n");
            }
        } catch (SQLException e) {
            System.out.println("Connection failed!");
        }
    }

    private void onSearchClothes() {
        searchClothesPanel.setVisible(true);
        changeOrderPanel.setVisible(false);
        eidPanel.setVisible(false);
        loginPanel.setVisible(false);
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

    private void onSubmitEid() {
        try {
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
                while (rs.next()) {
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
                while (rs.next()) {
                    int orderID = rs.getInt(1);
                    String handleDate = rs.getDate(2).toString();
                    outputPane.setText(outputPane.getText() +
                            "Order id: " + orderID + "\n" +
                            "Handling deadline: " + handleDate + "\n" +
                            "##############" + "\n");
                }
            }
        } catch (SQLException e) {
            outputPane.setText("Connection failed!");
        }
    }

    private void onViewYourOrders() {
        eidPanel.setVisible(true);
        searchClothesPanel.setVisible(false);
        changeOrderPanel.setVisible(false);
        loginPanel.setVisible(false);
    }

    private void onCancel() {
        dispose();
    }

    private void onEmployeeLogin() {
        loginPanel.setVisible(true);
        changeOrderPanel.setVisible(false);
        searchClothesPanel.setVisible(false);
        eidPanel.setVisible(false);
    }

    private void onLogin() {
        String uname = usernameField.getText();
        currentEmployee = uname;
        char[] pwd = passwordField.getPassword();
        if (uname.length() == 0 || pwd.length == 0) {
            outputPane.setText("Please enter a valid username and password!");
            return;
        }
        if (validatePassword(uname, pwd)) {
            outputPane.setText("Login successful!");
            modifyOrderButton.setVisible(true);
            createEmployeeButton.setVisible(true);
            createEmployeePanel.setVisible(true);
            searchClothesButton.setVisible(true);
            viewYourOrdersButton.setVisible(true);
        }
    }

    private void onCreateEmployeeRequest() {
        String newEName = newENameField.getText();
        if (newEName.length() > 20) {
            outputPane.setText("Employee name can only be upto 20 characters. Please try again.");
            return;
        }
        if (newEName.length() == 0) {
            outputPane.setText("Please enter an employee name!");
            return;
        }
        String newEDate = newEmploymentDateField.getText();
        if (!isValidDateFormat("yyyy-MM-dd", newEDate)) {
            outputPane.setText("Invalid date format! Try again with YYYY-MM-DD");
            return;
        }
        String newSalary = newSalaryField.getText();
        if (newSalary.length() == 0) {
            outputPane.setText("Please enter an employee salary!");
            return;
        }
        String newPassword = newPasswordField.getText();
        if (newPassword.length() > 25 || newPassword.length() < 8) {
            outputPane.setText("Password should be between 8 and 25 characters long. Please try again.");
            return;
        }
        // If all info entered is correct then we can send the query to the database!
        try {
            Statement s = MakeConnection();
            try {
                s.executeUpdate("INSERT INTO EMPLOYEE (ENAME, EMPLOYMENTDATE, SALARY, PASSWORD) VALUES ('" + newEName + "', '" + newEDate + "', " + newSalary + ", '" + newPassword + "')");
                ResultSet rs = s.executeQuery("SELECT EID FROM EMPLOYEE WHERE ENAME = '" + newEName + "' AND PASSWORD = '" + newPassword + "'");
                while (rs.next()) {
                    String newEID = rs.getString(1);
                    outputPane.setText("New employee created with EID: " + newEID +
                            "\nEmployee Name: " + newEName +
                            "\nEmployment Date: " + newEDate +
                            "\nSalary: " + newSalary +
                            "\nPassword: " + newPassword);
                    newENameField.setText("");
                    newEmploymentDateField.setText("");
                    newSalaryField.setText("");
                    newPasswordField.setText("");

                }
            } catch (SQLDataException e){
                outputPane.setText("Invalid data entered. Please try again!");
                return;
            }
        } catch (SQLException e) {
            int code = e.getErrorCode();
            String state = e.getSQLState();
            System.out.println("CODE: " + code + " STATE: " + state);
            outputPane.setText("Connection failed! Try again");
            return;
        }
    }

    /* ####################################
    *  #         HELPER METHODS           #
    *  #################################### */
    /**
     * Authenticates password with database to check if the correct password was given for the employee requested
     * @param uname         The EID which is used as an identifier for the employee
     * @param pwd           The provided password for the employee
     * @return              Result of authentication. True = successful, False = Failed
     */
    private boolean validatePassword(String uname, char[] pwd) {
        try {
            Statement s = MakeConnection();
            ResultSet rs = s.executeQuery("SELECT PASSWORD FROM EMPLOYEE WHERE EID = " + uname);
            if (rs.next()) {
                char[] correctpwd = rs.getString(1).toCharArray();
                if (correctpwd.length == 0 || pwd.length != correctpwd.length) {
                    outputPane.setText("Invalid username or password. Try again");
                    return false;
                } else {
                    for (int i = 0; i < correctpwd.length; i++) {
                        if (correctpwd[i] != pwd[i]) {
                            outputPane.setText("Invalid password entered! Try again.");
                            return false;
                        }
                    }
                    return true;
                }
            } else {
                outputPane.setText("Employee not found!");
                return false;
            }
        } catch (SQLException e) {
            outputPane.setText("Connection to database failed!");
            return false;
        }
    }

    /**
     * Attempts to connect to the database
     * @return        A statement that can be used to transfer queries/updates/etc to the database
     * @throws SQLException         In case the connection fails with the server, a SQL Exception is thrown
     */
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

    /**
     * Checks for valid date format
     * @param format    Date format to check for
     * @param value     Date value to check
     * @return          Result of value format being the given format to check
     */
    private static boolean isValidDateFormat(String format, String value) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            date = sdf.parse(value);
            if (!value.equals(sdf.format(date))) {
                date = null;
            }
        } catch (ParseException e) {
            System.out.println("Parse error");
        }
        return date != null;
    }

    /* #######################################
    *  #            QUERY METHODS            #
    *  ####################################### */

    /**
     * The general purpose of this method is to send a very standard query to the database. It consists of projection,
     * conditional selection, and a simple ordering at the end (which if not provided is not passed on to the query)
     * @param statement             The statement needed to execute a query on
     * @param select                The projection
     * @param table                 The table(s) to scan
     * @param condition             The selection condition
     * @param ordering              The ordering
     * @return                      The set which contains the output of the query
     */
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
    /**
     * The general purpose of this method is to send a very standard query to the database. It consists of projection,
     * conditional selection, and a simple ordering at the end (which if not provided is not passed on to the query)
     * @param statement             The statement needed to execute a query on
     * @param select                The projection
     * @param table                 The table(s) to scan
     * @param condition             The selection condition
     * @return                      The set which contains the output of the query
     */
    private ResultSet SendStandardQuery(Statement statement, String select, String table, String condition) {
        int sqlCode;
        String sqlState;
        try {
            return statement.executeQuery("SELECT " + select + " FROM " + table + " WHERE " + condition);
        } catch (SQLException e) {
            sqlCode = e.getErrorCode();
            sqlState = e.getSQLState();
            outputPane.setText("ERROR!!" + "\n" + "CODE: " + sqlCode + " STATE: " + sqlState);
        }
        return null;
    }
    /**
     * This method takes in a clothing unit name as a 'String' and queries into the database, looking for the
     * Colors that that unit has had. It does an equality join on CLOTHINGMODEL and CLOTHINGUNIT as well as checking
     * the model name to be what was given as a parameter.
     * @param clothingUnitName      The name of the clothing unit to find colors for.
     */
    private void QueryUnitColors(String clothingUnitName) {
        try {
            Statement s = MakeConnection();
            try {
                List<String> colors = new ArrayList<>();
                ResultSet resultSet = s.executeQuery("SELECT COLOR FROM CLOTHINGUNIT, CLOTHINGMODEL WHERE CLOTHINGMODEL.MODELNAME " +
                        "= CLOTHINGUNIT.MODELNAME AND CLOTHINGUNIT.MODELNAME = " + "'" + clothingUnitName +
                        "'" + " GROUP BY COLOR");
                while(resultSet.next()) {
                    colors.add(resultSet.getString(1));
                }
                colorDropDown.setModel(new DefaultComboBoxModel(colors.toArray()));
            } catch (SQLException e1) {
                outputPane.setText(clothModelNameTextField.getText() + " was not a valid search term!" +
                        "\n Perhaps it is not found in the database.");
            }
        } catch (SQLException e1) {
            outputPane.setText("Connection failed!");
        }
    }

    /**
     * Simple method to get today's date
     * @return      Today's date in yyyy-MM-dd format
     */
    private static String getToday() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date dt = new Date();
        return df.format(dt);
    }

    /**
     * Given an order ID, this method queries into the database to check if the order was a purchase or not.
     * @param orderid       The order ID
     * @return              The result of the check. True if order type was 'PURCHASE', false if anything else.
     */
    private boolean checkIfPurchase(String orderid) {
        try {
            Statement s = MakeConnection();
            ResultSet rs = s.executeQuery("SELECT ORDERTYPE from ORDER WHERE ORDERID = " + orderid);
            if (rs.next()) {
                String otype = rs.getString(1);
                return otype.equals("PURCHASE");
            }
        } catch (SQLException e) {
            outputPane.setText("Connection failed!");
        }
        return false;
    }

    /**
     * Simple method to check if an order exists or not
     * @param orderid           The order id to check for
     * @return                  True or false whether the order exists in the database or not.
     */
    private boolean checkIfOrderValid(String orderid) {
        try {
            Statement s = MakeConnection();
            ResultSet rs = s.executeQuery("SELECT * FROM ORDER WHERE ORDERID = " + orderid);
            return rs.next();
        } catch (SQLException e) {
            outputPane.setText("Connection Failed!");
            return false;
        }
    }
}