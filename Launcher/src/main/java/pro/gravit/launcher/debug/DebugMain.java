package pro.gravit.launcher.debug;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.ClientLauncherCoreModule;
import pro.gravit.launcher.client.ClientModuleManager;
import pro.gravit.launcher.managers.ConsoleManager;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.utils.helper.LogHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebugMain {
    public static final AtomicBoolean IS_DEBUG = new AtomicBoolean(false);
    public static String webSocketURL = System.getProperty("launcherdebug.websocket", "ws://localhost:9274/api");
    public static String projectName = System.getProperty("launcherdebug.projectname", "Minecraft");
    public static String[] moduleClasses = System.getProperty("launcherdebug.modules", "").split(",");
    public static String[] moduleFiles = System.getProperty("launcherdebug.modulefiles", "").split(",");
    public static LauncherConfig.LauncherEnvironment environment = LauncherConfig.LauncherEnvironment.valueOf(System.getProperty("launcherdebug.env", "STD"));

    public static void main(String[] args) throws Throwable {
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        IS_DEBUG.set(true);
        LogHelper.info("Launcher start in DEBUG mode (Only for developers)");
        LogHelper.debug("Initialization LauncherConfig");
        LauncherConfig config = new LauncherConfig(webSocketURL, new HashMap<>(), projectName, environment, new DebugLauncherTrustManager(DebugLauncherTrustManager.TrustDebugMode.TRUST_ALL));
        Launcher.setConfig(config);
        Launcher.applyLauncherEnv(environment);
        LauncherEngine.modulesManager = new ClientModuleManager();
        LauncherEngine.modulesManager.loadModule(new ClientLauncherCoreModule());
        for (String moduleClassName : moduleClasses) {
            if (moduleClassName.isEmpty()) continue;
            LauncherEngine.modulesManager.loadModule(newModule(moduleClassName));
        }
        for (String moduleFileName : moduleFiles) {
            if (moduleFileName.isEmpty()) continue;
            LauncherEngine.modulesManager.loadModule(Paths.get(moduleFileName));
        }
        LauncherEngine.modulesManager.initModules(null);
        LauncherEngine.initGson(LauncherEngine.modulesManager);
        ConsoleManager.initConsole();
        LauncherEngine.modulesManager.invokeEvent(new PreConfigPhase());
        LogHelper.debug("Initialization LauncherEngine");
        LauncherEngine instance = LauncherEngine.newInstance(false);
        instance.start(args);
    }

    @SuppressWarnings("unchecked")
    public static LauncherModule newModule(String className) throws ClassNotFoundException, InvocationTargetException {
        Class<? extends LauncherModule> clazz = (Class<? extends LauncherModule>) Class.forName(className);
        try {
            return (LauncherModule) MethodHandles.publicLookup().findConstructor(clazz, MethodType.methodType(void.class)).invoke();
        } catch (Throwable throwable) {
            throw new InvocationTargetException(throwable);
        }
    }
}
