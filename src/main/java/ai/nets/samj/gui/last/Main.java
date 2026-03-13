package ai.nets.samj.gui.last;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apposed.appose.TaskException;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.communication.model.DummyModel;
import ai.nets.samj.communication.model.EfficientTAMSmall;
import ai.nets.samj.communication.model.EfficientTAMTiny;
import ai.nets.samj.communication.model.SAM2Large;
import ai.nets.samj.communication.model.SAM2Small;
import ai.nets.samj.communication.model.SAM2Tiny;
import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.last.ImageSelection.ImageSelectionListener;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.gui.last.ModelDrawerPanel.ModelDrawerPanelListener;
import ai.nets.samj.gui.last.ModelSelection.ModelSelectionListener;
import ai.nets.samj.gui.roimanager.RoiManagerConsumer;
import ai.nets.samj.models.AbstractSamJ.BatchCallback;
import ai.nets.samj.ui.ConsumerInterface;
import ai.nets.samj.ui.ConsumerInterface.ConsumerCallback;
import io.bioimage.modelrunner.system.PlatformDetection;
import ai.nets.samj.ui.PromptBridge;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;

public class Main extends MainGUI {
	
    protected final List<SAMModel> modelList;
    protected ImageSelectionListener imageListener;
    protected ModelSelectionListener modelListener;
    protected ModelDrawerPanelListener modelDrawerListener;
    protected BatchCallback batchDrawerCallback;
    protected ConsumerCallback consumerCallback;
    protected PromptBridge promptBridge;
    protected Runnable cancelCallback;
    
    private final ExecutorService cmbModelsExecutor = Executors.newSingleThreadExecutor();
    private volatile Future<?> cmbModelsLastTask;

    private static final long serialVersionUID = -6511057540533292091L;

    public static final List<SAMModel> DEFAULT_MODEL_LIST = new ArrayList<>();
    static {
    	try {
	        DEFAULT_MODEL_LIST.add(new SAM2Tiny());
	        DEFAULT_MODEL_LIST.add(new SAM2Small());
	        DEFAULT_MODEL_LIST.add(new SAM2Large());
	        DEFAULT_MODEL_LIST.add(new EfficientTAMTiny());
	        DEFAULT_MODEL_LIST.add(new EfficientTAMSmall());
    	} catch (Exception ex) {
	        DEFAULT_MODEL_LIST.add(new DummyModel());
    	}
    }
    
    private static final ConsumerInterface DUMMY_CONSUMER = new ConsumerInterface() {

		@Override
		public List<ComboBoxItem> getListOfOpenImages() {return new ArrayList<>();}
		@Override
		public Object getFocusedImage() {return null;}
		@Override
		public String getFocusedImageName() {return null;}
		@Override
		public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getFocusedImageAsRai() {
			return null;
		}
		@Override
		public List<int[]> getPointRoisOnFocusImage() {return new ArrayList<>();}
		@Override
		public List<Rectangle> getRectRoisOnFocusImage() {return new ArrayList<>();}
		@Override
		public void activateListeners() {}
		@Override
		public void deactivateListeners() {}
		@Override
		public void setFocusedImage(Object image) {}
		@Override
		public void deselectImage() {}
		@Override
		public void deletePointRoi(int[] pp) {}
		@Override
		public void deleteRectRoi(Rectangle rect) {}
		@Override
		public boolean isValidPromptSelected() {return false;}
		@Override
		public void notifyBatchSamize(String modelName, String maskPrompt) {}
		@Override
		public void notifyPolygons(List<Mask> masks) {}
		@Override
		public RoiManagerConsumer getRoiManagerConsumer() {return null;}
    };

	public Main() {
		this(DEFAULT_MODEL_LIST, DUMMY_CONSUMER);
	}

	public Main(ConsumerInterface consumer) {
		this(DEFAULT_MODEL_LIST, consumer);	
	}

	public Main(List<SAMModel> modelList) {
		this(modelList, DUMMY_CONSUMER);
	}

