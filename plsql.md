# Pizza Sales Analytics — PL/SQL Package

Consolidated PL/SQL solutions for all 13 questions (5 Basic, 5 Intermediate, 3 Advanced).

## Schema

| Table | Columns |
|---|---|
| `order_details` | `order_details_id`, `order_id`, `pizza_id`, `quantity` |
| `orders` | `order_id`, `date`, `time` |
| `pizza_types` | `pizza_type_id`, `name`, `category`, `ingredients` |
| `pizzas` | `pizza_id`, `pizza_type_id`, `size`, `price` |

---

## Package Specification

```sql
CREATE OR REPLACE PACKAGE pizza_analytics_full AS

    -- BASIC
    PROCEDURE b1_total_orders;
    PROCEDURE b2_total_revenue;
    PROCEDURE b3_highest_priced_pizza;
    PROCEDURE b4_most_common_size;
    PROCEDURE b5_top5_pizza_types;

    -- INTERMEDIATE
    PROCEDURE i1_qty_by_category;
    PROCEDURE i2_orders_by_hour;
    PROCEDURE i3_category_distribution;
    FUNCTION  i4_avg_pizzas_per_day RETURN NUMBER;
    PROCEDURE i5_top3_by_revenue;

    -- ADVANCED
    PROCEDURE a1_revenue_pct_contribution;
    PROCEDURE a2_cumulative_revenue;
    PROCEDURE a3_top3_by_revenue_per_category(p_cursor OUT SYS_REFCURSOR);

    -- Convenience: run everything that prints via DBMS_OUTPUT
    PROCEDURE run_all;

END pizza_analytics_full;
/
```

---

## Package Body

### Basic

**B1. Total number of orders placed**
```sql
PROCEDURE b1_total_orders IS
    v_total NUMBER;
BEGIN
    SELECT COUNT(DISTINCT order_id) INTO v_total FROM orders;
    DBMS_OUTPUT.PUT_LINE('[B1] Total Orders: ' || v_total);
END b1_total_orders;
```

**B2. Total revenue generated from pizza sales**
```sql
PROCEDURE b2_total_revenue IS
    v_revenue NUMBER;
BEGIN
    SELECT SUM(od.quantity * p.price) INTO v_revenue
    FROM order_details od
    JOIN pizzas p ON od.pizza_id = p.pizza_id;

    DBMS_OUTPUT.PUT_LINE('[B2] Total Revenue: $' || ROUND(v_revenue, 2));
END b2_total_revenue;
```

**B3. Highest-priced pizza**
```sql
PROCEDURE b3_highest_priced_pizza IS
    CURSOR c_top IS
        SELECT pt.name, p.size, p.price
        FROM pizzas p
        JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
        ORDER BY p.price DESC;
    v_name  pizza_types.name%TYPE;
    v_size  pizzas.size%TYPE;
    v_price pizzas.price%TYPE;
BEGIN
    OPEN c_top;
    FETCH c_top INTO v_name, v_size, v_price;
    IF c_top%FOUND THEN
        DBMS_OUTPUT.PUT_LINE('[B3] Highest Priced Pizza: ' || v_name ||
                              ' (' || v_size || ') - $' || v_price);
    ELSE
        DBMS_OUTPUT.PUT_LINE('[B3] No pizzas found.');
    END IF;
    CLOSE c_top;
END b3_highest_priced_pizza;
```

**B4. Most common pizza size ordered**
```sql
PROCEDURE b4_most_common_size IS
    v_size pizzas.size%TYPE;
    v_qty  NUMBER;
BEGIN
    SELECT size, total_qty INTO v_size, v_qty
    FROM (
        SELECT p.size, SUM(od.quantity) AS total_qty
        FROM order_details od
        JOIN pizzas p ON od.pizza_id = p.pizza_id
        GROUP BY p.size
        ORDER BY total_qty DESC
    )
    WHERE ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('[B4] Most Common Size: ' || v_size || ' (' || v_qty || ' units)');
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('[B4] No order data found.');
END b4_most_common_size;
```

