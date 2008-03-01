package com.globalmentor.marmot.resource.audio;

import javax.mail.internet.ContentType;

import static com.globalmentor.io.ContentTypeConstants.*;

import com.globalmentor.io.ContentTypes;
import com.globalmentor.marmot.resource.*;

/**Resource kit for handling audio.
<p>Supported media types:</p>
<ul>
	<li><code>audio/mpeg</code></li>
</ul>
@author Garret Wilson
*/
public class AudioResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public AudioResourceKit()
	{
		super(new ContentType(ContentTypes.AUDIO_PRIMARY_TYPE, MPEG_SUBTYPE, null));
	}

}
