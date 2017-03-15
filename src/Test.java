/**
 * Created by nima on 3/15/2017.
 */

import java.sql.*;

public class Test {

    public static void main(String[] args) throws SQLException{
        try {
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
        } catch (Exception classNotFoundException) {
            System.out.println("Class not found!");
        }
        String url = "jdbc:db2://comp421.cs.mcgill.ca:50000/cs421";
        Connection con = DriverManager.getConnection(url, "CS421G42", "cashMeOutside420");
        Statement statement = con.createStatement();
    }
}
