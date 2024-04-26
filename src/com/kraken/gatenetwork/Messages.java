package com.kraken.gatenetwork;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Messages {

	//Main instance
	private GateNetwork plugin;
	
	//Globals
	private String language;
	private static ArrayList<String> LANGUAGES = new ArrayList<String>(Arrays.asList("english","spanish"));
	private boolean silentMode;
    private File msgFile;
    private FileConfiguration msgFileConfig;
	
    //Constructor
	public Messages(GateNetwork plugin, String language) {
		this.plugin = plugin;
        this.silentMode = plugin.getConfig().getBoolean("silent_mode");
        loadMessageFiles();
        setLanguage(language);
    }
	
	//Silent mode settings
	public void silence(boolean setting) {
		this.silentMode = setting;
	}
	
	//Load the message files
	public void loadMessageFiles() {
		for (String lang : LANGUAGES) {
		    File msgFile = new File("plugins/" + plugin.getPluginName() + "/lang/", lang.toLowerCase() + ".yml");
		    if (!msgFile.exists()) {
		    	plugin.saveResource("lang/" + lang.toLowerCase() + ".yml", false);
		    }
		}
    }
	
	//Language getter & setter
	public String getLanguage() {
		return this.language;
	}

	//Language settings
	public void setLanguage (String language) {
		this.language = language.toLowerCase();
		msgFile = new File("plugins/" + plugin.getPluginName() + "/lang/", language.toLowerCase() + ".yml");
		msgFileConfig = YamlConfiguration.loadConfiguration(msgFile);
	}
	
	//Console messages
	public void makeConsoleMsg(String msg) {
		if (!this.silentMode) {
			String v = msg.equals("cmdVersion")?plugin.getVersion():"";
			String msgStr = msgFileConfig.getString("console." + msg) + v;
			System.out.println(msgStr);
		}
	}
	
	//Player messages (in-game)
	public void makeMsg(Player player, String msg) {
		ArrayList<String> pass = new ArrayList<>(Arrays.asList("errorSilentMode", "cmdSilentOn", "cmdSilentOff"));
		if (!this.silentMode || pass.contains(msg)) {
			String v = msg.equals("cmdVersion")?plugin.getVersion():"";
			String msgStr = msgFileConfig.getString("player." + msg) + v;
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', msgStr));
		}
	}
		
}
