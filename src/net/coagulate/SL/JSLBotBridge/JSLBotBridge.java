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
    public final String commitId() { return JSLBotBridgeBuildInfo.COMMITID; }
    public final Date getBuildDate() { return JSLBotBridgeBuildInfo.BUILDDATE; }

    JSLBot bot;

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
            final JSLBot reference = bot;
            bot = null;
            reference.shutdown("SL System is shutting down");
        }
    }
    public void initialise() {
        for (final Method method : ClassTools.getAnnotatedMethods(JSLBot.CmdHelp.class)) {
            boolean warned = false;
            boolean firstParam = true;
            for (final Parameter param : method.getParameters()) {
                if (firstParam) {
                    firstParam = false;
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

    @Override
    public void maintenanceInternal() {}

    public void startup() {
        //if (!Config.getDevelopment()) {
            waitBot();
        //}
    }

    private static BotConfig botconfig;
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
            bot.waitConnection(60000);
        } catch (@Nonnull final IllegalStateException ignored) {
        }
        if (!bot.connected()) {
            bot.shutdown("Failed to connect");
        }
    }

    @Override
    public void processEvent(final EventQueue event) {
        if ("im".equalsIgnoreCase(event.getCommandName())) {
            event.claim();
            bot.im(new LLUUID(event.getData().getString("uuid")), event.getData().getString("message"));
            event.complete();
        }
        if ("recalcnames".equalsIgnoreCase(event.getCommandName())) {
            event.claim();
            if (recalcThread != null && recalcThread.isAlive()) {
                event.complete("RecalcThreadAlive");
            } else {
                recalcThread = new RecalcThread(event);
                recalcThread.start();
            }
        }
    }

    @Override
    public Object weakInvoke(final String command, final Object... arguments) {
        if ("IM".equalsIgnoreCase(command)) {
            bot.im(new LLUUID((String) arguments[0]), (String) arguments[1]);
            return null;
        }
        if ("GROUPINVITE".equalsIgnoreCase(command)) {
            bot.api().groupInvite((String) arguments[0], (String) arguments[1], (String) arguments[2]);
            return null;
        }
        return super.weakInvoke(command, arguments);
    }

    @Override
    protected int schemaUpgrade(final DBConnection db, final String schemaName, final int currentVersion) {
        // this entire function is not called (see initialise())
        return currentVersion;
    }

    private static Thread recalcThread;


    private class RecalcThread extends Thread {
        private int caseCorrections;
        private int renames;
        private int unknowns;
        private int noop;

        public RecalcThread(final EventQueue event) {
            final Logger log = SL.log("AvatarNameRecalc");
            log.info("Starting!");
            int processed = 0;
            int lastReport = UnixTime.getUnixTime();
            final int startTime = lastReport;
            final Set<User> allUsers = User.getAllUsers();
            final List<String> searchFor = new ArrayList<>(10);
            int oddLength = 0;
            for (final User user : allUsers) {
                if (user.getUUID().length() == 36) {
                    searchFor.add(user.getUUID());
                } else {
                    oddLength++;
                }
                processed++;
                if ((processed % 10) == 0) {
                    doLookup(searchFor);
                    searchFor.clear();
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException ignore) {
                    }
                    if ((UnixTime.getUnixTime() - lastReport) > 60) {
                        lastReport = UnixTime.getUnixTime();
                        final double usersPerSecond = (((double) processed) / ((double) (lastReport - startTime)));
                        final double expectRunTime = ((double) (allUsers.size() - processed)) / usersPerSecond;
                        final int eta = (int) expectRunTime;
                        log.info("Processed " + processed + "/" + allUsers.size() + ".  ETA: " + UnixTime.duration(eta, true));
                    }
                }
            }
            if (!searchFor.isEmpty()) {
                doLookup(searchFor);
            }
            log.info("Exiting - processed " + processed + " in " + UnixTime.duration(UnixTime.getUnixTime() - startTime, true) + " with " + renames + " renames, " + caseCorrections + " case corrections, " + unknowns + " failed lookups," + oddLength + " odd lengths and " + noop + " NOOPs.");
            event.complete();
        }

        private void doLookup(final List<String> searchFor) {
            final Map<String, String> map = ((Avatars) (bot.brain().getHandler("Avatars"))).resolveUUIDStrings(searchFor);
            for (final Map.Entry<String, String> entry : map.entrySet()) {
                final String uuid = entry.getKey();
                final String username = entry.getValue();
                if (username == null) {
                    SL.log("AvatarNameRecalc").warning("Failed to get an avatar name for " + uuid);
                    unknowns++;
                } else {
                    final User user = User.findUserKey(uuid);
                    if (!user.getUsername().equals(username)) {
                        if (!user.getUsername().equalsIgnoreCase(username)) {
                            SL.log("AvatarNameRecalc").info("Avatar rename for " + uuid + " from " + user.getUsername() + " to " + username);
                            renames++;
                        } else {
                            caseCorrections++;
                        }
                        user.setUsername(username);
                    } else { noop++; }
                }
            }
        }
    }
}
