package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Origin Code by SLF4J-Simple [Link=https://github.com/qos-ch/slf4j/tree/master/slf4j-simple]
 * Modified by NeutronStars
 */

public class NBotLoggerFactory implements ILoggerFactory
{
    private final ConcurrentMap<String, NBotLogger> loggerMap = new ConcurrentHashMap<>();
    private final List<String> logList = new ArrayList<>();

    public NBotLogger getLogger(String name)
    {
        NBotLogger logger = loggerMap.get(name);
        if (logger != null) {
            return logger;
        } else {
            NBotLogger newInstance = new NBotLogger(name, this);
            NBotLogger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

    protected void log(String log)
    {
        logList.add(log);
    }

    public void save() throws IOException
    {
        File folder = new File("logs");
        if(!folder.exists()) folder.mkdir();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("logs/"+NBotLogger.simpleDate.format(new Date()).replace(":", "-")+".log"), "UTF-8"));

        for(String str : logList) {
            writer.write(str);
            writer.newLine();
        }

        writer.flush();
    }
}
