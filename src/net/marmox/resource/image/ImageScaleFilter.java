package net.marmox.resource.image;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.*;

import javax.imageio.ImageIO;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import static com.garretwilson.awt.geom.GeometryUtilities.*;
import static com.garretwilson.lang.ObjectUtilities.*;
import com.garretwilson.net.ResourceIOException;
import com.garretwilson.rdf.RDFResource;
import com.garretwilson.util.Debug;
import com.globalmentor.marmot.resource.ResourceFilter;
import com.globalmentor.marmot.resource.image.ImageResourceKit.ImageAspect;

/**A filter for scaling an image.
@author Garret Wilson
*/
public class ImageScaleFilter implements ResourceFilter
{

	/**The image aspect for this scale.*/
	private final ImageAspect imageAspect;

		/**@return The image aspect for this scale.*/
		public ImageAspect getImageAspect() {return imageAspect;}

	/**Image aspect constructor.
	@param imageAspect The aspect of the image.
	@exception NullPointerException if the given image aspect is <code>null</code>.
	*/
	public ImageScaleFilter(final ImageAspect imageAspect)
	{
		this.imageAspect=checkInstance(imageAspect, "Image aspect cannot be null.");
	}
		
	/**Performs a scaling operation on a resource.
	@param resource The description of the resource.
	@param inputStream The source of the resource content to filter.
	@param outputStream The destination of the filtered resource content.
	@return The new description of the filtered resource content.
	@exception ResourceIOException if there is an error filtering the resource.
	*/
	public RDFResource filter(final RDFResource resource, final InputStream inputStream, final OutputStream outputStream) throws IOException
	{
//TODO del Debug.trace("ready to scale to aspect", imageAspect);
		final Dimension aspectDimension;	//determine the aspect dimension
		switch(getImageAspect())	//see what image aspect is called for
		{
			case PREVIEW:
				aspectDimension=new Dimension(800, 600);	//TODO use constants
				break;
			case THUMBNAIL:
				aspectDimension=new Dimension(200, 200);	//TODO use constants
				break;
			default:
				throw new AssertionError("Unrecognized image aspect: "+getImageAspect());
		}

//TODO del Debug.trace("aspect dimensions:", scaledDimension);

		final BufferedImage bufferedImage=ImageIO.read(inputStream);	//read the image
		final Dimension originalDimension=new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight());	//find the original dimensions of the image
//TODO del Debug.trace("original image dimension", originalDimension);
		if(originalDimension.width>aspectDimension.width || originalDimension.height>aspectDimension.height)	//if this image needs scaled
		{
			final Dimension newDimension=constrain(originalDimension, aspectDimension);	//constrain the dimension to the scaled dimension
//TODO del Debug.trace("scaling to dimension", newDimension);
			final ParameterBlock parameterBlock=new ParameterBlock();
			parameterBlock.addSource(bufferedImage);
//TODO del Debug.trace("reduce X", newDimension.getWidth()/originalDimension.getWidth());
			parameterBlock.add((float)(newDimension.getWidth()/originalDimension.getWidth()));
//TODO del Debug.trace("reduce Y", newDimension.getHeight()/originalDimension.getHeight());
			parameterBlock.add((float)(newDimension.getHeight()/originalDimension.getHeight()));
			parameterBlock.add(0.0f);
			parameterBlock.add(0.0f);
//TODO del			parameterBlock.add(new InterpolationNearest());
			parameterBlock.add(new InterpolationBicubic2(1));
			final PlanarImage newImage=JAI.create("scale", parameterBlock);
			ImageIO.write(newImage, "JPEG", outputStream);	//write the image out as a JPEG TODO use a constant
		}
		else	//if the image doesn't need scaled
		{
			ImageIO.write(bufferedImage, "JPEG", outputStream);	//write the image out as a JPEG TODO use a constant
		}
/*TODO fix so that we can copy the data unchanged; we've already used up the input stream at this point, though
		else	//if the image doesn't need scaled
		{
Debug.trace("copy the data verbatim");
			copy(inputStream, outputStream);	//copy the input stream to the output stream unmodified
Debug.trace("done copying");
		}
*/
		return resource;
	}
}
