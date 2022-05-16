package base;

import org.apache.log4j.Logger;

public class Log {
    private static final Logger Log = Logger.getLogger(Log.class.getName());

    public static void startLog (String testClassName){
        Log.info(testClassName + " Tests are Starting...");
    }

    public static void endLog (String testClassName){
        Log.info(testClassName + " Tests are Ending...");
    }

    public static void info (String message) {
        Log.info("----- " + message + " -----");
    }

    public static void warn (String message) {
        Log.warn(message);
    }

    public static void error (Exception e, String message) {
        info(e.getMessage());
    }

    public static void fatal (String message) {
        Log.fatal(message);
    }

    public static void debug (String message) {
        Log.debug(message);
    }
}

