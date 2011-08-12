package net.krinsoft.chat;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import net.krinsoft.chat.commands.CommandHandler;
import net.krinsoft.chat.listeners.ChatListener;
import net.krinsoft.chat.listeners.EntityListener;
import net.krinsoft.chat.listeners.PlayerListener;
import net.krinsoft.chat.util.ChatConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

/**
 *
 * @author krinsdeath
 */
public class ChatCore extends JavaPlugin {

    public enum Info {
        AUTHORS,
        NAME,
        FULLNAME,
        VERSION;
    }

    // logger
    private final static Logger LOGGER = Logger.getLogger("ChatSuite");

    // listeners
    private PlayerListener pListener;
    private EntityListener eListener;
    private ChatListener chatListener;

    // plugin info and managers
    private PluginDescriptionFile pdf;
    private PluginManager pm;
    private boolean debug = true;

    // configuration details and flags
    private Configuration config;
    private Configuration worldConfig;
    private boolean afkInvincibility = false;
    private boolean multiverseWorldAliases = false;
    private boolean allowChannels = false;

    private ChannelManager channelManager;
    private WorldManager worldManager;
    private CommandHandler commandHandler;

    // maps
    private HashMap<String, ChatPlayer> players = new HashMap<String, ChatPlayer>();
    private HashMap<String, String> aliases = new HashMap<String, String>();

    @Override
    public void onEnable() {
        pdf = getDescription();
        initConfiguration();
        initEvents();
        buildPlayers();
        log(info() + " enabled.");
    }

    @Override
    public void onDisable() {
        players = null;
        pListener = null;
        eListener = null;
        commandHandler = null;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        return commandHandler.handle(cs, cmd, label, args);
    }

    private void initConfiguration() {
        ChatConfiguration settings = new ChatConfiguration(this);
        config = new Configuration(settings.buildDefault("config.yml"));
        config.load();
        worldConfig = new Configuration(settings.buildDefault("worlds.yml"));
        worldConfig.load();
        allowChannels = config.getBoolean("plugin.allow_channels", false);
        afkInvincibility = config.getBoolean("plugin.afk_invincibility", false);
        channelManager = new ChannelManager(this);
        worldManager = new WorldManager(this);
        ChatPlayer.init(this);
    }

    private void initEvents() {
        // set up the listeners
        initListeners();
        pm = getServer().getPluginManager();

        // register events
        // ---
        // player events
        pm.registerEvent(Type.PLAYER_CHAT, pListener, Priority.Highest, this);
        pm.registerEvent(Type.PLAYER_JOIN, pListener, Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_QUIT, pListener, Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_KICK, pListener, Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_TELEPORT, pListener, Priority.Monitor, this);
        if (afkInvincibility) {
            pm.registerEvent(Type.PLAYER_MOVE, pListener, Priority.Low, this);
            pm.registerEvent(Type.ENTITY_DAMAGE, eListener, Priority.Normal, this);
        }
        // ---
        // chat event
        pm.registerEvent(Type.CUSTOM_EVENT, chatListener, Priority.Normal, this);
    }

    private void buildPlayers() {
        for (Player p : getServer().getOnlinePlayers()) {
            registerPlayer(p);
        }
    }

    private void initListeners() {
        pListener = new PlayerListener(this);
        eListener = new EntityListener(this);
        chatListener = new ChatListener(this);
        commandHandler = new CommandHandler(this);
    }

    // logging and information
    public void debug(String message) {
        if (debug) {
            message = "[" + info(Info.NAME) + "] [Debug] " + message;
            LOGGER.info(message);
        }
    }

    public void log(String message) {
        message = "[" + info(Info.NAME) + "] " + message;
        LOGGER.info(message);
    }

    public String info() {
        return info(Info.FULLNAME) + " by " + info(Info.AUTHORS);
    }

    public String info(Info i) {
        switch (i) {
            case AUTHORS: return pdf.getAuthors().toString().replaceAll("[\\[\\]]", "");
            case NAME: return pdf.getName();
            case FULLNAME: return pdf.getFullName();
            case VERSION: return pdf.getVersion();
            default: return pdf.getFullName();
        }
    }

    /**
     * Gets the ChatPlayer instance for the player specified
     * @param p
     * the player to fetch
     * @return
     * the player's ChatPlayer instance, or null
     */
    public ChatPlayer getPlayer(Player p) {
        if (players.get(p.getName()) == null) {
            registerPlayer(p);
        }
        return players.get(p.getName());
    }

    /**
     * Gets the ChatPlayer instance for the player specified
     * @param p
     * the player to fetch
     * @return
     * the player's ChatPlayer instance, or null
     */
    public ChatPlayer getPlayer(String p) {
        if (players.get(p) == null) {
            registerPlayer(getServer().getPlayer(p));
        }
        return players.get(p);
    }

    public boolean isPlayerRegistered(Player p) {
        return (players.get(p.getName()) == null);
    }

    public ConfigurationNode getGroupNode(String group) {
        return config.getNode("groups." + group);
    }

    public List<String> getGroups() {
        return config.getKeys("groups");
    }

    public int getWeight(String group) {
        return config.getInt("groups." + group + ".weight", 1);
    }

    public void registerPlayer(Player player) {
        if (players.containsKey(player.getName())) {
            return;
        }
        players.put(player.getName(), new ChatPlayer(player));
        getChannelManager().addPlayerToChannel(player, player.getWorld().getName());
        getChannelManager().addPlayerToChannel(player, config.getString("plugin.global_channel_name", "Global"));
        debug("Player '" + player.getName() + "' registered");
    }

    public void unregisterPlayer(Player player) {
        if (!players.containsKey(player.getName())) {
            return;
        }
        getChannelManager().removePlayerFromAllChannels(player);
        players.remove(player.getName());
        debug("Player '" + player.getName() + "' registered");
    }

    // WORLDS
    public WorldManager getWorldManager() {
        return this.worldManager;
    }
    
    // CHANNELS
    public ChannelManager getChannelManager() {
        return this.channelManager;
    }

}
