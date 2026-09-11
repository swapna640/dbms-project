-- 1) Retrieve the total number of orders placed.
SELECT COUNT(DISTINCT order_id) AS total_orders
FROM orders;
-- 2) Calculate the total revenue generated from pizza sales.
SELECT SUM(price * quantity) as total_revenue
FROM order_details
LEFT JOIN pizzas ON order_details.pizza_id = pizzas.pizza_id;
-- 3) Identify the highest-priced pizza.
SELECT pizza_type_id, price AS max_price
FROM pizzas
ORDER BY price DESC
LIMIT 1;
-- 4) Identify the most common pizza size ordered.
SELECT pizzas.size, SUM(order_details.quantity) AS total_quantity
FROM order_details
LEFT JOIN pizzas ON order_details.pizza_id = pizzas.pizza_id
GROUP BY pizzas.size
ORDER BY total_quantity DESC
LIMIT 1;
-- 5) List the top 5 most ordered pizza types along with their quantities.
SELECT pizza_id, MAX(quantity) AS max_quantity
FROM order_details
GROUP BY pizza_id
ORDER BY max_quantity DESC
limit 5;

-- 6) Join the necessary tables to find the total quantity of each pizza category ordered.
SELECT pizzas.pizza_type_id, SUM(order_details.quantity) AS "Total amount"
FROM order_details
LEFT JOIN pizzas ON pizzas.pizza_id = order_details.pizza_id
GROUP BY pizza_type_id;
-- 7) Determine the distribution of orders by hour of the day.
SELECT HOUR(time) AS Hour, COUNT(order_id) AS number_of_orders
FROM orders
GROUP BY Hour;
-- 8) Find the average order value (revenue per order)
SELECT AVG(order_total) AS avg_order_value
FROM (
    SELECT order_details.order_id, SUM(order_details.quantity * pizzas.price) AS order_total
    FROM order_details
    LEFT JOIN pizzas ON pizzas.pizza_id = order_details.pizza_id
    GROUP BY order_details.order_id
) AS order_totals;

-- 9) Group the orders by date and calculate the average number of pizzas ordered per day.
SELECT AVG(pizzas_per_day) AS avg_pizzas_per_day
FROM (
    SELECT orders.date, SUM(order_details.quantity) AS pizzas_per_day
    FROM order_details
    LEFT JOIN orders ON order_details.order_id = orders.order_id
    GROUP BY orders.date
) AS daily_totals;
-- 10) Determine the top 3 most ordered pizza types based on revenue.
SELECT pizzas.pizza_type_id, SUM(order_details.quantity * pizzas.price) AS revenue
FROM order_details
LEFT JOIN pizzas ON order_details.pizza_id = pizzas.pizza_id
GROUP BY pizzas.pizza_type_id
ORDER BY revenue DESC
LIMIT 3;

-- 11) Calculate the percentage contribution of each pizza type to total revenue.
SELECT
    pt.name AS pizza_type,
    ROUND(SUM(od.quantity * p.price), 2) AS revenue,
    ROUND(
        SUM(od.quantity * p.price) * 100.0 /
        (SELECT SUM(od2.quantity * p2.price)
         FROM order_details od2
         JOIN pizzas p2 ON od2.pizza_id = p2.pizza_id),
        2
    ) AS pct_of_total_revenue
FROM order_details od
JOIN pizzas p       ON od.pizza_id = p.pizza_id
JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
GROUP BY pt.name
ORDER BY pct_of_total_revenue DESC;
-- 12) Analyze the cumulative revenue generated over time.
WITH daily_revenue AS (
    SELECT
        o.date AS order_date,
        SUM(od.quantity * p.price) AS revenue
    FROM order_details od
    JOIN orders o  ON od.order_id = o.order_id
    JOIN pizzas p  ON od.pizza_id = p.pizza_id
    GROUP BY o.date
)
SELECT
    order_date,
    revenue,
    ROUND(SUM(revenue) OVER (ORDER BY order_date), 2) AS cumulative_revenue
FROM daily_revenue
ORDER BY order_date;
-- 13) Determine the top 3 most ordered pizza types based on revenue for each pizza category.
WITH revenue_by_type AS (
    SELECT
        pt.category,
        pt.name AS pizza_type,
        SUM(od.quantity * p.price) AS revenue
    FROM order_details od
    JOIN pizzas p       ON od.pizza_id = p.pizza_id
    JOIN pizza_types pt ON p.pizza_type_id = pt.pizza_type_id
    GROUP BY pt.category, pt.name
),
ranked AS (
    SELECT
        category,
        pizza_type,
        revenue,
        RANK() OVER (PARTITION BY category ORDER BY revenue DESC) AS rnk
    FROM revenue_by_type
)
SELECT category, pizza_type, revenue
FROM ranked
WHERE rnk <= 3
ORDER BY category, rnk;