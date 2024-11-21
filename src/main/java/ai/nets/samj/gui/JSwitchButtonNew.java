package ai.nets.samj.gui;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.JLabel;

public class JSwitchButtonNew extends AbstractButton {
    private Color colorDark = new Color(20,20,20);
    private Color black = new Color(0,0,0,100);
    private Color white = new Color(255,255,255,220);
    private Color red = new Color(220,20,20);
    private Color green = new Color(20,220,20);
    private final String trueLabel;
    private final String falseLabel;
    private int minWidth = 100; // Default minimum width
    private int minHeight = 30; // Default minimum height
    private boolean initialized = false;

    public JSwitchButtonNew(String trueLabel, String falseLabel) {
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
        
        // Set initial minimum size
        setMinimumSize(new Dimension(minWidth, minHeight));
        setPreferredSize(new Dimension(minWidth, minHeight));
        
        setModel(new DefaultButtonModel());
        setSelected(false);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                setSelected(!isSelected());
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!initialized) {
            initialized = true;
            // Now we can safely get FontMetrics
            calculateMinimumWidth();
        }
    }

    private void calculateMinimumWidth() {
        FontMetrics fm = getFontMetrics(getFont());
        if (fm != null) {
            double trueLenth = 5 + fm.stringWidth(trueLabel);
            double falseLenght = 5 + fm.stringWidth(falseLabel);
            int maxTextWidth = (int)Math.max(trueLenth, falseLenght);
            int gap = Math.max(5, 5 + (int)Math.abs(trueLenth - falseLenght));
            minWidth = maxTextWidth + gap * 4;
            setMinimumSize(new Dimension(minWidth, minHeight));
            setPreferredSize(new Dimension(minWidth, minHeight));
            revalidate();
        }
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        width = Math.max(width, minWidth);
        height = Math.max(height, minHeight);
        super.setBounds(x, y, width, height);
    }

    @Override
    public void setSelected(boolean b) {
        if(b) {
            setText(trueLabel);
            setBackground(green);
        } else {
            setBackground(red);
            setText(falseLabel);
        }
        super.setSelected(b);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Cast the basic Graphics object to Graphics2D which provides more advanced features
        Graphics2D g2 = (Graphics2D)g;
        // Enable anti-aliasing for smoother edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Get current component dimensions
        int width = getWidth();
        int height = getHeight();

        // Calculate thumb width as either half the component width or full height, whichever is smaller
        int thumbWidth = Math.min(width / 2, height);

        // Draw the main background of the switch using the background color (red or green)
        g2.setColor(getBackground());
        g2.fillRoundRect(1, 1, width - 2, height - 2, height/2, height/2);

        // Draw two borders: outer black and inner white
        g2.setColor(black);  // Note: this is using the field 'black = new Color(0,0,0,100)' - 100 is alpha (transparency)
        g2.drawRoundRect(1, 1, width - 2, height - 2, height/2, height/2);
        g2.setColor(white);  // Note: this is using the field 'white = new Color(255,255,255,100)' - 100 is alpha
        g2.drawRoundRect(2, 2, width - 4, height - 4, height/2, height/2);

        // Calculate thumb X position - when selected, move to right side
        int thumbX = isSelected() ? width - thumbWidth - 2 : 2;

        // Draw the thumb with gradient
        g2.setPaint(new GradientPaint(thumbX, 0, white, thumbX, height, white));
        g2.fillRoundRect(thumbX, 2, thumbWidth, height - 4, height/2, height/2);

        // Draw the three dots if thumb is wide enough
        if (thumbWidth > 14) {
            drawThumbDetails(g2, thumbX, thumbWidth, height);
        }

        // Configure text drawing
        g2.setColor(Color.WHITE);  // This is pure white (255,255,255,255)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Set font as bold sans-serif, with size being either half the height or 16, whichever is smaller
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.min(height/2, 16)));

        // Calculate text position
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int textWidth = fm.stringWidth(text);
        // Calculate X position for text based on whether switch is selected
        int textX = isSelected() ?
            (thumbX - textWidth)/2 :  // If selected, text goes on left side
            thumbX + thumbWidth + (width - thumbX - thumbWidth - textWidth)/2;  // If not selected, text goes on right side
        // Calculate Y position to center text vertically
        int textY = (height + fm.getAscent() - fm.getDescent())/2;

        // Draw the text
        g2.drawString(text, textX, textY);
    }
    
    private void drawThumbDetails(Graphics2D g2, int x, int thumbWidth, int height) {
        // Center point of thumb
        int centerX = x + thumbWidth/2;
        int centerY = height/2;
        
        // Scale the dots based on thumb size
        int dotSize = Math.max(1, thumbWidth/10);
        int dotGap = dotSize + 1;
        
        // Draw dots
        g2.setColor(colorDark);
        for (int i = -1; i <= 1; i++) {
            g2.fillRect(centerX + i*dotGap - dotSize/2, centerY - dotGap, dotSize, dotSize * 3);
        }
        /**
        g2.setColor(colorDark);
        for (int i = -1; i <= 1; i++) {
            g2.fillRect(centerX + i*dotGap - dotSize/2, centerY, dotSize, dotSize);
        }
        
        g2.setColor(colorDark);
        for (int i = -1; i <= 1; i++) {
            g2.fillRect(centerX + i*dotGap - dotSize/2, centerY + dotGap, dotSize, dotSize);
        }
        */
    }
}