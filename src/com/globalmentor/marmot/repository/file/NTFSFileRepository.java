package com.globalmentor.marmot.repository.file;

import java.io.*;

import static com.garretwilson.io.FileConstants.*;
import static com.garretwilson.io.FileUtilities.*;

/**Repository stored in an NTFS filesystem.
This implementation stores resource descriptions in NTFS Alternate Data Streams (ADS).
@author Garret Wilson
@see <a href="http://support.microsoft.com/kb/105763">How To Use NTFS Alternate Data Streams</a>
*/
public class NTFSFileRepository extends FileRepository
{

	/**Determines the file that holds the description of the given resource file.
	This version uses an NTFS Alternate Data Stream of {@value #MARMOT_DESCRIPTION_NAME} in the resource file.
	@param resourceFile The file of a resource.
	@return A new file designating the location of the resource description.
	*/
	protected File getResourceDescriptionFile(final File resourceFile)
	{
		return changeName(resourceFile, resourceFile.getName()+NTFS_ADS_DELIMITER+MARMOT_DESCRIPTION_NAME);	//return a file in the form "file.ext:marmot-description"
	}

}
