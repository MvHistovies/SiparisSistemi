package org.larune.siparis.storage;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.larune.siparis.model.LogEntry;
import org.larune.siparis.model.Order;
import org.larune.siparis.model.OrderDeliveryStat;
import org.larune.siparis.model.OrderLogSummary;

import java.io.File;
import java.sql.*;
import java.util.*;

public class SQLiteStorage {

    private final JavaPlugin plugin;
    private Connection conn;

    public SQLiteStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            File dbFile = new File(plugin.getDataFolder(), "orders.db");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement st = conn.createStatement()) {

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS orders (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      owner TEXT NOT NULL,
                      material TEXT NOT NULL,
                      total_amount INTEGER NOT NULL,
                      remaining_amount INTEGER NOT NULL,
                      unit_price INTEGER NOT NULL,
                      created_at INTEGER NOT NULL,
                      status TEXT NOT NULL
                    );
                """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS delivery_box (
                      owner TEXT NOT NULL,
                      material TEXT NOT NULL,
                      amount INTEGER NOT NULL,
                      PRIMARY KEY(owner, material)
                    );
                """);

                // LOG TABLOSU
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS order_logs (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      type TEXT NOT NULL,                 -- CREATE / DELIVER / CANCEL / ADMIN_DELETE vs
                      order_id INTEGER NOT NULL,
                      owner_uuid TEXT NOT NULL,
                      owner_name TEXT NOT NULL,
                      actor_uuid TEXT NOT NULL,
                      actor_name TEXT NOT NULL,
                      material TEXT NOT NULL,
                      amount INTEGER NOT NULL,
                      unit_price INTEGER NOT NULL,
                      pay INTEGER NOT NULL,
                      created_at INTEGER NOT NULL
                    );
                """);

                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_order_logs_order_id ON order_logs(order_id);");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_order_logs_created_at ON order_logs(created_at DESC);");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("SQLite init failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }

    // =========================
    // ORDERS
    // =========================

    public int insertOrder(UUID owner, Material mat, int totalAmount, long unitPrice) {
        String sql = "INSERT INTO orders(owner, material, total_amount, remaining_amount, unit_price, created_at, status) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, owner.toString());
            ps.setString(2, mat.name());
            ps.setInt(3, totalAmount);
            ps.setInt(4, totalAmount);
            ps.setLong(5, unitPrice);
            ps.setLong(6, System.currentTimeMillis());
            ps.setString(7, "ACTIVE");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateOrderRemainingAndStatus(int orderId, int remaining, String status) {
        String sql = "UPDATE orders SET remaining_amount=?, status=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, remaining);
            ps.setString(2, status);
            ps.setInt(3, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Order getOrder(int id) {
        String sql = "SELECT * FROM orders WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapOrder(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Order> listActiveOrders(int limit, int offset) {
        String sql = "SELECT * FROM orders WHERE status='ACTIVE' ORDER BY id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Order> out = new ArrayList<>();
                while (rs.next()) out.add(mapOrder(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Order> listOrdersByOwner(UUID owner, int limit, int offset) {
        String sql = "SELECT * FROM orders WHERE owner=? ORDER BY id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Order> out = new ArrayList<>();
                while (rs.next()) out.add(mapOrder(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countActiveOrdersByOwner(UUID owner) {
        String sql = "SELECT COUNT(*) AS c FROM orders WHERE owner=? AND status='ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** ADMIN: siparişi DB'den siler */
    public boolean deleteOrder(int id) {
        String sql = "DELETE FROM orders WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID owner = UUID.fromString(rs.getString("owner"));
        Material mat = Material.valueOf(rs.getString("material"));
        int total = rs.getInt("total_amount");
        int remaining = rs.getInt("remaining_amount");
        long unit = rs.getLong("unit_price");
        long created = rs.getLong("created_at");
        String status = rs.getString("status");
        return new Order(id, owner, mat, total, remaining, unit, created, status);
    }

    // =========================
    // DELIVERY BOX
    // =========================

    public Map<Material, Integer> getDeliveryBox(UUID owner) {
        String sql = "SELECT material, amount FROM delivery_box WHERE owner=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                Map<Material, Integer> map = new LinkedHashMap<>();
                while (rs.next()) {
                    Material m = Material.valueOf(rs.getString("material"));
                    int amt = rs.getInt("amount");
                    map.put(m, amt);
                }
                return map;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addToDeliveryBox(UUID owner, Material mat, int amount) {
        String sql = """
            INSERT INTO delivery_box(owner, material, amount) VALUES(?,?,?)
            ON CONFLICT(owner, material) DO UPDATE SET amount = amount + excluded.amount
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setString(2, mat.name());
            ps.setInt(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int takeFromDeliveryBox(UUID owner, Material mat, int amount) {
        int current = getDeliveryAmount(owner, mat);
        int take = Math.min(current, amount);
        if (take <= 0) return 0;

        int left = current - take;
        if (left <= 0) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM delivery_box WHERE owner=? AND material=?")) {
                ps.setString(1, owner.toString());
                ps.setString(2, mat.name());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE delivery_box SET amount=? WHERE owner=? AND material=?")) {
                ps.setInt(1, left);
                ps.setString(2, owner.toString());
                ps.setString(3, mat.name());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return take;
    }

    public int getDeliveryAmount(UUID owner, Material mat) {
        String sql = "SELECT amount FROM delivery_box WHERE owner=? AND material=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setString(2, mat.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("amount");
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // =========================
    // LOG INSERT
    // =========================

    public void insertLog(String type, int orderId,
                          UUID ownerUuid, String ownerName,
                          UUID actorUuid, String actorName,
                          Material material, int amount,
                          long unitPrice, long pay, long createdAt) {
        String sql = """
            INSERT INTO order_logs(type, order_id, owner_uuid, owner_name, actor_uuid, actor_name,
                                   material, amount, unit_price, pay, created_at)
            VALUES(?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setInt(2, orderId);
            ps.setString(3, ownerUuid.toString());
            ps.setString(4, ownerName);
            ps.setString(5, actorUuid.toString());
            ps.setString(6, actorName);
            ps.setString(7, material.name());
            ps.setInt(8, amount);
            ps.setLong(9, unitPrice);
            ps.setLong(10, pay);
            ps.setLong(11, createdAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // =========================
    // LOG LIST (RAW)
    // =========================

    public List<LogEntry> listLogs(int limit, int offset) {
        String sql = "SELECT * FROM order_logs ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<LogEntry> out = new ArrayList<>();
                while (rs.next()) out.add(mapLog(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private LogEntry mapLog(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String type = rs.getString("type");
        int orderId = rs.getInt("order_id");

        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        String ownerName = rs.getString("owner_name");

        UUID actorUuid = UUID.fromString(rs.getString("actor_uuid"));
        String actorName = rs.getString("actor_name");

        Material mat = Material.valueOf(rs.getString("material"));

        int amount = rs.getInt("amount");
        long unitPrice = rs.getLong("unit_price");
        long pay = rs.getLong("pay");
        long createdAt = rs.getLong("created_at");

        return new LogEntry(id, type, orderId, ownerUuid, ownerName, actorUuid, actorName, mat, amount, unitPrice, pay, createdAt);
    }

    // =========================
    // LOGS - ÖZET (SİPARİŞ BAŞINA 1 SATIR)
    // =========================

    public List<OrderLogSummary> listLogSummaries(int limit, int offset) {
        String sql = """
            SELECT
              order_id,
              MAX(created_at) AS last_at,
              MAX(CASE WHEN type='CREATE' THEN created_at END) AS created_at,

              MAX(owner_uuid) AS owner_uuid,
              MAX(owner_name) AS owner_name,
              MAX(material) AS material,

              MAX(CASE WHEN type='CREATE' THEN amount END) AS total_amount,
              MAX(CASE WHEN type='CREATE' THEN unit_price END) AS unit_price,

              COALESCE(SUM(CASE WHEN type='DELIVER' THEN amount ELSE 0 END), 0) AS delivered_amount,
              COALESCE(SUM(CASE WHEN type='DELIVER' THEN pay ELSE 0 END), 0) AS delivered_pay
            FROM order_logs
            GROUP BY order_id
            ORDER BY last_at DESC
            LIMIT ? OFFSET ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<OrderLogSummary> out = new ArrayList<>();
                while (rs.next()) {
                    int orderId = rs.getInt("order_id");
                    long lastAt = rs.getLong("last_at");
                    long createdAt = rs.getLong("created_at");

                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                    String ownerName = rs.getString("owner_name");

                    Material mat = Material.valueOf(rs.getString("material"));

                    int totalAmount = rs.getInt("total_amount");
                    long unitPrice = rs.getLong("unit_price");

                    int deliveredAmount = rs.getInt("delivered_amount");
                    long deliveredPay = rs.getLong("delivered_pay");

                    out.add(new OrderLogSummary(orderId, ownerUuid, ownerName, mat,
                            totalAmount, unitPrice,
                            deliveredAmount, deliveredPay,
                            createdAt, lastAt));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // =========================
    // LOGS - TESLİM BREAKDOWN (KİM NE KADAR)
    // =========================

    public List<OrderDeliveryStat> listDeliveriesForOrder(int orderId) {
        String sql = """
            SELECT actor_uuid, actor_name,
                   SUM(amount) AS amt,
                   SUM(pay) AS pay
            FROM order_logs
            WHERE order_id = ?
              AND type = 'DELIVER'
            GROUP BY actor_uuid, actor_name
            ORDER BY amt DESC
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                List<OrderDeliveryStat> out = new ArrayList<>();
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("actor_uuid"));
                    String name = rs.getString("actor_name");
                    int amt = rs.getInt("amt");
                    long pay = rs.getLong("pay");
                    out.add(new OrderDeliveryStat(uuid, name, amt, pay));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
