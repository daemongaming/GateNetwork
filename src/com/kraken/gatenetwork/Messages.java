package com.kraken.gatenetwork;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Messages {

	private GateNetwork plugin;
	String language;
	String VERSION;
	boolean silentMode;
	
    File msgFile;
    FileConfiguration msgFileConfig;
	
    //Constructor
	public Messages(GateNetwork plugin, String language) {
		
		  this.plugin = plugin;
          this.silentMode = plugin.getConfig().getBoolean("silent_mode");
          this.VERSION = plugin.getVersion();
          
          setLanguage(language);
          
    }
	
	//Silent mode settings
	public void silence(boolean setting) {
		this.silentMode = setting;
	}
	
	//Language settings
	public void setLanguage (String language) {
		this.language = language.toLowerCase();
		msgFile = new File("plugins/" + plugin.getPluginName() + "/lang/", language.toLowerCase() + ".yml");
		msgFileConfig = YamlConfiguration.loadConfiguration(msgFile);
	}
	
	//Console messages
	public void makeConsoleMsg(String msg) {
		
		if (this.silentMode) {
			return;
		}
		
		switch (msg) {	
			case "cmdVersion":
				System.out.println("v" + VERSION);
				break;
			default:
				System.out.println( msgFileConfig.getString("console." + msg) );
				break;
		}
		
	}
	
	//Player messages (in-game)
	public void makeMsg(Player player, String msg) {
		
		if (this.silentMode && !msg.equals("errorSilentMode") && !msg.equals("cmdSilentOn") && !msg.equals("cmdSilentOff")) {
			return;
		}

		String text = ChatColor.translateAlternateColorCodes('&', msgFileConfig.getString("player." + msg));
		String vText = (msg == "cmdVersion" ? VERSION : "");
		player.sendMessage( text + vText );
	
	}
		
}
