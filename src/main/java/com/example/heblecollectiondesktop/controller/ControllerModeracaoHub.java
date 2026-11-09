package com.example.heblecollectiondesktop.controller;

import java.io.IOException;
import java.net.URL;

import com.example.heblecollectiondesktop.model.Funcionario;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;

public class ControllerModeracaoHub {

    private Pane containerCentral;
    private Funcionario funcionarioLogado;

    public void setContainerCentral(Pane containerCentral) {
        this.containerCentral = containerCentral;
    }

    public void setFuncionarioLogado(Funcionario funcionarioLogado) {
        this.funcionarioLogado = funcionarioLogado;
    }

    private void carregarSubVisao(String fxmlPath) {
        if (containerCentral == null) {
            mostrarAlerta("Erro de Navegação", "O container central não foi configurado corretamente.");
            return;
        }

        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                url = getClass().getResource("/view/" + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1));
            }

            if (url == null) {
                mostrarAlerta("Erro FXML", "Não foi possível encontrar o arquivo: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent novaVisao = loader.load();

            Object controller = loader.getController();

            if (controller instanceof ControllerGerenciarFuncionarios) {
                ControllerGerenciarFuncionarios cgf = (ControllerGerenciarFuncionarios) controller;
                cgf.setContainerCentral(containerCentral);
                cgf.setFuncionarioLogado(funcionarioLogado);
            } else if (controller instanceof ControllerGerenciarEmpresas) {
                ControllerGerenciarEmpresas cge = (ControllerGerenciarEmpresas) controller;
                cge.setContainerCentral(containerCentral);
                cge.setFuncionarioLogado(funcionarioLogado);
            } else if (controller instanceof ControllerLogsModeracao) {
                ControllerLogsModeracao clm = (ControllerLogsModeracao) controller;
                clm.setContainerCentral(containerCentral);
                clm.setFuncionarioLogado(funcionarioLogado);
            }

            containerCentral.getChildren().setAll(novaVisao);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro de Carregamento", "Falha ao carregar a sub-tela: " + e.getMessage());
        }
    }

    @FXML
    private void abrirGerenciarFuncionarios(ActionEvent event) {
        carregarSubVisao("/com/example/heblecollectiondesktop/view/gerenciarFuncionarios.fxml");
    }

    @FXML
    private void handleGerenciarFuncionarios(ActionEvent event) {
        abrirGerenciarFuncionarios(event);
    }

    @FXML
    private void abrirGerenciarEmpresas(ActionEvent event) {
        carregarSubVisao("/com/example/heblecollectiondesktop/view/GerenciarEmpresas.fxml");
    }

    @FXML
    private void handleGerenciarEmpresas(ActionEvent event) {
        abrirGerenciarEmpresas(event);
    }

    @FXML
    private void abrirLogsModeracao(ActionEvent event) {
        carregarSubVisao("/com/example/heblecollectiondesktop/view/logsModeracao.fxml");
    }

    @FXML
    private void handleLogsModeracao(ActionEvent event) {
        abrirLogsModeracao(event);
    }

    @FXML
    private void voltarDashboard(ActionEvent event) {
        carregarSubVisao("/com/example/heblecollectiondesktop/view/dashboardContent.fxml");
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
} 