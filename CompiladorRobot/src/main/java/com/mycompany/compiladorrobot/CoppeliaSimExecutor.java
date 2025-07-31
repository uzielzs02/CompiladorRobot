package com.mycompany.compiladorrobot;

import co.nstant.in.cbor.CborException;
import com.coppeliarobotics.remoteapi.zmq.RemoteAPIClient;
import com.coppeliarobotics.remoteapi.zmq.RemoteAPIObject;
import java.io.IOException;
import java.util.*;
import javax.swing.JTextArea;

public class CoppeliaSimExecutor {

    private final List<String[]> cuadruplas;
    private RemoteAPIClient client;
    private RemoteAPIObject sim;
    private final Map<String, Integer> jointHandles = new HashMap<>();
    private final JTextArea outputArea;
    private final Map<String, Integer> enteros = new HashMap<>();
    private final Map<String, Integer> etiquetas = new HashMap<>();
    private float velocidadActual = 1.0f; 

    public CoppeliaSimExecutor(List<String[]> cuadruplas, JTextArea outputArea) {
        this.cuadruplas = cuadruplas;
        this.outputArea = outputArea;
    }

    private void log(String message) {
        if (outputArea != null) {
            outputArea.append(message + "\n");
        }
        System.out.println(message);
    }

    private boolean conectar() throws Exception {
        client = new RemoteAPIClient();
        sim = client.getObject("sim");

        // --- HANDLES DE LOS MOTORES ---
        jointHandles.put("base", ((Number) sim.call("getObjectHandle", "/NiryoOne/Niryo_joint1")[0]).intValue());
        jointHandles.put("hombro", ((Number) sim.call("getObjectHandle", "/NiryoOne/Niryo_joint2")[0]).intValue());
        jointHandles.put("codo", ((Number) sim.call("getObjectHandle", "/NiryoOne/Niryo_joint3")[0]).intValue());
        jointHandles.put("garraL", ((Number) sim.call("getObjectHandle", "/NiryoLGripper/leftJoint1")[0]).intValue());
        jointHandles.put("garraR", ((Number) sim.call("getObjectHandle", "/NiryoLGripper/rightJoint1")[0]).intValue());
        
        // --- NUEVOS HANDLES PARA LA LÓGICA DE AGARRE ---
        jointHandles.put("leftSensor", ((Number) sim.call("getObjectHandle", "/NiryoLGripper/leftSensor")[0]).intValue());
        jointHandles.put("rightSensor", ((Number) sim.call("getObjectHandle", "/NiryoLGripper/rightSensor")[0]).intValue());
        jointHandles.put("attachPtA", ((Number) sim.call("getObjectHandle", "/NiryoLGripper/attachPtA")[0]).intValue());
        jointHandles.put("attachPtB", ((Number) sim.call("getObjectHandle", "/NiryoLGripper/attachPtB")[0]).intValue());
        
        log("Conectado a CoppeliaSim y handles obtenidos.");
        sim.call("setStepping", true);
        sim.call("startSimulation");
        log("Simulacion iniciada en modo sincrono.");
        return true;
    }

    public void ejecutar() throws Exception {
        if (!conectar()) {
            throw new IOException("No se pudo conectar o preparar la simulación.");
        }

        try {
            for (int i = 0; i < cuadruplas.size(); i++) {
                String[] q = cuadruplas.get(i);
                if ("Etiqueta".equals(q[0]) && q.length > 3 && q[3] != null) {
                    etiquetas.put(q[3], i);
                }
            }

            for (int i = 0; i < cuadruplas.size(); i++) {
                String[] q = cuadruplas.get(i);
                String op = q[0];

                switch (op) {
                    case "Crea":
                        log("Robot creado: " + q[3]);
                        break;
                    case "Asigna":
                        if (q.length < 4 || q[3] == null) break;
                        String destino = q[3];

                        if (destino.startsWith("cont_") || destino.startsWith("limite_") || destino.equals("condicion")) {
                            enteros.put(destino, Integer.parseInt(q[1]));
                        } else if (jointHandles.containsKey(destino) || "garra".equals(destino)) {
                            moverMotor(destino, q[1], this.velocidadActual);
                        } else if (destino.equals("velocidad")) {
                            this.velocidadActual = calcularVelocidad(q[1]);
                            log("Velocidad para el PROXIMO movimiento establecida a: " + this.velocidadActual + " rad/s");
                        }
                        break;
                    case "Compara":
                        int v1 = enteros.getOrDefault(q[1], 0);
                        int v2 = enteros.getOrDefault(q[2], 0);
                        enteros.put(q[3], (v1 < v2) ? 1 : 0);
                        break;
                    case "SaltaSiFalso":
                        int valor = enteros.getOrDefault(q[1], 0);
                        if (valor == 0) {
                            String etiquetaDestino = q[3];
                            if (etiquetas.containsKey(etiquetaDestino)) {
                                i = etiquetas.get(etiquetaDestino) - 1;
                            }
                        }
                        break;
                    case "Aumenta":
                        int actual = enteros.getOrDefault(q[1], 0);
                        int incremento = Integer.parseInt(q[2]);
                        enteros.put(q[3], actual + incremento);
                        break;
                    case "RepiteDesde":
                        String etiquetaInicio = q[3];
                        if (etiquetas.containsKey(etiquetaInicio)) {
                            i = etiquetas.get(etiquetaInicio) - 1;
                        }
                        break;
                    case "Etiqueta":
                    case "Accede":
                        break;
                }
            }
        } finally {
            desconectar();
        }
    }

