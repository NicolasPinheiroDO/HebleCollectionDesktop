package com.example.heblecollectiondesktop.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.heblecollectiondesktop.model.ItemVenda;
import com.example.heblecollectiondesktop.model.Venda;

public class VendaDAO {

    private static final Logger LOGGER = Logger.getLogger(VendaDAO.class.getName());

    private static final String SQL_INSERT_VENDA =
            "INSERT INTO vendas (consumidor_id, cupom_id, valor_subtotal, valor_frete, valor_desconto, valor_total, status_venda) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_ITEM =
            "INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)";

    private static final String SQL_COLUNAS_VENDA =
            "id, consumidor_id, cupom_id, valor_subtotal, valor_frete, valor_desconto, valor_total, status_venda, criado_em";

    public boolean salvar(Venda venda) throws SQLException {
        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new SQLException("A venda precisa ter pelo menos um item.");
        }

        try (Connection conn = conexaoDB.getConexao()) {
            try {
                conn.setAutoCommit(false);

                int vendaId = inserirVenda(conn, venda);
                inserirItens(conn, vendaId, venda);

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

    private int inserirVenda(Connection conn, Venda venda) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_VENDA, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, venda.getConsumidorId());

            if (venda.getCupomId() != null) {
                stmt.setInt(2, venda.getCupomId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setBigDecimal(3, venda.getValorSubtotal());
            stmt.setBigDecimal(4, venda.getValorFrete());
            stmt.setBigDecimal(5, venda.getValorDesconto());
            stmt.setBigDecimal(6, venda.getValorTotal());
            stmt.setString(7, venda.getStatusVenda() != null ? venda.getStatusVenda() : "PENDENTE");

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("Falha ao obter o ID da venda gerada.");
            }
        }
    }

    private void inserirItens(Connection conn, int vendaId, Venda venda) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_ITEM)) {
            for (ItemVenda item : venda.getItens()) {
                stmt.setInt(1, vendaId);
                stmt.setInt(2, item.getProdutoId());
                stmt.setInt(3, item.getQuantidade());
                stmt.setBigDecimal(4, item.getPrecoUnitario());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void desfazer(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Erro ao desfazer a transação da venda", ex);
        }
    }

    private void restaurarAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Erro ao restaurar o auto-commit", ex);
        }
    }

    public List<ItemVenda> listarItensPorVenda(int vendaId) {
        List<ItemVenda> itens = new ArrayList<>();
        String sql = "SELECT i.id, i.venda_id, i.produto_id, i.quantidade, i.preco_unitario, p.nome AS nome_produto " +
                "FROM itens_venda i " +
                "JOIN produtos p ON i.produto_id = p.id " +
                "WHERE i.venda_id = ? " +
                "ORDER BY i.id";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vendaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ItemVenda item = new ItemVenda();
                    item.setIditem(rs.getInt("id"));
                    item.setVendaId(rs.getInt("venda_id"));
                    item.setProdutoId(rs.getInt("produto_id"));
                    item.setNomeProduto(rs.getString("nome_produto"));
                    item.setQuantidade(rs.getInt("quantidade"));
                    item.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
                    itens.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao listar itens da venda " + vendaId, e);
        }
        return itens;
    }

    public List<Venda> listarTodas() throws SQLException {
        String sql = "SELECT " + SQL_COLUNAS_VENDA + " FROM vendas ORDER BY criado_em DESC";
        List<Venda> vendas = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                vendas.add(mapearVenda(rs));
            }
        }
        return vendas;
    }

    public List<Venda> listarTodas(int limite, int deslocamento) throws SQLException {
        String sql = "SELECT " + SQL_COLUNAS_VENDA + " FROM vendas ORDER BY criado_em DESC LIMIT ? OFFSET ?";
        List<Venda> vendas = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limite);
            stmt.setInt(2, deslocamento);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vendas.add(mapearVenda(rs));
                }
            }
        }
        return vendas;
    }

    private Venda mapearVenda(ResultSet rs) throws SQLException {
        Venda v = new Venda();
        v.setIdvenda(rs.getInt("id"));
        v.setConsumidorId(rs.getInt("consumidor_id"));
        v.setCupomId(rs.getObject("cupom_id", Integer.class));
        v.setValorSubtotal(rs.getBigDecimal("valor_subtotal"));
        v.setValorFrete(rs.getBigDecimal("valor_frete"));
        v.setValorDesconto(rs.getBigDecimal("valor_desconto"));
        v.setValorTotal(rs.getBigDecimal("valor_total"));
        v.setStatusVenda(rs.getString("status_venda"));

        Timestamp timestamp = rs.getTimestamp("criado_em");
        if (timestamp != null) {
            v.setCriadoEm(timestamp.toLocalDateTime());
        }
        return v;
    }

    public String buscarResumoItens(int vendaId) {
        String sql = "SELECT p.nome, i.quantidade " +
                "FROM itens_venda i " +
                "JOIN produtos p ON i.produto_id = p.id " +
                "WHERE i.venda_id = ? " +
                "ORDER BY i.id";

        List<String> itensTexto = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vendaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itensTexto.add(rs.getInt("quantidade") + "x " + rs.getString("nome"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar resumo dos itens da venda " + vendaId, e);
        }

        if (itensTexto.isEmpty()) {
            return "Nenhum item";
        }
        return String.join(", ", itensTexto);
    }

    public Map<Integer, String> buscarResumosItens() {
        String sql = "SELECT i.venda_id, " +
                "GROUP_CONCAT(CONCAT(i.quantidade, 'x ', p.nome) ORDER BY i.id SEPARATOR ', ') AS resumo " +
                "FROM itens_venda i " +
                "JOIN produtos p ON i.produto_id = p.id " +
                "GROUP BY i.venda_id";

        Map<Integer, String> resumos = new HashMap<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                resumos.put(rs.getInt("venda_id"), rs.getString("resumo"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar resumo dos itens de todas as vendas", e);
        }
        return resumos;
    }
}