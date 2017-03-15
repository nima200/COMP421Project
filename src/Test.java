import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.awt.*;
import javax.swing.*;

public class Test {
    private JFrame _mainFrame;
    private JLabel _headerLabel;
    private JLabel _statusLabel;
    private JPanel _controlPanel;


    public Test() {
        PrepareGUI();
    }

    private void PrepareGUI() {
        _mainFrame = new JFrame("Test!");
        _mainFrame.setSize(600,600);
        _mainFrame.setLayout(new GridLayout(3,1));
        _headerLabel = new JLabel("",JLabel.CENTER);
        _statusLabel = new JLabel("",JLabel.CENTER);
        _statusLabel.setSize(350,100);
        // FOR PROGRAM TO EXIT WHEN PAGE CLOSED!
        _mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        _controlPanel = new JPanel();
        _controlPanel.setLayout(new FlowLayout());
        _mainFrame.add(_headerLabel);
        _mainFrame.add(_statusLabel);
        _mainFrame.add(_controlPanel);
        _mainFrame.setVisible(true);
    }

    private void ShowEventDemo() {
        _headerLabel.setText("Control in action: Button");

        // CREATION OF THE BUTTONS
        JButton okButton = new JButton("OK");
        JButton submitButton = new JButton("Submit");
        JButton cancelButton = new JButton("Cancel");
        // SUBMITTING THE COMMANDS THAT THESE BUTTONS SEND TO LISTENER
        okButton.setActionCommand("OK");
        submitButton.setActionCommand("SUBMIT");
        cancelButton.setActionCommand("CANCEL");
        // CONNECTING THE BUTTONS TO LISTENERS
        okButton.addActionListener(new ButtonClickListener());
        submitButton.addActionListener(new ButtonClickListener());
        cancelButton.addActionListener(new ButtonClickListener());

        _controlPanel.add(okButton);
        _controlPanel.add(submitButton);
        _controlPanel.add(cancelButton);

        _mainFrame.setVisible(true);
    }
    public static void main(String[] args) throws SQLException{
        try {
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
        } catch (Exception classNotFoundException) {
            System.out.println("Class not found!");
        }
        String url = "jdbc:db2://comp421.cs.mcgill.ca:50000/cs421";
        Connection con = DriverManager.getConnection(url, "CS421G42", "cashMeOutside420");
        Statement statement = con.createStatement();

        // FRAME
        Test test = new Test();
        test.ShowEventDemo();
    }

    private class ButtonClickListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            if( command.equals( "OK" ))  {
                _statusLabel.setText("Ok Button clicked.");
            } else if( command.equals( "SUBMIT" ) )  {
                _statusLabel.setText("Submit Button clicked.");
            } else  if ( command.equals("CANCEL") ) {
                _statusLabel.setText("Cancel Button clicked.");
            }
        }
    }
}
