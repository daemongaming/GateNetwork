package com.kraken.gatenetwork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.kraken.gatenetwork.Network.Gate;

public class Commands {
	
	//Globals
	private GateNetwork plugin;
	
	//Constructor
	public Commands(GateNetwork plugin) {
		this.plugin = plugin;
	}
    
    //Commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		//Get object instances from the main plugin instance
		Messages messenger = plugin.messenger;
		Network network = plugin.getNetwork();
		
		//Set up some values
		String command = cmd.getName();
    	Player player = Bukkit.getServer().getPlayerExact("krakenmyboy");
		boolean isPlayer = false;
		
		//Check if sender is a player
        if ( sender instanceof Player ) {
        	player = (Player) sender;
        	isPlayer = true;
        }
		
		//Gates config files
	    FileConfiguration gatesConfig = plugin.getFileConfig("gates");
		
		//Execute command processing
		switch ( command.toLowerCase() ) {
		
			case "gate":
				
				if (args.length == 0) {
					if (isPlayer) {
						messenger.makeMsg(player, "cmdVersion");
					} else {
						messenger.makeConsoleMsg("cmdVersion");
					}
				} else if (args.length == 1) {
					
					String worldName = player.getWorld().getName();
					boolean isGate = network.getGateLocs().containsKey(worldName);
					
					switch(args[0]) {
					
						//List all gates
						case "list":
							
							int count = 0;
							for (String gate : gatesConfig.getKeys(false)) {
						    	player.sendMessage(ChatColor.BLUE + "[GN]" + ChatColor.GRAY + " | Gate " + count + ": " + ChatColor.GREEN + gate);
						    	count++;
						    }
							
							break;
							
						//Build a gate
						case "build":
						case "place":
							
							if (!isPlayer) {
								messenger.makeConsoleMsg("errorPlayerCommand");
								break;
							}
							
							//Schematic file
						    FileConfiguration schematicConfig = plugin.getFileConfig("schematic");
						    
						    //Get the block array from file
						    List<String> blocks = new ArrayList<String>();
					        	
							//Get the location of the player
							Location sLoc = player.getLocation();
							
							//Get the block array from file
						    blocks = schematicConfig.getStringList("schematic");
							
							//Scan over each block and put it in the array
							int n = 0;
							for (int yn = 0; yn < 14; yn++) {
								for (int xn = 0; xn < 3; xn++) {
									for (int zn = 0; zn < 15; zn++) {
										Location target = new Location(sLoc.getWorld(), sLoc.getX()+xn, sLoc.getY()+yn, sLoc.getZ()+zn);
										Material mat = Material.getMaterial(blocks.get(n));
										target.getBlock().setType(mat);
										n++;
									}
								}
							}
							
							break;
					
						//Set a gate location to config
						case "new":
							
							if (!isPlayer) {
								messenger.makeConsoleMsg("errorPlayerCommand");
								break;
							}
						    
							Location loc = player.getLocation().add(0.0, 5.0, 0.0);
							String locStr = LocSerialization.getLiteStringFromLocation(loc);
							
							//Check if a gate already exists in world
							if (isGate) {
								messenger.makeMsg(player, "errorGateInWorld");
							} else {
								
								//Get misc gate data
								DialDevice dialDevice = plugin.getDialDevice();
								ArrayList<Integer> address = dialDevice.getRandomAddress();
								String pointoforigin = dialDevice.getRandomOriginSymbol();
								String server = plugin.getServerName();
								
								//Create a gate object
								Network net = plugin.getNetwork();
								Gate gate = net.new Gate(worldName, locStr, locStr, address, pointoforigin, worldName, false, "false", "false", "false", server);
								
								//Update the gate in config
								net.update(gate, true, true);
							    
							}
							
							break;
						
						//Delete a gate from config
						case "delete":
						case "remove":
						case "del":
							
							if (!isPlayer) {
								messenger.makeConsoleMsg("errorPlayerCommand");
								break;
							}
							
							//Check if gate exist in world
							if (!isGate) {
								messenger.makeMsg(player, "errorGateNotInWorld");
							} else {
							    //Send network update to delete the gate
							    Network net = plugin.getNetwork();
							    Gate gate = net.getGate(worldName);
							    net.delete(gate);
							}
							
							break;
							
						default:
							break;
					}
					
				} else if (args.length == 2) {
					
					switch(args[0]) {
					
						//Toggle config options by command
						case "toggle":
							
							if ( !plugin.options.keySet().contains(args[1]) ) {
								if (isPlayer) {
									messenger.makeMsg(player, "errorIllegalCommand");
								} else {
									messenger.makeConsoleMsg("errorArgumentFormat");
								}
								break;
							}
							
							//Toggle the option
							boolean option = plugin.options.get(args[1]);
							plugin.setOption(args[1], !option);
							
							//Get the message to send
							String toggleMsgLabel = "cmdOptionEnabled";
							toggleMsgLabel = !option ? "cmdOptionEnabled" : "cmdOptionDisabled";
							
							//Send a confirmation message
							if (isPlayer) {
								messenger.makeMsg(player, toggleMsgLabel);
							} else {
								messenger.makeConsoleMsg(toggleMsgLabel);
							}
							
							break;
					
						//Information about a world's gate
						case "info":
							
							if (gatesConfig.getKeys(false).contains(args[1])) {
								
								//Loop through each label and key in their matched arrays
								String[] dataLabels = {"Gate Location", "Dial Location", "Gate Address", "Point of Origin", "World Name", "Active Wormhole", "Start Time", "Connected to", "Gate Dialer", "Server Name"};
								String[] dataKeys = {"loc", "dial", "address", "pointoforigin", "name", "active", "start", "destination", "dialer", "server"};
								
								int n = 0; //Loop counter
								for (String dataLabel : dataLabels) {
									player.sendMessage(ChatColor.BLUE + "[GN]" + ChatColor.GRAY + " | " + dataLabel + ": " + ChatColor.GREEN + gatesConfig.getString(args[1] + "." + dataKeys[n]));
									n++;
								}
								
							} else {
								messenger.makeMsg(player, "errorGateNotInWorld");
							}
							
							break;
							
						//Force a wormhole connection between current world's gate and another gate
						case "connect":
						case "dial":
						case "activate":
							
							if (!isPlayer) {
								messenger.makeConsoleMsg("errorPlayerCommand");
								break;
							}
							
							boolean isGate = gatesConfig.getKeys(false).contains(args[1]);
							if (!isGate) {
								//World not found
								break;
							}
						    
						    //Get the world of the originating gate
							String origin = player.getLocation().getWorld().getName();
							
						    //Get the gate worlds
							String destination = args[1];
							
							//Get each symbol in the address of the destination gate
							ArrayList<Integer> address = new ArrayList<Integer>();
							
							for (int s = 0; s < 6; s++) {
								int symbol = (Integer) gatesConfig.getList(destination + ".address").get(s);
								address.add(symbol);
							}
							
							address.addAll(Arrays.asList(13, 22));
							
							//Attempt to dial the destination gate
							DialDevice dialDevice = plugin.getDialDevice();
							dialDevice.dialOut(address, origin, player);
							
							break;
							
						default:
							break;
							
					}
					
				} else if (args.length == 3) {
					
					switch(args[0]) {
					
						//Force a wormhole connection between two world's gates
						case "connect":
						case "dial":
						case "activate":
							
							if (!isPlayer) {
								messenger.makeConsoleMsg("errorPlayerCommand");
								break;
							}
							
							boolean isGate = gatesConfig.getKeys(false).contains(args[1]) && gatesConfig.getKeys(false).contains(args[2]);
							if (!isGate) {
								//World not found
								break;
							}
						    
						    //Get the gate worlds
							String origin = args[1];
							String destination = args[2];
							
							//Get each symbol in the address of the destination gate
							ArrayList<Integer> address = new ArrayList<Integer>();
							
							for (int s = 0; s < 6; s++) {
								int symbol = (Integer) gatesConfig.getList(destination + ".address").get(s);
								address.add(symbol);
							}
							
							address.addAll(Arrays.asList(13, 22));
							
							//Dial the destination gate
							DialDevice dialDevice = plugin.getDialDevice();
							dialDevice.dialOut(address, origin, player);
							
							break;
							
						default:
							break;
						
					}
					
				} else if (args.length == 4) {
					
					switch(args[0]) {
					
						//Set a gate's config info
						case "set":
							
							if (!gatesConfig.getKeys(false).contains(args[1])) {
								//World not found
								break;
							}

							//Get the gate info from the network
						    Network net = plugin.getNetwork();
						    Gate gate = net.getGate(args[1]);
						    
						    switch(args[2]) {
						    
							  //Set a gate dial location to config
								case "dial":
									
									if (!isPlayer) {
										messenger.makeConsoleMsg("errorPlayerCommand");
										break;
									}
									
									//Get the location to set the dial
									Location dialLoc = player.getLocation().subtract(0.0, 1.0, 0.0);
									String dialLocStr = LocSerialization.getLiteStringFromLocation(dialLoc);
									
								    //Set the config info for the gate's dial & save
									boolean isFalse = args[3].equalsIgnoreCase("false");
									gate.dial = isFalse?"false":dialLocStr;
									
								    //Send network update
								    net.update(gate, false, true);
								    
								    //Dial location set success message
								    
									break;
						    	
						    	//Set the gate address
						    	case "address":
						    		
						    		ArrayList<Integer> address = new ArrayList<>();
						    		String[] addressStr = args[3].split(",");
						    		
						    		if (addressStr.length != 6) {
					    				//Symbol numbers are formatted incorrectly
					    				break;
						    		}
						    		
						        	//Reserved symbol spaces on the dialing device
						        	ArrayList<Integer> reserved = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 4, 8, 13, 22, 36, 44}));
						        	
						    		for (String s : addressStr) {
						    			
						    			int si = Integer.parseInt(s);
						    			if (reserved.contains(si) || si < 0 || si > 44) {
						    				//Symbol numbers are formatted incorrectly
						    				break;
						    			} else {
						    				address.add(si);
						    			}
						    			
						    		}
						    		
						    		gate.address = address;
						    		
								    //Send network update
								    net.update(gate, false, true);
								    
								    //Address set success message
								    
						    		break;
						    	
						    	//Set the gate point of origin symbol
						    	case "pointoforigin":

					    			//Material not found check
						    		if (Material.getMaterial(args[3].toUpperCase()) == null) {
						    			break;
						    		}
						    		
						    		//Set point of origin symbol material
						    		gate.pointoforigin = args[3].toUpperCase();
						    		
								    //Send network update
								    net.update(gate, false, true);
								    
								    //Point of origin symbol set success message
								    
						    		break;
						    	
						    	//Set the gate name
						    	case "name":
						    		
						    		gate.name = args[3];
						    		
								    //Send network update
								    net.update(gate, false, true);
								    
								    //Gate name set success message
								    
						    		break;
						    		
						    	default:
						    		//Option to set not recognized
						    		break;
						    
						    }
						
						default:
							//Command not recognized
							break;
							
					}
					
				}
				
				return true;
				
			default:	
				
				if (isPlayer) {
					messenger.makeMsg(player, "errorIllegalCommand");
				} else {
					messenger.makeConsoleMsg("errorCommandFormat");
				}
				return true;
				
		}
        
    }
	
}
