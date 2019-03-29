package info.skyblond.Blake.Belladonna;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesUtils {
    public static final String DEFAULT_PATH = Share.filePrefix + ".//belladonna.properties";

    private static final Properties properties = new Properties();

    private PropertiesUtils(){}

    public static Property getProperties(){
        if(Property.property == null)
            readProperties(Paths.get(DEFAULT_PATH));
        return Property.property;
    }

    public static synchronized void readProperties(Path path){
        if(Files.isDirectory(path)){
            throw new RuntimeException("Cannot load properties file. \"belladonna.properties\" is a directory.");
        }

        boolean needUpdate = false;
        //set to true if some rows are not right and needed write to file.
        int tmpInt;
        String tmpString;

        if(Files.notExists(path))
            try {
                Share.logger.info("Properties file non exists.");
                Files.createFile(path);
                Share.logger.info("Created.");
            } catch (IOException e) {
                Share.logger.error("Failed to create properties file.");
                throw new RuntimeException(e);
            }

        Property.property = new Property();

        try {
            InputStream inputStream = Files.newInputStream(path);
            properties.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            Share.logger.error("Failed to load properties file.");
            throw new RuntimeException(e);
        }

        try {
            tmpInt = Integer.parseInt(properties.getProperty(Property.PORT_FIELD));
            Property.property.setPort(tmpInt);
        }catch (NumberFormatException e){
            Share.logger.error("Invalidate " + Property.PORT_FIELD + ". Using default: " + Property.property.getPort());
            needUpdate = true;
        }

//        try {
//            tmpInt = Integer.parseInt(properties.getProperty(Property.DATA_MODE_FIELD));
//            Property.property.setDataMode(tmpInt);
//        }catch (NumberFormatException e){
//            Share.logger.error("Invalidate " + Property.DATA_MODE_FIELD + ". Using default: " + Property.property.getDataMode());
//            needUpdate = true;
//        }

        if(true){
//        if(Property.property.getDataMode() == 0){ // file
            Share.logger.info("Using file mode.");
            tmpString = properties.getProperty(Property.DATA_DIRECTORY_FIELD);
            if(tmpString == null || tmpString.trim().equals("")){
                Share.logger.warn("Empty " + Property.DATA_DIRECTORY_FIELD + ". Using default: " + Property.property.getDataDirectory());
                needUpdate = true;
            }else {
                tmpString = Share.filePrefix + tmpString;
                Property.property.setDataDirectory(Paths.get(tmpString));
            }

            if (Files.notExists(PropertiesUtils.getProperties().getDataDirectory())) {
                try {
                    Files.createDirectories(PropertiesUtils.getProperties().getDataDirectory());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                Property.property.setMaxMessage(Long.parseLong(properties.getProperty(Property.MAX_MESSAGES_FIELD)));
            }catch (NumberFormatException e){
                Share.logger.error("Invalidate " + Property.MAX_MESSAGES_FIELD + ". Using default: " + Property.property.getMaxMessages());
                needUpdate = true;
            }


        }// else { // mysql
//            Share.logger.info("Using MySQL mode.");
//            tmpString = properties.getProperty(Property.MYSQL_ADDR_FIELD);
//            if(tmpString == null || tmpString.trim().equals("")){
//                throw new RuntimeException("Empty " + Property.MYSQL_ADDR_FIELD + ".");
//            }else {
//                Property.property.setMysqlAddr(tmpString);
//            }
//
//            try {
//                tmpInt = Integer.parseInt(properties.getProperty(Property.MYSQL_PORT_FIELD));
//                Property.property.setMysqlPort(tmpInt);
//            }catch (NumberFormatException e){
//                Share.logger.error("Invalidate " + Property.MYSQL_PORT_FIELD);
//                needUpdate = true;
//            }
//
//            tmpString = properties.getProperty(Property.MYSQL_DATABASE_NAME_FIELD);
//            if(tmpString == null || tmpString.trim().equals("")){
//                throw new RuntimeException("Empty " + Property.MYSQL_ADDR_FIELD + ".");
//            }else {
//                Property.property.setMysqlDatabaseName(tmpString);
//            }
//
//            tmpString = properties.getProperty(Property.MYSQL_USERNAME_FIELD);
//            if(tmpString == null || tmpString.trim().equals("")){
//                throw new RuntimeException("Empty " + Property.MYSQL_USERNAME_FIELD + ".");
//            }else {
//                Property.property.setMysqlUsername(tmpString);
//            }
//
//            tmpString = properties.getProperty(Property.MYSQL_PASSWORD_FIELD);
//            if(tmpString == null || tmpString.trim().equals("")){
//                throw new RuntimeException("Empty " + Property.MYSQL_PASSWORD_FIELD + ".");
//            }else {
//                Property.property.setMysqlPassword(tmpString);
//            }
//
//        }

        tmpString = properties.getProperty(Property.IP_FIELD);
        if(tmpString == null || tmpString.trim().equals("")){
            Share.logger.warn("Empty " + Property.IP_FIELD + ". Using default: " + Property.property.getIP());
            needUpdate = true;
        }else {
            Property.property.setIP(tmpString);
        }

        tmpString = properties.getProperty(Property.MESSAGE_THEME_FIELD);
        if(tmpString == null || tmpString.trim().equals("")){
            Share.logger.warn("Empty " + Property.MESSAGE_THEME_FIELD + ". Using default: " + Property.property.getMessageTheme());
            needUpdate = true;
        }else {
            tmpString = Share.filePrefix + tmpString;
            Property.property.setMessageTheme(Paths.get(tmpString));
        }

        tmpString = properties.getProperty(Property.CREATE_THEME_FIELD);
        if(tmpString == null || tmpString.trim().equals("")){
            Share.logger.warn("Empty " + Property.CREATE_THEME_FIELD + ". Using default: " + Property.property.getCreateTheme());
            needUpdate = true;
        }else {
            tmpString = Share.filePrefix + tmpString;
            Property.property.setCreateTheme(Paths.get(tmpString));
        }

        tmpString = properties.getProperty(Property.SUCCEED_THEME_FIELD);
        if(tmpString == null || tmpString.trim().equals("")){
            Share.logger.warn("Empty " + Property.SUCCEED_THEME_FIELD + ". Using default: " + Property.property.getSucceedTheme());
            needUpdate = true;
        }else {
            tmpString = Share.filePrefix + tmpString;
            Property.property.setSucceedTheme(Paths.get(tmpString));
        }

        try {
            Property.property.setMessageExpiredTime(Long.parseLong(properties.getProperty(Property.MESSAGE_EXPIRED_TIME_FIELD)));
        }catch (NumberFormatException e){
            Share.logger.error("Invalidate " + Property.MESSAGE_EXPIRED_TIME_FIELD + ". Using default: " + Property.property.getMessageExpiredTime());
            needUpdate = true;
        }

        if(needUpdate)
            writeProperties(path);

    }

    public static synchronized void writeProperties(Path path){
        if(Files.isDirectory(path)){
            throw new RuntimeException("Cannot write properties file. \"belladonna.properties\" is a directory.");
        }

        properties.clear();
        properties.setProperty(Property.PORT_FIELD, Property.property.getPort() + "");
        properties.setProperty(Property.IP_FIELD, Property.property.getIP() + "");
//        properties.setProperty(Property.DATA_MODE_FIELD, Property.property.getDataMode() + "");
        properties.setProperty(Property.DATA_DIRECTORY_FIELD, Property.property.getDataDirectory() + "");
        properties.setProperty(Property.MAX_MESSAGES_FIELD, Property.property.getMaxMessages() + "");
//        properties.setProperty(Property.MYSQL_ADDR_FIELD, Property.property.getMysqlAddr() + "");
//        properties.setProperty(Property.MYSQL_PORT_FIELD, Property.property.getMysqlPort() + "");
//        properties.setProperty(Property.MYSQL_DATABASE_NAME_FIELD, Property.property.getMysqlDatabaseName() + "");
//        properties.setProperty(Property.MYSQL_USERNAME_FIELD, Property.property.getMysqlUsername() + "");
//        properties.setProperty(Property.MYSQL_PASSWORD_FIELD, Property.property.getMysqlPassword() + "");
        properties.setProperty(Property.MESSAGE_THEME_FIELD, Property.property.getMessageTheme() + "");
        properties.setProperty(Property.CREATE_THEME_FIELD, Property.property.getCreateTheme() + "");
        properties.setProperty(Property.SUCCEED_THEME_FIELD, Property.property.getSucceedTheme() + "");
        properties.setProperty(Property.MESSAGE_EXPIRED_TIME_FIELD, Property.property.getMessageExpiredTime() + "");

        try {
            OutputStream outputStream = Files.newOutputStream(path);
            properties.store(outputStream, "Blake Belladonna");
            outputStream.close();

        } catch (IOException e) {
            Share.logger.error("Failed to write properties file.");
            throw new RuntimeException(e);
        }
    }

}

class Property{
    public static Property property = null;
    /*
        port           : user and admin
        data store mode : file(0) or mysql(1, non zero value)
            mysql address
            mysql port
            mysql database name
            mysql username
            mysql password

            message directory(file store)

        message theme

        message expired(in second, 0 for no expired)

    */

    private int port = 7000;
    public static final String PORT_FIELD = "port";
    private String ip = "0.0.0.0";
    public static final String IP_FIELD = "ip";

//    private int dataMode = 0;
//    public static final String DATA_MODE_FIELD = "date_mode";
//    private String mysqlAddr = "";
//    public static final String MYSQL_ADDR_FIELD = "mysql_addr";
//    private int mysqlPort = 3306;
//    public static final String MYSQL_PORT_FIELD = "mysql_port";
//    private String mysqlDatabaseName = "";
//    public static final String MYSQL_DATABASE_NAME_FIELD = "mysql_database_name";
//    private String mysqlUsername = "";
//    public static final String MYSQL_USERNAME_FIELD = "mysql_username";
//    private String mysqlPassword = "";
//    public static final String MYSQL_PASSWORD_FIELD = "mysql_password";

    private Path dataDirectory = Paths.get("messages");
    public static final String DATA_DIRECTORY_FIELD = "data_directory";

    private long maxMessage = 100000;
    public static final String MAX_MESSAGES_FIELD = "max_messages";

    private Path messageTheme = Paths.get("message.html");
    public static final String MESSAGE_THEME_FIELD = "message_theme";

    private Path createTheme = Paths.get("create.html");
    public static final String CREATE_THEME_FIELD = "create_theme";

    private Path succeedTheme = Paths.get("createSucceed.html");
    public static final String SUCCEED_THEME_FIELD = "succeed_theme";

    private long messageExpiredTime = 7*24*60*60L; //7days, in second
    public static final String MESSAGE_EXPIRED_TIME_FIELD = "message_expired_time";

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIP() {
        return ip;
    }

    public void setIP(String ip) {
        this.ip = ip;
    }

    //    public int getDataMode() {
//        return dataMode;
//    }
//
//    void setDataMode(int dataMode) {
//        this.dataMode = dataMode;
//    }
//
//    public String getMysqlAddr() {
//        return mysqlAddr;
//    }
//
//    void setMysqlAddr(String mysqlAddr) {
//        this.mysqlAddr = mysqlAddr;
//    }
//
//    public int getMysqlPort() {
//        return mysqlPort;
//    }
//
//    void setMysqlPort(int mysqlPort) {
//        this.mysqlPort = mysqlPort;
//    }
//
//    public String getMysqlDatabaseName() {
//        return mysqlDatabaseName;
//    }
//
//    void setMysqlDatabaseName(String mysqlDatabaseName) {
//        this.mysqlDatabaseName = mysqlDatabaseName;
//    }
//
//    public String getMysqlUsername() {
//        return mysqlUsername;
//    }
//
//    void setMysqlUsername(String mysqlUsername) {
//        this.mysqlUsername = mysqlUsername;
//    }
//
//    public String getMysqlPassword() {
//        return mysqlPassword;
//    }
//
//    void setMysqlPassword(String mysqlPassword) {
//        this.mysqlPassword = mysqlPassword;
//    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    void setDataDirectory(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public long getMaxMessages() {
        return maxMessage;
    }

    public void setMaxMessage(long maxMessage) {
        this.maxMessage = maxMessage;
    }

    public long getMessageExpiredTime() {
        return messageExpiredTime;
    }

    void setMessageExpiredTime(long messageExpiredTime) {
        this.messageExpiredTime = messageExpiredTime;
    }

    public Path getMessageTheme() {
        return messageTheme;
    }

    public void setMessageTheme(Path messageTheme) {
        this.messageTheme = messageTheme;
    }

    public Path getCreateTheme() {
        return createTheme;
    }

    public void setCreateTheme(Path createTheme) {
        this.createTheme = createTheme;
    }

    public Path getSucceedTheme() {
        return succeedTheme;
    }

    public void setSucceedTheme(Path succeedTheme) {
        this.succeedTheme = succeedTheme;
    }

    @Override
    public String toString() {
        return "Property{" +
                "port=" + port +
//                ", dataMode=" + dataMode +
//                ", mysqlAddr='" + mysqlAddr + '\'' +
//                ", mysqlPort=" + mysqlPort +
//                ", mysqlDatabaseName='" + mysqlDatabaseName + '\'' +
//                ", mysqlUsername='" + mysqlUsername + '\'' +
//                ", mysqlPassword='" + mysqlPassword + '\'' +
                ", dataDirectory=" + dataDirectory +
                ", messageTheme=" + messageTheme +
                ", createTheme=" + createTheme +
                ", messageExpiredTime=" + messageExpiredTime +
                '}';
    }
}
