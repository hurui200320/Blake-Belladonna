package info.skyblond.Blake.Belladonna;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Share {
    public static final Logger logger = LoggerFactory.getLogger("Blake Belladonna");
    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static Messages findMessageFile(String name) throws IOException {
        List<Messages> result = Collections.synchronizedList(new LinkedList<>());
        result.clear();
        Files.walkFileTree(PropertiesUtils.getProperties().getDataDirectory(), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(file);
                if(file.getFileName().toString().toUpperCase().equals(name.toUpperCase())){
                    String read = String.join("",Files.readAllLines(file));
                    Messages messages;
                    try{
                        messages = Share.gson.fromJson(read, Messages.class);
                    }catch (Exception e){
                        System.out.println(read);
                        e.printStackTrace();
                        Files.delete(file);
                        return FileVisitResult.TERMINATE;
                    }
                    result.add(messages);
                    Files.delete(file);
                    return FileVisitResult.TERMINATE;
                }
                return super.visitFile(file, attrs);
            }
        });
        if(result.size() == 0)
            return null;
        return result.get(0);
    }

    public static Messages findMessageMysql(String name){
        // TODO
        return null;
    }
}
