package com.cypherx.xauth;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.cypherx.xauth.auth.*;
import com.cypherx.xauth.commands.*;
import com.cypherx.xauth.database.DatabaseController;
import com.cypherx.xauth.listeners.*;
import com.cypherx.xauth.password.PasswordHandler;
//import com.cypherx.xauth.plugins.xPermissions;
import com.cypherx.xauth.strike.StrikeManager;

public class xAuth extends JavaPlugin {
	private DatabaseController dbCtrl;
	private MessageHandler msgCtrl;
	private PlayerManager plyrMngr;
	private PlayerDataHandler plyrDtHndlr;
	private PasswordHandler pwdHndlr;
	private LocationManager locMngr;
	private StrikeManager strkMngr;
	private String h2Version = "1.3.164";

	public void onDisable() {
		if (dbCtrl != null) {
			for (Player p : getServer().getOnlinePlayers()) {
				xAuthPlayer xp = plyrMngr.getPlayer(p);
				if (xp.isProtected())
					plyrMngr.unprotect(xp);
			}

			dbCtrl.close();
		}
	}

	public void onEnable() {
		// Create any required directories
		getDataFolder().mkdirs();

		// Create/load configuration and customizable messages
		loadConfiguration();

		// Disable plugin if auto-disable is set to true and server is running in online-mode
		if (getConfig().getBoolean("main.auto-disable") && getServer().getOnlineMode()) {
			xAuthLog.info("Auto-disabling, server is running in online-mode");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		File h2File = new File("lib", "h2-" + h2Version + ".jar");
		if (!h2File.exists() && getConfig().getBoolean("main.download-library") && !getConfig().getBoolean("mysql.enabled")) {
			xAuthLog.info("-------------------------------");
			xAuthLog.info("Downloading required H2 library..");
			downloadLib(h2File);
			xAuthLog.info("Download complete.");
			xAuthLog.info("");
			xAuthLog.info("Reload the server to enable xAuth.");
			xAuthLog.info("-------------------------------");

			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Initialize permissions support
		//xPermissions.init();

		// Initialize database controller
		dbCtrl = new DatabaseController(this);

		// Test connection to database
		if (!dbCtrl.isConnectable()) {
			xAuthLog.severe("Failed to establish " + dbCtrl.getDBMS() + " database connection!");

			// disable (for now, may change in the future)
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		xAuthLog.info("Successfully established connection to " + dbCtrl.getDBMS() + " database");
		dbCtrl.runUpdater();

		// Initialize ALL THE CLASSES
		plyrMngr	= new PlayerManager(this);
		plyrDtHndlr = new PlayerDataHandler(this);
		pwdHndlr 	= new PasswordHandler(this);
		locMngr 	= new LocationManager(this);
		strkMngr	= new StrikeManager(this);

		Collection<? extends Player> players = getServer().getOnlinePlayers();
		if (!players.isEmpty())
			plyrMngr.handleReload(players);

		// Initialize listeners
		new xAuthPlayerListener(this);
		new xAuthBlockListener(this);
		new xAuthEntityListener(this);

		// Register commands
		getCommand("register").setExecutor(new RegisterCommand(this));
		getCommand("login").setExecutor(new LoginCommand(this));
		getCommand("logout").setExecutor(new LogoutCommand(this));
		getCommand("changepw").setExecutor(new ChangePwdCommand(this));
		getCommand("xauth").setExecutor(new xAuthCommand(this));

		xAuthLog.info(String.format("v%s Enabled!", getDescription().getVersion()));
		lol();
	}

	private void loadConfiguration() {
		// configuration
		getConfig().options().copyDefaults(true);
		saveConfig();

		// messages
		msgCtrl = new MessageHandler(this);
		msgCtrl.getConfig().options().copyDefaults(true);
		msgCtrl.saveConfig();
		msgCtrl.reloadConfig();
	}

	private void downloadLib(File h2File) {
		File dir = new File("lib");
		if (!dir.exists())
			dir.mkdir();		

		Utils.downloadFile(h2File, "http://love-despite.com/cypher/bukkit/lib/" + h2File.getName());
	}

	public File getJar() { return getFile(); }
	public DatabaseController getDbCtrl() { return dbCtrl; }
	public MessageHandler getMsgHndlr() { return msgCtrl; }
	public PlayerManager getPlyrMngr() { return plyrMngr; }
	public PlayerDataHandler getPlyrDtHndlr() { return plyrDtHndlr; }
	public PasswordHandler getPwdHndlr() { return pwdHndlr; }
	public LocationManager getLocMngr() { return locMngr; }
	public StrikeManager getStrkMngr() { return strkMngr; }

	public Auth getAuthClass(xAuthPlayer p){
		if (isAuthURL())
			return new AuthURL(this, p.getIPAddress());
		else
			return new AuthSQL(this, p);
	}

	public boolean isAuthURL(){
		return this.getConfig().getBoolean("authurl.enabled");
	}

	public void reload() {
		loadConfiguration();
		//plyrMngr.reload();
	}

	private void lol() {
		Calendar cal = new GregorianCalendar(TimeZone.getDefault());
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);

		if (month == 3 && day == 1) {
			xAuthLog.warning("Your trial version of xAuth expires today!");
			xAuthLog.warning("Purchase the full version on Steam for $19.99.");
		} else if (month == 3 && day == 2)
			xAuthLog.info("April Fools!! xAuth will always be free!");
	}
}