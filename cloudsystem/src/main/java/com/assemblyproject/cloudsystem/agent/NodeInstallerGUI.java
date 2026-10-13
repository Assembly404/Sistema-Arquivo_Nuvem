package com.assemblyproject.cloudsystem.agent;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Instalador gráfico do agente CloudSystem (JavaFX).
 * <p>
 * Permite instalar e configurar o nó de armazenamento local
 * através de uma interface visual amigável.
 */
public class NodeInstallerGUI extends Application {

    private static final String DEFAULT_SERVER_URL = "https://cloudsystem-backend.onrender.com";
    private static final int DEFAULT_NODE_PORT = 8081;
    private static final long DEFAULT_QUOTA_GB = 100;
    private static final String NODE_JAR_NAME = "cloudsystem-node.jar";
    private static final String PROPERTIES_FILE = "node.properties";

    private NodeInstallerController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/installer/NodeInstaller.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        controller.setStage(primaryStage);
        controller.setInstaller(this);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/installer/installer.css").toExternalForm());

        primaryStage.setTitle("CloudSystem - Instalador do Nó de Armazenamento");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Baixa o JAR do agente do servidor.
     */
    public Task<File> downloadAgentJar(String serverUrl, String downloadPath) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateMessage("A conectar ao servidor...");
                URL url = new URL(serverUrl + "/nodes/jar");
                File jarFile = new File(downloadPath, NODE_JAR_NAME);

                try (InputStream in = url.openStream()) {
                    long totalBytes = url.openConnection().getContentLengthLong();
                    long downloadedBytes = 0;
                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    try (var fos = Files.newOutputStream(jarFile.toPath())) {
                        while ((bytesRead = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            downloadedBytes += bytesRead;
                            if (totalBytes > 0) {
                                updateProgress(downloadedBytes, totalBytes);
                                updateMessage(String.format("A descarregar... %.1f%%", (downloadedBytes * 100.0) / totalBytes));
                            }
                        }
                    }
                }
                updateMessage("Descarregamento concluído!");
                return jarFile;
            }
        };
    }

    /**
     * Cria o ficheiro de configuração node.properties.
     */
    public void createPropertiesFile(String serverUrl, int nodePort, String storagePath,
                                      long quotaGB, String pairingCode, String publicUrl) throws IOException {
        Properties props = new Properties();
        props.setProperty("cloud.node.server-url", serverUrl);
        props.setProperty("cloud.node.server-port", String.valueOf(nodePort));
        props.setProperty("cloud.node.storage-dir", storagePath);
        props.setProperty("cloud.node.quota-bytes", String.valueOf(quotaGB * 1024L * 1024 * 1024));
        props.setProperty("cloud.node.auto-register", "true");
        props.setProperty("cloud.node.pairing-code", pairingCode);
        if (publicUrl != null && !publicUrl.isBlank()) {
            props.setProperty("cloud.node.public-url", publicUrl);
        }

        File propsFile = new File(PROPERTIES_FILE);
        try (var fos = Files.newOutputStream(propsFile.toPath())) {
            props.store(fos, "CloudSystem Node - configuração local");
        }
    }

    /**
     * Inicia o agente como processo em background.
     */
    public Process startAgent(File jarFile) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath(), "--node.agent=true");
        pb.directory(jarFile.getParentFile());
        pb.inheritIO();
        return pb.start();
    }

    /**
     * Verifica se o Java 17+ está instalado.
     */
    public static boolean isJavaAvailable() {
        try {
            Process p = new ProcessBuilder("java", "-version").redirectErrorStream(true).start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            // Procura versão 17 ou superior
            return output.contains("version \"17") || output.contains("version \"18") ||
                   output.contains("version \"19") || output.contains("version \"20") ||
                   output.contains("version \"21") || output.contains("version \"22") ||
                   output.contains("version \"23");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Instala Java automaticamente (Linux/macOS).
     */
    public Task<Void> installJava() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("A instalar Java 17...");
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                if (os.contains("linux")) {
                    if (new File("/usr/bin/apt-get").exists()) {
                        pb = new ProcessBuilder("sudo", "apt-get", "update", "&&", "apt-get", "install", "-y", "openjdk-17-jre");
                    } else if (new File("/usr/bin/dnf").exists()) {
                        pb = new ProcessBuilder("sudo", "dnf", "install", "-y", "java-17-openjdk");
                    } else {
                        throw new IllegalStateException("Gestor de pacotes não suportado. Instale Java 17 manualmente.");
                    }
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("brew", "install", "openjdk@17");
                } else {
                    throw new IllegalStateException("Instalação automática só suportada em Linux/macOS.");
                }
                pb.inheritIO().start().waitFor();
                return null;
            }
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}