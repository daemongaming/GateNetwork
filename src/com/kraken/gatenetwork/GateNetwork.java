package com.kraken.gatenetwork;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
	public static GateNetwork plugin;
	private Network network;
	private DialDevice dial;
	private Traveling traveling;
	private Database db;
	
	//Lang objects
	public static String PLUGIN_NAME = "GateNetwork";
	public static String VERSION;
	public static String SERVER_NAME;
	ArrayList<String> languages = new ArrayList<String>();
	Messages messenger;
	
	//Options
	WeakHashMap<String, Boolean> options = new WeakHashMap<>();
	
	//Cooldowns
	ArrayList<Player> cooldowns = new ArrayList<Player>();
	
    @Override
    public void onEnable() {
    	
    	//Plugin start-up
    	plugin = this;
		PluginManager pm = getServer().getPluginManager();
		
		//Register plugin messages
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		//Copies the default config.yml from within the .jar if "plugins/<name>/config.yml" does not exist
		getConfig().options().copyDefaults(true);
		
		//Language/Messages handler class construction
		languages.add("english");
		languages.add("spanish");
		loadMessageFiles();
		messenger = new Messages(this, "english");
		
		//Load the build default files
		loadDefaultFiles();
		
	    //Loading default settings into options
		ArrayList<String> optionsToSet = new ArrayList<String>();
		optionsToSet.addAll(Arrays.asList("debug_mode", "silent_mode", "easy_mode", "op_required", "permissions"));
		
		//Load boolean options
		for (String opt : optionsToSet) {
			setOption(opt, getConfig().getBoolean(opt));
		}
    	
		//Get the server name from config
    	SERVER_NAME = getConfig().getString("server_name");
    	
    	//Check to enable silent mode
    	silencer(options.get("silent_mode"));
		
        //Starts and registers the main Listener
  		this.traveling = new Traveling(this);
  		pm.registerEvents((Listener) traveling, this);
    	
        //Starts and registers the DialDevice Listener
  		this.dial = new DialDevice(this);
  		pm.registerEvents((Listener) dial, this);
  		
  		//Starts and registers the Network Listener
    	this.network = new Network(this);
  		pm.registerEvents((Listener) network, this);
    	
  		//Set the database info
  		db = new Database(this);
        
        //Get the plugin version number
        VERSION = getFileConfig("plugin").getString("version");
    	
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
	
	//GETTERS
	
    //Return the database object
    public Database getDatabase() {
    	return db;
    }
    
	//Get a FileConfiguration from file name
	public File getFile(String fileName) {
	    File f = new File("plugins/" + PLUGIN_NAME, fileName + ".yml");
	    return f;
	}
    
	//Get a FileConfiguration from file name
	public FileConfiguration getFileConfig(String fileName) {
	    FileConfiguration fc = YamlConfiguration.loadConfiguration(getFile(fileName));
	    return fc;
	}
	
	//Get the plugin name
	public String getPluginName() {
		return PLUGIN_NAME;
	}
	
	//Get the plugin version
	public String getVersion() {
		return VERSION;
	}
	
	//Get the Bungee server name value loaded from config
	public String getServerName() {
		return SERVER_NAME;
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
    
	//SETTERS
    
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
    
    //Silent mode setting
    public void silencer(boolean silentMode) {
    	messenger.silence(silentMode);
    }
	
    //LOADERS
    
	//Load files from default if not present
	public void loadDefaultFiles() {
		
		//Default files to be loaded: schematic.yml, dial.yml
		String[] files = {"schematic", "dial"};
		
		//Check each file and save from defaults if not present
		for (String fName : files) {
			File file = new File("plugins/" + PLUGIN_NAME + "/", fName + ".yml");
		    if ( !file.exists() ) {
		    	saveResource(fName + ".yml", false);
		    }
		}
	    
    }
	
    //Load the files for messages based on language setting
	public void loadMessageFiles() {
		for (String lang : languages) {
		    File msgFile = new File("plugins/" + PLUGIN_NAME + "/lang/", lang.toLowerCase() + ".yml");
		    if ( !msgFile.exists() ) {
		    	saveResource("lang/" + lang.toLowerCase() + ".yml", false);
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
