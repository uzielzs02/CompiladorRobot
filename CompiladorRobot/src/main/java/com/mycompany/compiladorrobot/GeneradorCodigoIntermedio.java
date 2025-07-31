package com.mycompany.compiladorrobot;

import java.util.*;

public class GeneradorCodigoIntermedio {

    private final List<Token> tokens;
    private final List<String[]> cuadruplas = new ArrayList<>();
    private int tempCounter = 1;
    private int etiquetaCounter = 1;

    public GeneradorCodigoIntermedio(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<String[]> generar() {
        int i = 0;
        Stack<String> pilaEtiquetasInicio = new Stack<>();
        Stack<String> pilaEtiquetasFin = new Stack<>();
        Stack<String> pilaContadores = new Stack<>();
        Stack<String> pilaLimites = new Stack<>();

        while (i < tokens.size()) {
            Token t = tokens.get(i);

            // Instrucción: Robot r1
            if (t.tipo == TokenType.PALABRA_RESERVADA && t.valor.equals("Robot")) {
                if (i + 1 < tokens.size()) {
                    String id = tokens.get(i + 1).valor;
                    cuadruplas.add(new String[]{"Crea", "Robot", "—", id});
                    i += 2;
                } else {
                    i++;
                }
            }

            // Instrucción: r1.repetir = N {
            else if (i + 5 < tokens.size()
                    && tokens.get(i).tipo == TokenType.IDENTIFICADOR
                    && tokens.get(i + 1).valor.equals(".")
                    && tokens.get(i + 2).valor.equals("repetir")
                    && tokens.get(i + 3).valor.equals("=")
                    && tokens.get(i + 4).tipo == TokenType.NUMERO
                    && tokens.get(i + 5).valor.equals("{")) {

                String robot = tokens.get(i).valor;
                String limite = tokens.get(i + 4).valor;
                String cont = "cont_" + robot + "_" + etiquetaCounter;
                String lim = "limite_" + robot + "_" + etiquetaCounter;
                String etiquetaInicio = "REP_INICIO_" + etiquetaCounter;
                String etiquetaFin = "REP_FIN_" + etiquetaCounter;
                etiquetaCounter++;

                pilaContadores.push(cont);
                pilaLimites.push(lim);
                pilaEtiquetasInicio.push(etiquetaInicio);
                pilaEtiquetasFin.push(etiquetaFin);

                cuadruplas.add(new String[]{"Asigna", limite, "—", lim});
                cuadruplas.add(new String[]{"Asigna", "0", "—", cont});
                cuadruplas.add(new String[]{"Etiqueta", "—", "—", etiquetaInicio});
                cuadruplas.add(new String[]{"Compara", cont, lim, "condicion"});
                cuadruplas.add(new String[]{"SaltaSiFalso", "condicion", "—", etiquetaFin});

                i += 6;
            }


            else if (i + 9 < tokens.size() &&
                     // Valida la instrucción de movimiento
                     tokens.get(i).tipo == TokenType.IDENTIFICADOR &&
                     tokens.get(i + 1).valor.equals(".") &&
                     tokens.get(i + 2).valor.matches("base|hombro|codo|garra") &&
                     tokens.get(i + 3).valor.equals("=") &&
                     tokens.get(i + 4).tipo == TokenType.NUMERO &&
                     // Valida la instrucción de velocidad que le sigue
                     tokens.get(i + 5).tipo == TokenType.IDENTIFICADOR &&
                     tokens.get(i + 5).valor.equals(tokens.get(i).valor) && // Mismo robot
                     tokens.get(i + 6).valor.equals(".") &&
                     tokens.get(i + 7).valor.equals("velocidad") &&
                     tokens.get(i + 8).valor.equals("=") &&
                     tokens.get(i + 9).tipo == TokenType.NUMERO) {

                String robot = tokens.get(i).valor;
                String parte = tokens.get(i + 2).valor;
                String valorMovimiento = tokens.get(i + 4).valor;
                String valorVelocidad = tokens.get(i + 9).valor;


                cuadruplas.add(new String[]{"Asigna", valorVelocidad, "—", "velocidad"});

                String temp = "t" + tempCounter++;
                cuadruplas.add(new String[]{"Accede", robot, parte, temp});
                cuadruplas.add(new String[]{"Asigna", valorMovimiento, "—", parte});

                i += 10;
            }

            else if (t.tipo == TokenType.DELIMITADOR && t.valor.equals("}")) {
                if (!pilaContadores.isEmpty()) {
                    String cont = pilaContadores.pop();
                    String etiquetaInicio = pilaEtiquetasInicio.pop();
                    String etiquetaFin = pilaEtiquetasFin.pop();

                    cuadruplas.add(new String[]{"Aumenta", cont, "1", cont});
                    cuadruplas.add(new String[]{"RepiteDesde", "—", "—", etiquetaInicio});
                    cuadruplas.add(new String[]{"Etiqueta", "—", "—", etiquetaFin});
                }
                i++;
            }

            else {
                i++;
            }
        }
        return cuadruplas;
    }
}