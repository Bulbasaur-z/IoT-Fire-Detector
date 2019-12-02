package LogInfo;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class AppLog {
    private static final Logger logger =Logger.getLogger("AppLog");
    private static FileHandler fileHandler = null;

    public AppLog()
    {
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
    }
    public void StartLog(){
        setHandler();
    }

    public void CloseLog(){
        fileHandler.close();
    }

    public static Logger getLogger(){
        return logger;
    }

    private void setHandler(){
        String strDir = System.getProperty("user.dir") + "/log";
        DateFormat df = new SimpleDateFormat("dd_mm_yyyy_hh_mm_ss");
        File dir = new File(strDir);
        if(!dir.exists() || !dir.isDirectory()){
            dir.mkdir();
        }
        String FilePath = strDir + "/AppMasterServer_Run_Log_%u%g_" + ".txt";
        try {
            fileHandler = new FileHandler(FilePath, 20000, 2, false);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new FileLogFormat());
            logger.addHandler(fileHandler);
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}

class FileLogFormat extends Formatter{
    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    @Override
    public String getHead(Handler h){
        return super.getHead(h);
    }

    @Override
    public String getTail(Handler h){
        return super.getTail(h);
    }

    @Override
    public String format(LogRecord logRecord){
        StringBuilder builder = new StringBuilder(1000);
        builder.append("\n");
        builder.append(df.format(new Date(logRecord.getMillis()))).append("-");
        builder.append("[").append(logRecord.getThreadID()).append("]-");
        builder.append("[").append(logRecord.getSourceClassName()).append(".");
        builder.append(logRecord.getSourceMethodName()).append("]-");
        builder.append("[").append(logRecord.getLevel()).append("]-");
        builder.append(formatMessage(logRecord));
        Throwable err = logRecord.getThrown();
        if(err != null){
            builder.append("-").append(err);
        }
        builder.append("\n");
        return builder.toString();
    }
}