    private void moverMotor(String parte, String valorStr, float velocidadRad) throws Exception {
        float targetPositionDegrees = Float.parseFloat(valorStr);
        log("Moviendo '" + parte + "' a " + targetPositionDegrees + " grados a velocidad: " + velocidadRad + " rad/s");

        if ("garra".equals(parte)) {
            // --- INICIO DE LA LÓGICA DE SENSORES Y ANCLAJE ---
            if (targetPositionDegrees <= 10) { // Lógica para CERRAR Y AGARRAR
                log("Cerrando pinza y buscando objeto para anclar...");

                // 1. Mover los dedos a la posición de cierre
                float closeRad = (float) Math.toRadians(targetPositionDegrees);
                sim.call("setJointTargetVelocity", jointHandles.get("garraR"), velocidadRad);
                sim.call("setJointTargetVelocity", jointHandles.get("garraL"), velocidadRad);
                sim.call("setJointTargetPosition", jointHandles.get("garraR"), closeRad);
                sim.call("setJointTargetPosition", jointHandles.get("garraL"), -closeRad);
                waitForMovement(jointHandles.get("garraR"), closeRad);

                // 2. Leer los sensores
                List<Object> leftSensorData = Arrays.asList(sim.call("handleProximitySensor", jointHandles.get("leftSensor")));
                List<Object> rightSensorData = Arrays.asList(sim.call("handleProximitySensor", jointHandles.get("rightSensor")));
                boolean leftDetected = ((Number) leftSensorData.get(0)).intValue() > 0;
                boolean rightDetected = ((Number) rightSensorData.get(0)).intValue() > 0;

                // 3. Comprobar si CUALQUIERA de los sensores detectó algo
                if (leftDetected || rightDetected) {
                    int detectedObjectHandle = leftDetected ? ((Number) leftSensorData.get(3)).intValue() : ((Number) rightSensorData.get(3)).intValue();
                    if (detectedObjectHandle != -1) {
                        log("¡Objeto detectado! Anclando objeto con handle: " + detectedObjectHandle);
                        // 4. Anclar el objeto (el "imán")
                        sim.call("setObjectParent", jointHandles.get("attachPtB"), detectedObjectHandle, true);
                    }
                } else {
                    log("No se detectó ningún objeto para anclar.");
                }

            } else { // Lógica para ABRIR Y SOLTAR
                log("Abriendo pinza y soltando objeto...");
                // 1. Soltar el objeto devolviendo attachPtB a la pinza
                sim.call("setObjectParent", jointHandles.get("attachPtB"), jointHandles.get("attachPtA"), true);
                
                // 2. Mover los dedos a la posición de apertura
                float openRad = (float) Math.toRadians(targetPositionDegrees);
                sim.call("setJointTargetVelocity", jointHandles.get("garraR"), velocidadRad);
                sim.call("setJointTargetVelocity", jointHandles.get("garraL"), velocidadRad);
                sim.call("setJointTargetPosition", jointHandles.get("garraR"), openRad);
                sim.call("setJointTargetPosition", jointHandles.get("garraL"), -openRad);
                waitForMovement(jointHandles.get("garraR"), openRad);
            }
            log("Acción de la garra completada.");
            // --- FIN DE LA LÓGICA DE SENSORES Y ANCLAJE ---
        } else {
            // Lógica para los otros motores (sin cambios)
            int handle = jointHandles.get(parte);
            float posicionRadianes = (float) Math.toRadians(targetPositionDegrees);
            sim.call("setJointTargetVelocity", handle, velocidadRad);
            sim.call("setJointTargetPosition", handle, posicionRadianes);
            waitForMovement(handle, posicionRadianes);
        }
    }
    
    private float calcularVelocidad(String velocidadStr) {
        int velocidad = Integer.parseInt(velocidadStr);
        return switch (velocidad) {
            case 1 -> 2.0f; case 2 -> 1.5f; case 3 -> 1.0f;
            case 4 -> 0.5f; case 5 -> 0.2f; default -> 1.0f;
        };
    }

    private void waitForMovement(int jointHandle, float targetPositionRad) throws Exception {
        log("Esperando a que el motor alcance la posicion...");
        float tolerance = 0.02f;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 20000) {
            Object[] result = sim.call("getJointPosition", jointHandle);
            if (Math.abs(((Number) result[0]).floatValue() - targetPositionRad) < tolerance) {
                log("Posicion alcanzada."); return;
            }
            sim.call("step");
        }
        log("Advertencia: Timeout alcanzado esperando el movimiento del motor.");
    }

    private void desconectar() throws CborException {
        try { if (sim != null) sim.call("stopSimulation"); }
        catch (Exception e) { log("Error al detener la simulacion: " + e.getMessage()); }
        finally { if (client != null) client.close(); log("Conexion con CoppeliaSim cerrada."); }
    }
}