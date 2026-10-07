package service;

import java.sql.SQLException;
import java.util.*;

import dao.DatabaseConnection;
import dao.SqlQueryDao;
import model.QueryInfo;

public class PizzaService {

    private static SqlQueryDao dao;

    private static SqlQueryDao dao() throws SQLException {
        if (dao == null) {
            dao = new SqlQueryDao(DatabaseConnection.get());
        }
        return dao;
    }

    private static final List<QueryInfo> QUERIES = List.of(
            new QueryInfo(1,  "Total number of orders"),
            new QueryInfo(2,  "Total revenue"),
            new QueryInfo(3,  "Highest-priced pizza"),
            new QueryInfo(4,  "Most common pizza size ordered"),
            new QueryInfo(5,  "Top 5 most ordered pizza types"),
            new QueryInfo(6,  "Total quantity per pizza category"),
            new QueryInfo(7,  "Orders by hour of day"),
            new QueryInfo(8,  "Average order value"),
            new QueryInfo(9,  "Average pizzas ordered per day"),
            new QueryInfo(10, "Top 3 pizza types by revenue"),
            new QueryInfo(11, "Revenue contribution (%) per pizza type"),
            new QueryInfo(12, "Cumulative revenue over time"),
            new QueryInfo(13, "Top 3 pizza types by revenue per category")
    );

    public static void createTables() throws SQLException {
        dao().createTables();
    }

    public static void dropTables() throws SQLException {
        dao().dropTables();
    }

    public static List<QueryInfo> getQueries() {
        return QUERIES;
    }

    public static List<Map<String, Object>> run(int id) throws SQLException {
        SqlQueryDao d = dao();
        return switch (id) {
            case 1  -> single("total_orders", d.getTotalOrders());
            case 2  -> single("total_revenue", d.getTotalRevenue());
            case 3  -> d.getHighestPricedPizza();
            case 4  -> d.getMostCommonSize();
            case 5  -> d.getTop5OrderedPizzaTypes();
            case 6  -> d.getQuantityByCategory();
            case 7  -> d.getOrdersByHour();
            case 8  -> single("avg_order_value", d.getAverageOrderValue());
            case 9  -> single("avg_pizzas_per_day", d.getAvgPizzasPerDay());
            case 10 -> d.getTop3PizzaTypesByRevenue();
            case 11 -> d.getRevenueContribution();
            case 12 -> d.getCumulativeRevenue();
            case 13 -> d.getTop3ByRevenuePerCategory();
            default -> throw new IllegalArgumentException("Invalid query id: " + id);
        };
    }

    private static List<Map<String, Object>> single(String label, Object value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(label, value);
        return List.of(row);
    }
}