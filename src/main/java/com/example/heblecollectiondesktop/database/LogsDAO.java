package com.example.heblecollectiondesktop.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.heblecollectiondesktop.model.Funcionario;
import com.example.heblecollectiondesktop.model.LogModeracao;

public class LogsDAO {

    private static final Logger LOGGER = Logger.getLogger(LogsDAO.class.getName());

    private static final String SELECT_LOGS =
            "SELECT l.id, f.matricula AS funcionario_id, l.acao, l.alvo_afetado, l.detalhes, l.data_acao " +
                    "FROM logs_moderacao l " +
                    "LEFT JOIN funcionarios f ON l.autor_id = f.id ";

    public boolean salvar(LogModeracao log) {
        String sql = "INSERT INTO logs_moderacao (autor_id, acao, alvo_afetado, detalhes, data_acao) " +
                "VALUES ((SELECT id FROM funcionarios WHERE matricula = ? LIMIT 1), ?, ?, ?, ?)";

        String autor = log.getFuncionarioId() != null && !log.getFuncionarioId().isBlank() ? log.getFuncionarioId() : "SISTEMA";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, autor);
            stmt.setString(2, log.getAcao() != null && !log.getAcao().isBlank() ? log.getAcao() : "N/A");
            stmt.setString(3, log.getAlvoAfetado() != null && !log.getAlvoAfetado().isBlank() ? log.getAlvoAfetado() : "N/A");
            stmt.setString(4, log.getDetalhes() != null ? log.getDetalhes() : "");
            stmt.setTimestamp(5, Timestamp.valueOf(log.getDataAcao() != null ? log.getDataAcao() : LocalDateTime.now()));

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao salvar log no banco de dados", e);
            return false;
        }
    }

    public List<LogModeracao> listarTodos() {
        String sql = SELECT_LOGS + "ORDER BY l.data_acao DESC";
        List<LogModeracao> logs = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                logs.add(mapearResultSetParaLog(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao listar logs", e);
        }

        return logs;
    }

    public List<LogModeracao> buscarPorModerador(String matricula) {
        return buscarComFiltro("f.matricula", matricula, "moderador");
    }

    public List<LogModeracao> buscarPorAlvo(String alvo) {
        return buscarComFiltro("l.alvo_afetado", alvo, "alvo");
    }

    private List<LogModeracao> buscarComFiltro(String coluna, String valor, String descricao) {
        String sql = SELECT_LOGS + "WHERE " + coluna + " LIKE ? ORDER BY l.data_acao DESC";
        List<LogModeracao> logs = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + (valor != null ? valor : "") + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapearResultSetParaLog(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar logs por " + descricao, e);
        }

        return logs;
    }

    public void registrarLog(Funcionario moderador, String acao, String alvoAfetado, String detalhes) {
        String identificadorModerador = (moderador != null && moderador.getMatricula() != null && !moderador.getMatricula().isBlank())
                ? moderador.getMatricula()
                : "SISTEMA";

        registrarLog(identificadorModerador, acao, alvoAfetado, detalhes);
    }

    public void registrarLog(String matriculaModerador, String acao, String alvoAfetado, String detalhes) {
        if (matriculaModerador == null || matriculaModerador.isBlank()) {
            matriculaModerador = "SISTEMA";
        }
        if (alvoAfetado == null || alvoAfetado.isBlank()) {
            alvoAfetado = "N/A";
        }

        LogModeracao log = new LogModeracao(0, matriculaModerador, acao, alvoAfetado, detalhes, LocalDateTime.now());
        salvar(log);
    }

    public void registrarLog(Funcionario moderador, String acao, String detalhes) {
        registrarLog(moderador, acao, "N/A", detalhes);
    }

    public void registrarLog(String matriculaModerador, String acao, String detalhes) {
        registrarLog(matriculaModerador, acao, "N/A", detalhes);
    }

    private LogModeracao mapearResultSetParaLog(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String funcionarioId = rs.getString("funcionario_id");
        if (funcionarioId == null) {
            funcionarioId = "SISTEMA";
        }
        String acao = rs.getString("acao");
        String alvoAfetado = rs.getString("alvo_afetado");
        String detalhes = rs.getString("detalhes");

        Timestamp ts = rs.getTimestamp("data_acao");
        LocalDateTime dataAcao = ts != null ? ts.toLocalDateTime() : null;

        return new LogModeracao(id, funcionarioId, acao, alvoAfetado, detalhes, dataAcao);
    }
}