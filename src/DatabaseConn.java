package src;

import java.sql.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * DatabaseConn class manages all connection with the externally hosted database.
 */

public class DatabaseConn {
    private Connection connection;

    /**
     * Create the instance by trying to connect to the database.
     */
    public DatabaseConn(){
        try {
            this.connection = getMySqlConnection();
        } catch (Exception ex){ }
    }

    /**
     * Check if the object is indeed connected to the database.
     * @return <code>true</code> if the instance is connected to the database, <code>false</code> otherwise
     */
    public boolean connected(){
        return connection != null;
    }

    /**
     * Start the connection with necessary credentials and return the Connection object
     * @return The Connection object
     * @throws Exception throws all exception should the program fail to connect
     */
    public Connection getMySqlConnection()throws Exception{
        Connection ret = null;
        String PUBLIC_DNS = "ec2-52-14-15-164.us-east-2.compute.amazonaws.com";
        String mysqlUserName = "planner_admin";
        String mysqlPassword = "";
        Class.forName("com.mysql.cj.jdbc.Driver");
        String mysqlConnUrl = "jdbc:mysql://"+PUBLIC_DNS+":3306/planner";

        ret = DriverManager.getConnection(mysqlConnUrl, mysqlUserName , mysqlPassword);

        return ret;
    }

    /**
     * Relays an Action query to the database
     * @param update the query to pass on
     * @return integer value returned by the executeUpdate function
     */
    int insertInDB(PreparedStatement update){
        try {
            return update.executeUpdate();
        } catch (SQLException ex){
            ex.printStackTrace();
            return 0;
        }
    }

    /**
     * Relays a Select query statement to the database and returns the results
     * @param query the query statement to be relay
     * @return the result set of the query
     */
    ResultSet queryFromDB(PreparedStatement query){
        try {
            return query.executeQuery();
        }
        catch (SQLException ex){
            return null;
        }
    }

    /**
     * Adds the details of the user to the <code>account</code> table, generate a unique ID for the user, store a
     * hashed version of the password along with the ID in the <code>login</code> table.
     * @param toAdd the account object to extract data from and add to the database
     * @param pass the password of the account
     * @return the unique account ID of the user created by the database
     */
    public int addAccount(Account toAdd, String pass){
        PreparedStatement preStat1= null, preStat2 = null, preStat3 = null;
        String accountQuery = "INSERT INTO `accounts`(`account_name`, `account_timezone`) VALUES (?,?)";
        String idQuery = "SELECT LAST_INSERT_ID()";
        String passwordQuery = "INSERT INTO `login`(`account_ID`, `password`, `salt`) VALUES (?,?,?)";
        ArrayList<byte[]> passHash = hashPass(pass);
        try {
            preStat1 = connection.prepareStatement(accountQuery);
            preStat1.setString(1, toAdd.getName());
            preStat1.setString(2, toAdd.getTimeZone());
            insertInDB(preStat1);

            preStat3 = connection.prepareStatement(idQuery);
            ResultSet rs = queryFromDB(preStat3);
            rs.next();
            int assignedID = rs.getInt("LAST_INSERT_ID()");
            rs.close();

            preStat2 = connection.prepareStatement(passwordQuery);
            preStat2.setInt(1, assignedID);
            preStat2.setBytes(2, passHash.get(1));
            preStat2.setBytes(3, passHash.get(0));
            insertInDB(preStat2);
            return assignedID;
        } catch (SQLException sqlEx){
            sqlEx.printStackTrace();
        }
        return -1;
    }

    public boolean uniqueAccountName(String name){
        boolean retval = true;
        try {
            PreparedStatement preStat = connection.prepareStatement("SELECT account_name FROM accounts WHERE account_name = ?");
            preStat.setString(1, name);
            ResultSet rs = queryFromDB(preStat);
            retval = rs.next();
        } catch (SQLException ex){}
        return !retval;
    }

