package com.kraken.gatenetwork;

import java.io.File;
import java.sql.*;
import java.util.Arrays;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;

public class Database {
	
	//Main instance
	private GateNetwork plugin;
	
    //JDBC driver
    static String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    //Create the connection object
    private static Connection connection = null;
    
    //Connection credentials & info
    private String dbName = null;
    private String url = null;
    private String user = null;
    private String pass = null;
    
    //Flag to check if the table is present and ready for queries
    boolean formatted = false;

    //Constructor
    public Database(GateNetwork plugin) {
    	
    	//Get the instance of the main plugin class
    	this.plugin = plugin;
    	
    	//Database config file
	    File dbFile = plugin.getFile("database");
	    FileConfiguration dbConfig = plugin.getFileConfig("database");
	    Set<String> dbConfigKeys = dbConfig.getKeys(false);
	    
	    //Try to connect to the database using config file info
	    boolean valuesPresent = dbConfigKeys.containsAll(Arrays.asList("url", "user", "pass", "dbName"));
	    
	    if (valuesPresent) {
	    	
	    	//Database connection values missing error
	        plugin.getLogger().info("Attempting to set up database connector using values from database.yml...");
	    	
	    	//Get the database connection info/credentials
	    	url = dbConfig.getString("url");
	    	user = dbConfig.getString("user");
	    	pass = dbConfig.getString("pass");
	    	dbName = dbConfig.getString("dbName");
	    	
	    	//Make sure the database is formatted correctly
	    	connection = getConnection();
	    	if (connection != null) {
		        formatted = checkDatabaseFormat();
		    	closeConnection();
	    	} else {
		        plugin.getLogger().info("Please close your server, add your database credentials to the database.yml file, and restart.");
	    	}
	    
    	} else {
    		
	        //Database connection values missing error
	        plugin.getLogger().info("Database credentials not found. Creating default database.yml...");
	        
	        //Set default keys and empty values to be filled in by admin
	        dbConfig.set("url", "");
	        dbConfig.set("user", "");
	        dbConfig.set("pass", "");
	        dbConfig.set("dbName", "");
	        
	        plugin.saveCustomFile(dbConfig, dbFile);
	        
	        plugin.getLogger().info("Please close your server, add your database credentials to the database.yml file, and restart.");
	        
	    }
        
    }
    
    //Format the database appropriately if not already so
    public boolean checkDatabaseFormat() {

		//Create a table for travel data
    	try (Statement statement = connection.createStatement()) {
    		
    		String query = "CREATE TABLE IF NOT EXISTS travel (player varchar(255), destination varchar(255), origin varchar(255))";
    		
    		statement.execute(query);
    		
    		return true;
    		
		} catch (SQLException e) {
			plugin.getLogger().info("Could not initially format database: exception thrown on query.");
			e.printStackTrace();
		}
    	
		//Create a table for gate network data
    	try (Statement statement = connection.createStatement()) {
    		
    		String query = "CREATE TABLE IF NOT EXISTS gates (world varchar(255), loc varchar(255), dial varchar(255), "
					+ "address varchar(255), pointoforigin varchar(255), name varchar(255), active varchar(255), "
					+ "start varchar(255), destination varchar(255), dialer varchar(255), server varchar(255))";
    		
    		//Create a table for travel data
    		statement.execute(query);
    		
    		return true;
    		
		} catch (SQLException e) {
			plugin.getLogger().info("Could not initially format database: exception thrown on query.");
			e.printStackTrace();
		}
    	
    	return false;
    	
    }
    
    //Close the JDBC connection to database
    public boolean closeConnection() {

    	//Close the connection
    	try {
            if (connection != null && !connection.isClosed()){
                connection.close();
                return true;
            }
        } catch(Exception e) {
			plugin.getLogger().info("Could not close connection to database: exception thrown on closing.");
            e.printStackTrace();
        }
    	
    	return false;
        
    }
    
    //Return the JDBC connection to database
    public Connection getConnection() {
    	
    	if (url == null || url == "" || dbName == null || dbName == "" || user == null || user == "" || pass == null || pass == "") {
	        return null;
    	}
    	
    	//Check if a connection is already made
    	try {
            if (connection == null || connection.isClosed()){
            	Class.forName(JDBC_DRIVER);
                connection = DriverManager.getConnection("jdbc:mysql://" + url + "/" + dbName, user, pass);
            }
        } catch(Exception e) {
			plugin.getLogger().info("Could not connect to database: exception thrown on connection.");
            e.printStackTrace();
        }
    	
    	return connection;
        
    }
    
    //Return the name of the database being used
    public String getDbName() {
    	return dbName;
    }
    
}