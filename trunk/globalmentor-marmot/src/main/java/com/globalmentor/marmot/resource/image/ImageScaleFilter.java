/*
 * Copyright © 1996-2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.marmot.resource.image;

/*TODO del ImageJ
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
*/

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.*;

import org.urframework.URFResource;
//JAI import javax.media.jai.*;

import static com.globalmentor.java.Objects.*;

import static com.globalmentor.io.Files.*;

import com.globalmentor.awt.geom.*;
import com.globalmentor.marmot.resource.ResourceFileContentFilter;
import com.globalmentor.marmot.resource.ResourceContentFilter;
import com.globalmentor.net.ResourceIOException;

/**A filter for scaling an image.
@author Garret Wilson
*/
public class ImageScaleFilter implements ResourceContentFilter	//TODO remove dependency on AWT; this is the only dependency on AWT in the entire globalmentor-marmot project
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
	public final static Dimension2D PREVIEW_DIMENSIONS=new ImmutableDimension2D(800, 600);

	/**The dimensions of the figure aspect.*/
	public final static Dimension2D FIGURE_DIMENSIONS=new ImmutableDimension2D(400, 300);
	
	/**The dimensions of the thumbnail aspect.*/
	public final static Dimension2D THUMBNAIL_DIMENSIONS=new ImmutableDimension2D(200, 600);

	/**Performs a scaling operation on a resource.
	@param resource The description of the resource.
	@param inputStream The source of the resource content to filter.
	@param outputStream The destination of the filtered resource content.
	@return The new description of the filtered resource content.
	@exception ResourceIOException if there is an error filtering the resource.
	*/
	public URFResource filter(final URFResource resource, final InputStream inputStream, final OutputStream outputStream) throws ResourceIOException	//TODO review http://archives.java.sun.com/cgi-bin/wa?A2=ind0311&L=jai-interest&F=&S=&P=15036 and http://www.leptonica.com/scaling.html
	{
//TODO del Log.trace("ready to scale to aspect", imageAspect);
		final Dimension2D aspectDimensions;	//determine the aspect dimensions
		switch(getImageAspect())	//see what image aspect is called for
		{
			case PREVIEW:
				aspectDimensions=PREVIEW_DIMENSIONS;
				break;
			case FIGURE:
				aspectDimensions=FIGURE_DIMENSIONS;
				break;
			case THUMBNAIL:
				aspectDimensions=THUMBNAIL_DIMENSIONS;
				break;
			default:
				throw new AssertionError("Unrecognized image aspect: "+getImageAspect());
		}

//TODO del Log.trace("aspect dimensions:", scaledDimension);
		try
		{
			final ImageInputStream imageInputStream=ImageIO.createImageInputStream(inputStream);	//create an image input stream from the input stream
			if(imageInputStream==null)	//if we weren't able to create an image input stream
			{
      	throw new ResourceIOException(resource.getURI(), "Unable to create image input stream for resource "+resource.getURI());
			}
      final Iterator<ImageReader> imageReaderIterator=ImageIO.getImageReaders(imageInputStream);	//get an iterator to image readers for this image input stream 
      if(!imageReaderIterator.hasNext())	//if there are no image readers available for this image
      {
      	throw new ResourceIOException(resource.getURI(), "No image readers available for resource "+resource.getURI());
      }
      final ImageReader imageReader=imageReaderIterator.next();	//get the first image reader available
      final ImageReadParam imageReadParam=imageReader.getDefaultReadParam();	//get the default image reading parameters
      imageReader.setInput(imageInputStream, true, true);	//tell the image reader to read from the image input stream
      final BufferedImage bufferedImage;
//TODO fix      IIOMetadata iioMetadata=null;
			try
			{
//TODO don't do this blindly; this copies *all* metadata and messes up the palette on at least black-and-white images				iioMetadata=imageReader.getImageMetadata(0);	//get the metadata for the first image (and only image that we currently support)
				bufferedImage=imageReader.read(0, imageReadParam);	//tell the image reader to read the image
			}
			finally
			{
				imageReader.dispose();	//tell the image reader we don't need it any more
			}
//TODO del when works			final BufferedImage bufferedImage=ImageIO.read(inputStream);	//read the image
			final int originalWidth=bufferedImage.getWidth();
			final int originalHeight=bufferedImage.getHeight();
	//TODO del Log.trace("original image dimension", originalDimension);
      final BufferedImage newImage;
				//the multi-resizing technique described at http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html produces many black lines for normal JAI scaling
			if(originalWidth>aspectDimensions.getWidth() || originalHeight>aspectDimensions.getHeight())	//if this image needs scaled
			{
				final Dimension2D originalDimensions=new ImmutableDimension2D(originalWidth, originalHeight);	//find the original dimensions of the image
				final Dimension2D newDimensions=Geometry.constrain(originalDimensions, aspectDimensions);	//constrain the dimension to the scaled dimension
				final int newWidth=(int)newDimensions.getWidth();
				final int newHeight=(int)newDimensions.getHeight();
/*JAI method
	//TODO del Log.trace("scaling to dimension", newDimension);
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
        newImage = new BufferedImage(newWidth, newHeight, type);
        Graphics2D graphics = newImage.createGraphics();
	      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(scaledImage, null, null);
        graphics.dispose();
        scaledImage.flush();
//TODO del when works				ImageIO.write(newImage, "JPEG", outputStream);	//write the image out as a JPEG TODO use a constant; see http://www.digitalsanctuary.com/tech-blog/java/how-to-resize-uploaded-images-using-java-better-way.html for alternate writing approach
			}
			else	//if the image doesn't need scaled
			{
        newImage=bufferedImage;	//write the buffered image unchanged
//TODO del when works				ImageIO.write(bufferedImage, "JPEG", outputStream);	//write the image out as a JPEG TODO use a constant
			}
			final ImageWriter imageWriter=ImageIO.getImageWriter(imageReader);	//get an image writer that corresponds to the reader; this will hopefully conserve the metadata as well
			final ImageWriteParam imageWriteParam=imageWriter.getDefaultWriteParam();	//get default parameters for writing the image
			if(imageWriteParam.canWriteCompressed())	//if the writer can compress images (if we don't do this check, an exception will be thrown if the image writer doesn't support compression, e.g. for PNG files)
			{
				imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);	//indicate that we'll explicitly set the compression quality
				imageWriteParam.setCompressionQuality(1.0f);	//use the highest quality available; see http://www.universalwebservices.net/web-programming-resources/java/adjust-jpeg-image-compression-quality-when-saving-images-in-java
			}
			final ImageOutputStream imageOutputStream=ImageIO.createImageOutputStream(outputStream);	//create an image output stream from the output stream
			if(imageOutputStream==null)	//if we weren't able to create an image output stream
			{
      	throw new ResourceIOException(resource.getURI(), "Unable to create image output stream for resource "+resource.getURI());
			}
			imageWriter.setOutput(imageOutputStream);	//tell the image writer to write to the image output stream
			final IIOImage iioImage=new IIOImage(newImage, null, null);	//create an iioImage to write	TODO fix , preserving the metadata, if any, we got from the original file
			try
			{
				imageWriter.write(null, iioImage, imageWriteParam);	//tell the image writer to read the image using the custom parameters
			}
			finally
			{
				imageWriter.dispose();	//tell the image writer we don't need it any more
			}
	/*TODO fix so that we can copy the data unchanged; we've already used up the input stream at this point, though
			else	//if the image doesn't need scaled
			{
	Log.trace("copy the data verbatim");
				copy(inputStream, outputStream);	//copy the input stream to the output stream unmodified
	Log.trace("done copying");
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
	//TODO del Log.trace("original image dimension", originalDimension);
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
