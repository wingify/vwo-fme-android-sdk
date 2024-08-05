package com.vwo.interfaces;

import com.vwo.VWO;

import org.jetbrains.annotations.NotNull;

/**
 * Callback interface to receive notifications about the VWO initialization process.
 * <p>
 * <b>Usage:</b>
 * <p>
 * <b>Kotlin:</b>
 * <p><pre>
 *    val callback = object : IVwoInitCallback {
 *       override fun vwoInitSuccess(message: String) {
 *           println("VWO initialization successful: $message")
 *       }
 *
 *       override fun vwoInitFailed(message: String) {
 *           println("VWO initialization failed: $message")
 *       }
 *    }
 *    </pre>
 * </p>
 * <b>Java:</b>
 * <p><pre>
 *    IVwoInitCallback callback = new IVwoInitCallback() {
 *
 *       public void vwoInitSuccess(String message) {
 *           System.out.println("VWO initialization successful: " + message);
 *       }
 *
 *       public void vwoInitFailed(String message) {
 *           System.out.println("VWO initialization failed: " + message);
 *       }
 *    };
 * </pre>
 * </p>
 */
public interface IVwoInitCallback {
    /**
     * Called when VWO initialization is successful.
     * @param message String message
     */
    void vwoInitSuccess(@NotNull VWO vwo, @NotNull String message);

    /**
     * Called when VWO initialization is failed.
     * <p>
     * Most common reason for failure is device unable to connect to Internet.
     *
     * @param message String message
     */
    void vwoInitFailed(@NotNull String message);
}