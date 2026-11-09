package com.example.heblecollectiondesktop.controller;

import com.example.heblecollectiondesktop.database.TicketDAO;
import com.example.heblecollectiondesktop.model.Ticket;
import com.example.heblecollectiondesktop.model.Tipo;
import com.example.heblecollectiondesktop.model.status;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class ControllerSuporteFuncionario {

    @FXML
    private Button btnAbrirChamado;

    @FXML
    private TextField txMatricula;

    @FXML
    private TextArea txDescricao;

    private final TicketDAO ticketDAO = new TicketDAO();

    @FXML
    private void abrirChamado(ActionEvent event) {
        String assuntoText = txMatricula.getText() != null ? txMatricula.getText().trim() : "";
        String detalhes = txDescricao.getText() != null ? txDescricao.getText().trim() : "";

        if (assuntoText.isEmpty() || detalhes.isEmpty()) {
            exibirAlertaAviso("Preencha todos os campos obrigatórios.");
            return;
        }

        Tipo tipoTicket = Tipo.SENHA;
        status statusInicial = status.pendente;
        int idFuncionarioLogado = 0;

        Ticket novoTicket = new Ticket(
                0,
                assuntoText,
                tipoTicket,
                detalhes,
                statusInicial,
                idFuncionarioLogado
        );

        boolean sucesso = ticketDAO.Adicionar(novoTicket);

        if (sucesso) {
            alertaSucesso();
            voltar(event);
        } else {
            exibirAlertaAviso("Não foi possível registrar o ticket. Tente novamente.");
        }
    }

    @FXML
    private void voltar(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/heblecollectiondesktop/view/login.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene()
                    .getWindow();

            Scene novaCena = new Scene(root);
            stage.setScene(novaCena);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void alertaSucesso() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "O ticket foi criado! Espere o seu problema ser resolvido.");
        alert.setTitle("Sucesso");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void exibirAlertaAviso(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING, mensagem);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}