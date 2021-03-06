package fr.neutronstars.nbot;

import fr.neutronstars.nbot.command.CommandManager;
import fr.neutronstars.nbot.command.defaut.ConsoleCommand;
import fr.neutronstars.nbot.command.defaut.DefaultCommand;
import fr.neutronstars.nbot.command.defaut.HelpCommand;
import fr.neutronstars.nbot.exception.NBotConfigurationException;
import fr.neutronstars.nbot.plugin.PluginManager;
import fr.neutronstars.nbot.util.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.impl.NBotLogger;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.File;

/**
 * Created by NeutronStars on 30/07/2017
 */
public class NBotStart
{
    private static final NBotLogger logger = StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger("NBot");

    public static void main(String... args)
    {
        System.setProperty("file.encoding", "UTF-8");

        loadFolders("guilds", "plugins", "config", "tmp");

        Configuration configuration = loadConfiguration();
        setDefaultConfiguration(configuration);

        NBotLogger.load(configuration);

        logger.info(String.format("Starting %1$s v%2$s by %3$s...", NBot.getName(), NBot.getVersion(), NBot.getAuthor()));

        logger.info("Checking the last update...");
        checkUpdate();

        logger.info(String.format("Loading libraries of JDA v%1$s...", NBot.getJdaVersion()));

        PluginManager pluginManager = new PluginManager(configuration.getString("loadedFormat"), configuration.getString("enabledFormat"), configuration.getString("disabledFormat"));
        CommandManager.registerCommands(null, new DefaultCommand(), new ConsoleCommand(), new HelpCommand());
        pluginManager.registerCommands();

        try
        {
            NBotServer server = new NBotServer(configuration, pluginManager);
            NBot.setServer(server);
            loop(server);
        } catch(Exception e)
        {
            logger.error(e.getMessage(), e);
            NBot.saveLogger();
        }
    }

    private static void loop(NBotServer server)
    {
        long lns = System.nanoTime();
        double ns = 1000000000.0/20.0;
        long ls = System.currentTimeMillis();

        int tps = 0;

        while(true)
        {
            if(System.nanoTime() - lns > ns)
            {
                lns += ns;
                update();
                tps++;
            }

            if(System.currentTimeMillis() - ls >= 1000)
            {
                ls = System.currentTimeMillis();
                server.setTps(tps);
                tps = 0;
            }
        }
    }

    private static void update()
    {
        NBot.getScheduler().updateTasks();
    }

    private static Configuration loadConfiguration()
    {
        return  Configuration.loadConfiguration(new File("config/config.json"));
    }

    private static void setDefaultConfiguration(Configuration configuration)
    {
        if(configuration == null)
        {
            logger.error("The config cannot be null.", new NBotConfigurationException("The config cannot be null."));
            NBot.saveLogger();
            System.exit(0);
        }

        if(!configuration.has("token")) configuration.set("token", "Insert your token here.");

        if(!configuration.has("playing")) configuration.set("playing", "null");

        if(!configuration.has("loadedFormat")) configuration.set("loadedFormat", "%1$s v%2$s by %3$s is loaded.");
        if(!configuration.has("enabledFormat")) configuration.set("enabledFormat", "%1$s v%2$s by %3$s is enabled.");
        if(!configuration.has("disabledFormat")) configuration.set("disabledFormat", "%1$s v%2$s by %3$s is disabled.");

        if(!configuration.has("loggerTimeFormat")) configuration.set("loggerTimeFormat", "HH:mm:ss");
        if(!configuration.has("loggerLineFormat")) configuration.set("loggerLineFormat", "[{DATE}] [{LEVEL}-{NAME}] {MESSAGE}");

        configuration.save();
    }

    private static void loadFolders(String... names)
    {
        for(String name: names)
        {
            File file = new File(name);
            if(!file.exists()) file.mkdir();
        }
    }

    private static void checkUpdate()
    {
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet("https://api.github.com/repos/NeutronStars/NBot_v2_0/releases/latest");
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);

            JSONObject object = new JSONObject(EntityUtils.toString(result.getEntity(), "UTF-8"));
            String latest = object.getString("tag_name");

            if(NBot.getVersion().equalsIgnoreCase(latest))
            {
                logger.info("You have the last update.");
                return;
            }

            logger.warn("New update available!");
            logger.warn("You can download now here : "+object.getString("html_url"));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
