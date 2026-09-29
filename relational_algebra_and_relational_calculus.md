1) Total number of orders
RA
  γ COUNT(order_id) → total_orders (orders)
TRC
  { t | t.total_orders = COUNT{ o.order_id | o ∈ orders } }


2) Total revenue
RA
  γ SUM(quantity × price) → total_revenue (order_details ⋈ pizzas)
TRC
  { t | t.total_revenue = SUM{ d.quantity × p.price |
          d ∈ order_details ∧ p ∈ pizzas ∧ d.pizza_id = p.pizza_id } }
   

3) Highest-priced pizza
This can be written in pure algebra and pure calculus, with no aggregate. Keep the pizzas for which no higher-priced pizza exists.
RA
  pizzas − π p1.* ( σ p1.price < p2.price ( ρ p1(pizzas) × ρ p2(pizzas) ) )
TRC
  { p | p ∈ pizzas ∧ ¬∃q ( q ∈ pizzas ∧ q.price > p.price ) }
Ties are all returned, as in the optimized SQL.


4) Most common pizza size
Let S = _size γ SUM(quantity) → total_quantity (OP).
RA
   S − π s1.* ( σ s1.total_quantity < s2.total_quantity ( ρ s1(S) × ρ s2(S) ) )
Equivalent with limit: λ₁( τ total_quantity DESC (S) ).
TRC
   { t | t ∈ S ∧ ¬∃u ( u ∈ S ∧ u.total_quantity > t.total_quantity ) }
where
   S = { s | ∃o ∈ OP ( s.size = o.size ∧
           s.total_quantity = SUM{ x.quantity | x ∈ OP ∧ x.size = s.size } ) }


5) Top 5 most ordered pizza types
Let T = _pizza_type_id γ SUM(quantity) → total_quantity (OP).
RA
   λ₅ ( τ total_quantity DESC (T) )
TRC (a tuple qualifies if fewer than 5 types sell more)
   { t | t ∈ T ∧ COUNT{ u | u ∈ T ∧ u.total_quantity > t.total_quantity } < 5 }

   
6) Quantity per pizza category
RA
   _category γ SUM(quantity) → total_quantity (OPT)
TRC
   { t | ∃x ∈ OPT ( t.category = x.category ∧
           t.total_quantity = SUM{ y.quantity | y ∈ OPT ∧ y.category = t.category } ) }


7) Orders by hour of day
RA
   _HOUR(time)→hour γ COUNT(order_id) → number_of_orders (orders)
TRC
   { t | ∃o ∈ orders ( t.hour = HOUR(o.time) ∧
           t.number_of_orders = COUNT{ x.order_id | x ∈ orders ∧ HOUR(x.time) = t.hour } ) }


8) Average order value
Let OT = _order_id γ SUM(quantity × price) → order_total (OP).
RA
   γ AVG(order_total) → avg_order_value (OT)
TRC
   { t | t.avg_order_value = AVG{ u.order_total | u ∈ OT } }
   OT = { u | ∃d ∈ order_details ( u.order_id = d.order_id ∧
           u.order_total = SUM{ x.quantity × p.price | x ∈ order_details ∧ p ∈ pizzas ∧
                                x.order_id = u.order_id ∧ x.pizza_id = p.pizza_id } ) }


9) Average pizzas per day
Let DT = _date γ SUM(quantity) → pizzas_per_day (order_details ⋈ orders).
RA
   γ AVG(pizzas_per_day) → avg_pizzas_per_day (DT)
TRC
   { t | t.avg_pizzas_per_day = AVG{ u.pizzas_per_day | u ∈ DT } }
   DT = { u | ∃o ∈ orders ( u.date = o.date ∧
           u.pizzas_per_day = SUM{ x.quantity | x ∈ order_details ∧ y ∈ orders ∧
                                   x.order_id = y.order_id ∧ y.date = u.date } ) }
10) Top 3 pizza types by revenue
Let R = _pizza_type_id γ SUM(quantity × price) → revenue (OP).
RA
   λ₃ ( τ revenue DESC (R) )
TRC
   { t | t ∈ R ∧ COUNT{ u | u ∈ R ∧ u.revenue > t.revenue } < 3 }


11) Percentage contribution of each pizza type to total revenue
Let:
R   = _name γ SUM(quantity × price) → revenue (OPT)
Tot = γ SUM(quantity × price) → total (OP)
RA
   τ pct DESC ( π name, revenue, ROUND(revenue × 100 / total, 2) → pct ( R × Tot ) )
Tot has exactly one tuple, so the cross product just attaches the grand total to every row.
TRC
   { t | ∃r ∈ R ( t.name = r.name ∧ t.revenue = r.revenue ∧
           t.pct = ROUND( r.revenue × 100 /
                   SUM{ x.quantity × p.price | x ∈ order_details ∧ p ∈ pizzas ∧
                                               x.pizza_id = p.pizza_id }, 2) ) }


12) Cumulative revenue over time
Let D = _date γ SUM(quantity × price) → revenue (order_details ⋈ orders ⋈ pizzas).
RA
   τ date ( _{d1.date, d1.revenue} γ SUM(d2.revenue) → cumulative_revenue
             ( σ d2.date ≤ d1.date ( ρ d1(D) × ρ d2(D) ) ) )
TRC
   { t | ∃d1 ∈ D ( t.date = d1.date ∧ t.revenue = d1.revenue ∧
           t.cumulative_revenue = SUM{ d2.revenue | d2 ∈ D ∧ d2.date ≤ d1.date } ) }
The self-join with ≤ is the algebraic form of SUM() OVER (ORDER BY date).


13) Top 3 pizza types per category by revenue
Let R = _{category, name} γ SUM(quantity × price) → revenue (OPT).
RA (extended, with window operator)
   π category, name, revenue (
     σ rnk ≤ 3 (
       ω RANK() OVER (PARTITION BY category ORDER BY revenue DESC) → rnk (R) ) )
TRC. RANK() equals 1 plus the number of tuples in the same category with higher revenue, so:
{    t | t ∈ R ∧ COUNT{ u | u ∈ R ∧ u.category = t.category ∧ u.revenue > t.revenue } < 3 }
This matches RANK() ≤ 3, including ties.
