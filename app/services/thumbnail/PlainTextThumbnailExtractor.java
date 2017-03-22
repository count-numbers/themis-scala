package services.thumbnail;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Creates a thumbnail from plain text by writing tiny lines on a canvas.
 *
 */
public class PlainTextThumbnailExtractor implements ThumbnailExtractor {

	public static String[] COMPATIBLE_CONTENT_TYPES = { "text/plain", "text/xml", "application/xml", "text/" };

	@Override
	public void extractFromFile(Path srcPath, Path thumbPath) throws IOException {
		BufferedImage img = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHints(ThumbnailUtils.HQ_RENDERING_HINTS);
		// fill with white
		g.setColor(Color.WHITE);
		g.fillRect(0,  0, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
		
		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
		g.setFont(font);
		g.setColor(Color.DARK_GRAY);
		
		try (BufferedReader reader = new BufferedReader(new FileReader(srcPath.toFile()))) {
			int i = 0;
			String line;
			while (((line = reader.readLine()) != null) && i < 10) {
				g.drawString(line, 10, (i+2)*12);
				i++;
			}
			ImageIO.write(img, "png", thumbPath.toFile());
		}
	}

	@Override
	public String[] getCompatibleMimeTypes() {
		return COMPATIBLE_CONTENT_TYPES;
	}
}
