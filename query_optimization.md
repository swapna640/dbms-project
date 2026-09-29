#Recommended indexes

  CREATE INDEX idx_od_pizza_qty ON order_details (pizza_id, quantity);   -- covering for pizza-level aggregates
  CREATE INDEX idx_od_order_pizza ON order_details (order_id, pizza_id, quantity); -- covering for order-level aggregates
  CREATE INDEX idx_orders_date ON orders (date);
  CREATE INDEX idx_pizzas_type ON pizzas (pizza_type_id);
  -- pizzas(pizza_id), pizza_types(pizza_type_id), orders(order_id) should already be primary keys

Optimized queries

1) Total orders. order_id is unique, so DISTINCT forces a needless dedupe.
   SELECT COUNT(*) AS total_orders FROM orders;

2) Total revenue. Uses INNER JOIN and qualified columns.
   SELECT SUM(od.quantity * p.price) AS total_revenue
   FROM order_details od
   JOIN pizzas p ON p.pizza_id = od.pizza_id;

3) Highest-priced pizza. ORDER BY ... LIMIT 1 silently drops ties. This version returns all tied pizzas and can use an index on price.
   SELECT pizza_id, pizza_type_id, size, price
   FROM pizzas
   WHERE price = (SELECT MAX(price) FROM pizzas);

4) Most common size. Aggregate by pizza_id first (about 96 groups), then roll up by size.
   WITH qty AS (
     SELECT pizza_id, SUM(quantity) AS q
     FROM order_details
     GROUP BY pizza_id
   )
   SELECT p.size, SUM(qty.q) AS total_quantity
   FROM qty
   JOIN pizzas p ON p.pizza_id = qty.pizza_id
   GROUP BY p.size
   ORDER BY total_quantity DESC
   LIMIT 1;
   
5) Top 5 most ordered pizza types. Bug: the original used MAX(quantity), which is the largest single line item, not the total ordered. It also grouped by size-specific pizza_id, not type.
   WITH qty AS (
     SELECT pizza_id, SUM(quantity) AS q
     FROM order_details
     GROUP BY pizza_id
   )
   SELECT p.pizza_type_id, SUM(qty.q) AS total_quantity
   FROM qty
   JOIN pizzas p ON p.pizza_id = qty.pizza_id
   GROUP BY p.pizza_type_id
   ORDER BY total_quantity DESC
   LIMIT 5;

6) Quantity per pizza category. Bug: the original grouped by pizza_type_id, not category, and had an unqualified column.
   WITH qty AS (
     SELECT pizza_id, SUM(quantity) AS q
     FROM order_details
     GROUP BY pizza_id
   )
   SELECT pt.category, SUM(qty.q) AS total_quantity
   FROM qty
   JOIN pizzas p        ON p.pizza_id = qty.pizza_id
   JOIN pizza_types pt  ON pt.pizza_type_id = p.pizza_type_id
   GROUP BY pt.category
   ORDER BY total_quantity DESC;

7) Orders by hour. COUNT(*) is cheaper than COUNT(order_id). MySQL 8 no longer sorts GROUP BY output implicitly, so add ORDER BY. For frequent use, add a stored generated column with an index, so the HOUR() result is precomputed.
   SELECT HOUR(`time`) AS hour, COUNT(*) AS number_of_orders
   FROM orders
   GROUP BY hour
   ORDER BY hour;

   -- optional
   ALTER TABLE orders ADD COLUMN order_hour TINYINT AS (HOUR(`time`)) STORED, ADD INDEX idx_hour (order_hour);

8) Average order value. This removes the derived table (no temp table). Total revenue divided by the number of orders is mathematically the same, in one pass.
   SELECT SUM(od.quantity * p.price) / COUNT(DISTINCT od.order_id) AS avg_order_value
   FROM order_details od
   JOIN pizzas p ON p.pizza_id = od.pizza_id;

9) Average pizzas per day. Also single-pass with no derived table.
  SELECT SUM(od.quantity) / COUNT(DISTINCT o.date) AS avg_pizzas_per_day
  FROM order_details od
  JOIN orders o ON o.order_id = od.order_id;

10) Top 3 pizza types by revenue. Aggregate before joining.
   WITH rev AS (
     SELECT pizza_id, SUM(quantity) AS q
     FROM order_details
     GROUP BY pizza_id
   )
   SELECT p.pizza_type_id, SUM(rev.q * p.price) AS revenue
   FROM rev
   JOIN pizzas p ON p.pizza_id = rev.pizza_id
   GROUP BY p.pizza_type_id
   ORDER BY revenue DESC
   LIMIT 3;

11) % contribution to revenue. The original scanned the fact table twice (once in the subquery). A window function over the grouped result needs only one scan.
    WITH sales AS (
        SELECT pizza_id, SUM(quantity) AS q
        FROM order_details
        GROUP BY pizza_id
    ),
    by_type AS (
        SELECT p.pizza_type_id, SUM(s.q * p.price) AS revenue
        FROM sales s
        JOIN pizzas p ON p.pizza_id = s.pizza_id
        GROUP BY p.pizza_type_id
    )
    SELECT pt.name AS pizza_type,
        ROUND(bt.revenue, 2) AS revenue,
        ROUND(bt.revenue * 100 / SUM(bt.revenue) OVER (), 2) AS pct_of_total_revenue
    FROM by_type bt
    JOIN pizza_types pt ON pt.pizza_type_id = bt.pizza_type_id
    ORDER BY pct_of_total_revenue DESC;

12) Cumulative revenue. The default window frame is RANGE, which is slower because it must handle peer rows. An explicit ROWS frame is cheaper, and it's safe here because there is one row per date. The extra ORDER BY is dropped since the window's ordering already applies.
    WITH daily AS (
       SELECT o.date AS order_date, SUM(od.quantity * p.price) AS revenue
       FROM order_details od
       JOIN orders o ON o.order_id = od.order_id
       JOIN pizzas p ON p.pizza_id = od.pizza_id
       GROUP BY o.date
   )
   SELECT order_date,
          ROUND(revenue, 2) AS revenue,
          ROUND(SUM(revenue) OVER (ORDER BY order_date ROWS UNBOUNDED PRECEDING), 2) AS cumulative_revenue
   FROM daily
   ORDER BY order_date;

13) Top 3 pizza types per category. Same pre-aggregation. I kept RANK() (ties included); swap to ROW_NUMBER() if you need exactly 3 rows per category.
    WITH sales AS (
       SELECT pizza_id, SUM(quantity) AS q
       FROM order_details
       GROUP BY pizza_id
   ),
   by_type AS (
      SELECT pt.category, pt.name AS pizza_type, SUM(s.q * p.price) AS revenue
      FROM sales s
      JOIN pizzas p       ON p.pizza_id = s.pizza_id
      JOIN pizza_types pt ON pt.pizza_type_id = p.pizza_type_id
      GROUP BY pt.category, pt.name
   ),
   ranked AS (
     SELECT *, RANK() OVER (PARTITION BY category ORDER BY revenue DESC) AS rnk
     FROM by_type
   )
   SELECT category, pizza_type, ROUND(revenue, 2) AS revenue
   FROM ranked
   WHERE rnk <= 3
   ORDER BY category, rnk;
    
