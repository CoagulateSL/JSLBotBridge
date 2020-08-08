package net.coagulate.SL.JSLBotBridge;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.Packets.Types.LLUUID;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import net.coagulate.SL.SLModule;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class JSLBotBridge extends SLModule {

    public static final int MAJORVERSION=0;
    public static final int MINORVERSION=1;
    public static final int BUGFIXVERSION=7;
    public static final String COMMITID ="MANUAL";
    public final int majorVersion() { return MAJORVERSION; }
    public final int minorVersion() { return MINORVERSION; }
    public final int bugFixVersion() { return BUGFIXVERSION; }
    public final String commitId() { return COMMITID; }
    JSLBot bot=null;

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
                "CnC,Sink,Health,Regions,Teleportation,Agent,Objects,Groups" //todo ,net.coagulate.SL.Bots.BotInterface,net.coagulate.SL.Services.RegionMonitor"+".SimStats"
        );
        botconfig.put("loginlocation","home");
        botconfig.put("CnC.authorisation.owneruuid",Config.getBotOwnerUUID());
        botconfig.put("CnC.authorisation.ownerusername",Config.getBotOwnerUsername());
        botconfig.put("CnC.publiccommandprefix","*");
        botconfig.put("CnC.privatecommandprefix","*");
        botconfig.put("CnC.homesickfor",Config.getBotHomeRegion());
        botconfig.put("loginuri",Config.getJSLBotBridgeLoginURI());
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
    protected int schemaUpgrade(DBConnection db, String schemaname, int currentversion) {
        return currentversion;
    }
}
