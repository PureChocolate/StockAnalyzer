-- Create the Company table
CREATE TABLE company (
    Ticker VARCHAR(10) PRIMARY KEY,
    Name VARCHAR(255) NOT NULL
);

-- Create the Price/Volume table
CREATE TABLE pricevolume (
    Ticker VARCHAR(10),
    TransDate DATE,
    OpenPrice DECIMAL(10, 2),
    HighPrice DECIMAL(10, 2),
    LowPrice DECIMAL(10, 2),
    ClosingPrice DECIMAL(10, 2),
    Volume INT,
    AdjustedClose DECIMAL(10, 2),
    PRIMARY KEY (Ticker, TransDate),
    FOREIGN KEY (Ticker) REFERENCES company(Ticker)
);

CREATE INDEX idx_transdate ON pricevolume (TransDate);