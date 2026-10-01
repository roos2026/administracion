package com.academia;

/**
 * Punto de entrada Launcher para evitar el error de componentes de JavaFX
 * al empaquetar y ejecutar archivos JAR ejecutables.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
