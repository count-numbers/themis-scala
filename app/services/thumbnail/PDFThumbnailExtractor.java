package services.thumbnail;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFRenderer;

/**
 * Extracts thumbnail from the first page of the PDF.
 * 
 * This class has no instance methods and is therefore thread safe.
 */
public class PDFThumbnailExtractor implements ThumbnailExtractor {

	public static String[] COMPATIBLE_CONTENT_TYPES = { "application/pdf" };

	@Override
	public String[] getCompatibleMimeTypes() {
		return COMPATIBLE_CONTENT_TYPES;
	}

	public void extractFromFile(Path srcPath, Path thumbPath, int maxWidth, int maxHeight) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile (srcPath.toFile(), "r")) {
			FileChannel fc = raf.getChannel ();
			ByteBuffer buf = fc.map (FileChannel.MapMode.READ_ONLY, 0, fc.size ());
			PDFFile pdfFile = new PDFFile (buf);

			PDFPage page = pdfFile.getPage(0, true);
			try {
				page.waitForFinish();
			} catch (InterruptedException e) {
				// cannot happen
				throw new RuntimeException("Interrupted while waiting for PDF to load.", e);
			}

			double aspectRatio = page.getWidth() / page.getHeight();
			double imgWidth = maxWidth;
			double imgHeight = maxHeight;
			if (aspectRatio > 1) { // portrait
				imgHeight = imgWidth / aspectRatio;
			} else {
				imgWidth = imgHeight * aspectRatio;
			}

			BufferedImage img = new BufferedImage((int)Math.round(imgWidth), (int)Math.round(imgHeight), BufferedImage.TYPE_INT_RGB);
			
			Graphics2D g = (Graphics2D)img.getGraphics();
			g.setRenderingHints(ThumbnailUtils.HQ_RENDERING_HINTS);
			PDFRenderer renderer = new PDFRenderer(page, g, new Rectangle(img.getWidth(), img.getHeight()), null, Color.WHITE);
			renderer.run();
			ImageIO.write(img, "png", thumbPath.toFile());
		}
	}
}
