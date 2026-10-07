package dao;

import java.sql.*;
import java.util.*;


public class SqlQueryDao {

    private final Connection conn;

    public SqlQueryDao(Connection conn) {
        this.conn = conn;
    }

    public void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pizza_types (
                    pizza_type_id VARCHAR(50) PRIMARY KEY,
                    name          VARCHAR(100),
                    category      VARCHAR(50),
                    ingredients   TEXT
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pizzas (
                    pizza_id      VARCHAR(50) PRIMARY KEY,
                    pizza_type_id VARCHAR(50) NOT NULL,
                    `size`        VARCHAR(10),
                    price         DECIMAL(10,2),
                    FOREIGN KEY (pizza_type_id) REFERENCES pizza_types(pizza_type_id)
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS orders (
                    order_id INT PRIMARY KEY,
                    `date`   DATE,
                    `time`   TIME
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS order_details (
                    order_details_id INT PRIMARY KEY AUTO_INCREMENT,
                    order_id         INT NOT NULL,
                    pizza_id         VARCHAR(50) NOT NULL,
                    quantity         INT,
                    FOREIGN KEY (order_id) REFERENCES orders(order_id),
                    FOREIGN KEY (pizza_id) REFERENCES pizzas(pizza_id)
                )""");
        }
    }

    public void dropTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS order_details");
            st.executeUpdate("DROP TABLE IF EXISTS orders");
            st.executeUpdate("DROP TABLE IF EXISTS pizzas");
            st.executeUpdate("DROP TABLE IF EXISTS pizza_types");
        }
    }

    public long getTotalOrders() throws SQLException {
        return scalarLong("SELECT COUNT(DISTINCT order_id) FROM orders");
    }

    public double getTotalRevenue() throws SQLException {
        return scalarDouble("""
            SELECT SUM(p.price * od.quantity)
            FROM order_details od
            JOIN pizzas p ON od.pizza_id = p.pizza_id""");
    }

    public List<Map<String, Object>> getHighestPricedPizza() throws SQLException {
        return query("""
            SELECT pt.name, p.pizza_id, p.price AS max_price
            FROM pizzas p
            JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
            ORDER BY p.price DESC
            LIMIT 1""");
    }

    public List<Map<String, Object>> getMostCommonSize() throws SQLException {
        return query("""
            SELECT p.`size`, SUM(od.quantity) AS total_quantity
            FROM order_details od
            JOIN pizzas p ON od.pizza_id = p.pizza_id
            GROUP BY p.`size`
            ORDER BY total_quantity DESC
            LIMIT 1""");
    }

    public List<Map<String, Object>> getTop5OrderedPizzaTypes() throws SQLException {
        return query("""
            SELECT pt.name, SUM(od.quantity) AS total_quantity
            FROM order_details od
            JOIN pizzas p       ON od.pizza_id = p.pizza_id
            JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
            GROUP BY pt.name
            ORDER BY total_quantity DESC
            LIMIT 5""");
    }

    public List<Map<String, Object>> getQuantityByCategory() throws SQLException {
        return query("""
            SELECT pt.category, SUM(od.quantity) AS total_quantity
            FROM order_details od
            JOIN pizzas p       ON od.pizza_id = p.pizza_id
            JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
            GROUP BY pt.category
            ORDER BY total_quantity DESC""");
    }

    public List<Map<String, Object>> getOrdersByHour() throws SQLException {
        return query("""
            SELECT HOUR(`time`) AS hour, COUNT(order_id) AS number_of_orders
            FROM orders
            GROUP BY HOUR(`time`)
            ORDER BY hour""");
    }

    public double getAverageOrderValue() throws SQLException {
        return scalarDouble("""
            SELECT AVG(order_total) FROM (
                SELECT od.order_id, SUM(od.quantity * p.price) AS order_total
                FROM order_details od
                JOIN pizzas p ON od.pizza_id = p.pizza_id
                GROUP BY od.order_id
            ) AS order_totals""");
    }

    public double getAvgPizzasPerDay() throws SQLException {
        return scalarDouble("""
            SELECT AVG(pizzas_per_day) FROM (
                SELECT o.`date`, SUM(od.quantity) AS pizzas_per_day
                FROM order_details od
                JOIN orders o ON od.order_id = o.order_id
                GROUP BY o.`date`
            ) AS daily_totals""");
    }

    public List<Map<String, Object>> getTop3PizzaTypesByRevenue() throws SQLException {
        return query("""
            SELECT pt.name, SUM(od.quantity * p.price) AS revenue
            FROM order_details od
            JOIN pizzas p       ON od.pizza_id = p.pizza_id
            JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
            GROUP BY pt.name
            ORDER BY revenue DESC
            LIMIT 3""");
    }

    public List<Map<String, Object>> getRevenueContribution() throws SQLException {
        return query("""
            SELECT
                pt.name AS pizza_type,
                ROUND(SUM(od.quantity * p.price), 2) AS revenue,
                ROUND(SUM(od.quantity * p.price) * 100.0 /
                    (SELECT SUM(od2.quantity * p2.price)
                     FROM order_details od2
                     JOIN pizzas p2 ON od2.pizza_id = p2.pizza_id), 2) AS pct_of_total_revenue
            FROM order_details od
            JOIN pizzas p       ON od.pizza_id = p.pizza_id
            JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
            GROUP BY pt.name
            ORDER BY pct_of_total_revenue DESC""");
    }

    public List<Map<String, Object>> getCumulativeRevenue() throws SQLException {
        return query("""
            WITH daily_revenue AS (
                SELECT o.`date` AS order_date, SUM(od.quantity * p.price) AS revenue
                FROM order_details od
                JOIN orders o ON od.order_id = o.order_id
                JOIN pizzas p ON od.pizza_id = p.pizza_id
                GROUP BY o.`date`
            )
            SELECT order_date, revenue,
                   ROUND(SUM(revenue) OVER (ORDER BY order_date), 2) AS cumulative_revenue
            FROM daily_revenue
            ORDER BY order_date""");
    }

    public List<Map<String, Object>> getTop3ByRevenuePerCategory() throws SQLException {
        return query("""
            WITH revenue_by_type AS (
                SELECT pt.category, pt.name AS pizza_type,
                       SUM(od.quantity * p.price) AS revenue
                FROM order_details od
                JOIN pizzas p       ON od.pizza_id = p.pizza_id
                JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
                GROUP BY pt.category, pt.name
            ),
            ranked AS (
                SELECT category, pizza_type, revenue,
                       RANK() OVER (PARTITION BY category ORDER BY revenue DESC) AS rnk
                FROM revenue_by_type
            )
            SELECT category, pizza_type, revenue
            FROM ranked
            WHERE rnk <= 3
            ORDER BY category, rnk""");
    }

    private List<Map<String, Object>> query(String sql) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private long scalarLong(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private double scalarDouble(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
}