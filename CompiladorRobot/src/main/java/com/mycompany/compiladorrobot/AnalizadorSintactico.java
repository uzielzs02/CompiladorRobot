/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.compiladorrobot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 *
 * @author ulise
 */


public class AnalizadorSintactico {

    private final List<Token> tokens;
    private int posicion;
    private final List<String> errores = new ArrayList<>();
    private final Set<Integer> lineasConError = new HashSet<>();

    public AnalizadorSintactico(List<Token> tokens) {
        this.tokens = tokens;
        this.posicion = 0;
    }

    public void analizar() {
        while (posicion < tokens.size()) {
            int inicio = posicion;
            int lineaActual = tokens.get(posicion).linea;

            if (!esInstruccionValida()) {
                Token t = (posicion < tokens.size()) ? tokens.get(posicion) : tokens.get(tokens.size() - 1);
                errores.add("Error en línea " + lineaActual + ": Error de sintaxis");
                lineasConError.add(lineaActual);
                avanzarLinea(inicio);
            }
        }
    }

    private boolean esInstruccionValida() {
        // Declaración: Robot r1
        if (match(TokenType.PALABRA_RESERVADA, "Robot")) {
            return match(TokenType.IDENTIFICADOR);
        }

        // Bloque de repetición: r1.repetir = N {
        if (match(TokenType.IDENTIFICADOR)) {
            if (match(TokenType.DELIMITADOR, ".") &&
                match(TokenType.PALABRA_RESERVADA, "repetir") &&
                match(TokenType.OPERADOR, "=") &&
                match(TokenType.NUMERO) &&
                match(TokenType.DELIMITADOR, "{")) {

                // Validar instrucciones internas hasta encontrar }
                while (posicion < tokens.size() &&
                       !(tokens.get(posicion).tipo == TokenType.DELIMITADOR &&
                         tokens.get(posicion).valor.equals("}"))) {

                    int inicioBloque = posicion;
                    if (!esInstruccionValida()) {
                        Token t = (posicion < tokens.size()) ? tokens.get(posicion) : tokens.get(tokens.size() - 1);
                        errores.add("Error en línea " + t.linea + ": Error de sintaxis dentro del bloque 'repetir'");
                        lineasConError.add(t.linea);
                        avanzarLinea(inicioBloque);
                    }
                }

                return match(TokenType.DELIMITADOR, "}");
            }

            posicion--; 
            if (match(TokenType.DELIMITADOR, ".")) {
                if (match(TokenType.PALABRA_RESERVADA, "base", "hombro", "codo", "garra", "velocidad")) {
                    if (match(TokenType.OPERADOR, "=")) {
                        return match(TokenType.NUMERO);
                    }
                }
            }
        }

        return false;
    }


    private boolean match(TokenType tipo, String... lexemas) {
        if (posicion >= tokens.size()) return false;
        Token actual = tokens.get(posicion);
        if (actual.tipo == tipo) {
            if (lexemas.length == 0 || contiene(lexemas, actual.valor)) {
                posicion++;
                return true;
            }
        }
        return false;
    }

    private boolean contiene(String[] lista, String valor) {
        for (String s : lista) {
            if (s.equalsIgnoreCase(valor)) return true;
        }
        return false;
    }

    private void avanzarLinea(int desde) {
        int lineaActual = tokens.get(desde).linea;
        while (posicion < tokens.size() && tokens.get(posicion).linea == lineaActual) {
            posicion++;
        }
    }

    public List<String> getErrores() {
        return errores;
    }

    public Set<Integer> getLineasConError() {
        return lineasConError;
    }
}