package info.skyblond.Blake.Belladonna;

/*
* TODO:
*   1. user's interface
*       2.1 onsuccess, show code
*   3. To code. Web interface to add "#!&$>" before code
*/


import io.javalin.Javalin;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final boolean isDebug = false; // turn to false when make final artifacts

    public static final String POST_TITLE_FIELD = "title";
    public static final String POST_CONTENT_FIELD = "content";

    public static void main(String[] args) {

        if(isDebug){
            Share.filePrefix = ".//"; //using class path when debug.
        }

        PropertiesUtils.getProperties();

        Javalin app = Javalin.create();

        app.get("/create", ctx -> {
            ctx.header("content-type","text/html; charset=UTF-8");
            ctx.result(String.join("\n", Files.readAllLines(PropertiesUtils.getProperties().getCreateTheme())));
        });

        app.get("/show/:checksum", ctx -> {
            Messages messages;
            if(true)
//            if(PropertiesUtils.getProperties().getDataMode() == 0)
                messages = Messages.findMessageFile(ctx.pathParam("checksum").toUpperCase());
            else
                messages = Messages.findMessageMysql(ctx.pathParam("checksum").toUpperCase());
            if(messages == null){
                ctx.status(410);
                return;
            }
            ctx.header("content-type","text/html; charset=UTF-8");
            ctx.result(String.join("\n", Files.readAllLines(PropertiesUtils.getProperties().getMessageTheme())).replace("{{% title %}}", messages.getTitle()).replace("{{% content %}}", messages.getContent()));
        });

        app.post("/new-message", ctx -> {
            File[] list = PropertiesUtils.getProperties().getDataDirectory().toFile().listFiles();
            if(list != null && list.length >= PropertiesUtils.getProperties().getMaxMessages()){
                ctx.status(500);
                ctx.result("Too many message files in data directory!");
                Share.logger.error("Too many message files in data directory!");
                return;
            }

            Map<String, List<String>> raw = ctx.formParamMap();
            if(raw.keySet().containsAll(Arrays.asList(POST_TITLE_FIELD, POST_CONTENT_FIELD))){
                Messages messages = new Messages();
                messages.setTitle(String.join("",raw.get(POST_TITLE_FIELD)));
                String[] body = String.join("",raw.get(POST_CONTENT_FIELD)).split("\n");
                StringBuilder sb = new StringBuilder();
                for(String s : body){
                    if(s.startsWith("#!&$>") && s.length() > 5){
                        sb.append(s.substring(5));
                    }else {
                        sb.append("<p>" + s + "</p>\n");
                    }
                }
                messages.setContent(sb.toString());

                String result;
                if(true){
//                if(PropertiesUtils.getProperties().getDataMode() == 0){
                    result = messages.storeToFile();
                }else{
                    result = messages.storeToMysql();
                }

                if(result != null)
                    ctx.result(result);
                else
                    ctx.status(500);
                ctx.status(201);
                ctx.header("content-type","text/html; charset=UTF-8");
                ctx.result(String.join("\n", Files.readAllLines(PropertiesUtils.getProperties().getSucceedTheme())).replace("{{% code %}}", result));
            }else {
                ctx.status(400);
                ctx.result("bad request");
            }
        });

        app.get("/*", ctx -> {
            ctx.redirect("https://github.com/hurui200320/Blake-Belladonna");
        });

        //period call gc and delete expired message.
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if(PropertiesUtils.getProperties().getMessageExpiredTime() != 0)
                        Files.walkFileTree(PropertiesUtils.getProperties().getDataDirectory(), new SimpleFileVisitor<Path>(){
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Objects.requireNonNull(file);
                                Messages messages;
                                try{
                                    messages = Share.gson.fromJson(String.join("",Files.readAllLines(file)), Messages.class);
                                }catch (Exception e){
                                    Files.delete(file);
                                    return FileVisitResult.TERMINATE;
                                }

                                if(messages.isExpired() && Files.exists(file))
                                    Files.delete(file);

                                return super.visitFile(file, attrs);
                            }
                        });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.gc();
                }

            }
        }, 0, 1, TimeUnit.HOURS);

        app.server(() -> {
            Server server = new Server();
            ServerConnector serverConnector = new ServerConnector(server);
            serverConnector.setHost(PropertiesUtils.getProperties().getIP());
            serverConnector.setPort(PropertiesUtils.getProperties().getPort());
            server.setConnectors(new Connector[]{serverConnector});
            return server;
        });

        app.start(PropertiesUtils.getProperties().getPort());
    }
}