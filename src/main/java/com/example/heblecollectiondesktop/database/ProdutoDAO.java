package com.example.heblecollectiondesktop.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.heblecollectiondesktop.model.Produto;

public class ProdutoDAO {

    private static final Logger LOGGER = Logger.getLogger(ProdutoDAO.class.getName());

    private static final String SELECT_BASE =
            "SELECT p.id, p.nome, p.descricao, p.preco, p.quantidade_estoque, p.status, p.ativo, " +
                    "COALESCE(e.nome, 'Não vinculada') AS nome_empresa " +
                    "FROM produtos p " +
                    "LEFT JOIN empresas e ON p.empresa_id = e.id ";

    public List<Produto> listarTodos() {
        return consultar(SELECT_BASE + "ORDER BY p.id DESC", "Erro ao listar produtos");
    }

    public List<Produto> listarPendentes() {
        return consultar(SELECT_BASE + "WHERE p.status = 'PENDENTE' ORDER BY p.id DESC",
                "Erro ao listar produtos pendentes");
    }

    public boolean atualizarProduto(Produto produto) {
        String sql = "UPDATE produtos SET nome = ?, descricao = ?, preco = ?, quantidade_estoque = ?, status = ?, ativo = ? WHERE id = ?";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setDouble(3, produto.getPreco());
            stmt.setInt(4, produto.getQuantidadeEstoque());
            stmt.setString(5, produto.getStatus());
            stmt.setBoolean(6, produto.isAtivo());
            stmt.setInt(7, produto.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar produto: " + e.getMessage(), e);
        }
    }

    public boolean atualizarStatus(int idProduto, String novoStatus) {
        String sql = "UPDATE produtos SET status = ? WHERE id = ?";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, novoStatus);
            stmt.setInt(2, idProduto);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status do produto: " + e.getMessage(), e);
        }
    }

    public boolean deletar(int idProduto) {
        String sql = "UPDATE produtos SET ativo = false WHERE id = ?";

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idProduto);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar produto: " + e.getMessage(), e);
        }
    }

    public List<Produto> buscarPorTermo(String termo) {
        return buscarPorTermo(termo, false);
    }

    public List<Produto> buscarPorTermo(String termo, boolean somenteAprovados) {
        String sql = SELECT_BASE +
                "WHERE p.ativo = 1 " + (somenteAprovados ? "AND p.status = 'APROVADO' " : "") +
                "AND (p.nome LIKE ? OR p.descricao LIKE ? OR e.nome LIKE ?) " +
                "ORDER BY p.id DESC";

        List<Produto> lista = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String filtro = "%" + termo + "%";
            stmt.setString(1, filtro);
            stmt.setString(2, filtro);
            stmt.setString(3, filtro);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar produtos por termo", e);
        }
        return lista;
    }

    private List<Produto> consultar(String sql, String mensagemErro) {
        List<Produto> lista = new ArrayList<>();

        try (Connection conexao = conexaoDB.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(mensagemErro + ": " + e.getMessage(), e);
        }

        return lista;
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getDouble("preco"),
                rs.getInt("quantidade_estoque"),
                rs.getString("status"),
                rs.getString("nome_empresa"),
                rs.getBoolean("ativo")
        );
    }
}