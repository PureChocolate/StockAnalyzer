# Market Data ETL (Java & SQL)

A robust ETL utility designed to extract historical stock data from a MySQL database, reconcile data integrity issues caused by stock splits, and execute a 50-day moving average trading strategy.

##  Technical Logic: Data Reconciliation
Handling historical financial data requires more than simple queries; it requires ensuring data integrity against corporate actions that create "price cliffs."

### Stock Split Heuristic
The engine identifies and corrects for splits (e.g., a 2:1 split showing a 50% overnight drop) by:
1. **Detection:** Scanning for price fluctuations exceeding a 40% threshold within a single session.
2. **Verification:** Cross-referencing volume spikes and historical ratios to confirm a split vs. market volatility.
3. **Reconciliation:** Automatically back-adjusting the database to normalize the 50-day Moving Average (MA) calculations.

##  Technologies Used
- **Language:** Java 11+ (Utilizing Streams, Deques, and java.time for efficient data handling).
- **Database:** MySQL.
- **Integration:** JDBC with Prepared Statements for secure, optimized database communication.

## Setup
1. Use `schema.sql` to set up the required database tables.
2. Provide database credentials in a `.txt` properties file (see code for expected parameters).
3. Compile and run `StockAnalyzer.java`.
4. To run: Copy `ConnectionParameters_RemoteComputer.txt.example` to `ConnectionParameters_RemoteComputer.txt` and add your database credentials.
