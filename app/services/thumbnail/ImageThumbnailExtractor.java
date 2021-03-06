package services.thumbnail;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Extracts thumbnails from plain images.
 * 
 * @author Simon
 *
 */
public class ImageThumbnailExtractor implements ThumbnailExtractor {

	public static String[] COMPATIBLE_CONTENT_TYPES = { "image/png",
			"image/jpg", "image/jpeg", "image/" };

	@Override
	public String[] getCompatibleMimeTypes() {
		return COMPATIBLE_CONTENT_TYPES;
	}

	@Override
	public void extractFromFile(Path srcPath, Path thumbPath, int maxWidth, int maxHeight) throws IOException {
		BufferedImage srcImage = ImageIO.read(srcPath.toFile());

		double aspectRatio = (double)srcImage.getWidth() / (double)srcImage.getHeight();
		double imgWidth = maxWidth;
		double imgHeight = maxHeight;
		if (aspectRatio > 1) { // portrait
			imgHeight = imgWidth / aspectRatio;
		} else {
			imgWidth = imgHeight * aspectRatio;
		}
		double scale = imgWidth / (double)srcImage.getWidth();

		BufferedImage result = new BufferedImage((int)Math.round(imgWidth), (int)Math.round(imgHeight), BufferedImage.TYPE_INT_ARGB);
//		AffineTransform at = new AffineTransform();
//		at.scale(scale, scale);
//		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);

		AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
		result.createGraphics().drawRenderedImage(srcImage, at);
//		result = scaleOp.filter(srcImage, result);
		
		ImageIO.write(result, "png", thumbPath.toFile());
	}
}