**B5. Top 5 most ordered pizza types with quantities**
```sql
PROCEDURE b5_top5_pizza_types IS
BEGIN
    FOR rec IN (
        SELECT pt.name, SUM(od.quantity) AS total_qty
        FROM order_details od
        JOIN pizzas p       ON od.pizza_id = p.pizza_id
        JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
        GROUP BY pt.name
        ORDER BY total_qty DESC
        FETCH FIRST 5 ROWS ONLY
    ) LOOP
        DBMS_OUTPUT.PUT_LINE('[B5] ' || rec.name || ' -> ' || rec.total_qty);
    END LOOP;
END b5_top5_pizza_types;
```

---

### Intermediate

**I1. Total quantity of each pizza category ordered**
```sql
PROCEDURE i1_qty_by_category IS
BEGIN
    FOR rec IN (
        SELECT pt.category, SUM(od.quantity) AS total_qty
        FROM order_details od
        JOIN pizzas p       ON od.pizza_id = p.pizza_id
        JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
        GROUP BY pt.category
        ORDER BY total_qty DESC
    ) LOOP
        DBMS_OUTPUT.PUT_LINE('[I1] ' || rec.category || ': ' || rec.total_qty);
    END LOOP;
END i1_qty_by_category;
```

**I2. Distribution of orders by hour of the day**
```sql
PROCEDURE i2_orders_by_hour IS
BEGIN
    FOR rec IN (
        SELECT TO_CHAR(o.time, 'HH24') AS order_hour,
               COUNT(DISTINCT o.order_id) AS num_orders
        FROM orders o
        GROUP BY TO_CHAR(o.time, 'HH24')
        ORDER BY order_hour
    ) LOOP
        DBMS_OUTPUT.PUT_LINE('[I2] Hour ' || rec.order_hour || ': ' || rec.num_orders || ' orders');
    END LOOP;
END i2_orders_by_hour;
```

**I3. Category-wise distribution of pizzas (% share)**
```sql
PROCEDURE i3_category_distribution IS
    v_total NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM order_details;

    FOR rec IN (
        SELECT pt.category, COUNT(od.order_details_id) AS num_orders
        FROM order_details od
        JOIN pizzas p       ON od.pizza_id = p.pizza_id
        JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
        GROUP BY pt.category
        ORDER BY num_orders DESC
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            '[I3] ' || rec.category || ': ' || rec.num_orders || ' orders (' ||
            ROUND(rec.num_orders * 100.0 / v_total, 2) || '%)'
        );
    END LOOP;
END i3_category_distribution;
```

**I4. Average number of pizzas ordered per day**
```sql
FUNCTION i4_avg_pizzas_per_day RETURN NUMBER IS
    v_avg NUMBER;
BEGIN
    SELECT ROUND(AVG(daily_qty), 2) INTO v_avg
    FROM (
        SELECT o.date, SUM(od.quantity) AS daily_qty
        FROM order_details od
        JOIN orders o ON od.order_id = o.order_id
        GROUP BY o.date
    );
    RETURN v_avg;
END i4_avg_pizzas_per_day;
```

**I5. Top 3 most ordered pizza types based on revenue**
```sql
PROCEDURE i5_top3_by_revenue IS
BEGIN
    FOR rec IN (
        SELECT pt.name, SUM(od.quantity * p.price) AS revenue
        FROM order_details od
        JOIN pizzas p       ON od.pizza_id = p.pizza_id
        JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
        GROUP BY pt.name
        ORDER BY revenue DESC
        FETCH FIRST 3 ROWS ONLY
    ) LOOP
        DBMS_OUTPUT.PUT_LINE('[I5] ' || rec.name || ' -> $' || ROUND(rec.revenue, 2));
    END LOOP;
END i5_top3_by_revenue;
```

---

### Advanced

