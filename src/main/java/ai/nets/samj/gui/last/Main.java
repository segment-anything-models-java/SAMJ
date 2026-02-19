package ai.nets.samj.gui.last;

import javax.swing.SwingUtilities;

public class Main extends MainGUI {

    private static final long serialVersionUID = -6511057540533292091L;

	public Main() {
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
}
