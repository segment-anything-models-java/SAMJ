/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.communication.model;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;

import java.awt.Polygon;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.AbstractSamJ;
import ai.nets.samj.EfficientViTSamJ;
import ai.nets.samj.SamEnvManager;
import ai.nets.samj.ui.SAMJLogger;

/**
 * Instance to communicate with EfficientViTSAM l1
 * @author Carlos Garcia Lopez de Haro
 * @author Vladimir Ulman
 */
public class EfficientViTSAML1 implements SAMModel {

	private EfficientViTSamJ efficientSamJ;
	private SAMJLogger log;
	private Boolean installed = false;
	/**
	 * "Official" name of the model
	 */
	public static final String FULL_NAME = "EfficientViTSAM-l1";
	/**
	 * Axes order required for the input image by the model
	 */
	public static final String INPUT_IMAGE_AXES = "xyc";
	
	private static final String HTML_DESCRIPTION = "EfficientViT-SAM, fourth biggest version (L1) <br>"
	        + "<strong>Weights size:</strong> 190.9 MB <br>"
	        + "<strong>Speed:</strong> 2nd out of 6 <br>"
	        + "<strong>Performance:</strong> 5th out of 6 <br>"
	        + "<strong>GitHub Repository:</strong> <a href=\"https://github.com/mit-han-lab/efficientvit\">"
	        + "https://github.com/mit-han-lab/efficientvit</a> <br>"
	        + "<strong>Paper:</strong> <a href=\"https://arxiv.org/pdf/2402.05008.pdf\">EfficientViT-SAM: Accelerated "
	        + "Segment Anything Model Without Performance Loss</a>";
	
	/**
	 * Create an empty instance of the model
	 */
	public EfficientViTSAML1() {
		
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return FULL_NAME;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return HTML_DESCRIPTION + (!this.installed ? SAMModel.HTML_NOT_INSTALLED : "");
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public boolean isInstalled() {
		return installed;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public SAMModel instantiate(final RandomAccessibleInterval<?> image, final SAMJLogger useThisLoggerForIt) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			return new EfficientViTSAML1(image,useThisLoggerForIt);
		} catch (IOException | InterruptedException | RuntimeException e) {
			useThisLoggerForIt.error(FULL_NAME + " experienced an error: " + e.getMessage());
			throw e;
		}
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public void setInstalled(boolean installed) {
		this.installed = installed;		
	}

	/**
	 * Create an instance of the model that loads the model and encodes an image
	 * @param image
	 * 	the image to be encoded
	 * @param log
	 * 	a logging functional interface to be able to keep track of what the model is doing
	 * @throws IOException if any of the files to run a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public EfficientViTSAML1(final RandomAccessibleInterval<?> image, final SAMJLogger log)
								throws IOException, RuntimeException, InterruptedException {
		this.log = log;
		AbstractSamJ.DebugTextPrinter filteringLogger = text -> {
			int idx = text.indexOf("contours_x");
			if (idx > 0) this.log.info( text.substring(0,idx) );
			else this.log.info( text );
		};
		efficientSamJ = EfficientViTSamJ.initializeSam("l1",
				SamEnvManager.create(), Cast.unchecked(image),
				filteringLogger, false);
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public List<Polygon> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			List<int[]> list = listOfPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			List<int[]> negList = listOfNegPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			if (negList.size() == 0) return efficientSamJ.processPoints(list);
			else return efficientSamJ.processPoints(list, negList);
		} catch (IOException | RuntimeException | InterruptedException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public List<Polygon> fetch2dSegmentation(Interval boundingBox2D) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			//order to processBox() should be: x0,y0, x1,y1
			final int bbox[] = {
				(int)boundingBox2D.min(0),
				(int)boundingBox2D.min(1),
				(int)boundingBox2D.max(0),
				(int)boundingBox2D.max(1)
			};
			return efficientSamJ.processBox(bbox);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public <T extends RealType<T> & NativeType<T>> List<Polygon> fetch2dSegmentationFromMask(RandomAccessibleInterval<T> rai) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			return efficientSamJ.processMask(rai);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public void notifyUiHasBeenClosed() {
		log.info(FULL_NAME+": OKAY, I'm closing myself...");
		closeProcess();
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public void closeProcess() {
		efficientSamJ.close();
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public String getInputImageAxes() {
		return INPUT_IMAGE_AXES;
	}
}