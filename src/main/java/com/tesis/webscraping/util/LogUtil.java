package com.tesis.webscraping.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import org.slf4j.MDC;

/**
 * Utilidad central para logging. Proporciona métodos estáticos para
 * loggear de forma concisa desde cualquier clase. No crea logger en cada llamada
 * si prefieres usar logger estático, también se incluye getLogger(Class).
 */
public final class LogUtil {

    private LogUtil() {
        // util class
    }

    // Obtiene un logger para la clase indicada (convención estándar)
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    // Métodos estáticos convenientes
    public static void trace(Class<?> clazz, String msg, Object... args) {
        getLogger(clazz).trace(msg, args);
    }

    public static void debug(Class<?> clazz, String msg, Object... args) {
        getLogger(clazz).debug(msg, args);
    }

    public static void info(Class<?> clazz, String msg, Object... args) {
        getLogger(clazz).info(msg, args);
    }

    public static void warn(Class<?> clazz, String msg, Object... args) {
        getLogger(clazz).warn(msg, args);
    }

    public static void error(Class<?> clazz, String msg, Object... args) {
        getLogger(clazz).error(msg, args);
    }

    // overloads para excepciones (preserva stacktrace)
    public static void error(Class<?> clazz, String msg, Throwable t) {
        getLogger(clazz).error(msg, t);
    }

    public static void warn(Class<?> clazz, String msg, Throwable t) {
        getLogger(clazz).warn(msg, t);
    }

    // Simple helpers para MDC (contexto por request/hilo)
    public static void putMdc(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }

    public static void removeMdc(String key) {
        if (key != null) {
            MDC.remove(key);
        }
    }

    public static void clearMdc() {
        MDC.clear();
    }

    /**
     * Ejecuta un Runnable con un mapa de valores en MDC y los limpia al terminar.
     * Útil para ejecutar tareas en hilos y que queden loggeadas con contexto.
     */
    public static void runWithMdc(Map<String, String> context, Runnable task) {
        if (context != null) {
            context.forEach(MDC::put);
        }
        try {
            task.run();
        } finally {
            if (context != null) {
                context.keySet().forEach(MDC::remove);
            }
        }
    }
}
