/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.compiladorrobot;
import java.util.*;
import java.util.regex.*;
/**
 *
 * @author ulise
 */
public class AnalizadorLexico {
    private static final Set<String> PALABRA_RESERVADA = Set.of(
        "base", "hombro", "codo", "garra", "Robot", "repetir", "velocidad"
    );


    private static final String[][] PATRONES = {
        {"DELIMITADOR", "[(){}\\[\\];,\\.]"},
        // ---- INICIO DE LA MODIFICACIÓN ----
        {"NUMERO", "-?\\d+"}, // Permite números negativos (ej: -45)
        {"OPERADOR", "(==|!=|<=|>=|->|\\+\\+|--|\\+=|-=|\\*=|/=|&&|\\|\\||[+\\-*/=<>!&|])"},
        // ---- FIN DE LA MODIFICACIÓN ----
        {"SIMBOLO_DESCONOCIDO", "[@#$%^&*~`´¿¡]+"},
        {"IDENTIFICADOR", "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b"}
    };

public static List<Token> analizar(String entrada) {
    List<Token> tokens = new ArrayList<>();
    if (entrada == null || entrada.strip().isEmpty()) return tokens;

    String[] lineas = entrada.split("\n");
    int numeroLinea = 1;

    for (String linea : lineas) {
        String input = linea.strip();
        while (!input.isEmpty()) {
            input = input.stripLeading();
            if (input.isEmpty()) break;

            boolean matched = false;

            for (String[] patron : PATRONES) {
                Pattern pattern = Pattern.compile("^" + patron[1]);
                Matcher matcher = pattern.matcher(input);
                if (matcher.find()) {
                    String lexema = matcher.group();
                    TokenType tipo;

                    if (patron[0].equals("IDENTIFICADOR") && PALABRA_RESERVADA.contains(lexema)) {
                        tipo = TokenType.PALABRA_RESERVADA;
                    } else {
                        tipo = TokenType.valueOf(patron[0]);
                    }

                    tokens.add(new Token(tipo, lexema, numeroLinea));
                    input = input.substring(lexema.length());
                    matched = true;
                    break;
                }
            }

            if (!matched && !input.isEmpty()) {
                tokens.add(new Token(TokenType.ERROR, String.valueOf(input.charAt(0)), numeroLinea));
                input = input.substring(1);
            }
        }
        numeroLinea++; // siguiente línea
    }

    return tokens;
}


}