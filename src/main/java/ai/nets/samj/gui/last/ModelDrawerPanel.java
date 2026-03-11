package ai.nets.samj.gui.last;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.apposed.appose.Builder.ProgressConsumer;

import ai.nets.samj.communication.model.SAMModel;

/**
 * Panel controller that drives ModelDrawerPanelGui using background tasks.
 * Java 8 compatible.
 */
public class ModelDrawerPanel extends ModelDrawerPanelGui implements ActionListener {

    private static final long serialVersionUID = 4287546137999029365L;

    private volatile SAMModel model;

    // Keep listener API compatible; make it thread-safe to avoid ConcurrentModificationException.
    private final List<ModelDrawerPanelListener> listeners = new CopyOnWriteArrayList<ModelDrawerPanelListener>();

    private final HtmlLogger logger;
    
    // working line state
    private volatile String workingLabel;
    private int dots = 0;

    // Background tasks
    private SwingWorker<String, Void> infoWorker;
    private SwingWorker<Boolean, Void> installedWorker;
    private SwingWorker<Void, Void> installWorker;
    private SwingWorker<Void, Void> uninstallWorker;

    // “Working…” animation on EDT
    private final Timer workingTimer;

    // Prevent stale async updates after selecting another model
    private volatile long requestId = 0L;

    // Simple busy state
    private volatile boolean busy = false;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Color WORKING_COLOR = new Color(107, 114, 128);
    private final static String INSTALLING_STRING = "Installing";
    private final static String UNINSTALLING_STRING = "Uninstalling";

    protected ModelDrawerPanel() {
        super();

        this.logger = new HtmlLogger(html);

        this.install.addActionListener(this);
        this.uninstall.addActionListener(this);

        // Working animation uses HtmlLogger’s “blank message => working line” behavior.
        this.workingTimer = new Timer(600, e -> logger.log("", WORKING_COLOR)); // gray-ish
        this.workingTimer.setRepeats(true);
    }

    /* ---------------- public API (kept compatible) ---------------- */

    public void addListener(ModelDrawerPanelListener listener) {
        if (listener != null) this.listeners.add(listener);
    }

    public void addModelDrawerPanelListener(ModelDrawerPanelListener listener) {
        addListener(listener);
    }

    public void removeModelDrawerPanelListener(ModelDrawerPanelListener listener) {
        if (listener != null) this.listeners.remove(listener);
    }

