package services.thumbnail;

import java.io.IOException;
import java.nio.file.Path;

/** Extracts a thumbnail from a given input file. The caller must ensure that the implementation
 *  is capable of handling the respective file type.
 *  
 */
public interface ThumbnailExtractor {

	/**
	 * 
	 * @param srcPath the source file
	 * @param thumbPath the file which should contain the thumbnail after execution.
	 * @throws IOException
	 */
	void extractFromFile(Path srcPath, Path thumbPath, int maxWidth, int maxHeight) throws IOException;

	String[] getCompatibleMimeTypes();
}
