-- Initialize database schema and populate with sample data

-- Create tables for a simple e-commerce/sales scenario
CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    country VARCHAR(50),
    signup_date DATE NOT NULL,
    total_spent DECIMAL(10, 2) DEFAULT 0
);

CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    order_date TIMESTAMP NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL
);

-- Insert sample customers
INSERT INTO customers (name, email, country, signup_date, total_spent) VALUES
('Alice Johnson', 'alice@example.com', 'USA', '2023-01-15', 1250.00),
('Bob Smith', 'bob@example.com', 'Canada', '2023-02-20', 890.50),
('Charlie Brown', 'charlie@example.com', 'UK', '2023-03-10', 2340.75),
('Diana Prince', 'diana@example.com', 'USA', '2023-04-05', 567.25),
('Eve Davis', 'eve@example.com', 'Germany', '2023-05-12', 1890.00),
('Frank Miller', 'frank@example.com', 'France', '2023-06-18', 445.50),
('Grace Lee', 'grace@example.com', 'Japan', '2023-07-22', 3210.00),
('Henry Wilson', 'henry@example.com', 'Australia', '2023-08-30', 678.90),
('Iris Chen', 'iris@example.com', 'Singapore', '2023-09-14', 1560.25),
('Jack Taylor', 'jack@example.com', 'USA', '2023-10-08', 920.00);

-- Insert sample products
INSERT INTO products (name, category, price, stock_quantity, description) VALUES
('Laptop Pro', 'Electronics', 1299.99, 45, 'High-performance laptop for professionals'),
('Wireless Mouse', 'Electronics', 29.99, 200, 'Ergonomic wireless mouse'),
('Office Chair', 'Furniture', 249.99, 30, 'Comfortable ergonomic office chair'),
('Desk Lamp', 'Furniture', 45.50, 80, 'LED desk lamp with adjustable brightness'),
('Coffee Maker', 'Appliances', 89.99, 60, 'Programmable coffee maker'),
('Water Bottle', 'Accessories', 19.99, 150, 'Stainless steel insulated water bottle'),
('Backpack', 'Accessories', 59.99, 100, 'Durable travel backpack'),
('Monitor 27"', 'Electronics', 349.99, 55, '27-inch 4K monitor'),
('Keyboard Mechanical', 'Electronics', 129.99, 75, 'RGB mechanical keyboard'),
('Headphones', 'Electronics', 199.99, 90, 'Noise-cancelling headphones');

-- Insert sample orders
INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
(1, '2023-11-01 10:30:00', 1329.98, 'completed'),
(2, '2023-11-02 14:15:00', 299.98, 'completed'),
(3, '2023-11-03 09:45:00', 1649.97, 'completed'),
(4, '2023-11-04 16:20:00', 89.99, 'completed'),
(5, '2023-11-05 11:00:00', 579.98, 'completed'),
(1, '2023-11-06 13:30:00', 249.99, 'processing'),
(6, '2023-11-07 10:15:00', 199.99, 'completed'),
(7, '2023-11-08 15:45:00', 1949.96, 'completed'),
(8, '2023-11-09 12:00:00', 179.97, 'shipped'),
(9, '2023-11-10 14:30:00', 699.97, 'completed'),
(10, '2023-11-11 09:20:00', 479.97, 'processing'),
(3, '2023-11-12 16:45:00', 349.99, 'completed'),
(4, '2023-11-13 11:30:00', 59.99, 'shipped'),
(5, '2023-11-14 13:15:00', 129.99, 'completed'),
(2, '2023-11-15 10:45:00', 45.50, 'completed');

-- Insert sample order items
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
(1, 1, 1, 1299.99),
(1, 2, 1, 29.99),
(2, 3, 1, 249.99),
(2, 4, 1, 45.50),
(3, 1, 1, 1299.99),
(3, 8, 1, 349.99),
(4, 5, 1, 89.99),
(5, 9, 2, 129.99),
(5, 10, 1, 199.99),
(6, 3, 1, 249.99),
(7, 10, 1, 199.99),
(8, 1, 1, 1299.99),
(8, 8, 1, 349.99),
(8, 2, 1, 29.99),
(9, 6, 3, 19.99),
(9, 7, 2, 59.99),
(10, 9, 2, 129.99),
(10, 5, 1, 89.99),
(11, 7, 3, 59.99),
(11, 6, 5, 19.99),
(12, 8, 1, 349.99),
(13, 7, 1, 59.99),
(14, 9, 1, 129.99),
(15, 4, 1, 45.50);

-- Create indexes for better query performance
CREATE INDEX idx_customers_country ON customers(country);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_products_category ON products(category);

-- Grant privileges
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO studyuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO studyuser;
