package com.example.heblecollectiondesktop.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.heblecollectiondesktop.model.Ticket;
import com.example.heblecollectiondesktop.model.Tipo;
import com.example.heblecollectiondesktop.model.status;

public class TicketDAO {

    private static final Logger LOGGER = Logger.getLogger(TicketDAO.class.getName());

    public List<Ticket> listarTodos() {
        String sql = "SELECT id, assunto, tipo, descricao, status, funcionario_id FROM tickets ORDER BY criado_em DESC";
        List<Ticket> lista = new ArrayList<>();

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Tipo tipoEnum = null;
                try {
                    String tipoStr = rs.getString("tipo");
                    if (tipoStr != null) {
                        tipoEnum = Tipo.valueOf(tipoStr.toUpperCase());
                    }
                } catch (Exception e) {
                    tipoEnum = Tipo.GERAL;
                }

                status statusEnum = null;
                try {
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        statusEnum = status.valueOf(statusStr.toLowerCase());
                    }
                } catch (Exception e) {
                    statusEnum = status.pendente;
                }

                lista.add(new Ticket(
                        rs.getInt("id"),
                        rs.getString("assunto"),
                        tipoEnum,
                        rs.getString("descricao"),
                        statusEnum,
                        rs.getInt("funcionario_id")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todos os tickets: " + e.getMessage(), e);
        }

        return lista;
    }

    public boolean atualizarStatus(int idTicket, String novoStatus) {
        String sql = "UPDATE tickets SET status = ? WHERE id = ?";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, novoStatus);
            stmt.setInt(2, idTicket);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status do ticket: " + e.getMessage(), e);
        }
    }

    public boolean deletar(int idTicket) {
        String sql = "DELETE FROM tickets WHERE id = ?";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idTicket);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao deletar ticket " + idTicket, e);
            return false;
        }
    }

    public boolean Adicionar(Ticket ticket) {
        String sql = "INSERT INTO tickets (assunto, tipo, descricao, status, funcionario_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, ticket.getAssunto());
            stmt.setString(2, ticket.getTipo() != null ? ticket.getTipo().name() : "GERAL");
            stmt.setString(3, ticket.getDescricao());
            stmt.setString(4, ticket.getStatus() != null ? ticket.getStatus().name() : "aberto");

            if (ticket.getFuncionarioId() > 0) {
                stmt.setInt(5, ticket.getFuncionarioId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar ticket: " + e.getMessage(), e);
        }
    }
}