    /**
     * Loads the details of the user in an Account instance based on user name and password,
     * @param accName name of the account
     * @param password password associated with the account
     * @return the account instance with the necessary details, returns <code>null</code> if there is no such
     * account
     * @throws SQLException any SQL error that the method faces
     */
    public Account login(String accName, String password) throws SQLException {
        PreparedStatement preStat1 = null, preStat3 = null; ResultSet rs1 = null, rs3 = null;
        String initialQuery = "SELECT login.password, login.salt FROM login JOIN accounts on " +
                "accounts.ID = login.account_ID WHERE accounts.account_name = ?";
        preStat1 = connection.prepareStatement(initialQuery);
        preStat1.setString(1, accName);
        rs1 = queryFromDB(preStat1);
        if(rs1.next()){
            if(Arrays.equals(rs1.getBytes("password"), verifyHash(password, rs1.getBytes("salt")))){
                String loadQuery = "SELECT * FROM `accounts` WHERE `account_name`= ?";
                preStat3 = connection.prepareStatement(loadQuery);
                preStat3.setString(1,accName);
                rs3 = queryFromDB(preStat3); rs3.next();
                return new Account(rs3.getInt("ID"), rs3.getString("account_name"),
                        rs3.getString("account_timezone"));
            }
        }
        return null;
    }

    /**
     * Change the name or timezone of the user on the database
     * @param accName name of the account
     * @param newStr the new String to replace it with
     * @param field the field to replace
     * @return <code>true</code> if the changed successfully, <code>false</code> otherwise
     * @throws SQLException any SQL error faced by the method
     */
    public boolean changeUser(String accName, String newStr, String field) throws SQLException{
        PreparedStatement preStat = null;
        String update = "UPDATE accounts SET "+ field +" = ? WHERE account_name = ?";
        try {
            preStat = connection.prepareStatement(update);
            preStat.setString(1, newStr);
            preStat.setString(2, accName);
            int rs = insertInDB(preStat);
            if(rs == 1) return true;
            else return false;
        } catch (SQLException ex){
            throw ex;
        } finally {
            preStat.close();
        }
    }

    /**
     * Change the password of the user
     * @param accName name of the account to change the password of
     * @param oldPass current password to verify the authenticity
     * @param newpass new password
     * @return <code>true</code> if the old password is entered currently and the database is updated with new
     * password, <code>false</code> otherwise
     * @throws SQLException any SQL error faced by the method
     */
    public boolean changeUserPass(String accName, String oldPass, String newpass) throws SQLException{
        PreparedStatement preStat = null; ResultSet rs = null;
        String initialQuery = "SELECT login.account_ID, login.password, login.salt FROM login JOIN accounts on " +
                "accounts.ID = login.account_ID WHERE accounts.account_name = ?";
        preStat = connection.prepareStatement(initialQuery);
        preStat.setString(1, accName);

        rs = queryFromDB(preStat);
        rs.next();
        if(Arrays.equals(rs.getBytes("password"), verifyHash(oldPass, rs.getBytes("salt")))){
            ArrayList<byte[]> newPassHash = hashPass(newpass);
            String updateDB = "UPDATE login SET login.password = ?, login.salt = ? WHERE account_ID = ?";
            PreparedStatement preStat2 = connection.prepareStatement(updateDB);
            preStat2.setBytes(1, newPassHash.get(1));
            preStat2.setBytes(2, newPassHash.get(0));
            preStat2.setInt(3, rs.getInt("account_ID"));
            insertInDB(preStat2);
            preStat.close(); preStat2.close(); rs.close();
            return true;
        }
        return false;
    }

    /**
     * Add an event to the event table with the necessary details.
     * @param toAdd the event to add
     * @return the ID of the event auto-generated by the database, returns -1 if faced with error
     */
    public int addEvent(Event toAdd){
        PreparedStatement preStat = null, preStat2 = null;
        String insert = "INSERT INTO `events`(`account_ID`, `event_name`, `event_desc`, `start_time`, `end_time`, " +
                "`event_location`, `event_frq`) VALUES(?,?,?,?,?,?,?)";
        try {
            preStat = connection.prepareStatement(insert);
            preStat.setInt(1, toAdd.getUserID());
            preStat.setString(2, toAdd.getName());
            preStat.setString(3, toAdd.getDescription());
            preStat.setString(4, toAdd.getStartTimeString());
            preStat.setString(5, toAdd.getEndTimeString());
            preStat.setString(6, toAdd.getLocation());
            preStat.setString(7, toAdd.getFrequency());
            insertInDB(preStat);

            preStat2 = connection.prepareStatement("SELECT LAST_INSERT_ID()\n");
            ResultSet rs = queryFromDB(preStat2);
            rs.next();
            int event_ID = rs.getInt("LAST_INSERT_ID()");
            rs.close();
            return event_ID;
        } catch (SQLException ex){
            ex.printStackTrace();
            return -1;
        }finally {
            try {
                preStat.close(); preStat2.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        }
    }

    /**
     * Return an AllEvents instance loaded with all the events attributed to the logged in user.
     * @param ofUser the user to load events of
     * @return AllEvents instance loaded with all the events of the specified user.
     */
    public AllEvents loadEvents(Account ofUser){
        try {
            PreparedStatement preState = connection.prepareStatement("SELECT * FROM events WHERE account_ID = ?");
            preState.setInt(1, ofUser.getID());

            ResultSet rs = queryFromDB(preState);
            AllEvents eventList = new AllEvents();
            while (rs.next()){
                Event newEvent = new Event(rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8));
                eventList.addEvent(newEvent);
            }
            return eventList;
        } catch (SQLException ex){
            ex.printStackTrace();
        }
        return null;
    }
    
