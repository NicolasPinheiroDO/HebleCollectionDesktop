package com.example.heblecollectiondesktop.controller;

import com.example.heblecollectiondesktop.database.ConsumidoresDAO;
import com.example.heblecollectiondesktop.database.LogsDAO;
import com.example.heblecollectiondesktop.model.Consumidores;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ControllerEditarConsumidor {

    private static final String STATUS_ATIVO = "Ativo";
    private static final String STATUS_BANIDO = "Banido";

    private final ConsumidoresDAO consumidorDAO = new ConsumidoresDAO();
    private final LogsDAO logsDAO = new LogsDAO();

    private Consumidores consumidorEditar;

    @FXML private Label LabelNome;
    @FXML private Label LabelID;
    @FXML private ComboBox<String> CbMudarStatus;
    @FXML private TextArea txtObservacao;

    public void initialize() {
        if (CbMudarStatus != null) {
            CbMudarStatus.setItems(FXCollections.observableArrayList(STATUS_ATIVO, STATUS_BANIDO));
        }
        if (consumidorEditar != null) {
            SetLabel();
            preSelecionarStatus();
        }
    }

    public void setConsumidorEditar(Consumidores consumidor) {
        this.consumidorEditar = consumidor;
        if (consumidor != null) {
            SetLabel();
            preSelecionarStatus();
        }
    }

    private void SetLabel() {
        if (consumidorEditar == null) return;
        LabelNome.setText(consumidorEditar.getNomconsumidor());
        LabelID.setText(String.valueOf(consumidorEditar.getIDconsumidor()));
    }

    private void preSelecionarStatus() {
        if (CbMudarStatus == null || consumidorEditar == null) return;

        String statusAtual = consumidorEditar.getStatusconsumidor();
        if (statusAtual == null || statusAtual.isBlank()) return;

        String valorCombo;
        if (statusAtual.equalsIgnoreCase(STATUS_ATIVO) || statusAtual.equalsIgnoreCase("Permitido")) {
            valorCombo = STATUS_ATIVO;
        } else if (statusAtual.equalsIgnoreCase(STATUS_BANIDO)) {
            valorCombo = STATUS_BANIDO;
        } else {
            return; // status desconhecido, não pré-seleciona
        }

        Platform.runLater(() -> CbMudarStatus.setValue(valorCombo));
    }

    @FXML
    public void EditarConsumidor() {

        if (consumidorEditar == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro",
                    "Nenhum consumidor selecionado para edição.");
            return;
        }

        String selecionado = (CbMudarStatus != null) ? CbMudarStatus.getValue() : null;

        if (selecionado == null || selecionado.isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Status Obrigatório",
                    "Por favor, selecione um Status para a edição.");
            return;
        }

        try {
            String observacao = (txtObservacao.getText() != null)
                    ? txtObservacao.getText().trim() : "";

            String statusAtual = consumidorEditar.getStatusconsumidor();

            if (selecionado.equalsIgnoreCase(STATUS_ATIVO)) {
                if (statusAtual != null && statusAtual.equalsIgnoreCase(STATUS_ATIVO)) {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Sem alteração",
                            "Este consumidor já está ativo. Nada a alterar.");
                    return;
                }

                boolean sucesso = consumidorDAO.atualizarStatusConsumidor(
                        consumidorEditar.getIDconsumidor(), STATUS_ATIVO);

                if (sucesso) {
                    consumidorEditar.setStatusconsumidor(STATUS_ATIVO); // mantém modelo coerente

                    String detalhes = "Status: " + selecionado
                            + (observacao.isEmpty() ? "" : " | Obs: " + observacao);
                    logsDAO.registrarLog("Suporte", "RETIRANDO_BANIMENTO",
                            consumidorEditar.getNomconsumidor(), detalhes);

                    mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso",
                            "Banimento do consumidor retirado e ação registrada no log da moderação!");
                    fecharJanela();
                } else {
                    mostrarAlerta(Alert.AlertType.ERROR, "Erro",
                            "Não foi possível concluir o desbanimento do consumidor.");
                }
                return;
            }

            if (selecionado.equalsIgnoreCase(STATUS_BANIDO)) {
                if (statusAtual != null && statusAtual.equalsIgnoreCase(STATUS_BANIDO)) {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Sem alteração",
                            "Este consumidor já está banido. Nada a alterar.");
                    return;
                }

                boolean sucesso = consumidorDAO.atualizarStatusConsumidor(
                        consumidorEditar.getIDconsumidor(), STATUS_BANIDO);

                if (sucesso) {
                    consumidorEditar.setStatusconsumidor(STATUS_BANIDO);

                    String detalhes = "Status: " + selecionado
                            + (observacao.isEmpty() ? "" : " | Obs: " + observacao);
                    logsDAO.registrarLog("Suporte", "APLICANDO_BANIMENTO",
                            consumidorEditar.getNomconsumidor(), detalhes);

                    mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso",
                            "Consumidor banido e ação registrada no log da moderação!");
                    fecharJanela();
                } else {
                    mostrarAlerta(Alert.AlertType.ERROR, "Erro",
                            "Não foi possível concluir o banimento do consumidor.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Erro de Banco de Dados",
                    "Falha ao editar consumidor: " + e.getMessage());
        }
    }

    @FXML
    public void Limpar() {
        if (CbMudarStatus != null) {
            CbMudarStatus.getSelectionModel().clearSelection();
        }
        if (txtObservacao != null) {
            txtObservacao.clear();
        }
    }

    @FXML
    public void Cancelar() {
        fecharJanela();
    }

    private void fecharJanela() {
        if (LabelNome != null && LabelNome.getScene() != null) {
            Stage janelaAtual = (Stage) LabelNome.getScene().getWindow();
            janelaAtual.close();
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}