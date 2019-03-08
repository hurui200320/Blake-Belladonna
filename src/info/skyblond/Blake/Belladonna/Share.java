package info.skyblond.Blake.Belladonna;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Share {
    public static final Logger logger = LoggerFactory.getLogger("Blake Belladonna");
    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    public static String filePrefix = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().toString() + "//";

}
