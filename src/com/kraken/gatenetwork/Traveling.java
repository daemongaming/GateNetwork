package com.kraken.gatenetwork;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.kraken.gatenetwork.Network.Gate;

public class Traveling implements Listener {
	
	//Globals
	private GateNetwork plugin;
	
	//Player travel check cooldown
	private ArrayList<Player> travelCooldown = new ArrayList<>();
	
	public Traveling(GateNetwork plugin) {
		this.plugin = plugin;
	}
      
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		
		//Variable values
		Player p = (Player) e.getPlayer();
		Location loc = e.getTo();
		World world = p.getWorld();
		String worldName = world.getName();
		Network net = plugin.getNetwork();
		WeakHashMap<String, String> connections = net.connections.get();
		
		//Travel cooldown check
		boolean traveling = travelCooldown.contains(p);
		if (traveling) {
			return;
		}
		
		//Check if the world has an active gate connection
		boolean isOrigin = connections.containsKey(worldName);
		boolean isDestination = connections.containsValue(worldName);
		
		//No gate detected in world
		if (!isOrigin && !isDestination) {
			return;
		}
		
		//Easy mode stuff
		boolean easyMode = plugin.options.get("easy_mode");
		boolean isEasyDestination = isDestination && easyMode;
		
		//Get the gate object
		Gate gate = net.getGate(worldName);
		Location gateLoc = LocSerialization.getLiteLocationFromString(gate.loc);
		
		//Check if the location is a destination gate wormhole and not on easy mode
		if (isDestination && !easyMode) {
			
			boolean inGate = loc.distance(gateLoc) < 6 && loc.getBlockX() == gateLoc.getBlockX();
						
			//Wrong end of the wormhole, buddy
			if (inGate && !traveling) {
				//Vaporize the player
		    	world.playSound(loc, Sound.ENTITY_PLAYER_SPLASH, 4.0F, 1.0F);
				p.setHealth(0.0);
				plugin.messenger.makeMsg(p, "eventVaporized");
				return;
			}
			
		}

		//Check if the location is an originating gate wormhole, or just any wormhole on easy mode
		else if (isOrigin || isEasyDestination) {
			
			//Get the destination gate object
			Gate dGate = net.getGate(gate.destination);
		    
			boolean inGate = loc.distance(gateLoc) < 6 && loc.getBlockX() == gateLoc.getBlockX();
			
			//Right end of the wormhole. Nice!
			if (inGate) {
				
				if (plugin.options.get("debug_mode")) {
					plugin.getLogger().info("Player detected in gate wormhole, processing teleport...");
				}
				
				//Add player to the travel check cooldown to prevent repeated successive checks
				travelCooldown.add(p);
				
				BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
				scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
	                @Override
	                public void run() {
				    	travelCooldown.remove(p);
	                }
	            }, 20);
				
				//Perform the teleport
				if (dGate.server.equals(gate.server)) {
					
					if (plugin.options.get("debug_mode")) {
						plugin.getLogger().info("Player used locally connected wormhole...");
					}
					
					//Direct, same-server teleport code
					Location dLoc = LocSerialization.getLiteLocationFromString(dGate.loc);
					
    				//Teleport location generated from gate center location
    				dLoc.add(2.5, -3, 0.5);
    				// Turn the player the right way
    				dLoc.setYaw(-90);
    				dLoc.setPitch(0);
        			
					net.teleport(p, dLoc);
					
					playTravelEffects(p, gate.name, dGate.name);
					
				} else {
					
					//Set the player's travel data
					Database db = plugin.getDatabase();
					Connection conn = db.getConnection();
					String query = "INSERT INTO travel(player, destination, origin) VALUES(?, ?, ?)";
					
					try (PreparedStatement putTravelData = conn.prepareStatement(query)) {
						
						if (plugin.options.get("debug_mode")) {
							plugin.getLogger().info("Attempting to insert player travel data into database...");
						}
						
						String dWorld = plugin.getFileConfig("gates").getString(worldName + ".destination");
						putTravelData.setString(1, p.getDisplayName());
						putTravelData.setString(2, dWorld);
						putTravelData.setString(3, worldName);
						putTravelData.executeUpdate();
						
						if (plugin.options.get("debug_mode")) {
							plugin.getLogger().info("Player travel data inserted into database.");
						}
						
					} catch (SQLException e1) {
						if (plugin.options.get("debug_mode")) {
							plugin.getLogger().info("Exception thrown while inserting player travel data into database.");
						}
						e1.printStackTrace();
					}

					//Server-to-server connection, sending player to hub world for the server
					connectPlayer(p, dGate.server);
					
					//Close the database connection
					db.closeConnection();
					
				}
				
			}
			
		}
		
	}
	
	//Play special effects at the wormhole location for a player (title, sounds, etc.)
	public void playTravelEffects(Player player, String origin, String destination) {
		
		//Get the player's location and world
		Location loc = player.getLocation();
		World world = loc.getWorld();
		
		//Play the wormhole travel sounds
    	world.playSound(loc, Sound.BLOCK_PORTAL_TRAVEL, 2.0F, 8.0F);
    	world.playSound(loc, Sound.ENTITY_PLAYER_SPLASH, 4.0F, 1.0F);
		
		//Display world welcome message via titles
    	String title = ChatColor.GOLD + "" + ChatColor.BOLD + destination;
    	String subtitle = ChatColor.GOLD + "from " + origin;
		player.sendTitle(title, subtitle, 20, 100, 40);
		
	}
	
	//Connect a player directly to another server on the network
	public void connectPlayer(Player player, String server) {
		
		if (plugin.options.get("debug_mode")) plugin.getLogger().info("Connecting player " + player.getDisplayName() + " to server " + server + "...");
		
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		
		out.writeUTF("Connect");
		out.writeUTF(server);
		
		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
		
	}
	
	public ArrayList<Player> getTravelers() {
		return travelCooldown;
	}
      
}
