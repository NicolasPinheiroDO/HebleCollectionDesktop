package com.example.heblecollectiondesktop.database;

import com.example.heblecollectiondesktop.model.Empresa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmpresaDAO {

    private static final Logger LOGGER = Logger.getLogger(EmpresaDAO.class.getName());

    private static final String COLUNAS = "id, nome, cnpj, estilo, aprovado";
    private static final int ERRO_MYSQL_REGISTRO_REFERENCIADO = 1451;

    public List<Empresa> listarPendentes() {
        return listar("SELECT " + COLUNAS + " FROM empresa WHERE aprovado = 0 ORDER BY id DESC",
                "Erro ao listar empresas pendentes");
    }

    public List<Empresa> listarAprovadas() {
        return listar("SELECT " + COLUNAS + " FROM empresa WHERE aprovado = 1 ORDER BY nome ASC",
                "Erro ao listar empresas aprovadas");
    }

    public List<Empresa> listarTodas() {
        return listar("SELECT " + COLUNAS + " FROM empresa ORDER BY nome ASC",
                "Erro ao listar todas as empresas");
    }

    public boolean atualizar(Empresa empresa) {
        return atualizarDados(empresa.getId(), empresa.getNome(), empresa.getCnpj(), empresa.getEstilo(),
                "Erro ao atualizar dados da empresa");
    }

    public boolean atualizarCampos(int id, String nome, String cnpj, String estilo) {
        return atualizarDados(id, nome, cnpj, estilo, "Erro ao atualizar campos da empresa");
    }

    public boolean atualizarStatusAprovacao(int id, boolean aprovado) {
        String sql = "UPDATE empresa SET aprovado = ? WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, aprovado);
            stmt.setInt(2, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status da empresa: " + e.getMessage(), e);
        }
    }

    public Empresa buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM empresa WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar empresa por ID: " + e.getMessage(), e);
        }

        return null;
    }

    public boolean deletar(int idEmpresa) {
        String sql = "DELETE FROM empresa WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idEmpresa);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getErrorCode() == ERRO_MYSQL_REGISTRO_REFERENCIADO) {
                LOGGER.warning("Empresa " + idEmpresa + " não pode ser excluída: possui produtos vinculados.");
            } else {
                LOGGER.log(Level.SEVERE, "Erro ao deletar empresa " + idEmpresa, e);
            }
            return false;
        }
    }

    public boolean possuiProdutos(int idEmpresa) {
        String sql = "SELECT 1 FROM produtos WHERE empresa_id = ? LIMIT 1";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idEmpresa);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar produtos da empresa " + idEmpresa, e);
            return false;
        }
    }

    private boolean atualizarDados(int id, String nome, String cnpj, String estilo, String mensagemErro) {
        String sql = "UPDATE empresa SET nome = ?, cnpj = ?, estilo = ? WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            stmt.setString(2, cnpj);
            stmt.setString(3, estilo);
            stmt.setInt(4, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(mensagemErro + ": " + e.getMessage(), e);
        }
    }

    private List<Empresa> listar(String sql, String mensagemErro) {
        List<Empresa> empresas = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                empresas.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(mensagemErro + ": " + e.getMessage(), e);
        }

        return empresas;
    }

    private Empresa mapear(ResultSet rs) throws SQLException {
        return new Empresa(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("cnpj"),
                rs.getString("estilo"),
                rs.getBoolean("aprovado")
        );
    }
}