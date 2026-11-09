package com.example.heblecollectiondesktop.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.heblecollectiondesktop.model.Cargo;
import com.example.heblecollectiondesktop.model.Funcionario;
import com.example.heblecollectiondesktop.model.Gerente;

public class FuncionarioDAO {

    private static final Logger LOGGER = Logger.getLogger(FuncionarioDAO.class.getName());

    private static final String COLUNAS = "id, matricula, senha, cargo";

    public Funcionario autenticar(String matricula, String senha) throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM funcionarios WHERE matricula = ? LIMIT 1";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, matricula);

            int id;
            String armazenada;
            Funcionario funcionario;

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                id = rs.getInt("id");
                armazenada = rs.getString("senha");

                if (!SenhaUtil.verificar(senha, armazenada)) {
                    return null;
                }
                funcionario = mapear(rs);
            }

            if (!SenhaUtil.estaEmHash(armazenada)) {
                atualizarHash(conexao, id, SenhaUtil.gerarHash(senha));
            }
            return funcionario;
        }
    }

    private void atualizarHash(Connection conexao, int id, String hash) {
        try (PreparedStatement stmt = conexao.prepareStatement("UPDATE funcionarios SET senha = ? WHERE id = ?")) {
            stmt.setString(1, hash);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Não foi possível converter a senha do funcionário " + id + " para hash", e);
        }
    }

    public int migrarSenhasParaHash() throws SQLException {
        Map<Integer, String> pendentes = new LinkedHashMap<>();

        try (Connection conn = conexaoDB.getConexao()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, senha FROM funcionarios");
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String atual = rs.getString("senha");
                    if (atual != null && !SenhaUtil.estaEmHash(atual)) {
                        pendentes.put(rs.getInt("id"), atual);
                    }
                }
            }

            if (pendentes.isEmpty()) {
                return 0;
            }

            try {
                conn.setAutoCommit(false);
                try (PreparedStatement upd = conn.prepareStatement("UPDATE funcionarios SET senha = ? WHERE id = ?")) {
                    for (Map.Entry<Integer, String> e : pendentes.entrySet()) {
                        upd.setString(1, SenhaUtil.gerarHash(e.getValue()));
                        upd.setInt(2, e.getKey());
                        upd.addBatch();
                    }
                    upd.executeBatch();
                }
                conn.commit();
                return pendentes.size();
            } catch (SQLException | RuntimeException e) {
                desfazer(conn);
                throw e;
            } finally {
                restaurarAutoCommit(conn);
            }
        }
    }

    public List<Funcionario> listarTodos() throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT " + COLUNAS + " FROM funcionarios ORDER BY id DESC";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Funcionario> pesquisar(String termo) throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT " + COLUNAS + " FROM funcionarios WHERE matricula LIKE ? OR cargo LIKE ? ORDER BY id DESC";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            String argumento = "%" + termo + "%";
            stmt.setString(1, argumento);
            stmt.setString(2, argumento);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public void salvar(Funcionario funcionario) throws SQLException {
        String sql = "INSERT INTO funcionarios (matricula, senha, cargo) VALUES (?, ?, ?)";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, funcionario.getMatricula());
            stmt.setString(2, SenhaUtil.prepararParaGravar(funcionario.getSenha()));
            stmt.setString(3, funcionario.getCargo().name());

            stmt.executeUpdate();
        }
    }

    public boolean atualizar(Funcionario funcionario) {
        String sql = "UPDATE funcionarios SET matricula = ?, senha = ?, cargo = ? WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, funcionario.getMatricula());
            stmt.setString(2, SenhaUtil.prepararParaGravar(funcionario.getSenha()));
            stmt.setString(3, funcionario.getCargo().name());
            stmt.setInt(4, funcionario.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao executar UPDATE do funcionário " + funcionario.getId(), e);
            return false;
        }
    }

    public boolean atualizarSenhaPorMatricula(String matricula, String novaSenha) throws SQLException {
        String sql = "UPDATE funcionarios SET senha = ? WHERE matricula = ?";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, SenhaUtil.prepararParaGravar(novaSenha));
            stmt.setString(2, matricula);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deletar(int idFuncionarioDeletar) throws SQLException {
        String sqlDelete = "DELETE FROM funcionarios WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {

            stmt.setInt(1, idFuncionarioDeletar);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deletar(int idFuncionarioDeletar, String matriculaModerador, String motivo, String observacao) throws SQLException {
        String sqlSelect = "SELECT matricula FROM funcionarios WHERE id = ?";
        String sqlDelete = "DELETE FROM funcionarios WHERE id = ?";
        String sqlLog = "INSERT INTO logs_moderacao (funcionario_id, autor_id, acao, alvo_afetado, detalhes, data_acao) " +
                "VALUES (?, (SELECT id FROM funcionarios WHERE matricula = ? LIMIT 1), ?, ?, ?, ?)";

        try (Connection conn = conexaoDB.getConexao()) {
            try {
                conn.setAutoCommit(false);

                String alvoAfetado = "ID: " + idFuncionarioDeletar;
                try (PreparedStatement stmtSelect = conn.prepareStatement(sqlSelect)) {
                    stmtSelect.setInt(1, idFuncionarioDeletar);
                    try (ResultSet rs = stmtSelect.executeQuery()) {
                        if (rs.next()) {
                            alvoAfetado = rs.getString("matricula");
                        }
                    }
                }

                int linhasAfetadas;
                try (PreparedStatement stmtDelete = conn.prepareStatement(sqlDelete)) {
                    stmtDelete.setInt(1, idFuncionarioDeletar);
                    linhasAfetadas = stmtDelete.executeUpdate();
                }

                if (linhasAfetadas == 0) {
                    conn.rollback();
                    return false;
                }

                String detalhes = "Motivo: " + motivo
                        + (observacao == null || observacao.isBlank() ? "" : " | Obs: " + observacao);

                try (PreparedStatement stmtLog = conn.prepareStatement(sqlLog)) {
                    stmtLog.setString(1, matriculaModerador);
                    stmtLog.setString(2, matriculaModerador);
                    stmtLog.setString(3, "EXCLUSAO_FUNCIONARIO");
                    stmtLog.setString(4, alvoAfetado);
                    stmtLog.setString(5, detalhes);
                    stmtLog.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    stmtLog.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                desfazer(conn);
                throw e;
            } finally {
                restaurarAutoCommit(conn);
            }
        }
    }

    private Funcionario mapear(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String mat = rs.getString("matricula");
        String pass = rs.getString("senha");
        Cargo cargo = converterCargo(rs.getString("cargo"));

        if (cargo == Cargo.GERENTE) {
            return new Gerente(id, mat, pass);
        }
        return new Funcionario(id, mat, pass, cargo);
    }

    private Cargo converterCargo(String cargoBanco) {
        if (cargoBanco != null && !cargoBanco.isBlank()) {
            try {
                return Cargo.valueOf(cargoBanco.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Cargo desconhecido no banco ('" + cargoBanco + "'). Assumindo FUNCIONARIO.");
            }
        }
        return Cargo.FUNCIONARIO;
    }

    private void desfazer(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao desfazer a transação", ex);
        }
    }

    private void restaurarAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Erro ao restaurar o auto-commit", ex);
        }
    }
}