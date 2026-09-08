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

-- Join the necessary tables to find the total quantity of each pizza category ordered.
-- Determine the distribution of orders by hour of the day.
-- Join relevant tables to find the category-wise distribution of pizzas.
-- Group the orders by date and calculate the average number of pizzas ordered per day.
-- Determine the top 3 most ordered pizza types based on revenue.

-- Advanced:
-- Calculate the percentage contribution of each pizza type to total revenue.
-- Analyze the cumulative revenue generated over time.
-- Determine the top 3 most ordered pizza types based on revenue for each pizza category.