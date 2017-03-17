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
    private JPanel loginPane;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton createEmployeeButton;
    private JPanel createEmployeePane;
    private JTextField newPasswordField;
    private JTextField newEmploymentDateField;
    private JTextField newENameField;
    private JTextField newSalaryField;
    private JLabel newENameLabel;
    private JLabel newEmploymentDateLabel;
    private JLabel newSalaryLabel;
    private JLabel newPasswordLabel;

    private MontrealApparel() throws SQLException{
        setSize(1280,720);
        setContentPane(contentPane);
        setModal(true);

        /* ACTION LISTENERS FOR BUTTONS TO RESPOND */
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
        searchClothesButton.addActionListener(e -> onSearchClothes());
        submitClothButton.addActionListener(e -> onSubmitClothSearch());
        employeeLoginButton.addActionListener(e -> onEmployeeLogin());
        loginButton.addActionListener(e -> onLogin());
        createEmployeeButton.addActionListener(e -> onCreateEmployeeRequest());
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
    private void onSubmitClothSearch() {
        String color = colorDropDown.getSelectedItem().toString();
        String modelName = clothModelNameTextField.getText().toString();
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
        eidPanel.setVisible(false);
        loginPane.setVisible(false);
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
        searchClothesPanel.setVisible(false);
        loginPane.setVisible(false);
    }

    private void onCancel() {
        dispose();
    }

    private void onEmployeeLogin() {
        loginPane.setVisible(true);
        searchClothesPanel.setVisible(false);
        eidPanel.setVisible(false);
    }

    private void onLogin() {
        String uname = usernameField.getText();
        char[] pwd = passwordField.getPassword();
        if (uname.length() == 0 || pwd.length == 0) {
            outputPane.setText("Please enter a valid username and password!");
            return;
        }
        if (validatePassword(uname, pwd)) {
            outputPane.setText("Login successful!");
            createEmployeeButton.setVisible(true);
            createEmployeePane.setVisible(true);
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
     * @throws SQLException
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
     * <ul>
     *     This method takes in a clothing unit name as a 'String' and queries into the database, looking for the
     *     Colors that that unit has had. It does an equality join on CLOTHINGMODEL and CLOTHINGUNIT as well as checking
     *     the modelname to be what was given as a parameter.
     * </ul>
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
}
