package com.kraken.gatenetwork;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitScheduler;

public class Network {
	
	//Main instance
	private GateNetwork plugin;
	
	//Network constructor
    public Network(GateNetwork plugin) {
    	
    	this.plugin = plugin;
  		
    	//Check wormholes timer (every 300 ticks, or 15 sec)
    	BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
        		updateAllGates(false);
            }
        }, 300, 300);
		
    }
    
    //Gate class object (would immutable records be better? we'll see...)
    public class Gate {
    	
    	//Gate data vars
    	String world;
    	String loc;
    	String dial;
    	ArrayList<Integer> address;
    	String pointoforigin;
    	String name;
    	boolean active;
    	String start;
    	String destination;
    	String dialer;
    	String server;
    	
    	//Gate check values
    	boolean isOrigin;
    	boolean sameServer;
    	
    	//Gate constructor
    	public Gate(String world, String loc, String dial, ArrayList<Integer> address, String pointoforigin, String name, boolean active, String start, String destination, String dialer, String server) {
    		
    		//Set gate data vars
    		this.world = world;
    		this.loc = loc;
    		this.dial = dial;
    		this.address = address;
    		this.pointoforigin = pointoforigin;
    		this.name = name;
    		this.active = active;
    		this.start = start;
    		this.destination = destination;
    		this.dialer = dialer;
    		this.server = server;
    		
    		//Set gate check values
    		this.isOrigin = !dialer.equalsIgnoreCase("false");
    		this.sameServer = server.equals(plugin.getServerName());
    		
    	}
    	
    }
    
    //Active wormhole connections class object
	public WeakHashMap<String, String> getConnections() {
	    
	    //A map of all wormholes (originWorld=destinationWorld)
		WeakHashMap<String, String> connections = new WeakHashMap<String, String>();

		//Get all gates
		WeakHashMap<String, Gate> gates = getGates();
		
		//Check if the gate is an active origin gate and add to the connections map
		for (String world : gates.keySet()) {
			Gate gate = gates.get(world);
			if (gate.active && (!gate.dialer.equals("false"))) {
				connections.put(world, gate.destination);
			}
		}
		
		return connections;
		
	}
	
	//Update the gates file with new gate info
	public void updateConfig(Gate gate) {

		//Update the gates config file
	    File gatesFile = plugin.getFile("gates");
	    FileConfiguration gatesConfig = plugin.getFileConfig("gates");
	    
	    String[] keys = {"loc", "dial", "address", "pointoforigin", "name", "active", "start", "destination", "dialer", "server"};
	    ArrayList<Object> vals = new ArrayList<>(Arrays.asList(gate.loc, gate.dial, gate.address, gate.pointoforigin, gate.name, gate.active, 
	    														gate.active?gate.start:false, gate.active?gate.destination:false, gate.isOrigin?gate.dialer:false, gate.server));
	    
	    int k = 0;
	    for (String key : keys) {
	    	gatesConfig.set(gate.world + "." + key, vals.get(k));
	    	k++;
	    }
		
		plugin.saveCustomFile(gatesConfig, gatesFile);
		
	}
	
	//Update the gate network database and local config files
	public void update(Gate gate, boolean isNew, boolean sending) {
		
		//Get the database connection
		Connection dbConn = plugin.getDatabase().getConnection();
		
		//Send an update
		if (sending) {
			
			updateConfig(gate);
			
	    	String query = "";
	    	if (isNew) {
	    		query = "INSERT INTO gates(world, loc, dial, address, pointoforigin, name, active, start, destination, dialer, server) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	    	} else {
	    		query = "UPDATE gates SET world=?, loc=?, dial=?, address=?, pointoforigin=?, name=?, active=?, start=?, destination=?, dialer=?, server=? WHERE world=?";
	    	}
			
			try (PreparedStatement setGatesData = dbConn.prepareStatement(query)) {
				
				String gateAddress = "";
				for (int s : gate.address) {
					gateAddress += s;
					if (s != gate.address.get(5)) {
						gateAddress += ",";
					}
				}
				
				String[] vals = {gate.world, gate.loc, gate.dial, gateAddress, gate.pointoforigin, gate.name, gate.active?"true":"false", gate.start, gate.destination, gate.dialer, gate.server};
	
				int n = 1;
				for (String val : vals) {
					setGatesData.setString(n, val);
					n++;
				}
				
				if (!isNew) {
					setGatesData.setString(12, gate.world);
				}
				
				setGatesData.executeUpdate();
				
			} catch (SQLException e1) {
				plugin.getLogger().info("Error setting gates data.");
				e1.printStackTrace();
			}
		
		//Get an update
		} else {
			
			//Get the gate data
    		String query = "SELECT world, loc, dial, address, pointoforigin, name, active, start, destination, dialer, server FROM gates WHERE world=?";
    		
    		try (PreparedStatement getGatesData = dbConn.prepareStatement(query)) {
    			
    			getGatesData.setString(1, gate.world);
    			ResultSet rs = getGatesData.executeQuery();
    			
    			while (rs.next()) {
    				
    				String worldName = rs.getString(1);
    				String loc = rs.getString(2);
    				String dial = rs.getString(3);
    				
    				String[] addressStr = rs.getString(4).split(",");
    				ArrayList<Integer> address = new ArrayList<>();
    				for (int s=0; s<6; s++) {
    					address.add(Integer.parseInt(addressStr[s]));
    				}
    				
    				String pointoforigin = rs.getString(5);
    				String name = rs.getString(6);
    				String activeStr = rs.getString(7);
    				boolean active = activeStr.equalsIgnoreCase("true")?true:false;
    				String start = rs.getString(8);
    				String destination = rs.getString(9);
    				String dialer = rs.getString(10);
    				String server = rs.getString(11);
    				
    				boolean alreadyActive = gate.active;
    				
    				gate = new Gate(worldName, loc, dial, address, pointoforigin, name, active, start, destination, dialer, server);
    	    		
    	    		updateConfig(gate);
    	    		
    	    		//Update the wormhole accordingly
    	    		boolean local = gate.sameServer;
    	    		boolean discrepancy = (gate.active && !alreadyActive) || (!gate.active && alreadyActive);
    	    		if (local && discrepancy) {
    	    			Wormhole wormhole = new Wormhole(plugin);
    	    			Location gateLoc = LocSerialization.getLiteLocationFromString(gate.loc);
    	    			ArrayList<Location> locs = new ArrayList<Location>(Arrays.asList(gateLoc));
    	    			boolean establishing = false;
    	    			wormhole.wormholeEffects(gate.active, establishing, locs);
    	    		}
    				
    			}
    			
    		} catch (SQLException e1) {
    			plugin.getLogger().info("Error getting gates data.");
    			e1.printStackTrace();
    		}
			
		}

	}
	
	//Delete a gate from the network
	public void delete(Gate gate) {
	    
		//Get the database connection
		Connection dbConn = plugin.getDatabase().getConnection();
		
		//Get the gate data
		String query = "DELETE FROM gates WHERE world=?";
		
		try (PreparedStatement getGatesData = dbConn.prepareStatement(query)) {
			getGatesData.setString(1, gate.world);
			getGatesData.executeUpdate();
		} catch (SQLException e1) {
			plugin.getLogger().info("Error deleting gates data.");
			e1.printStackTrace();
		}

		//Get the gates config file
	    File gatesFile = plugin.getFile("gates");
	    FileConfiguration gatesConfig = plugin.getFileConfig("gates");
		
		//Clear the world's config info & save
	    gatesConfig.set(gate.world, null);
	    plugin.saveCustomFile(gatesConfig, gatesFile);
	    
	    //Close the connection
	    plugin.getDatabase().closeConnection();
	    
	}
	 
	//Check for network updates on all worlds
	public void updateAllGates(boolean sending) {
	    
	    //Open the database connection
	    Database db = plugin.getDatabase();
	    Connection dbConn = db.getConnection();

		//Get all the gate worlds
		ArrayList<String> worlds = new ArrayList<>();
	    
		String query = "SELECT world FROM gates";
		
		try (PreparedStatement getGateWorlds = dbConn.prepareStatement(query)) {
			
			ResultSet rs = getGateWorlds.executeQuery();
			
			while (rs.next()) {
				String worldName = rs.getString("world");
				worlds.add(worldName);
			}
			
		} catch (SQLException e1) {
			plugin.getLogger().info("Error getting gates data.");
			e1.printStackTrace();
		}
		
		//Update the gate data for each world
	    for (String world : worlds) {
	    	
	    	Gate gate;
	    	
	    	FileConfiguration gatesConfig = plugin.getFileConfig("gates");
	    	if (gatesConfig.getKeys(false).contains(world)) {
	    		gate = getGate(world);
	    		update(gate, false, sending);
	    	} else if (!sending) {
	    		gate = new Gate(world, world+"0,0,0", world+"0,0,0", new ArrayList<Integer>(Arrays.asList(1,2,3,5,6,7)), "AIR", world, false, "false", "false", "false", plugin.getServerName());
	    		update(gate, false, false);
	    	} else {
				plugin.getLogger().info("Error sending gate data: world not found.");
	    	}
	    	
	    }
	    
		//Check the wormholes for max time limit reached and update accordingly
		checkTimer();
	    
	    //Close the database connection
	    db.closeConnection();
	    
	}
	
	//Get a Gate object from config by world name
	public Gate getGate(String world) {

		//Gates config files
	    FileConfiguration gatesConfig = plugin.getFileConfig("gates");
	    
		String loc = gatesConfig.getString(world + ".loc");
		String dial = gatesConfig.getString(world + ".dial");
		
		ArrayList<Integer> address = new ArrayList<>();
		for (int s=0; s<6; s++) {
			address.add((Integer) gatesConfig.getList(world + ".address").get(s));
		}
		
		String pointoforigin = gatesConfig.getString(world + ".pointoforigin");
		String name = gatesConfig.getString(world + ".name");
		boolean active = gatesConfig.getBoolean(world + ".active");
		String start = gatesConfig.getString(world + ".start");
		String destination = gatesConfig.getString(world + ".destination");
		String dialer = gatesConfig.getString(world + ".dialer");
		String server = gatesConfig.getString(world + ".server");
		Gate gate = new Gate(world, loc, dial, address, pointoforigin, name, active, start, destination, dialer, server);
		
		return gate;
		
	}
	
	//Get all gates as Gate objects with their corresponding data assigned
	public WeakHashMap<String, Gate> getGates() {
		
		//Gates map (world=gate)
		WeakHashMap<String, Gate> gates = new WeakHashMap<>();
		
		//Gates config files
	    FileConfiguration gatesConfig = plugin.getFileConfig("gates");

	    //Loop through all worlds and get the gate info
		Set<String> worlds = gatesConfig.getKeys(false);
    	
    	for (String world : worlds) {
			Gate gate = getGate(world);
			gates.put(world, gate);
    	}
		
		return gates;
		
	}
	
	//Get all of the addresses in the gates config file
	public ArrayList<Integer> getAddress(String world) {
		
		//Gates config files
	    FileConfiguration gatesConfig = plugin.getFileConfig("gates");
  			
		//Populate symbols into an address
		ArrayList<Integer> address = new ArrayList<>();
		
		for (int i=0;i<6;i++) {
			int symbol = (Integer) gatesConfig.getList(world + ".address").get(i);
			address.add(symbol);
		}
		
		return address;
		
	}
	
	//Get all of the addresses in the gates config file
	public WeakHashMap<ArrayList<Integer>, String> getAddresses() {
		
		//Addresses map (<address>=world)
		WeakHashMap<ArrayList<Integer>, String> addresses = new WeakHashMap<>();
		
		//Gates config files
	    FileConfiguration gatesConfig = plugin.getFileConfig("gates");
  		
  		//Put the address and world into the addresses map
  		for (String world : gatesConfig.getKeys(false)) {
  			ArrayList<Integer> address = getAddress(world);
			addresses.put(address, world);
		}
		
		return addresses;
		
	}
	
	//Get all of the gate locations in the gates config file
	public WeakHashMap<String, Location> getGateLocs() {
	    
		//Gate locations map (world=gateLoc)
		WeakHashMap<String, Location> gateLocs = new WeakHashMap<String, Location>();

		//Gates map (world=gate)
	    WeakHashMap<String, Gate> gates = getGates();		
		
	    //Loop through all gates
		for (Gate gate : gates.values()) {
			
			if (gate.sameServer) {
				//Add the gate Location
				Location loc = LocSerialization.getLiteLocationFromString(gate.loc);
				gateLocs.put(gate.world, loc);
			}
			
		}
		
		return gateLocs;
		
	}
	
	//Get all of the dial locations in the gates config file
	public WeakHashMap<String, Location> getDialLocs() {
	    
		//Dial locations map (world=dialLoc)
		WeakHashMap<String, Location> dialLocs = new WeakHashMap<String, Location>();

		//Gates map (world=gate)
	    WeakHashMap<String, Gate> gates = getGates();
		
	    //Loop through all gates
		for (Gate gate : gates.values()) {
			
			//Check if dial has not been set
			boolean hasDial = !gate.dial.equals(gate.loc);
			
			if (hasDial && gate.sameServer) {
				//Add the dial location
				Location dialLoc = LocSerialization.getLiteLocationFromString(gate.dial);
				dialLocs.put(gate.world, dialLoc);
			}
			
		}
		
		return dialLocs;
		
	}
	
	//Get a timer representing when active connections were created
	public WeakHashMap<ArrayList<String>, String> getTimer() {
		
	    //Timer map (<origin=destination>=startTime)
		WeakHashMap<ArrayList<String>, String> timer = new WeakHashMap<ArrayList<String>, String>();
	    
		//Gates map (world=gate)
	    WeakHashMap<String, Gate> gates = getGates();
		
		//Loop through all gates
		for (Gate gate : gates.values()) {;
			
			//Check if the gate is an origin gate that is active
			if (gate.active && gate.isOrigin) {
				//Add the connection to the timer
				ArrayList<String> conn = new ArrayList<String>();
				conn.add(gate.world);
				conn.add(gate.destination);
				timer.put(conn, gate.start);
			}
			
		}
		
		return timer;
		
	}
	
	public void checkTimer() {
		
    	//Close wormholes that have reached max time limit  (~38 min in-canon = ~32 sec in-game = ~630 game ticks)
    	WeakHashMap<ArrayList<String>, String> timer = getTimer();
    	Set<ArrayList<String>> conns = timer.keySet();
        for (ArrayList<String> conn : conns) {
    	    LocalDateTime now = LocalDateTime.now();
    	    LocalDateTime then = LocalDateTime.parse(timer.get(conn));
        	if ( ChronoUnit.SECONDS.between(then, now) > 32 ) {
        		boolean easyMode = plugin.options.get("easy_mode");
        		if (!easyMode) {
        			Wormhole wormhole = new Wormhole(plugin);
        			wormhole.establish(false, "none", conn.get(0), conn.get(1));
        		}
        	}
        }
        
	}
      
}
