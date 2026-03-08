# Stock Analyzer (Java & SQL)

A technical tool designed to extract historical stock data from a MySQL database, detect and adjust for stock splits, and execute a 50-day moving average trading strategy.

## Key Features
- **ETL Pipeline:** Automates data extraction from SQL via JDBC.
- **Split Detection:** Implements a mathematical heuristic to identify historical stock splits (2:1, 3:1, 3:2) and back-adjusts prices to maintain data integrity.
- **Strategy Simulation:** Calculates net cash positions by simulating trades based on a 50-day moving window of closing prices.

## Technologies Used
- **Language:** Java (utilizing Streams, Deques, and java.time)
- **Database:** MySQL
- **Integration:** JDBC for secure, prepared-statement queries.

## Setup
1. Use `schema.sql` to set up the required database tables.
2. Provide database credentials in a `.txt` properties file (see code for expected parameters).
3. Compile and run `StockAnalyzer.java`.