    public void setSelectedModel(SAMModel model) {
        this.model = model;

        final long rid = ++requestId;

        // Update title on EDT
        onEdt(() -> setTitle(model != null ? model.getName() : "\u00A0"));

        // Cancel/stop any previous UI work and refresh
        cancelNonInstallTasks();
        stopWorking();

        refreshButtons(rid);
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible) {
            interruptThreads(); // keep method name for compatibility
        }
        super.setVisible(visible);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == install) {
            installModel();
        } else if (e.getSource() == uninstall) {
            uninstallModel();
        }
    }

    /**
     * Kept for compatibility with your older code.
     * Now cancels SwingWorkers + stops the timer (best-effort interrupt).
     */
    public void interruptThreads() {
        stopWorking();
        cancelAllTasks();
    }

    /* ---------------- UI refresh logic ---------------- */

    private void refreshButtons(final long rid) {
    	refreshButtons(rid, false);
    }

    private void refreshButtons(final long rid, final boolean afterInstallation) {
        final SAMModel m = this.model;

        // Always start from disabled.
        onEdt(() -> {
            install.setEnabled(false);
            uninstall.setEnabled(false);
        });

        if (m == null) return;

        // Cancel previous installed worker if any
        if (installedWorker != null) installedWorker.cancel(true);
        
        startWorking("Loading info");

        installedWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return Boolean.valueOf(m.isInstalled());
            }

            @Override
            protected void done() {
                if (rid != requestId) return; // stale result
                stopWorking();
                if (busy) return;             // while busy, we keep buttons disabled

                try {
                	boolean installed;
                	if (this.isCancelled()) {
                		installed = false;
                	} else
                		installed = get().booleanValue();

                    onEdt(() -> {
                        // If there is no installation manager, disable install/uninstall (same intent as your original)
                        if (m.getInstallationManger() == null) {
                            install.setEnabled(false);
                            uninstall.setEnabled(false);
                            return;
                        }

                        install.setEnabled(!installed);
                        uninstall.setEnabled(installed);
                    });
                    // IMPORTANT: don't mix document-insert logger with setText()/append without resetting logger.
                    logger.clear();
                    String description = m.getDescription(false);
                    
                    if (!installed && afterInstallation)
                    	description = SAMModel.HTML_ERROR_INSTALLING + SAMModel.HTML_NOT_INSTALLED + description;
                    else if (!installed)
                    	description = SAMModel.HTML_NOT_INSTALLED + description;
                    html.append(description);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    onEdt(() -> {
                        install.setEnabled(false);
                        uninstall.setEnabled(false);
                    });
                    logger.clear();
                    logger.log("Failed to load model info.");
                }
            }
        };

        installedWorker.execute();
    }

    /* ---------------- install / uninstall ---------------- */

    private void installModel() {
        final SAMModel m = this.model;
        if (m == null) return;

        // If no manager: just refresh & exit (same intent as your original)
        if (m.getInstallationManger() == null) {
            onEdt(() -> {
                refreshButtons(requestId);
                notifyGuiEnabled(true);
            });
            return;
        }

        setBusy(true);

        // Ensure we’re in “logger mode”
        logger.clear();
        
        startWorking(INSTALLING_STRING);

        cancelInstallTasksOnly(); // if an old install/uninstall worker exists
        
        notifyInstalling(true);

        installWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                ProgressConsumer progress = (title, current, maximum) -> {
                    double pct = (maximum <= 0) ? 0.0 : (100.0 * current / maximum);
                    String label = (title == null || title.trim().isEmpty()) ? "Downloading" : title;
                    logger.log(label + ": " + String.format(java.util.Locale.US, "%.1f", pct) + "%", new Color(37, 99, 235));
                };

                m.getInstallationManger().setOutputConsumer(str -> logger.log(str, Color.BLACK));
                m.getInstallationManger().setErrorConsumer(str -> logger.log(str, new Color(185, 28, 28))); // red-ish
                m.getInstallationManger().setProgressConsumer(progress);

                // This is the long-running call
                m.getInstallationManger().installEverything();

                return null;
            }

            @Override
            protected void done() {
                setBusy(false);

                notifyInstalling(false);
                try {
                    // surface exceptions if any
                    get();
                    logger.log("Installation complete.", new Color(22, 163, 74)); // green-ish
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.log("Installation failed: " + ex.getMessage(), new Color(185, 28, 28));
                }

                refreshButtons(requestId, true);
            }
        };

        installWorker.execute();
    }

    private void uninstallModel() {
        final SAMModel m = this.model;
        if (m == null) return;

        if (m.getInstallationManger() == null) {
            onEdt(() -> {
                refreshButtons(requestId);
                notifyGuiEnabled(true);
            });
            return;
        }

        setBusy(true);

        logger.clear();
        startWorking(UNINSTALLING_STRING);

        cancelInstallTasksOnly();

        uninstallWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                m.getInstallationManger().uninstall();
                return null;
            }

            @Override
            protected void done() {
                stopWorking();
                setBusy(false);

                try {
                    get();
                    logger.log("Uninstall complete.", new Color(22, 163, 74));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.log("Uninstall failed: " + ex.getMessage(), new Color(185, 28, 28));
                }

                refreshButtons(requestId);
            }
        };

        uninstallWorker.execute();
    }

    /* ---------------- working animation ---------------- */

    private void startWorking(String message) {
        logger.clear();
        workingLabel = (message == null || message.trim().isEmpty()) ? "": message.trim();
        dots = 0;

        // Print immediately (so user sees it right away)
        logger.log(buildWorkingLine(), WORKING_COLOR);

        if (!workingTimer.isRunning()) workingTimer.start();
    }

    private void stopWorking() {
        if (workingTimer.isRunning()) workingTimer.stop();
        dots = 0;
    }
    
    private String buildWorkingLine() {
        String time = LocalTime.now().format(TIME_FMT);

        dots = (dots % 3) + 1; // 1..3
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dots; i++) sb.append('.');

        return time + " " + workingLabel + sb.toString();
    }

    /* ---------------- busy state + utilities ---------------- */

    private void setBusy(boolean busy) {
        this.busy = busy;

        onEdt(() -> {
            // Disable buttons while busy
            install.setEnabled(!busy);
            uninstall.setEnabled(!busy);
        });

        notifyGuiEnabled(!busy);

        if (busy) {
            onEdt(() -> {
                install.setEnabled(false);
                uninstall.setEnabled(false);
            });
        }
    }

    private void notifyInstalling(final boolean installing) {
        onEdt(() -> {
            for (ModelDrawerPanelListener l : listeners) {
                try {
                    l.setInstalling(installing);
                } catch (Exception ignore) {
                    // keep UI resilient
                }
            }
        });
    }

    private void notifyGuiEnabled(final boolean enabled) {
        onEdt(() -> {
            for (ModelDrawerPanelListener l : listeners) {
                try {
                    l.setGUIEnabled(enabled);
                } catch (Exception ignore) {
                    // keep UI resilient
                }
            }
        });
    }

    private void cancelNonInstallTasks() {
        if (infoWorker != null && !infoWorker.isDone() && !infoWorker.isCancelled()) infoWorker.cancel(true);
        if (installedWorker != null && !installedWorker.isDone() && !installedWorker.isCancelled()) installedWorker.cancel(true);
    }

    private void cancelInstallTasksOnly() {
        if (installWorker != null) installWorker.cancel(true);
        if (uninstallWorker != null) uninstallWorker.cancel(true);
    }

    private void cancelAllTasks() {
        cancelNonInstallTasks();
        cancelInstallTasksOnly();
    }

    private void onEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    /* ---------------- listener (kept as-is for compatibility) ---------------- */

    public interface ModelDrawerPanelListener {
        void setGUIEnabled(boolean enabled);
        void setInstalling(boolean installing);
    }
}