	public Main(List<SAMModel> modelList, ConsumerInterface consumer) {
		super(consumer.getRoiManagerConsumer());
		
		this.modelList = modelList;
		this.consumer = consumer;

		createListeners();

		this.consumer.setPromptBridge(promptBridge);
		
        this.consumer.setGuiCallback(() -> {
        	SwingUtilities.invokeLater(() -> {
                selectionPanel.cmbImages.updateList();
                if (selectionPanel.cmbImages.getSelectedObject() == null)
                	return;
                selectionPanel.go.setEnabled(false);
                selectionPanel.go.showAnimation(true);
        	});
            new Thread(() -> {
            	boolean isInstalled = selectionPanel.cmbModels.getSelectedModel().isInstalled();
            	SwingUtilities.invokeLater(() -> {
                	selectionPanel.go.setEnabled(isInstalled);
                	selectionPanel.go.showAnimation(false);
            	});
            }).start();
		});
        this.consumer.setCallback(consumerCallback);
        
        this.consumerCallback.validPromptChosen(this.consumer.isValidPromptSelected());
        
        this.drawersPanel.modelDrawerPanel.addModelDrawerPanelListener(modelDrawerListener);
		
		this.selectionPanel.getModelSelection().getButton().addActionListener(e -> Main.this.toggleModelDrawer());
		this.selectionPanel.getImageSelection().getButton().addActionListener(e -> Main.this.toggleImageDrawer());
		this.selectionPanel.go.addActionListener(e -> loadModel());
        this.bottomPanel.export.addActionListener(e -> consumer.getRoiManagerConsumer().exportMask());
        this.centerPanel.instantCard.chkInstant.addActionListener(
        		e -> setInstantPromptsEnabled(this.centerPanel.instantCard.chkInstant.isSelected() && this.centerPanel.isPromptValid()));
        this.centerPanel.batchCard.btnBatchSAMize.addActionListener(e -> batchSAMize());
        this.centerPanel.batchCard.stopProgressBtn.addActionListener(null);
        this.close.addActionListener(e -> close());
		this.bottomPanel.acceleratorEnabled.addActionListener((e) -> warnIfLoaded());
		this.help.addActionListener(e -> System.err.println("Not implemented"));
		SwingUtilities.invokeLater(() -> changeGUI());
        
		this.selectionPanel.cmbModels.setListener(modelListener);
		this.selectionPanel.cmbModels.setModels(this.modelList);

		this.selectionPanel.cmbImages.setConsumer(consumer);
		this.selectionPanel.cmbImages.setListener(this.imageListener);
	}

    protected void setInstantPromptsEnabled(boolean enabled) {
        if (enabled) {
            consumer.activateListeners();
        } else {
            consumer.deactivateListeners();
        }
    }

    protected void loadModel() {
        SwingUtilities.invokeLater(() -> {
        	setLoading();
        	selectionPanel.cmbModels.getSelectedModel().setDevice(bottomPanel.getDevice());
			int slice = selectionPanel.cmbImages.getSelectedCurrentSlice();
			int frame = selectionPanel.cmbImages.getSelectedCurrentFrame();
			int nFrames = selectionPanel.cmbImages.getSelectedNFrames();
			int nSlices = selectionPanel.cmbImages.getSelectedNSlices();
			boolean propagate = centerPanel.isInstantShowing() ? centerPanel.instantCard.propagate3D.isSelected() : centerPanel.batchCard.propagate3D.isSelected();
	        new Thread(() -> {
	            try {
	                // TODO try removing Cast
	            	
	            	Main.this.selectionPanel.cmbModels.loadModel(Cast.unchecked(Main.this.selectionPanel.cmbImages.getSelectedRai()),
	            			slice, frame, nSlices, nFrames, propagate);
	                consumer.setFocusedImage(Main.this.selectionPanel.cmbImages.getSelectedObject());
	                drawersPanel.imageDrawerPanel.roiManager.setImage(selectionPanel.cmbImages.getSelectedObject());
	                consumer.setModel(Main.this.selectionPanel.cmbModels.getSelectedModel());
	                setInstantPromptsEnabled(Main.this.centerPanel.instantCard.chkInstant.isSelected() && centerPanel.isPromptValid());
	                Main.this.selectionPanel.cmbModels.getSelectedModel().setReturnOnlyBiggest(bottomPanel.returnLargest.isSelected());
	                SwingUtilities.invokeLater(() -> Main.this.manageLoaded(true));
	            } catch (Exception ex) {
	            	SwingUtilities.invokeLater(() -> Main.this.manageLoaded(false));
	                ex.printStackTrace();
	            }
	        }).start();
        });
    }
    
    protected void warnIfLoaded() {
    	SAMModel selectedModel = selectionPanel.cmbModels.getSelectedModel();
    	if (!selectedModel.isLoaded())
    		return;
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Changing the device will require reloading the model on the new device." + System.lineSeparator()
                + "Do you want to continue?",
                "Change device",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
    	selectedModel.closeProcess();
    	modelListener.changeDrawerPanel(selectedModel);
    }
    
