package com.kraken.gatenetwork;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class GateNetwork extends JavaPlugin {

	//Main instances
	private Network network;
	private DialDevice dial;
	private Traveling traveling;
	private Database db;
	private Messages messenger;
	
	//Plugin settings mappings
	WeakHashMap<String, Boolean> options = new WeakHashMap<>();
	
	//Cooldowns
	ArrayList<Player> cooldowns = new ArrayList<Player>();
	
    @Override
    public void onEnable() {
    	
    	//Plugin start-up
		PluginManager pm = getServer().getPluginManager();
		
		//Register plugin messages
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		//Default files
		getConfig().options().copyDefaults(true); // default config.yml
		loadDefaultFiles(); // custom yml configs
		
	    //Plugin settings
		String[] opts = {"debug_mode", "silent_mode", "easy_mode", "op_required", "permissions"};
		for (String opt : opts) {
			setOption(opt, getConfig().getBoolean(opt));
		}
		
		//Messages
		String lang = getConfig().getString("language");
		messenger = new Messages(this, lang);
		
        //Traveling
  		this.traveling = new Traveling(this);
  		pm.registerEvents((Listener) traveling, this);
    	
        //DialDevice
  		this.dial = new DialDevice(this);
  		pm.registerEvents((Listener) dial, this);
  		
  		//Network
    	this.network = new Network(this);
    	
  		//Database
  		db = new Database(this);
    	
    }
    
    @Override
    public void onDisable() {
    	
    	//Check to enable debug messages
    	boolean debug_mode = options.get("debug_mode");
    	
    	//Close database connection
    	if (debug_mode) {
            getLogger().info("Closing any database connection...");
    	}
    	db.closeConnection();
    	
    	//Unregister plugin messages
    	if (debug_mode) {
            getLogger().info("Unregistering plugin message channels...");
    	}
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
                
    }
    
    //Commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Commands cmds = new Commands(this);
		return cmds.onCommand(sender, cmd, label, args);
    }
	
    //Return the database object
    public Database getDatabase() {
    	return db;
    }
    
	//Get a FileConfiguration from file name
	public File getFile(String fileName) {
	    File f = new File("plugins/" + getPluginName(), fileName + ".yml");
	    return f;
	}
    
	//Get a FileConfiguration from file name
	public FileConfiguration getFileConfig(String fileName) {
	    FileConfiguration fc = YamlConfiguration.loadConfiguration(getFile(fileName));
	    return fc;
	}
	
	//Get the plugin name
	public String getPluginName() {
		return "GateNetwork";
	}
	
	//Get the plugin version
	public String getVersion() {
		return getFileConfig("plugin").getString("version");
	}
	
	//Get the Bungee server name value loaded from config
	public String getServerName() {
		return getConfig().getString("server_name");
	}
	
	//Return the global class instances
	public Network getNetwork() {
		return this.network;
	}
	
	public DialDevice getDialDevice() {
		return this.dial;
	}
	
	public Traveling getTraveling() {
		return this.traveling;
	}
	
    //Get the Messages
    public Messages getMessenger() {
    	return this.messenger;
    }
    
    //Save the player file
    public void saveCustomFile(FileConfiguration fileConfig, File file) {
    	try {
			fileConfig.save(file);
		} catch (IOException e) {
			System.out.println("Error saving custom config file: " + file.getName());
		}
    }
	
    //Options setting
    public void setOption(String option, boolean setting) {
    	getConfig().set(option, setting);
    	saveConfig();
    	options.put(option, setting);
    	if (options.get("debug_mode")) {
    		getLogger().info(option + " setting: " + setting );
    	}
    }
    
	//Load files from default if not present
	private void loadDefaultFiles() {
		
		//Default files to be loaded: schematic.yml, dial.yml
		String[] files = {"schematic", "dial"};
		
		//Check each file and save from defaults if not present
		for (String fName : files) {
			File file = new File("plugins/" + getPluginName() + "/", fName + ".yml");
		    if ( !file.exists() ) {
		    	saveResource(fName + ".yml", false);
		    }
		}
	    
    }
    
    //Remove cooldown after 0.5 sec
    public void removeCooldown(Player player) {
    	BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
            	cooldowns.remove(player);
            }
        }, 10);
    }
	
}
