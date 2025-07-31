CompiladorRobot 🤖
Este proyecto es un compilador completo desarrollado en Java para un Lenguaje de Dominio Específico (DSL) diseñado para programar y controlar de manera sencilla un brazo robótico Niryo One dentro del entorno de simulación CoppeliaSim.

El objetivo es abstraer la complejidad de la programación robótica de bajo nivel, ofreciendo una sintaxis intuitiva que es analizada, validada y ejecutada directamente en el simulador.

Características Principales
Lenguaje de Dominio Específico (DSL): Una sintaxis simple y declarativa para definir robots, mover sus articulaciones (base, hombro, codo), controlar la garra y establecer la velocidad de los movimientos.

Fases Completas de Compilación:

Análisis Léxico: Convierte el código fuente en una secuencia de tokens.

Análisis Sintáctico: Valida que la secuencia de tokens siga las reglas gramaticales del lenguaje.

Análisis Semántico: Verifica la lógica del código, como la declaración de variables, el uso correcto de identificadores y que los valores numéricos estén dentro de los rangos permitidos para cada articulación.

Generación de Código Intermedio: Produce una representación del programa en forma de cuádruplas, facilitando su posterior interpretación y ejecución.

Interfaz Gráfica de Usuario (GUI): Creada con Java Swing y estilizada con FlatLaf para una apariencia moderna. Incluye un editor de código avanzado (RSyntaxTextArea) con resaltado de sintaxis.

Ejecución Directa en CoppeliaSim: El código intermedio es interpretado y enviado al simulador a través de su API remota (ZMQ), permitiendo ver el resultado de la programación en el brazo robótico virtual en tiempo real.
