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

import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ai.nets.samj.models.AbstractSamJ;
import ai.nets.samj.models.EfficientViTSamJ;
import ai.nets.samj.annotation.Mask;
import ai.nets.samj.install.EfficientViTSamEnvManager;
import ai.nets.samj.install.SamEnvManagerAbstract;
import ai.nets.samj.ui.SAMJLogger;

/**
 * Instance to communicate with EfficientViTSAM l2
 * @author Carlos Garcia Lopez de Haro
 * @author Vladimir Ulman
 */
public class EfficientViTSAML2 implements SAMModel {

	private EfficientViTSamJ efficientSamJ;
	private final SamEnvManagerAbstract manager;
	private SAMJLogger log = new SAMJLogger() {

		@Override
		public void info(String text) {
			System.out.println(text);
		}

		@Override
		public void warn(String text) {
			System.err.println("[WARNING] -- " + text);
		}

		@Override
		public void error(String text) {
			System.err.println(text);
		}
		
	};
	private Boolean installed = false;
	private boolean onlyBiggest = false;
	/**
	 * Model complete name
	 */
	public static final String FULL_NAME = "EfficientViTSAM-l2";
	/**
	 * Axes order required for the input image by the model
	 */
	public static final String INPUT_IMAGE_AXES = "xyc";
	
	private static final String HTML_DESCRIPTION = "EfficientViT-SAM, third biggest version (L2) <br>"
	        + "<strong>Weights size:</strong> 245.7 MB <br>"
	        + "<strong>Speed:</strong> 3rd out of 6 <br>"
	        + "<strong>Performance:</strong> 4th out of 6 <br>"
	        + "<strong>GitHub Repository:</strong> <a href=\"https://github.com/mit-han-lab/efficientvit\">"
	        + "https://github.com/mit-han-lab/efficientvit</a> <br>"
	        + "<strong>Paper:</strong> <a href=\"https://arxiv.org/pdf/2402.05008.pdf\">EfficientViT-SAM: Accelerated "
	        + "Segment Anything Model Without Performance Loss</a>";

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
	public <T extends RealType<T> & NativeType<T>> void setImage(final RandomAccessibleInterval<T> image, final SAMJLogger useThisLoggerForIt) 
			throws IOException, InterruptedException, RuntimeException {
		Objects.requireNonNull(image, "The image cannot be null.");
		if (useThisLoggerForIt != null) 
			this.log = useThisLoggerForIt;
		if (this.efficientSamJ == null)
			this.efficientSamJ = EfficientViTSamJ.initializeSam("l2", manager);
		try {
			AbstractSamJ.DebugTextPrinter filteringLogger = text -> {
				int idx = text.indexOf("contours_x");
				if (idx > 0) this.log.info( text.substring(0,idx) );
				else this.log.info( text );
			};
			this.efficientSamJ.setDebugPrinter(filteringLogger);
			this.efficientSamJ.setImage(Cast.unchecked(image));;
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME + " experienced an error: " + e.getMessage());
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
	 */
	public EfficientViTSAML2() {
		this.manager = EfficientViTSamEnvManager.create(EfficientViTSamEnvManager.DEFAULT_DIR, "l2");
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public List<Mask> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			List<int[]> list = listOfPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			List<int[]> negList = listOfNegPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			if (negList.size() == 0) return efficientSamJ.processPoints(list, !onlyBiggest);
			else return efficientSamJ.processPoints(list, negList, !onlyBiggest);
		} catch (IOException | RuntimeException | InterruptedException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	@Override
	public List<Mask> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D,
			Rectangle zoomedRectangle) throws IOException, RuntimeException, InterruptedException {
		try {
			List<int[]> list = listOfPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			List<int[]> negList = listOfNegPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			if (negList.size() == 0) return efficientSamJ.processPoints(list, zoomedRectangle, !onlyBiggest);
			else return efficientSamJ.processPoints(list, negList, zoomedRectangle, !onlyBiggest);
		} catch (IOException | RuntimeException | InterruptedException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public List<Mask> fetch2dSegmentation(Interval boundingBox2D) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			//order to processBox() should be: x0,y0, x1,y1
			final int bbox[] = {
				(int)boundingBox2D.min(0),
				(int)boundingBox2D.min(1),
				(int)boundingBox2D.max(0),
				(int)boundingBox2D.max(1)
			};
			return efficientSamJ.processBox(bbox, !onlyBiggest);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public <T extends RealType<T> & NativeType<T>> List<Mask> fetch2dSegmentationFromMask(RandomAccessibleInterval<T> rai) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			return efficientSamJ.processMask(rai, !onlyBiggest);
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
		if (log != null)
			log.info(FULL_NAME+": OKAY, I'm closing myself...");
		closeProcess();
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public void closeProcess() {
	if (efficientSamJ != null)
			efficientSamJ.close();
		efficientSamJ = null;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public String getInputImageAxes() {
		return INPUT_IMAGE_AXES;
	}

	@Override
	public void setReturnOnlyBiggest(boolean onlyBiggest) {
		this.onlyBiggest = onlyBiggest;
	}

	@Override
	public String persistEncoding() throws IOException, InterruptedException {
		try {
			return efficientSamJ.persistEncoding();
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", unable to persist the encoding: "+e.getMessage());
			throw e;
		}
	}

	@Override
	public void selectEncoding(String encodingName) throws IOException, InterruptedException {
		try {
			efficientSamJ.selectEncoding(encodingName);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", unable to persist the encoding named '" + encodingName + "': "+e.getMessage());
			throw e;
		}
	}

	@Override
	public void deleteEncoding(String encodingName) throws IOException, InterruptedException {
		try {
			efficientSamJ.deleteEncoding(encodingName);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", unable to delete the encoding named '" + encodingName + "': "+e.getMessage());
			throw e;
		}
	}

	@Override
	public SamEnvManagerAbstract getInstallationManger() {
		return this.manager;
	}
}