    protected < T extends RealType< T > & NativeType< T > > void batchSAMize() {
    	RandomAccessibleInterval<T> rai;
    	if (this.consumer.getFocusedImage() != this.selectionPanel.cmbImages.getSelectedObject())
    		rai = this.consumer.getFocusedImageAsRai();
    	else
    		rai = null;
    	List<int[]> pointPrompts = this.consumer.getPointRoisOnFocusImage();
    	List<Rectangle> rectPrompts = this.consumer.getRectRoisOnFocusImage();
    	if ((pointPrompts.size() == 0 && rectPrompts.size() == 0 && rai == null)
    			|| (pointPrompts.size() == 0 && rectPrompts.size() == 0 && !(Util.getTypeFromInterval(rai) instanceof IntegerType))) {
        	centerPanel.batchCard.setWarningLayout(true);
    		return;
    	}
    	centerPanel.batchCard.stopProgressBtn.setEnabled(true);
    	consumer.setFocusedImage(selectionPanel.cmbImages.getSelectedObject());
        drawersPanel.imageDrawerPanel.roiManager.setImage(selectionPanel.cmbImages.getSelectedObject());
    	new Thread(() -> {
    		try {
    			consumer.notifyBatchSamize(selectionPanel.cmbModels.getSelectedModel().getName(), 
    					rai == null ? null : consumer.getFocusedImageName() );
    			selectionPanel.cmbModels.getSelectedModel().processBatchOfPrompts(pointPrompts, rectPrompts, rai, batchDrawerCallback);
			} catch (IOException | TaskException | InterruptedException e) {
				e.printStackTrace();
			}
    		SwingUtilities.invokeLater(() -> centerPanel.batchCard.stopProgressBtn.setEnabled(false));
    	}).start();;
    	pointPrompts.stream().forEach(pp -> consumer.deletePointRoi(pp));
    	rectPrompts.stream().forEach(pp -> consumer.deleteRectRoi(pp));
    }

    public void close() {
        selectionPanel.cmbModels.unLoadModel();
        drawersPanel.modelDrawerPanel.interruptThreads();
    }
    
    protected void createListeners() {
        if (imageListener != null) return;

        imageListener = new ImageSelectionListener() {
            @Override
            public void modelActionsOnImageChanged() {
            }

            @Override
            public void imageActionsOnImageChanged() {
                consumer.deactivateListeners();
                consumer.deselectImage();

                if (Main.this.selectionPanel.cmbImages.getSelectedObject() == null) {
                    Main.this.manageInstalled(false);
                    Main.this.drawersPanel.imageDrawerPanel.roiManager.block(true);
                    return;
                }

                Main.this.selectionPanel.go.showAnimation(true);
                new Thread(() -> {
                    boolean installed = Main.this.selectionPanel.cmbModels.getSelectedModel().isInstalled();
                    SwingUtilities.invokeLater(() -> {
                    	Main.this.selectionPanel.go.setEnabled(installed);
                    	if (installed)
                            Main.this.manageLoaded(false);
                    	Main.this.selectionPanel.go.showAnimation(false);
                        if (Main.this.selectionPanel.go.isEnabled())
                            Main.this.drawersPanel.imageDrawerPanel.roiManager.updateButtonsEnabled();
                        	return;
                    });
                }).start();
            }
        };

        modelListener = new ModelSelectionListener() {
            @Override
            public void changeDrawerPanel(SAMModel selected) {
            	boolean cudaSupported = selected.getInstallationManger().getPixiEnv() == "cuda";
            	boolean isMps = PlatformDetection.isMacOS() && PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64);
            	if (!cudaSupported && !isMps) {
            		bottomPanel.acceleratorEnabled.setSelected(false);
            		bottomPanel.acceleratorEnabled.setEnabled(false);
            	} else {
            		bottomPanel.acceleratorEnabled.setEnabled(true);
            	}
                if (Main.this.isDrawerOpen 
                		&& Main.this.selectionPanel.cmbModels.getSelectedModel() == selected
                		&& selected.isLoaded()) {
                	Main.this.setImageDrawerOpen(true);
                    Main.this.manageLoaded(true);
                    return;
                }

                drawersPanel.modelDrawerPanel.setSelectedModel(selected);
                Future<?> old = cmbModelsLastTask;
                if (old != null) old.cancel(true);

                cmbModelsLastTask = cmbModelsExecutor.submit(() -> {
                    boolean installed = selected.isInstalled();
                    if (Thread.currentThread().isInterrupted()) return;

                    SwingUtilities.invokeLater(() -> {
                    	if (installed && Main.this.selectionPanel.cmbImages.getSelectedObject() != null) {
                            Main.this.manageLoaded(false);
                        } else if (installed) {
                            Main.this.manageInstalled(false);
                        } else {
                            Main.this.setModelDrawerOpen(true);
                            Main.this.manageInstalled(false);
                        }
                    });
                });
            }
        };

