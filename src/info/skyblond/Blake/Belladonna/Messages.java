package info.skyblond.Blake.Belladonna;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.CRC32;

public class Messages {
    private String title = "", content = "";
    private Timestamp sendTime = new Timestamp(System.currentTimeMillis());

    public boolean isExpired(){
        if(PropertiesUtils.getProperties().getMessageExpiredTime() == 0)
            return false;
        if(System.currentTimeMillis() - this.sendTime.getTime() >= PropertiesUtils.getProperties().getMessageExpiredTime()*1000)
            return true;
        return false;
    }

    public String getTitle() {
        return new String(Base64.getDecoder().decode(title.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public void setTitle(String title) {
        this.title = Base64.getEncoder().encodeToString(title.trim().getBytes(StandardCharsets.UTF_8));
    }

    public String getContent() {
        return new String(Base64.getDecoder().decode(content.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public void setContent(String content) {
        this.content = Base64.getEncoder().encodeToString(content.trim().getBytes(StandardCharsets.UTF_8));
    }

    public Timestamp getSendTime() {
        return sendTime;
    }

    public String storeToFile(){
        String json = Share.gson.toJson(this);
        String name;
        try {
            if (Files.notExists(PropertiesUtils.getProperties().getDataDirectory()))
                Files.createDirectories(PropertiesUtils.getProperties().getDataDirectory());

            CRC32 crc32 = new CRC32();
            crc32.update(json.getBytes(StandardCharsets.UTF_8));
            name = Long.toHexString(crc32.getValue()) + Long.toHexString(new Random().nextLong());

            if(Files.exists(Paths.get(PropertiesUtils.getProperties().getDataDirectory() + "/" + name.toUpperCase()))){
                Share.logger.error("File already exists: " + name.toUpperCase());
                return null;
            }

            Writer writer = Files.newBufferedWriter(Paths.get(
                    PropertiesUtils.getProperties().getDataDirectory() + "/" + name.toUpperCase()));
            writer.write(json);
            writer.close();

        }catch (IOException e){
            e.printStackTrace();
            Share.logger.error("Failed to store message: " + json);
            return null;
        }
        return name;
    }

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
                        messages = null;
                    }finally {
                        if(Files.exists(file))
                            Files.delete(file);
                    }
                    if(messages != null)
                        result.add(messages);
                    return FileVisitResult.TERMINATE;
                }
                return super.visitFile(file, attrs);
            }
        });
        if(result.size() == 0)
            return null;
        return result.get(0);
    }

    public String storeToMysql(){
        //TODO
        return null;
    }

    public static Messages findMessageMysql(String name){
        // TODO
        return null;
    }

    @Override
    public String toString() {
        return "Messages{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", sendTime=" + sendTime +
                '}';
    }
}
