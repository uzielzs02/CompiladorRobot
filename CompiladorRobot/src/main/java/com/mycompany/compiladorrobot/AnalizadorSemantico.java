/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.compiladorrobot;

import java.util.*;

/**
 *
 * @author ulise
 */
public class AnalizadorSemantico {
    private final List<Token> tokens;
    private final Set<String> identificadoresDeclarados = new HashSet<>();
    private final List<String> errores = new ArrayList<>();
    private final Set<Integer> lineasConError = new HashSet<>();

    private static final Map<String, int[]> RANGOS = Map.of(
        "base", new int[]{-360, 360},
        "hombro", new int[]{-90, 90},
        "codo", new int[]{-90, 179},
        "garra", new int[]{0, 45}
    );

    public AnalizadorSemantico(List<Token> tokens) {
        this.tokens = tokens;
    }

public void analizar() {
    List<Token> tokensExpandido = new ArrayList<>();

    for (int i = 0; i < tokens.size(); i++) {
        Token t = tokens.get(i);

        // Manejo de bloques repetir
        if (t.tipo == TokenType.IDENTIFICADOR &&
            i + 5 < tokens.size() &&
            tokens.get(i + 1).tipo == TokenType.DELIMITADOR && tokens.get(i + 1).valor.equals(".") &&
            tokens.get(i + 2).tipo == TokenType.PALABRA_RESERVADA && tokens.get(i + 2).valor.equals("repetir") &&
            tokens.get(i + 3).tipo == TokenType.OPERADOR && tokens.get(i + 3).valor.equals("=") &&
            tokens.get(i + 4).tipo == TokenType.NUMERO &&
            tokens.get(i + 5).tipo == TokenType.DELIMITADOR && tokens.get(i + 5).valor.equals("{")) {

            String robot = t.valor;
            int repeticiones = Integer.parseInt(tokens.get(i + 4).valor);
            int linea = t.linea;

            if (repeticiones < 1 || repeticiones > 100) {
                errores.add("❌ Error semántico en línea " + linea +
                            ": El número de repeticiones debe estar entre 1 y 100. Valor actual: " + repeticiones);
                lineasConError.add(linea);
                continue;
            }

            // Agrega cabecera repetir + llave abierta
            for (int k = i; k <= i + 5; k++) {
                tokensExpandido.add(tokens.get(k));
            }

            int j = i + 6;
            List<List<Token>> instrucciones = new ArrayList<>();

            while (j < tokens.size() &&
                   !(tokens.get(j).tipo == TokenType.DELIMITADOR && tokens.get(j).valor.equals("}"))) {

                List<Token> instruccion = new ArrayList<>();

                if (j + 4 < tokens.size()) {
                    Token id = tokens.get(j);
                    Token punto = tokens.get(j + 1);
                    Token parte = tokens.get(j + 2);
                    Token igual = tokens.get(j + 3);
                    Token valor = tokens.get(j + 4);

                    if (id.tipo == TokenType.IDENTIFICADOR &&
                        punto.tipo == TokenType.DELIMITADOR && punto.valor.equals(".") &&
                        parte.tipo == TokenType.PALABRA_RESERVADA &&
                        igual.tipo == TokenType.OPERADOR && igual.valor.equals("=") &&
                        valor.tipo == TokenType.NUMERO) {

                        instruccion.add(id);
                        instruccion.add(punto);
                        instruccion.add(parte);
                        instruccion.add(igual);
                        instruccion.add(valor);
                        j += 5;

                        if (parte.valor.matches("base|hombro|codo|garra")) {
                            if (j + 4 < tokens.size()) {
                                Token vid = tokens.get(j);
                                Token vpunto = tokens.get(j + 1);
                                Token vinst = tokens.get(j + 2);
                                Token vigual = tokens.get(j + 3);
                                Token vvalor = tokens.get(j + 4);

                                if (vid.tipo == TokenType.IDENTIFICADOR && vid.valor.equals(id.valor) &&
                                    vpunto.tipo == TokenType.DELIMITADOR && vpunto.valor.equals(".") &&
                                    vinst.tipo == TokenType.PALABRA_RESERVADA && vinst.valor.equals("velocidad") &&
                                    vigual.tipo == TokenType.OPERADOR && vigual.valor.equals("=") &&
                                    vvalor.tipo == TokenType.NUMERO) {

                                    instruccion.add(vid);
                                    instruccion.add(vpunto);
                                    instruccion.add(vinst);
                                    instruccion.add(vigual);
                                    instruccion.add(vvalor);
                                    j += 5;
                                } else {
                                    errores.add("Error semántico en línea " + id.linea +
                                            ": Se esperaba una instrucción de velocidad inmediatamente después de '" + parte.valor + "'");
                                    lineasConError.add(id.linea);
                                }
                            } else {
                                errores.add("Error semántico en línea " + id.linea +
                                        ": Se esperaba una instrucción de velocidad inmediatamente después de '" + parte.valor + "'");
                                lineasConError.add(id.linea);
                            }
                        }

                        instrucciones.add(instruccion);
                    } else {
                        j++;
                    }
                } else {
                    j++;
                }
            }

            for (int r = 0; r < repeticiones; r++) {
                for (List<Token> instr : instrucciones) {
                    tokensExpandido.addAll(instr);
                }
            }

            // Agrega la llave de cierre
            if (j < tokens.size()) {
                tokensExpandido.add(tokens.get(j));
            }
            i = j;
        } else {
            tokensExpandido.add(t);
        }
    }

    // Validación semántica sobre tokensExpandido
    for (int i = 0; i < tokensExpandido.size(); i++) {
        Token actual = tokensExpandido.get(i);

        if (actual.tipo == TokenType.PALABRA_RESERVADA && actual.valor.equals("Robot")) {
            if (i + 1 < tokensExpandido.size() && tokensExpandido.get(i + 1).tipo == TokenType.IDENTIFICADOR) {
                String id = tokensExpandido.get(i + 1).valor;
                int linea = tokensExpandido.get(i + 1).linea;
                if (identificadoresDeclarados.contains(id)) {
                    errores.add("Error semántico en línea " + linea + ": El identificador '" + id + "' ya fue declarado.");
                    lineasConError.add(linea);
                } else {
                    identificadoresDeclarados.add(id);
                }
            }
        }

        if (actual.tipo == TokenType.IDENTIFICADOR &&
            i + 4 < tokensExpandido.size() &&
            tokensExpandido.get(i + 1).valor.equals(".") &&
            tokensExpandido.get(i + 2).tipo == TokenType.PALABRA_RESERVADA &&
            tokensExpandido.get(i + 3).valor.equals("=") &&
            tokensExpandido.get(i + 4).tipo == TokenType.NUMERO) {

            String parte = tokensExpandido.get(i + 2).valor;
            int valor = Integer.parseInt(tokensExpandido.get(i + 4).valor);
            int linea = actual.linea;

            if (RANGOS.containsKey(parte)) {
                int[] rango = RANGOS.get(parte);
                if (valor < rango[0] || valor > rango[1]) {
                    errores.add("Error semántico en línea " + linea + ": El valor para '" + parte +
                            "' debe estar entre " + rango[0] + " y " + rango[1] + ". Valor dado: " + valor);
                    lineasConError.add(linea);
                }

                // Validar velocidad inmediatamente después
                if (i + 9 < tokensExpandido.size()) {
                    Token vid = tokensExpandido.get(i + 5);
                    Token vpunto = tokensExpandido.get(i + 6);
                    Token vinst = tokensExpandido.get(i + 7);
                    Token vigual = tokensExpandido.get(i + 8);
                    Token vvalor = tokensExpandido.get(i + 9);

                    if (!(vid.tipo == TokenType.IDENTIFICADOR && vid.valor.equals(actual.valor) &&
                          vpunto.tipo == TokenType.DELIMITADOR && vpunto.valor.equals(".") &&
                          vinst.tipo == TokenType.PALABRA_RESERVADA && vinst.valor.equals("velocidad") &&
                          vigual.tipo == TokenType.OPERADOR && vigual.valor.equals("=") &&
                          vvalor.tipo == TokenType.NUMERO)) {

                        errores.add("Error semántico en línea " + linea +
                                ": Se esperaba una instrucción de velocidad inmediatamente después de '" + parte + "'");
                        lineasConError.add(linea);
                    }
                } else {
                    errores.add("Error semántico en línea " + linea +
                            ": Se esperaba una instrucción de velocidad inmediatamente después de '" + parte + "'");
                    lineasConError.add(linea);
                }
            } else if (parte.equals("velocidad")) {
                if (valor < 1 || valor > 5) {
                    errores.add("Error semántico en línea " + linea +
                            ": La velocidad debe estar entre 1 y 5. Valor dado: " + valor);
                    lineasConError.add(linea);
                }
            }
            
        }
    }
}


public List<String> getErrores() {
    return errores;
}

public Set<Integer> getLineasConError() {
    return lineasConError;
}

}