        modelDrawerListener = new ModelDrawerPanelListener() {
            @Override
            public void setGUIEnabled(boolean enabled) {
            	Main.this.selectionPanel.cmbModels.setEnabled(enabled);
            	Main.this.selectionPanel.cmbImages.setEnabled(enabled);

                if (!enabled) {
                    // TODO setTwoThirdsEnabled(false);
                    return;
                }

                if (Main.this.selectionPanel.cmbImages.getSelectedObject() != null) {
                    new Thread(() -> {
                        boolean installed = Main.this.selectionPanel.cmbModels.getSelectedModel().isInstalled();
                        manageInstalled(false);
                        if (installed) {
                            SwingUtilities.invokeLater(() -> Main.this.manageLoaded(false));
                        }
                    }).start();
                }
            }

			@Override
			public void setInstalling(boolean installing) {
				Main.this.selectionPanel.cmbImages.setEnabled(!installing);
				Main.this.selectionPanel.cmbModels.setEnabled(!installing);
				
			}
        };
        
        promptBridge = new PromptBridge () {

			@Override
			public List<Mask> sendRectanglePrompt(long[] xywh) {
				int slice = selectionPanel.cmbImages.getSelectedCurrentSlice();
				int frame = selectionPanel.cmbImages.getSelectedCurrentFrame();
				boolean propagate = centerPanel.isInstantShowing() ? centerPanel.instantCard.propagate3D.isSelected() : centerPanel.batchCard.propagate3D.isSelected();
				final Interval rectInterval = new FinalInterval(
						new long[] { xywh[0], xywh[1] },
						new long[] { xywh[0] + xywh[2] -1, xywh[1] + xywh[3] - 1 } );
				try {
					SAMModel samj = selectionPanel.cmbModels.getSelectedModel();
					List<Mask> samjMask = samj.fetch2dSegmentation(rectInterval, slice, frame, propagate);
					Main.this.drawersPanel.imageDrawerPanel.addToRoiManager(samjMask, "rect", samj.getName());
					return samjMask;
				} catch (Exception ex) {
					ex.printStackTrace();
					return new ArrayList<>();
				}
			}

			@Override
			public List<Mask> sentPointPrompt(List<Localizable> posPoints) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<Mask> sentPointPrompt(List<Localizable> posPoints, Rectangle zoomedArea) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<Mask> sentPointPrompt(List<Localizable> posPoints, List<Localizable> negPoints) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<Mask> sentPointPrompt(List<Localizable> posPoints, List<Localizable> negPoints,
					Rectangle zoomedArea) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void notifyCloseModel() {
				// TODO Auto-generated method stub
				
			}
        	
        };

        batchDrawerCallback = new BatchCallback() {
            private int nRois;

            @Override
            public void setTotalNumberOfRois(int nRois) {
                this.nRois = nRois;
                SwingUtilities.invokeLater(() -> centerPanel.batchCard.batchProgress.setValue(0));
            }

            @Override
            public void updateProgress(int n) {
                SwingUtilities.invokeLater(() ->
                centerPanel.batchCard.batchProgress.setValue((int) Math.round(100 * n / (double) nRois))
                );
            }

            @Override
            public void drawRoi(List<Mask> masks) {
                SwingUtilities.invokeLater(() -> consumer.notifyPolygons(masks));
            }

            @Override
            public void deletePointPrompt(List<int[]> promptList) {
                SwingUtilities.invokeLater(() ->
                    promptList.forEach(consumer::deletePointRoi)
                );
            }

            @Override
            public void deleteRectPrompt(List<int[]> promptList) {
                SwingUtilities.invokeLater(() ->
                    promptList.stream()
                        .map(r -> new Rectangle(r[0], r[1], r[2] - r[0], r[3] - r[1]))
                        .forEach(consumer::deleteRectRoi)
                );
            }
        };

        consumerCallback = new ConsumerCallback() {
            @Override
            public void validPromptChosen(boolean isValid) {
                centerPanel.setValidPrompt(isValid);
            }
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("MainGUI");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

            Main gui = new Main();
            frame.setContentPane(gui);

            // Pick one:
            frame.setSize(250, 400);          // fixed size for quick testing
            // frame.pack();                  // use if your components have preferred sizes

            frame.setLocationRelativeTo(null); // center on screen
            frame.setVisible(true);
        });
    }

	public void setCancelCallback(Runnable cancelCallback) {
		this.cancelCallback = cancelCallback;
	}
}
