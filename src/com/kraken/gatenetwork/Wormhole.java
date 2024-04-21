package com.kraken.gatenetwork;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import com.kraken.gatenetwork.Network.Gate;

public class Wormhole {
	
	//Globals
	private GateNetwork plugin;
	
	//Wormhole constructor
    public Wormhole(GateNetwork plugin) {
    	this.plugin = plugin;
    }
    
    //Open or close a wormhole
	public void establish(boolean active, String playerName, String origin, String destination) {
		
		//Origin and destination gate objects w/ data
		Network net = plugin.getNetwork();
		Gate oGate = net.getGate(origin);
		Gate dGate = net.getGate(destination);
	    ArrayList<Gate> gatesToUpdate = new ArrayList<>();
	    gatesToUpdate.addAll(Arrays.asList(oGate, dGate));
		
		//Local gates list
	    ArrayList<Location> locs = new ArrayList<>();
		
	    //Update the network for the origin and destination gates
	    for (Gate gate : gatesToUpdate) {

	    	//Set the gate data accordingly
	    	gate.active = active;
		    gate.start = active?LocalDateTime.now().toString():"false";
		    gate.isOrigin = gate.world.equals(origin);
	    	boolean isActiveOrigin = gate.isOrigin && active;
		    gate.dialer = isActiveOrigin?playerName:"false";
		    String dWorld = isActiveOrigin?dGate.world:oGate.world;
		    gate.destination = active?dWorld:"false";
		    
	    	//Update the config and network database
		    net.update(gate, false, true);
		    
		    //Add gate to local list
		    if (gate.sameServer) {
		    	Location loc = LocSerialization.getLiteLocationFromString(gate.loc);
	    		locs.add(loc);
		    }
		    
	    }

	    //Add the wormhole blocks and effects
	    wormholeEffects(active, true, locs);
    	
    }
    
    //Wormhole creation animation
    public void wormholeEffects(boolean active, boolean establishing, ArrayList<Location> locs) {
    	
    	//Set up the scheduler for delayed tasks
	    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
	    
	    //Loop through each wormhole location given
	    for (Location loc : locs) {
		    	
	    	if (active) {
	    		
		    	//Dial sequence success, wormhole forming sound effect
		    	loc.getWorld().playSound(loc, Sound.BLOCK_COPPER_STEP, 14.0F, 0.5F);
	            scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
	                @Override
	                public void run() {
				    	loc.getWorld().playSound(loc, Sound.BLOCK_COPPER_STEP, 8.0F, 0.5F);
	                }
	            }, 10);
		    	
		    	//Puddle creation
		    	List<Block> blocks = getNearbyBlocks(loc, 5, true);
		    	
		    	for (Block block : blocks) {
		    		block.setType(Material.WATER);
		    		if (establishing) {
		    			wormholeKill(loc);
		    		}
		    	}
		    	
		    	//Wormhole back blocks creation
		    	List<Block> backBlocks = getNearbyBlocks(loc.subtract(1.0, 0.0, 0.0), 5, false);
		    	
		    	for (Block block : backBlocks) {
		    		block.setType(Material.SEA_LANTERN);
		    		wormholeKill(loc);
		    	}
		    	
		    	//Wormhole opening sound effect
		    	loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 20.0F, 6.0F);
		    	
	    	} else {
	    		
	  	    	//Wormhole closing sound effect
	  	    	loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 20.0F, 6.0F);
	      	
	  	    	//Puddle removal
	  	    	List<Block> blocks = getNearbyBlocks(loc, 5, false);
	  	    	
	  	    	for (Block block : blocks) {
	  	    		double y = block.getLocation().getBlockY();
	  	    		double locY = loc.getBlockY();
	  	    		double z = block.getLocation().getBlockZ();
	  	    		double locZ = loc.getBlockZ();
		    		block.setType(Material.AIR);
		    		boolean isSideBlock = (y-locY == 0 && z-locZ == 5) || (y-locY == 0 && z-locZ == -5);
		    		if (isSideBlock) {
		    			Location blockLoc = block.getLocation().add(0.0, 1.0, 0.0);
			    		blockLoc.getBlock().setType(Material.AIR);
		    		}
	  	    	}
	  	    	
	  	    	//Wormhole back blocks removal
	  	    	List<Block> blocks3 = getNearbyBlocks(loc.subtract(1.0, 0.0, 0.0), 5, false);
	  	    	for (Block block : blocks3) {
		    		block.setType(Material.AIR);
	  				block.getLocation().getWorld().spawnParticle(Particle.WATER_SPLASH, block.getLocation(), 8);
	  	    	}	
	  	    	
	    	}
	    	
	    }
	    
    }
    
    //Get nearby blocks, pretty straightforward
    public static ArrayList<Block> getNearbyBlocks(Location loc, int radius, boolean puddleCreation) {
    	
    	ArrayList<Block> blocks = new ArrayList<Block>();
    	
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        World w = loc.getWorld();
        int rSquared = radius * radius;
        for (int y = cy - radius; y <= cy + radius; y++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if ((cz - z) * (cz - z) + (cy - y) * (cy - y) <= rSquared) {
                	if (puddleCreation) {
                		if (
                				//Lord, help me, for I am sinning:
                				((y - cy) == 5) && ((z - cz) == 0) || // Top block
                				((y - cy) == 5) && ((z - cz) == 1) || 
                				((y - cy) == 5) && ((z - cz) == -1) || 
                				((y - cy) == 4) && ((z - cz) == 2) || 
                				((y - cy) == 4) && ((z - cz) == -2) || 
                				((y - cy) == 4) && ((z - cz) == 3) || 
                				((y - cy) == 4) && ((z - cz) == -3) || 
                				((y - cy) == 3) && ((z - cz) == 4) || 
                				((y - cy) == 3) && ((z - cz) == -4)
                			) {
                			blocks.add(w.getBlockAt(cx, y, z));
                		} else if (
                					((y - cy) == 0) && ((z - cz) == 5) || // Side right block
                					((y - cy) == 0) && ((z - cz) == -5) // Side left block
                				) {
                			blocks.add(w.getBlockAt(cx, y+1, z));
                		}
                	} else {
                		blocks.add(w.getBlockAt(cx, y, z));
                	}
                }
            }
        }
        
        return blocks;
    	
    }
    
    //Kill any entities too near to the wormhole
    public void wormholeKill(Location loc) {
    	
    	List<Entity> entities = (List<Entity>) loc.getWorld().getNearbyEntities(loc, 5, 5, 5);
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity) {
				boolean inGate = loc.getBlockX() == entity.getLocation().getBlockX();
				boolean traveling = false;
				if (entity instanceof Player) {
					Player player = (Player) entity;
					traveling = plugin.getTraveling().getTravelers().contains(player);
				}
				if (inGate && !traveling) {
					//Vaporize the entity
					((LivingEntity) entity).setHealth(0.0);
				}
			}
		}
		
    }
    
}
