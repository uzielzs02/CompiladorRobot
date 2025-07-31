/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.compiladorrobot;

/**
 *
 * @author ulise
 */
public class Token {
    public TokenType tipo;
    public String valor;
    public int linea;

    public Token(TokenType tipo, String valor, int linea) {
        this.tipo = tipo;
        this.valor = valor;
        this.linea = linea;
    }

    @Override
    public String toString() {
        return "<" + tipo + ", " + valor + ", línea " + linea + ">";
    }
}
