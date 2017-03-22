package services.thumbnail;

import java.awt.RenderingHints;
import java.util.HashMap;

public class ThumbnailUtils {

	/** For the convenience of {@link ThumbnailExtractor}s. */
	public static final RenderingHints HQ_RENDERING_HINTS = new RenderingHints(new HashMap<>());
	static {
		HQ_RENDERING_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		HQ_RENDERING_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		HQ_RENDERING_HINTS.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}
}
