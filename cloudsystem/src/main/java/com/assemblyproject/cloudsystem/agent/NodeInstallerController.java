package com.assemblyproject.cloudsystem.agent;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;

/**
 * Controlador da interface do instalador gráfico.
 */
public class NodeInstallerController {

    @FXML private VBox rootPane;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private TextField pairingCodeField;
    @FXML private TextField serverUrlField;
    @FXML private TextField publicUrlField;
    @FXML private TextField storagePathField;
    @FXML private TextField portField;
    @FXML private Spinner<Integer> quotaSpinner;
    @FXML private CheckBox autoTunnelCheck;
    @FXML private Button browseStorageBtn;
    @FXML private Button installBtn;
    @FXML private Button cancelBtn;
    @FXML private Button startTunnelBtn;
    @FXML private TextArea logArea;
    @FXML private Hyperlink helpLink;

    private Stage stage;
    private NodeInstallerGUI installer;
    private Process agentProcess;
    private Process tunnelProcess;
    private Timeline tunnelLogTail;

    @FXML
    public void initialize() {
        // Valores padrão
        serverUrlField.setText("https://cloudsystem-backend.onrender.com");
        portField.setText("8081");
        storagePathField.setText(System.getProperty("user.home") + "/cloudstorage");
        quotaSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 100));

        // Estado inicial
        progressBar.setProgress(-1); // indeterminate
        progressBar.setVisible(false);
        progressLabel.setVisible(false);

        // Ações
        browseStorageBtn.setOnAction(this::browseStorage);
        installBtn.setOnAction(this::startInstallation);
        cancelBtn.setOnAction(e -> Platform.exit());
        startTunnelBtn.setOnAction(this::startCloudflareTunnel);
        helpLink.setOnAction(e -> showHelp());

        // Validação em tempo real
        pairingCodeField.textProperty().addListener((obs, old, val) -> validateForm());
        storagePathField.textProperty().addListener((obs, old, val) -> validateForm());
        validateForm();
    }

    public void setStage(Stage stage) { this.stage = stage; }
    public void setInstaller(NodeInstallerGUI installer) { this.installer = installer; }

    private void validateForm() {
        boolean valid = !pairingCodeField.getText().isBlank()
                && !storagePathField.getText().isBlank()
                && new File(storagePathField.getText()).mkdirs();
        installBtn.setDisable(!valid);
    }

    private void browseStorage(ActionEvent e) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Escolher pasta para armazenamento");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            storagePathField.setText(dir.getAbsolutePath());
        }
    }

    private void startInstallation(ActionEvent e) {
        String pairingCode = pairingCodeField.getText().trim();
        String serverUrl = serverUrlField.getText().trim();
        String publicUrl = publicUrlField.getText().trim();
        String storagePath = storagePathField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        int quotaGB = quotaSpinner.getValue();

        if (pairingCode.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Código de pareamento obrigatório");
            return;
        }

        // Verificar Java
        if (!NodeInstallerGUI.isJavaAvailable()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Java não encontrado");
            alert.setHeaderText("Java 17+ não está instalado");
            alert.setContentText("Deseja instalar automaticamente (requer sudo/admin)?");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                runJavaInstall();
                return;
            }
            return;
        }

        // Iniciar instalação
        setInstalling(true);
        appendLog("Iniciando instalação do nó CloudSystem...");

        // 1. Baixar JAR
        String downloadDir = new File(".").getAbsolutePath();
        Task<File> downloadTask = installer.downloadAgentJar(serverUrl, downloadDir);
        downloadTask.progressProperty().addListener((obs, old, val) -> {
            Platform.runLater(() -> {
                progressBar.setProgress(val.doubleValue());
                progressLabel.setText(String.format("%.0f%%", val.doubleValue() * 100));
            });
        });
        downloadTask.messageProperty().addListener((obs, old, val) -> Platform.runLater(() -> appendLog(val)));
        downloadTask.setOnSucceeded(event -> onDownloadSucceeded(event));
        downloadTask.setOnFailed(event -> onTaskFailed(event));

        new Thread(downloadTask).start();
    }

    private void onDownloadSucceeded(WorkerStateEvent e) {
        Task<File> task = (Task<File>) e.getSource();
        File jarFile = task.getValue();
        appendLog("Agente descarregado: " + jarFile.getName() + " (" + (jarFile.length() / 1024 / 1024) + " MB)");

        try {
            // 2. Criar configuração
            installer.createPropertiesFile(
                    serverUrlField.getText().trim(),
                    Integer.parseInt(portField.getText().trim()),
                    storagePathField.getText().trim(),
                    quotaSpinner.getValue(),
                    pairingCodeField.getText().trim(),
                    publicUrlField.getText().trim()
            );
            appendLog("Configuração guardada em node.properties");

            // 3. Se public-url não definida e auto-tunnel ativo, criar túnel
            if (publicUrlField.getText().trim().isEmpty() && autoTunnelCheck.isSelected()) {
                appendLog("A criar túnel Cloudflare automaticamente...");
                startCloudflareTunnelAuto(jarFile);
            } else {
                // 4. Iniciar agente
                startAgentProcess(jarFile);
            }
        } catch (Exception ex) {
            onError("Erro ao criar configuração: " + ex.getMessage());
        }
    }

    private void startCloudflareTunnelAuto(File jarFile) {
        try {
            // Verificar se cloudflared existe
            ProcessBuilder check = new ProcessBuilder("cloudflared", "--version");
            check.redirectErrorStream(true);
            Process p = check.start();
            p.waitFor();
            if (p.exitValue() != 0) {
                appendLog("cloudflared não encontrado. Instale em: https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation");
                startAgentProcess(jarFile);
                return;
            }

            // Iniciar túnel
            tunnelProcess = new ProcessBuilder("cloudflared", "tunnel", "--url", "http://localhost:" + portField.getText().trim())
                    .redirectErrorStream(true).start();

            // Capturar URL do túnel
            tunnelLogTail = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                try {
                    String output = new String(tunnelProcess.getInputStream().readAllBytes());
                    int idx = output.indexOf("https://");
                    if (idx >= 0) {
                        int end = output.indexOf(' ', idx);
                        if (end < 0) end = output.length();
                        String url = output.substring(idx, end).trim();
                        Platform.runLater(() -> {
                            publicUrlField.setText(url);
                            appendLog("Túnel ativo: " + url);
                            tunnelLogTail.stop();
                            startAgentProcess(jarFile);
                        });
                    }
                } catch (Exception ignored) {}
            }));
            tunnelLogTail.setCycleCount(Timeline.INDEFINITE);
            tunnelLogTail.play();

        } catch (Exception ex) {
            appendLog("Erro ao criar túnel: " + ex.getMessage());
            startAgentProcess(jarFile);
        }
    }

    private void startCloudflareTunnel(ActionEvent e) {
        try {
            if (tunnelProcess != null && tunnelProcess.isAlive()) {
                tunnelProcess.destroy();
                appendLog("Túnel anterior parado.");
            }
            tunnelProcess = new ProcessBuilder("cloudflared", "tunnel", "--url", "http://localhost:" + portField.getText().trim())
                    .redirectErrorStream(true).start();
            appendLog("Túnel iniciado. Verifique o log para a URL.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erro ao iniciar túnel: " + ex.getMessage());
        }
    }

    private void startAgentProcess(File jarFile) {
        try {
            appendLog("A iniciar agente na porta " + portField.getText() + "...");
            agentProcess = installer.startAgent(jarFile);
            appendLog("Agente iniciado! (PID: " + agentProcess.pid() + ")");
            appendLog("Endpoint local: http://localhost:" + portField.getText() + "/nodes/health");

            Platform.runLater(() -> {
                progressBar.setVisible(false);
                progressLabel.setVisible(false);
                statusLabel.setText("Agente a correr. Pode fechar esta janela.");
                installBtn.setDisable(true);
                cancelBtn.setText("Fechar");
            });

        } catch (Exception ex) {
            onError("Erro ao iniciar agente: " + ex.getMessage());
        }
    }

    private void runJavaInstall() {
        setInstalling(true);
        Task<Void> javaTask = installer.installJava();
        javaTask.setOnSucceeded(ev -> {
            appendLog("Java instalado com sucesso!");
            startInstallation(null);
        });
        javaTask.setOnFailed(ev -> onTaskFailed(ev));
        new Thread(javaTask).start();
    }

    private void onTaskFailed(WorkerStateEvent event) {
        Task<?> task = (Task<?>) event.getSource();
        Throwable ex = task.getException();
        onError(ex != null ? ex.getMessage() : "Erro desconhecido");
    }

    private void onError(String msg) {
        appendLog("ERRO: " + msg);
        showAlert(Alert.AlertType.ERROR, msg);
        setInstalling(false);
    }

    private void setInstalling(boolean installing) {
        Platform.runLater(() -> {
            progressBar.setVisible(installing);
            progressLabel.setVisible(installing);
            progressBar.setProgress(installing ? -1 : 0);
            installBtn.setDisable(installing);
            pairingCodeField.setDisable(installing);
            serverUrlField.setDisable(installing);
            publicUrlField.setDisable(installing);
            storagePathField.setDisable(installing);
            portField.setDisable(installing);
            quotaSpinner.setDisable(installing);
            autoTunnelCheck.setDisable(installing);
            browseStorageBtn.setDisable(installing);
            startTunnelBtn.setDisable(installing);
        });
    }

    private void appendLog(String msg) {
        Platform.runLater(() -> {
            logArea.appendText("[" + java.time.LocalTime.now().toString().substring(0,8) + "] " + msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ajuda - Instalador CloudSystem");
        alert.setHeaderText("Como obter o código de pareamento");
        alert.setContentText("""
                1. Acesse o CloudSystem no navegador
                2. Vá em: Definições → Dispositivos → Adicionar dispositivo
                3. Copie o código gerado (ex: A7K9M2P4)
                4. Cole no campo "Código de Pareamento"
                
                Sobre o túnel (--public-url):
                • Necessário se o servidor está na nuvem e este PC atrás de router (NAT)
                • O Cloudflare Tunnel (cloudflared) cria uma URL HTTPS pública grátis
                • Instale: https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation
                • Marque "Criar túnel automaticamente" para configurar tudo sozinho
                
                Quota: espaço em GB que este dispositivo disponibiliza (0 = ilimitado)
                """);
        alert.showAndWait();
    }
}