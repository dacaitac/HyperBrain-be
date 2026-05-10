package com.hyperbrain.sopfc.it.util;

import java.net.Socket;
import org.junit.jupiter.api.Assumptions;

public class ServiceAssumptions {

    /**
     * Verifica si un servicio está escuchando en un host y puerto específicos.
     * Si no está disponible, el test se saltará (skipped) en lugar de fallar.
     */
    public static void assumeServiceIsActive(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            // El servicio está activo si podemos abrir el socket
        } catch (Exception e) {
            Assumptions.abort("⚠️ El servicio en " + host + ":" + port + " no está activo. Saltando test.");
        }
    }

    public static void assumeAppleServicesAreActive() {
        assumeServiceIsActive("127.0.0.1", 1995);
    }
}
