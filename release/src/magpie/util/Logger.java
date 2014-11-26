package magpie.util;

import magpie.Activator;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class Logger {
	static String PluginID = "Magpie";
	static ILog logfile = null;

	public static void info(String msg) {
		log(msg, null, Status.INFO);
	}
	public static void error(String msg) {
		log(msg, null, Status.ERROR);
	}

	public static void log(String msg, Exception e, int status) {
		if (logfile == null) {
			Bundle bundle = Platform.getBundle(PluginID);
			logfile = Platform.getLog(bundle);
		}
		logfile.log(new Status(Status.INFO, PluginID, status, msg, e));
	}
}
