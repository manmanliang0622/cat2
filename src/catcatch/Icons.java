package catcatch;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Icons {

    private static final Map<String, Image> cache = new HashMap<>();

    public static ImageView get(String name, double size) {
        Image img = cache.computeIfAbsent(name, k -> {
            File f = new File("assets/icons/" + k + ".png");
            if (!f.exists()) return null;
            try (FileInputStream fis = new FileInputStream(f)) {
                Image i = new Image(fis);
                return i.isError() ? null : i;
            } catch (Exception e) {
                return null;
            }
        });

        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        if (img != null) iv.setImage(img);
        return iv;
    }
}
