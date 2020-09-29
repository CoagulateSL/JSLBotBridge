package net.coagulate.SL.JSLBotBridge;

import net.coagulate.Core.BuildInfo.JSLBotBridgeBuildInfo;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.JSLBot.Handlers.Avatars;
import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.Packets.Types.LLUUID;
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.EventQueue;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.HTML.ServiceTile;
import net.coagulate.SL.SL;
import net.coagulate.SL.SLModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.logging.Logger;

public class JSLBotBridge extends SLModule {
    public final int majorVersion() { return JSLBotBridgeBuildInfo.MAJORVERSION; }
    public final int minorVersion() { return JSLBotBridgeBuildInfo.MINORVERSION; }
    public final int bugFixVersion() { return JSLBotBridgeBuildInfo.BUGFIXVERSION; }
    public final String commitId() { return JSLBotBridgeBuildInfo.COMMITID; }
    public final Date getBuildDate() { return JSLBotBridgeBuildInfo.BUILDDATE; }

    JSLBot bot=null;

    @Nullable
    @Override
    public Map<ServiceTile, Integer> getServices() {
        return null;
    }

    @Nonnull
    @Override
    public String getName() {
        return "JSLBotBridge";
    }
    @Nonnull
    public String getDescription() { return "JSLBot bridge module for core SL services"; }

    public void shutdown() {
        if (bot != null) {
            JSLBot reference=bot; bot=null;
            reference.shutdown("SL System is shutting down");
        }
    }
    public void initialise() {
        //LLCATruster.doNotUse(); // as in we use our own truster later on
        for (Method method:ClassTools.getAnnotatedMethods(JSLBot.CmdHelp.class)) {
            boolean warned=false;
            boolean firstparam=true;
            for (Parameter param:method.getParameters()) {
                if (firstparam) {
                    firstparam=false;
                } else {
                    if (!warned && !param.isAnnotationPresent(JSLBot.Param.class)) {
                        SL.log("JSLBotBridge").severe("Malformed JSLBot CmdHelp/Param annotations in " + method.getDeclaringClass().getCanonicalName() + "." + method.getName());
                        warned = true;
                    }
                }
            }
        }
        // schemaCheck(SL.getDB(),"jslbotbridge",1); // seems pointless for the moment
        bot = new JSLBot(getBotConfig());
        bot.registershutdownhook = false;
        bot.ALWAYS_RECONNECT = true;
        bot.start();
    }

    @Override
    public void maintenance() {}

    public void startup() {
        if (!Config.getDevelopment()) {
            waitBot();
        }
    }

    private static BotConfig botconfig=null;
    @Nonnull
    public static BotConfig getBotConfig() {
        if (botconfig==null) {
            botconfig=new BotConfig();
        }
        botconfig.put("firstname",Config.getBotFirstName());
        botconfig.put("lastname",Config.getBotLastName());
        botconfig.put("password",Config.getBotPassword());
        botconfig.put("CnC.authoriser","OwnerOnly");
        botconfig.put("handlers",
                "Avatars,CnC,Sink,Health,Regions,Teleportation,Agent,Objects,Groups" //todo ,net.coagulate.SL.Bots.BotInterface,net.coagulate.SL.Services.RegionMonitor"+".SimStats"
        );
        botconfig.put("loginlocation","home");
        botconfig.put("CnC.authorisation.owneruuid",Config.getBotOwnerUUID());
        botconfig.put("CnC.authorisation.ownerusername",Config.getBotOwnerUsername());
        botconfig.put("CnC.publiccommandprefix","*");
        botconfig.put("CnC.privatecommandprefix","*");
        botconfig.put("CnC.homesickfor",Config.getBotHomeRegion());
        botconfig.put("loginuri",Config.getJSLBotBridgeLoginURI());
        if (!Config.getJSLBotBridgeSeat().isBlank()) { botconfig.put("homeseat",Config.getJSLBotBridgeSeat()); }
        return botconfig;
    }

    private void waitBot() {
        try {
            bot.getLogger("waitConnection").config("Waiting for JSLBridge bot to connect");
            bot.waitConnection(30000);
        } catch (@Nonnull final IllegalStateException ignored) {
        }
        if (!bot.connected()) {
            bot.shutdown("Failed to connect");
        }
    }

    @Override
    public void processEvent(EventQueue event) {
        if (event.getCommandName().equalsIgnoreCase("im")) {
            event.claim();
            bot.im(new LLUUID(event.getData().getString("uuid")),event.getData().getString("message"));
            event.complete();
        }
        if (event.getCommandName().equalsIgnoreCase("recalcnames")) {
            event.claim();
            if (recalcthread!=null && recalcthread.isAlive()) {
                event.complete("RecalcThreadAlive");
            } else {
                recalcthread=new RecalcThread(event);
                recalcthread.start();
            }
        }
    }

    @Override
    public Object weakInvoke(String command, Object... arguments) {
        if (command.equalsIgnoreCase("IM")) {
            bot.im(new LLUUID((String)arguments[0]),(String)arguments[1]);
            return null;
        }
        if (command.equalsIgnoreCase("GROUPINVITE")) {
            bot.api().groupInvite((String)arguments[0],(String)arguments[1],(String)arguments[2]);
            return null;
        }
        return super.weakInvoke(command, arguments);
    }

    @Override
    protected int schemaUpgrade(DBConnection db, String schemaName, int currentVersion) {
        return currentVersion;
    }

    private static Thread recalcthread=null;


    private class RecalcThread extends Thread {
        private int casecorrections=0;
        private int renames=0;
        public RecalcThread(EventQueue event) {
            Logger log=SL.log("AvatarNameRecalc");
            log.info("Starting!");
            int processed=0;
            int lastreport= UnixTime.getUnixTime();
            int starttime=lastreport;
            Set<User> allusers=User.getAllUsers();
            List<String> searchfor=new ArrayList<>(10);
            for (User user:allusers) {
                searchfor.add(user.getUUID());
                processed++;
                if ((processed % 10) ==0 ) {
                    doLookup(searchfor);
                    searchfor.clear();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                    if ((UnixTime.getUnixTime() - lastreport) > 60) {
                        lastreport = UnixTime.getUnixTime();
                        double usersPerSecond = (((double) processed) / ((double) (lastreport - starttime)));
                        double expectRunTime = ((double) (allusers.size() - processed)) / usersPerSecond;
                        int eta = (int) expectRunTime;
                        log.info("Processed " + processed + "/" + allusers.size() + ".  ETA: " + UnixTime.duration(eta, true));
                    }
                }
            }
            if (!searchfor.isEmpty()) { doLookup(searchfor); }
            log.info("Exiting - processed "+processed+" with "+renames+" renames and "+casecorrections+" case corrections");
            event.complete();
        }

        private void doLookup(List<String> searchfor) {
            Map<String,String> map=((Avatars)(bot.brain().getHandler("Avatars"))).resolveUUIDStrings(searchfor);
            for (Map.Entry<String,String> entry:map.entrySet()) {
                String uuid=entry.getKey();
                String username=entry.getValue();
                if (username==null) {
                    SL.log("AvatarNameRecalc").warning("Failed to get an avatar name for " + uuid);
                } else {
                    User user = User.findUserKey(uuid);
                    if (!user.getUsername().equals(username)) {
                        user.setUsername(username);
                        if (!user.getUsername().equalsIgnoreCase(username)) {
                            SL.log("AvatarNameRecalc").info("Avatar rename for " + uuid + " from " + user.getUsername() + " to " + username);
                            renames++;
                        } else {
                            casecorrections++;
                        }
                    }
                }
            }
        }
    }
}
