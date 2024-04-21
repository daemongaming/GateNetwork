package com.kraken.gatenetwork;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DialDevice implements Listener {
	
	//Globals
	private GateNetwork plugin;
	
	//The GUI inventory menu for the dial device
	public static Inventory dialGUI = Bukkit.createInventory(null, 45, "Dialing Device");
	
	//Current dialing sequence for each player
	private WeakHashMap<Player, ArrayList<Integer>> selected;
	
	//Construct the dial device instance
	public DialDevice(GateNetwork plugin) {
		this.plugin = plugin;
		selected = new WeakHashMap<Player, ArrayList<Integer>>();
    }
	
	//Create the inventory menu GUI for the dial device
	public static boolean openDialGUI(Player player, String worldName) {
		
		//Populate glyph symbols
		ArrayList<ItemStack> symbols = new ArrayList<>();
		
		//Get the materials from the dial config file
		FileConfiguration dialConfig = getFileConfig("dial");
		List<String> mats = dialConfig.getStringList("symbols");
		
		//Add each item to the symbols list
		for (String matStr : mats) {
			
			//Default (error) block
			Material mat = Material.getMaterial("BARRIER");
			
			//Check for point-of-origin material spot
			if (matStr.equalsIgnoreCase("origin")) {
				mat = getOriginMaterial(worldName);
			//Get the material from string
			} else {
				mat = Material.getMaterial(matStr.toUpperCase());
			}

			//Put the item into symbols
			ItemStack symbol = new ItemStack(mat, 1);
			symbols.add(symbol);
			
		}
		
		//Add each symbol to the inventory GUI menu
		int num = 0;
		for (ItemStack symbol : symbols) {
			dialGUI.setItem(num, symbol);
			num++;
		}

		//Open the dial GUI
		player.openInventory(dialGUI);
		
		return true;
		
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		
		//Player info
		Player player = (Player) e.getPlayer();
		
		//Right-click block interaction
		if ( e.getAction() == Action.RIGHT_CLICK_BLOCK && !plugin.cooldowns.contains(player) ) {
			
			//Set up the cooldown for the player interaction
			plugin.cooldowns.add(player);
			plugin.removeCooldown(player);
			
			//Check values
			String wName = player.getWorld().getName();
			Network net = plugin.getNetwork();
			WeakHashMap<String, Location> dialLocs = net.getDialLocs();
			Location loc = e.getClickedBlock().getLocation();
			
			//Check if world has a dial device
			if ( dialLocs.containsKey(wName) ) {
				
				//Check if the location clicked was a dial device main block/button
				if ( dialLocs.get(wName).equals(loc) ) {
				
					//Gates config files
				    FileConfiguration gatesConfig = getFileConfig("gates");
				    
					//Check values
					WeakHashMap<String, String> connections = net.connections.get();
					boolean isOrigin = connections.containsKey(wName);
					boolean isDestination = connections.containsValue(wName);
					String dwName = gatesConfig.getString(wName + ".destination");
					String dialer = gatesConfig.getString(dwName + ".dialer");
					boolean isDialer = player.getName().equals(dialer);
				
					//Close wormhole if it's an origination gate dial
					if (isOrigin || (isDestination && isDialer)) {
								
						Wormhole wormhole = new Wormhole(plugin);
						wormhole.establish(false, "none", isOrigin?wName:dwName, isOrigin?dwName:wName);
						plugin.messenger.makeMsg(player, "eventGateDeactivated");
						return;
						
					//Access the dial GUI
					} else if (!isDestination) {
				
						//Clear the previous presses
						if (selected.containsKey(player)) {
							selected.remove(player);
						}
						
						//Open the dial GUI
						openDialGUI(player, wName);
						
					}
					
				}
			
			}
		
		}
		
	}
	
	@EventHandler
	public void onSymbolSelect(InventoryClickEvent e) {

		//Check if the inventory click event is happening in a dialing device GUI menu
		boolean isDialMenu = e.getView().getTitle().equals("Dialing Device");
		
		if (isDialMenu) {
			
			Player player = (Player) e.getWhoClicked();
			
			if (e.getCurrentItem() != null) {
				
				//Give the symbol a glow
				int slot = e.getSlot();
				ItemStack symbol = new ItemStack(e.getInventory().getItem(slot).getType(), 1);
				symbol.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
				
				e.getInventory().setItem(slot, symbol);
				
    	    	//Add symbol to the dialed sequence so far
				ArrayList<Integer> dialed = new ArrayList<>();
				
				//Get previous symbol selections in the sequence
				boolean inSelected = selected.containsKey(player);
				if (inSelected) {
					dialed = selected.get(player);
				}
				
				//Put the currently dialed sequence into the selection mapping
				dialed.add(slot);
				selected.put(player, dialed);
				
				//Finish dialing
				if (slot == 22 && dialed.size() == 8) {

					Network net = plugin.getNetwork();
					net.updateAllGates(false);
					
					dialOut(dialed, player.getWorld().getName(), player);
					
					if (inSelected) {
						dialed.clear();
					}
					
					player.closeInventory();
				
				} else {
					//Play dial symbol-press sound
			    	player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 4.0F, 0.5F);
				}
				
				//Reset dialer
				if (dialed.size() == 8) {
					if (inSelected) {
						dialed.clear();
					}
					player.closeInventory();
				}
			
			}
			
			e.setCancelled(true);
		
		}
		
	}
	
	//Get a FileConfiguration from file name
	public static FileConfiguration getFileConfig(String fileName) {
	    File f = new File("plugins/GateNetwork", fileName + ".yml");
	    FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
	    return fc;
	}
	
	//Get the Point of Origin item material for a world
	public static Material getOriginMaterial(String worldName) {
		
		FileConfiguration gatesConfig = getFileConfig("gates");
		String originMaterial = gatesConfig.getString(worldName + ".pointoforigin");
		
		return Material.getMaterial(originMaterial);
	
	}
	
	//Generate a random unique point of origin material name
    public String getRandomOriginSymbol() {
    	
    	//Default values
    	String symbol = "BARRIER";
    	String[] MATS = {"ANGLER", "ARCHER", "ARMS_UP", "BLADE", "BREWER", "BURN", "DANGER", "EXPLORER", "FRIEND", "HEART", 
    					"HEARTBREAK", "HOWL", "MINER", "MOURNER", "PLENTY", "PRIZE", "SHEAF", "SHELTER", "SKULL", "SNORT"};
    	
    	//Get all of the point of origin symbols already used
    	FileConfiguration gatesConfig = getFileConfig("gates");
    	ArrayList<String> used = new ArrayList<>();
    	for (String gate : gatesConfig.getKeys(false)) {
    		String usedSymbol = gatesConfig.getString(gate + ".pointoforigin");
    		used.add(usedSymbol);
    	}
    	
    	//All symbols currently in use, set as the default value
    	if (used.size() >= MATS.length) {
			symbol = "BARRIER";
		//Generate a new symbol and check if it's in use
		} else {
			
	    	boolean inUse = true;
	    	int checkCount = 0;
	    	while (inUse) {
	    		
	    		//Break if all mats have been checked
	    		if (checkCount >= MATS.length) {
	    			symbol = "BARRIER";
	    			inUse = false;
	    			break;
	    		}
	    		
	        	//Get the random symbol
	        	Random rand = new Random();
	        	int r = rand.nextInt(MATS.length);
	        	symbol = MATS[r] + "_POTTERY_SHERD"; //Currently, pottery sherd items are used
	        	
	        	//Check if another symbol needs to be generated
	    		inUse = used.contains(symbol);
	    		checkCount++;
	    		
	    	}
    	
		}
    	
    	//Return the unique symbol
    	return symbol;
    	
    }
    
    //Generate a random unique gate address
    public ArrayList<Integer> getRandomAddress() {
    	
    	ArrayList<Integer> address = new ArrayList<>();
    	
    	//Get all of the gate addresses already used
    	WeakHashMap<ArrayList<Integer>, String> addresses = plugin.getNetwork().getAddresses();
    	
    	//Reserved symbol spaces on the dialing device
    	ArrayList<Integer> reserved = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 4, 8, 13, 22, 36, 44}));
    	
    	//Generate a new random address
    	boolean inUse = true;
    	while (inUse) {
    		
        	//Get six random unique symbols from the pool of symbols
    		for (int i=0; i<6; i++) {
	        	Random rand = new Random();
	        	int r = rand.nextInt(45);
    			do r = rand.nextInt(45); while (address.contains(r) || reserved.contains(r));
    			address.add(r);
    		}
        	
        	//Check if another symbol needs to be generated
    		inUse = addresses.containsKey(address);
    		
    	}
    	
    	//Return the unique symbol
    	return address;
    	
    }
	
    //Dial out from a gate and attempt a wormhole connection
	public int dialOut(ArrayList<Integer> sequence, String origin, Player p) {
		
		//Debug messaging
		boolean debugMode = plugin.options.get("debug_mode");
		
		if (debugMode) {
			plugin.getLogger().info("Dial out detected at " + origin + ".");
		}
		
		//Check if network address is in the list of acceptable gate addresses
		ArrayList<Integer> sixDigitAddress = new ArrayList<Integer>(Arrays.asList(sequence.get(0), sequence.get(1), sequence.get(2), sequence.get(3), sequence.get(4), sequence.get(5)));
		
		if (debugMode) {
			plugin.getLogger().info("Six digit address dialed: " + sixDigitAddress.toString());
		}
		
		//Check all addresses to see if dial matches one
		Network net = plugin.getNetwork();
		WeakHashMap<ArrayList<Integer>, String> addresses = net.getAddresses();
		
		if (addresses.containsKey(sixDigitAddress) && sequence.get(7).equals(22)) {

			WeakHashMap<String, String> connections = net.connections.get();
			String destination = addresses.get(sixDigitAddress);
			boolean active = connections.containsValue(destination) || connections.containsKey(destination);
			
			//Abort connection if origin and destination are the same, or the gate is already active
			if (origin.equals(destination) || active) {
				
				if (debugMode) {
					plugin.getLogger().info("Connection to " + destination + " aborted, destination gate already connected or is the same gate.");
				}

				//Play dial red crystal press sound
		    	p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 10.0F, 2.0F);
		    	plugin.messenger.makeMsg(p, "eventDialFailure");
		    	
				return 0;
				
			}
			
			//Make a network connection and establish a wormhole
			if (debugMode) {
				plugin.getLogger().info("Connection to " + destination + " gate approved, connecting...");
			}
			
			Wormhole wormhole = new Wormhole(plugin);
			wormhole.establish(true, p.getDisplayName(), origin, destination);

			//Play dial red crystal press sound
	    	p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 6.0F, 0.5F);
	    	plugin.messenger.makeMsg(p, "eventDialSuccess");
	    	
			return 1;
			
		} else {
			
			if (debugMode) {
				plugin.getLogger().info("Connection aborted, gate address not valid.");
			}

			//Play dial red crystal press sound
	    	p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 8.0F, 1.0F);
	    	plugin.messenger.makeMsg(p, "eventDialFailure");
	    	
			return 0;
			
		}
		
	}
	
}