**A1. Percentage contribution of each pizza type to total revenue**
```sql
PROCEDURE a1_revenue_pct_contribution IS
BEGIN
    FOR rec IN (
        SELECT pt.name,
               SUM(od.quantity * p.price) AS revenue,
               ROUND(SUM(od.quantity * p.price) * 100.0 /
                     SUM(SUM(od.quantity * p.price)) OVER (), 2) AS pct
        FROM order_details od
        JOIN pizzas p       ON od.pizza_id = p.pizza_id
        JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
        GROUP BY pt.name
        ORDER BY pct DESC
    ) LOOP
        DBMS_OUTPUT.PUT_LINE('[A1] ' || rec.name || ': $' || ROUND(rec.revenue,2) || ' (' || rec.pct || '%)');
    END LOOP;
END a1_revenue_pct_contribution;
```

**A2. Cumulative revenue generated over time**
```sql
PROCEDURE a2_cumulative_revenue IS
    v_running_total NUMBER := 0;
BEGIN
    FOR rec IN (
        SELECT o.date AS order_date, SUM(od.quantity * p.price) AS revenue
        FROM order_details od
        JOIN orders o ON od.order_id = o.order_id
        JOIN pizzas p ON od.pizza_id = p.pizza_id
        GROUP BY o.date
        ORDER BY o.date
    ) LOOP
        v_running_total := v_running_total + rec.revenue;
        DBMS_OUTPUT.PUT_LINE(
            '[A2] ' || TO_CHAR(rec.order_date, 'YYYY-MM-DD') || ': $' ||
            ROUND(rec.revenue, 2) || ' | Cumulative: $' || ROUND(v_running_total, 2)
        );
    END LOOP;
END a2_cumulative_revenue;
```

**A3. Top 3 most ordered pizza types by revenue, per category**
```sql
PROCEDURE a3_top3_by_revenue_per_category(p_cursor OUT SYS_REFCURSOR) IS
BEGIN
    OPEN p_cursor FOR
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
        ORDER BY category, rnk;
END a3_top3_by_revenue_per_category;
```

---

### run_all — executes every printable procedure

```sql
PROCEDURE run_all IS
BEGIN
    b1_total_orders;
    b2_total_revenue;
    b3_highest_priced_pizza;
    b4_most_common_size;
    b5_top5_pizza_types;
    i1_qty_by_category;
    i2_orders_by_hour;
    i3_category_distribution;
    DBMS_OUTPUT.PUT_LINE('[I4] Avg Pizzas/Day: ' || i4_avg_pizzas_per_day);
    i5_top3_by_revenue;
    a1_revenue_pct_contribution;
    a2_cumulative_revenue;
END run_all;

END pizza_analytics_full;
/
```

---

## Usage

**Run everything at once:**
```sql
SET SERVEROUTPUT ON;
BEGIN
    pizza_analytics_full.run_all;
END;
/

-- A3 separately (returns a REF CURSOR):
VAR rc REFCURSOR;
EXEC pizza_analytics_full.a3_top3_by_revenue_per_category(:rc);
PRINT rc;
```

**Call any single procedure/function:**
```sql
BEGIN pizza_analytics_full.i5_top3_by_revenue; END;
/
SELECT pizza_analytics_full.i4_avg_pizzas_per_day FROM dual;
```

## Notes

- `b4_most_common_size` uses `ROWNUM = 1` on a pre-sorted inline view since `SELECT INTO` can't combine directly with `FETCH FIRST` in a scalar-into context.
- `a2_cumulative_revenue` computes the running total procedurally (`v_running_total := v_running_total + rec.revenue`) rather than via the `SUM() OVER()` analytic, since this is the more PL/SQL-native approach (loop-driven).
- `a3_top3_by_revenue_per_category` returns a `SYS_REFCURSOR` so results can be consumed by application code (Java, .NET, etc.) rather than just printed.
- Requires Oracle 12c+ for `FETCH FIRST n ROWS ONLY`. On older versions, replace with `ROWNUM`-based subqueries.
- If `orders.time` is stored as `VARCHAR2` rather than `DATE`/`TIMESTAMP`, replace `TO_CHAR(o.time, 'HH24')` with `SUBSTR(o.time, 1, 2)`.