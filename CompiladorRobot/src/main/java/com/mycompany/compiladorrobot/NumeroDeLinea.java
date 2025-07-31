/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.compiladorrobot;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.Element;

public class NumeroDeLinea extends JComponent {
    private final JTextArea textArea;
    private final Font font;

    public NumeroDeLinea(JTextArea textArea) {
        this.textArea = textArea;
        font = new Font("Monospaced", Font.PLAIN, 12);
        setFont(font);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        FontMetrics fm = g.getFontMetrics(font);
        Insets insets = textArea.getInsets();
        int ascent = fm.getAscent();
        int lineHeight = fm.getHeight();
        int visibleLines = textArea.getHeight() / lineHeight;

        int startOffset = textArea.viewToModel2D(new Point(0, insets.top));
        Element root = textArea.getDocument().getDefaultRootElement();
        int startLine = root.getElementIndex(startOffset);

        int y = insets.top + ascent;

        for (int i = startLine; i < startLine + visibleLines + 1; i++) {
            if (i >= root.getElementCount()) break;
            String lineNumber = String.valueOf(i + 1);
            int x = getWidth() - fm.stringWidth(lineNumber) - 5;
            g.drawString(lineNumber, x, y);
            y += lineHeight;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int lineCount = textArea.getLineCount();
        int maxDigits = String.valueOf(lineCount).length();
        int width = getFontMetrics(font).charWidth('0') * maxDigits + 10;
        return new Dimension(width, Integer.MAX_VALUE);
    }
}
