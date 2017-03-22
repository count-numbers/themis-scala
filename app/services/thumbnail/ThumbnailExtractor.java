package services.thumbnail;

import java.io.IOException;
import java.nio.file.Path;

/** Extracts a thumbnail from a given input file. The caller must ensure that the implementation
 *  is capable of handling the respective file type.
 *  
 */
public interface ThumbnailExtractor {
	
	/** Height and width of the thumbnail. */
	public static final int THUMBNAIL_SIZE = 200;
	
	/**
	 * 
	 * @param srcPath the source file
	 * @param thumbPath the file which should contain the thumbnail after execution.
	 * @throws IOException
	 */
	public void extractFromFile(Path srcPath, Path thumbPath) throws IOException;

	public String[] getCompatibleMimeTypes();
}