    public ArrayList<userAlarm> loadAlarmsFromDB(int userID){
    	try {
    		PreparedStatement preState = connection.prepareStatement("SELECT * FROM alarm WHERE account_ID = ?");
    		preState.setInt(1, userID);
    		
    		ResultSet rs = queryFromDB(preState);
    		ArrayList<userAlarm> al = new ArrayList<userAlarm>();
    		while(rs.next()) {
    			userAlarm ua =  new userAlarm(rs.getString(3),rs.getString(5),rs.getString(4),rs.getInt(6));
    			al.add(ua);
    		}
    		return al;
    	}catch (SQLException ex){
            ex.printStackTrace();
        }
    	return null;
    }

    /**
     * Creates and returns a hashed version of the provided string along with the randomly generated salt used to
     * create the hash
     * @param password the password to hash
     * @return an ArrayList of two byte array containing the hashed password and salt
     */
    ArrayList<byte[]> hashPass(String password){
        ArrayList<byte[]> retval = new ArrayList<>();
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            retval.add(salt);

            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            retval.add(factory.generateSecret(spec).getEncoded());
            return retval;

        } catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * 
     * @param accountID
     * @param alarmName
     * @param aHours
     * @param aMins
     * @param amOrPm
     * @param shouldRepeat
     * @param daysRepeating
     */
    public void addAlarm(int accountID,String alarmName, String aHours, String aMins, String amOrPm, boolean shouldRepeat, ArrayList<String> daysRepeating) {
    	PreparedStatement preStat = null;
    	String insert = "INSERT INTO `alarm`(`account_id`, `alarm_name`, `days_of_week`, `time_of_day`, `is_active`)"+
    	"VALUES (?,?,?,?,?)";
    	String days="";
    	for(int i=0; i<daysRepeating.size();i++) {
    		days+=daysRepeating.get(i);
    		if(i!=daysRepeating.size()-1) {
    			days+=",";
    		}
    	}
    	String time="";
    	if(amOrPm.equalsIgnoreCase("pm")) {
    		time = String.valueOf(Integer.valueOf(aHours)+12);	
    	}
    	else {
    		time = aHours;
    	}
    	time+=":"+aMins;
    	int isActive = 1;
    	if(alarmName.equals(""))alarmName = "Alarm";
    	try {
    		preStat = connection.prepareStatement(insert);
    		preStat.setInt(1, accountID);
    		preStat.setString(2, alarmName);
    		preStat.setString(3, days);
    		preStat.setString(4, time);
    		preStat.setInt(5, isActive);
    		insertInDB(preStat);
    		
    	}catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            try {
                preStat.close();
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        }
    }

    /**
     * Returns a hashed-version of the given password with a given salt, used to verify password given at login and
     * during password change.
     * @param givenPassword the password to check
     * @param givenSalt the salt attributed to the account
     * @return a hashed byte array created with the password and salt
     */
    byte[] verifyHash(String givenPassword, byte[] givenSalt){
        try {
            KeySpec spec = new PBEKeySpec(givenPassword.toCharArray(), givenSalt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception ex){
            return null;
        }
    }

    /**
     * Close the connection
     * @throws SQLException any SQL error faced by the method
     */
    public void closeConnection() throws SQLException{
        connection.close();
    }
}
