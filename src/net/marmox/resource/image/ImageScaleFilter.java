package net.marmox.resource.image;

/*TODO del ImageJ
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
*/

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.*;
import java.net.URI;

import javax.imageio.ImageIO;
import javax.media.jai.*;

import static com.globalmentor.java.Objects.*;

import static com.globalmentor.io.Files.*;

import com.globalmentor.marmot.resource.ResourceFileContentFilter;
import com.globalmentor.marmot.resource.ResourceContentFilter;
import com.globalmentor.marmot.resource.image.ImageResourceKit.ImageAspect;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.urf.URFResource;
import com.globalmentor.util.Debug;

import com.guiseframework.geometry.*;

/**A filter for scaling an image.
@author Garret Wilson
*/
public class ImageScaleFilter implements ResourceContentFilter
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

	/**The dimensions of the preview aspect.*/
	public final static Dimensions PREVIEW_DIMENSIONS=new Dimensions(800, 600, Unit.PIXEL);

	/**The dimensions of the thumbnail aspect.*/
	public final static Dimensions THUMBNAIL_DIMENSIONS=new Dimensions(200, 600, Unit.PIXEL);

	/**Performs a scaling operation on a resource.
	@param resource The description of the resource.
	@param inputStream The source of the resource content to filter.
	@param outputStream The destination of the filtered resource content.
	@return The new description of the filtered resource content.
	@exception ResourceIOException if there is an error filtering the resource.
	*/
	public URFResource filter(final URFResource resource, final InputStream inputStream, final OutputStream outputStream) throws ResourceIOException	//TODO review http://archives.java.sun.com/cgi-bin/wa?A2=ind0311&L=jai-interest&F=&S=&P=15036 and http://www.leptonica.com/scaling.html
	{
//TODO del Debug.trace("ready to scale to aspect", imageAspect);
		final Dimensions aspectDimensions;	//determine the aspect dimensions
		switch(getImageAspect())	//see what image aspect is called for
		{
			case PREVIEW:
				aspectDimensions=PREVIEW_DIMENSIONS;
				break;
			case THUMBNAIL:
				aspectDimensions=THUMBNAIL_DIMENSIONS;
				break;
			default:
				throw new AssertionError("Unrecognized image aspect: "+getImageAspect());
		}

//TODO del Debug.trace("aspect dimensions:", scaledDimension);
		try
		{
			final BufferedImage bufferedImage=ImageIO.read(inputStream);	//read the image
			final int originalWidth=bufferedImage.getWidth();
			final int originalHeight=bufferedImage.getHeight();
	//TODO del Debug.trace("original image dimension", originalDimension);
				//the multi-resizing technique described at http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html produces many black lines for normal JAI scaling
			if(originalWidth>aspectDimensions.getWidth().getValue() || originalHeight>aspectDimensions.getHeight().getValue())	//if this image needs scaled
			{
				final Dimensions originalDimensions=new Dimensions(originalWidth, originalHeight, Unit.PIXEL);	//find the original dimensions of the image
				final Dimensions newDimensions=originalDimensions.constrain(aspectDimensions);	//constrain the dimension to the scaled dimension
	
				final int newWidth=(int)newDimensions.getWidth().getValue();
				final int newHeight=(int)newDimensions.getHeight().getValue();
/*JAI method
	//TODO del Debug.trace("scaling to dimension", newDimension);
				final ParameterBlock parameterBlock=new ParameterBlock();
				parameterBlock.addSource(bufferedImage);
	
					//TODO use for subsample average
			parameterBlock.add(newDimensions.getWidth().getValue()/originalDimensions.getWidth().getValue());
			parameterBlock.add(newDimensions.getHeight().getValue()/originalDimensions.getHeight().getValue());
					//TODO use for scale
//TODO use for scale				parameterBlock.add((float)(newDimensions.getWidth().getValue()/originalDimensions.getWidth().getValue()));
//TODO use for scale				parameterBlock.add((float)(newDimensions.getHeight().getValue()/originalDimensions.getHeight().getValue()));
				parameterBlock.add(0.0f);
				parameterBlock.add(0.0f);
	//TODO del			parameterBlock.add(new InterpolationNearest());
	//TODO del			parameterBlock.add(new InterpolationBicubic2(1));
				parameterBlock.add(Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2));
				
				final RenderingHints renderingHints=new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);	//add a preference for quality over speed TODO use a constant
					//TODO find fix for SubsampleAverage black line on bottom
				final PlanarImage newImage=JAI.create("SubsampleAverage", parameterBlock, renderingHints);	//according to http://www.i-proving.ca/space/Technologies/Java+Advanced+Imaging subsample average produces better quality than "scale", and this seems to be true 
//TODO lower quality scaling				final PlanarImage newImage=JAI.create("scale", parameterBlock, renderingHints);
*/
	
	/*alternate Java2D method from http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html that is of average quality but perhaps slower
				final int type=(bufferedImage.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
				final int newWidth=(int)newDimensions.getWidth().getValue();
				final int newHeight=(int)newDimensions.getHeight().getValue();
				BufferedImage newImage = new BufferedImage(newWidth, newHeight, type);
	      final Graphics2D g2 = newImage.createGraphics();
	      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	      g2.drawImage(bufferedImage, 0, 0, newWidth, newHeight, null);
	      g2.dispose();
	*/

//this technique, modified from http://www.hanhuy.com/pfn/java-image-thumbnail-comparison , produces images virtually identical to JAI subsample average but is really slow---but leaves no black lines like the current JAI
        final Image scaledImage = bufferedImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
				final int type=(bufferedImage.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        final BufferedImage newImage = new BufferedImage(newWidth, newHeight, type);
        Graphics2D graphics = newImage.createGraphics();
	      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(scaledImage, null, null);
        graphics.dispose();
        scaledImage.flush();

				ImageIO.write(newImage, "JPEG", outputStream);	//write the image out as a JPEG TODO use a constant; see http://www.digitalsanctuary.com/tech-blog/java/how-to-resize-uploaded-images-using-java-better-way.html for alternate writing approach
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
		}
		catch(final IOException ioException)
		{
			throw ResourceIOException.toResourceIOException(ioException, resource.getURI());
		}
		return resource;
	}

	/**Performs a filtering operation on a resource.
	@param resource The description of the resource.
	@param inputFile The source of the resource content to filter.
	@param outputFile The destination of the filtered resource content.
	@return The new description of the filtered resource content.
	@exception ResourceIOException if there is an error filtering the resource.
	*/
/*TODO del; this is too slow
	public URFResource filter(final URFResource resource, final File inputFile, final File outputFile) throws ResourceIOException
	{
		final Dimensions aspectDimensions;	//determine the aspect dimensions
		switch(getImageAspect())	//see what image aspect is called for
		{
			case PREVIEW:
				aspectDimensions=PREVIEW_DIMENSIONS;
				break;
			case THUMBNAIL:
				aspectDimensions=THUMBNAIL_DIMENSIONS;
				break;
			default:
				throw new AssertionError("Unrecognized image aspect: "+getImageAspect());
		}

		try
		{
	    final ImagePlus ip = new ImagePlus(inputFile.getPath());
			final int originalWidth=ip.getWidth();
			final int originalHeight=ip.getHeight();
	//TODO del Debug.trace("original image dimension", originalDimension);
			if(originalWidth>aspectDimensions.getWidth().getValue() || originalHeight>aspectDimensions.getHeight().getValue())	//if this image needs scaled
			{
				final Dimensions originalDimensions=new Dimensions(originalWidth, originalHeight, Unit.PIXEL);	//find the original dimensions of the image
				final Dimensions newDimensions=originalDimensions.constrain(aspectDimensions);	//constrain the dimension to the scaled dimension
	
				final int newWidth=(int)newDimensions.getWidth().getValue();
				final int newHeight=(int)newDimensions.getHeight().getValue();
	    
		    ImageProcessor imp = ip.getProcessor();
		    imp.setInterpolate(true);
		    ImageProcessor imp2 = imp.resize(newWidth, newHeight);
		    ImagePlus ip2 = new ImagePlus("Resized Image", imp2);
		    FileSaver fs = new FileSaver(ip2);
		    fs.saveAsJpeg(outputFile.getPath());
			}
			else	//if the image doesn't need to be scaled
			{
				moveFile(inputFile, outputFile);	//just move the file
			}
		}
		catch(final IOException ioException)
		{
			throw ResourceIOException.toResourceIOException(ioException, resource.getURI());
		}
		return resource;
	}
*/
}
