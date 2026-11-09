package com.example.heblecollectiondesktop.database;

import com.example.heblecollectiondesktop.model.Consumidores;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsumidoresDAO {

    private static final Logger LOGGER = Logger.getLogger(ConsumidoresDAO.class.getName());

    private static final String COLUNAS =
            "id, nomconsumidor, cpfconsumidor, senhaconsumidor, emailconsumidor, " +
                    "celularconsumidor, cepconsumidor, enderecoconsumidor, dataNascconsumidor, statusconsumidor";

    private static final int ERRO_MYSQL_REGISTRO_REFERENCIADO = 1451;

    public List<Consumidores> listarConsumidores() {
        String sql = "SELECT " + COLUNAS + " FROM consumidores ORDER BY id DESC";
        List<Consumidores> lista = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao listar consumidores", e);
        }

        return lista;
    }

    public List<Consumidores> pesquisarPorNome(String nome) {
        String sql = "SELECT " + COLUNAS + " FROM consumidores WHERE nomconsumidor LIKE ? ORDER BY id DESC";
        List<Consumidores> lista = new ArrayList<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nome + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao pesquisar consumidores", e);
        }

        return lista;
    }

    public Consumidores buscarPorId(int idConsumidor) {
        String sql = "SELECT " + COLUNAS + " FROM consumidores WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idConsumidor);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar consumidor por ID " + idConsumidor, e);
        }

        return null;
    }

    public String buscarNomeClientePorId(int consumidorId) {
        String sql = "SELECT nomconsumidor FROM consumidores WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, consumidorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String nome = rs.getString("nomconsumidor");
                    if (nome != null) {
                        return nome;
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar nome do consumidor " + consumidorId, e);
        }

        return "Cliente Desconhecido";
    }

    public Map<Integer, String> buscarNomesClientes() {
        String sql = "SELECT id, nomconsumidor FROM consumidores";
        Map<Integer, String> nomes = new HashMap<>();

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                nomes.put(rs.getInt("id"), rs.getString("nomconsumidor"));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar nomes dos consumidores", e);
        }

        return nomes;
    }

    public void atualizar(Consumidores consumidor) {
        String sql = "UPDATE consumidores SET nomconsumidor = ?, cpfconsumidor = ?, senhaconsumidor = ?, emailconsumidor = ?, " +
                "celularconsumidor = ?, cepconsumidor = ?, enderecoconsumidor = ?, dataNascconsumidor = ?, statusconsumidor = ? " +
                "WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, consumidor.getNomconsumidor());
            stmt.setString(2, consumidor.getCpfconsumidor());
            stmt.setString(3, consumidor.getSenhaconsumidor());
            stmt.setString(4, consumidor.getEmailconsumidor());
            stmt.setString(5, consumidor.getCelularconsumidor());
            stmt.setString(6, consumidor.getCepconsumidor());
            stmt.setString(7, consumidor.getEnderecoconsumidor());

            String dataNasc = consumidor.getDataNascconsumidor();
            if (dataNasc == null || dataNasc.isBlank()) {
                stmt.setNull(8, Types.DATE);
            } else {
                stmt.setString(8, dataNasc.trim());
            }

            stmt.setString(9, consumidor.getStatusconsumidor());
            stmt.setInt(10, consumidor.getIDconsumidor());

            stmt.executeUpdate();
            LOGGER.info("Consumidor atualizado com sucesso!");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao atualizar consumidor " + consumidor.getIDconsumidor(), e);
        }
    }

    public boolean atualizarStatusConsumidor(int idConsumidor, String novoStatus) {
        if (novoStatus == null
                || !(novoStatus.equalsIgnoreCase("Ativo") || novoStatus.equalsIgnoreCase("Banido"))) {
            LOGGER.warning("Status inválido recebido: " + novoStatus);
            return false;
        }

        String sql = "UPDATE consumidores SET statusconsumidor = ? WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novoStatus);
            stmt.setInt(2, idConsumidor);

            boolean alterou = stmt.executeUpdate() > 0;
            if (alterou) {
                LOGGER.info("Status do consumidor " + idConsumidor + " atualizado para " + novoStatus);
            } else {
                LOGGER.warning("Nenhuma linha alterada ao atualizar status do consumidor " + idConsumidor
                        + " (ID inexistente?)");
            }
            return alterou;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao atualizar o status do consumidor " + idConsumidor, e);
            return false;
        }
    }

    public boolean deletar(int idConsumidor) {
        String sql = "DELETE FROM consumidores WHERE id = ?";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idConsumidor);
            boolean apagou = stmt.executeUpdate() > 0;
            if (apagou) {
                LOGGER.info("Consumidor excluído com sucesso!");
            }
            return apagou;

        } catch (SQLException e) {
            if (e.getErrorCode() == ERRO_MYSQL_REGISTRO_REFERENCIADO) {
                LOGGER.warning("Consumidor " + idConsumidor + " não pode ser excluído: possui vendas vinculadas.");
            } else {
                LOGGER.log(Level.SEVERE, "Erro ao excluir consumidor " + idConsumidor, e);
            }
            return false;
        }
    }

    public boolean possuiVendas(int idConsumidor) {
        String sql = "SELECT 1 FROM vendas WHERE consumidor_id = ? LIMIT 1";

        try (Connection conn = conexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idConsumidor);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar vendas do consumidor " + idConsumidor, e);
            return false;
        }
    }

    private Consumidores mapear(ResultSet rs) throws SQLException {
        return new Consumidores(
                rs.getInt("id"),
                rs.getString("nomconsumidor"),
                rs.getString("cpfconsumidor"),
                rs.getString("senhaconsumidor"),
                rs.getString("emailconsumidor"),
                rs.getString("celularconsumidor"),
                rs.getString("cepconsumidor"),
                rs.getString("enderecoconsumidor"),
                rs.getString("dataNascconsumidor"),
                rs.getString("statusconsumidor")
        );
    }
}