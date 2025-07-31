package com.mycompany.compiladorrobot;

import com.formdev.flatlaf.FlatIntelliJLaf;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.List;

public class PruebaFormato extends JFrame {
    private RSyntaxTextArea editorCodigo;
    private JTable tablaSimbolos;
    private JTable tablaIntermedio;
    private JTextArea txtResultado;
    private JLabel barraEstado;

    public PruebaFormato() {
        setTitle("Compilador Robot");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 700); 
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // AÑADIMOS EL NUEVO PANEL SUPERIOR QUE INCLUIRÁ TU TOOLBAR
        add(crearPanelSuperior(), BorderLayout.NORTH);

        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, crearPanelEditor(), crearPanelDeTablas());
        splitPrincipal.setResizeWeight(0.5);
        JSplitPane splitVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPrincipal, crearPanelConsola());
        splitVertical.setResizeWeight(0.6);
        add(splitVertical, BorderLayout.CENTER);

        barraEstado = new JLabel("Listo.");
        barraEstado.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(barraEstado, BorderLayout.SOUTH);
    }
    
    // Nuevo método para añadir el título sobre tu barra de herramientas
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lblTitulo = new JLabel("COMPILADOR ROBOT", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI Black", Font.BOLD, 32));
        panel.add(lblTitulo, BorderLayout.NORTH);
        
        // Llamamos a tu método original para crear la barra, que se añade aquí
        panel.add(crearBarraDeHerramientas(), BorderLayout.CENTER);
        return panel;
    }

    private JToolBar crearBarraDeHerramientas() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Color backgroundColor = new Color(70, 130, 180);
        Color hoverColor = new Color(100, 150, 200);
        Color textColor = Color.WHITE;
        Font buttonFont = new Font("Segoe UI", Font.BOLD, 12);
        
        // --- AHORA PASAMOS LA RUTA DEL ICONO A TU MÉTODO PERSONALIZADO ---
        JButton btnAbrir = crearBotonPersonalizado("Abrir Archivo", "/iconos/open.png", backgroundColor, hoverColor, textColor, buttonFont);
        btnAbrir.addActionListener(this::abrirArchivo);

        JButton btnGuardar = crearBotonPersonalizado("Guardar Archivo", "/iconos/save.png", backgroundColor, hoverColor, textColor, buttonFont);
        btnGuardar.addActionListener(this::guardarArchivo);

        JButton btnAnalizar = crearBotonPersonalizado("Analizar", "/iconos/play.png", new Color(76, 175, 80), new Color(102, 187, 106), textColor, buttonFont);
        btnAnalizar.addActionListener(e -> analizarCodigo());

        JButton btnLimpiar = crearBotonPersonalizado("Limpiar", "/iconos/delete.png", new Color(244, 67, 54), new Color(239, 83, 80), textColor, buttonFont);
        btnLimpiar.addActionListener(e -> limpiarTodo());

        // Se mantiene tu espaciado original
        toolBar.add(btnAbrir);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));
        toolBar.add(btnGuardar);
        toolBar.add(Box.createRigidArea(new Dimension(20, 0)));
        toolBar.add(btnAnalizar);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));
        toolBar.add(btnLimpiar);

        return toolBar;
    }

    // TU MÉTODO ORIGINAL, MODIFICADO PARA ACEPTAR ICONOS
    private JButton crearBotonPersonalizado(String texto, String rutaIcono, Color bgColor, Color hoverColor, Color textColor, Font font) {
        JButton button = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                // Tu lógica de dibujado se mantiene intacta
                if (getModel().isArmed()) {
                    g.setColor(hoverColor.darker());
                } else if (getModel().isRollover()) {
                    g.setColor(hoverColor);
                } else {
                    g.setColor(bgColor);
                }
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
            @Override
            protected void paintBorder(Graphics g) {
                // Sin borde
            }
        };

        // Lógica para cargar y aplicar el icono
        try {
            Image img = ImageIO.read(getClass().getResource(rutaIcono));
            button.setIcon(new ImageIcon(img.getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
        } catch (Exception ex) {
            System.err.println("No se pudo cargar icono: " + rutaIcono);
        }
        
        button.setIconTextGap(8); // Espacio entre icono y texto
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFont(font);
        button.setForeground(textColor);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JPanel crearPanelEditor() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Editor de Código"));
        editorCodigo = new RSyntaxTextArea(20, 60);
        editorCodigo.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        editorCodigo.setCodeFoldingEnabled(true);
        editorCodigo.setAntiAliasingEnabled(true);
        editorCodigo.setFont(new Font("Consolas", Font.PLAIN, 14));
        
        // Aplicamos un tema claro al editor que combine con FlatIntelliJLaf
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
            theme.apply(editorCodigo);
        } catch (IOException e) {
            System.err.println("No se pudo cargar el tema del editor.");
        }

        RTextScrollPane scroll = new RTextScrollPane(editorCodigo);
        panel.add(scroll);
        return panel;
    }

    // El resto de tus métodos (crearPanelDeTablas, analizarCodigo, etc.) no necesitan cambios
    // y se mantienen exactamente como los tenías.
    private JTabbedPane crearPanelDeTablas() {
        JTabbedPane pestañas = new JTabbedPane();
        tablaSimbolos = new JTable(new DefaultTableModel(new Object[]{"Tipo", "ID", "Método", "Valor", "Parámetro"}, 0));
        pestañas.addTab("Tabla de Símbolos", new JScrollPane(tablaSimbolos));
        tablaIntermedio = new JTable(new DefaultTableModel(new Object[]{"Operador", "Op 1", "Op 2", "Resultado"}, 0));
        pestañas.addTab("Código Intermedio", new JScrollPane(tablaIntermedio));
        return pestañas;
    }

    private JPanel crearPanelConsola() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Consola de Resultados"));
        panel.setMinimumSize(new Dimension(0, 200));
        txtResultado = new JTextArea();
        txtResultado.setEditable(false);
        txtResultado.setFont(new Font("Monospaced", Font.PLAIN, 13));
        panel.add(new JScrollPane(txtResultado), BorderLayout.CENTER);
        return panel;
    }

    private void limpiarTodo() {
        editorCodigo.setText("");
        txtResultado.setText("");
        ((DefaultTableModel) tablaSimbolos.getModel()).setRowCount(0);
        ((DefaultTableModel) tablaIntermedio.getModel()).setRowCount(0);
        barraEstado.setText("Listo.");
    }

    private void abrirArchivo(ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                editorCodigo.read(br, null);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al abrir archivo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarArchivo(ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(archivo)) {
                pw.write(editorCodigo.getText());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar archivo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void analizarCodigo() {
        String texto = editorCodigo.getText();
        txtResultado.setText("");
        DefaultTableModel modeloSimbolos = (DefaultTableModel) tablaSimbolos.getModel();
        modeloSimbolos.setRowCount(0);
        DefaultTableModel modeloCuadruplas = (DefaultTableModel) tablaIntermedio.getModel();
        modeloCuadruplas.setRowCount(0);
        List<Token> tokens = AnalizadorLexico.analizar(texto);
        AnalizadorSintactico sintactico = new AnalizadorSintactico(tokens);
        sintactico.analizar();
        List<String> erroresSintacticos = sintactico.getErrores();
        txtResultado.append("=== Análisis Sintáctico ===\n");
        if (!erroresSintacticos.isEmpty()) {
            erroresSintacticos.forEach(err -> txtResultado.append(err + "\n"));
            txtResultado.setForeground(Color.RED);
            barraEstado.setText("Errores de sintaxis encontrados.");
            return;
        } else {
            txtResultado.append("No se encontraron errores sintácticos.\n\n");
        }
        for (int i = 0; i < tokens.size(); i++) {
            Token t1 = tokens.get(i);
            if (t1.tipo == TokenType.PALABRA_RESERVADA && t1.valor.equals("Robot")) {
                if (i + 1 < tokens.size() && tokens.get(i + 1).tipo == TokenType.IDENTIFICADOR) {
                    Token t2 = tokens.get(i + 1);
                    modeloSimbolos.addRow(new Object[]{t1.tipo, t2.valor, "-", t1.valor, "Declaración"});
                    i++;
                }
            } else if (i + 4 < tokens.size()) {
                Token t2 = tokens.get(i + 1), t3 = tokens.get(i + 2), t4 = tokens.get(i + 3), t5 = tokens.get(i + 4);
                if (t1.tipo == TokenType.IDENTIFICADOR && t2.valor.equals(".") && t4.valor.equals("=") && t5.tipo == TokenType.NUMERO) {
                    String metodo = t3.valor;
                    String parametro = switch (metodo) {
                        case "base", "hombro", "codo", "garra" -> "Movimiento";
                        case "velocidad", "repetir" -> "Método";
                        default -> "-";
                    };
                    modeloSimbolos.addRow(new Object[]{t1.tipo, t1.valor, metodo, t5.valor, parametro});
                    i += 4;
                }
            } else if (t1.tipo == TokenType.DELIMITADOR && (t1.valor.equals("{") || t1.valor.equals("}"))) {
                modeloSimbolos.addRow(new Object[]{t1.tipo, "-", "-", t1.valor, t1.valor.equals("{") ? "Bloque Inicio" : "Bloque Fin"});
            }
        }
        AnalizadorSemantico semantico = new AnalizadorSemantico(tokens);
        semantico.analizar();
        List<String> erroresSemanticos = semantico.getErrores();
        txtResultado.append("\n=== Análisis Semántico ===\n");
        if (!erroresSemanticos.isEmpty()) {
            erroresSemanticos.forEach(err -> txtResultado.append(err + "\n"));
            txtResultado.setForeground(Color.RED);
            barraEstado.setText("Errores semánticos encontrados.");
            return;
        } else {
            txtResultado.append("No se encontraron errores semánticos.\n");
            txtResultado.setForeground(new Color(0, 153, 76));
        }
        GeneradorCodigoIntermedio generador = new GeneradorCodigoIntermedio(tokens);
        List<String[]> cuadruplas = generador.generar();
        for (String[] fila : cuadruplas) {
            modeloCuadruplas.addRow(fila);
        }
        try {
            txtResultado.append("\n=== Ejecutando en CoppeliaSim ===\n");
            CoppeliaSimExecutor executor = new CoppeliaSimExecutor(cuadruplas, txtResultado);
            executor.ejecutar();
            txtResultado.append("--- Secuencia completada con éxito ---\n");
            barraEstado.setText("Análisis y ejecución finalizados.");
        } catch (Exception ex) {
            txtResultado.append("ERROR al ejecutar en CoppeliaSim: " + ex.getMessage() + "\n");
            barraEstado.setText("Fallo al ejecutar en CoppeliaSim.");
        }
    }

    public static void main(String[] args) {
        try {
            // Se mantiene el tema que te gusta
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Error al cargar el tema.");
        }

        SwingUtilities.invokeLater(() -> new PruebaFormato().setVisible(true));
    }
}