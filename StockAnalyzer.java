import java.util.*;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.PrintWriter;
import java.time.*;
import java.time.format.*;
import java.util.stream.*;


class StockAnalyzer {
   
   static class StockData {
      private String ticker;
      private LocalDate date;
      private double openPrice;
      private double highPrice;
      private double lowPrice;
      private double closingPrice;
      private int numTraded;
      private double closingAdj;
      private String split = "";
    
      public StockData(String ticker,LocalDate date, double openPrice, double highPrice,
      double lowPrice, double closingPrice, int traded, double closingAdj){
         this.ticker = ticker;
         this.date = date;
         this.openPrice = openPrice;
         this.highPrice = highPrice;
         this.lowPrice = lowPrice;
         this.closingPrice = closingPrice;
         this.numTraded = traded;
         this.closingAdj = closingAdj;
      }

      public String getTicker(){
         return ticker;
      }

      public String getDate(){
         return date.format(formatter);
      }

      public LocalDate getDateObj(){
         return date;
      }

      public double getOpen(){
         return openPrice;
      }

      public void setOpen(double newOpen){
         this.openPrice = newOpen ;
      }

      public double getLow(){
         return lowPrice;
      }

      public void setLow(double newLow){
         this.lowPrice = newLow ;
      }

      public double getHigh(){
         return highPrice;
      }

      public void setHigh(double newHigh){
         this.highPrice = newHigh ;
      }

      public double getClosing(){
         return closingPrice;
      }

      public void setClosing(double newClosing){
         this.closingPrice = newClosing ;
      }

      public double getAdjustedClose(){
         return closingAdj;
      }

      public int getTraded(){
         return numTraded;
      }

      public String getSplit(){
         return split;
      }

      public void setSplit(String s){
         split = s;
      }

      public String toString(){
         String ret = "[";
         ret += ticker + ", " + this.getDate() + ", " + openPrice + ", " + highPrice + ", " + lowPrice + ", " + closingPrice + ", " + numTraded + ", " + closingAdj + "]";
         return ret;
      }
   }
   
   static Connection conn;
   static final String prompt = "Enter ticker symbol [start/end dates]: ";
   static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
   
   public static void main(String[] args) throws Exception {
	//   String paramsFile = "ConnectionParameters_Computer.txt";
      String paramsFile = "ConnectionParameters_RemoteComputer.txt";

      if (args.length >= 1) {
         paramsFile = args[0];
      }
      
      Properties connectprops = new Properties();
      connectprops.load(new FileInputStream(paramsFile));
      try {
         Class.forName("com.mysql.cj.jdbc.Driver");
         String dburl = connectprops.getProperty("dburl");
         String username = connectprops.getProperty("user");
         conn = DriverManager.getConnection(dburl, connectprops);
         System.out.println("Database connection is established");
         
         Scanner in = new Scanner(System.in);
         System.out.print(prompt);
         String input = in.nextLine().trim();
         
         while (input.length() > 0) {
            String[] params = input.split("\\s+");
            String ticker = params[0];
            String startdate = null, enddate = null;

            //Assuming we dont allow a single beggining/ending dates for a stock, we can say that if we dont have
            //Fully specified range or no range, the input is not valid and proceed with just the ticker.
            if(params.length != 3 && params.length != 1){
               System.out.println("Possible incorrect input stock and dates, assuming no start/end. Please make sure there are no unnecessary spaces.\n" +
               "The format should follow [Ticker] [yyyy.mm.dd] [yyyy.mm.dd]");
            }
            if (getName(ticker)) {
               if (params.length == 3) {                  
                  //verify that there is proper format for given dates, else default to no range
                  try{
                     LocalDate.parse(params[1],formatter);
                     LocalDate.parse(params[2],formatter);
                     startdate = params[1];
                     enddate = params[2];
                  } catch(DateTimeParseException e){
                     System.out.println("Impropper format for dates, assuming no start/end.");
                  }                  
               }
               Deque<StockData> data = getStockData(ticker, startdate, enddate);
               System.out.println("\nExecuting investment strategy");
               doStrategy(ticker, data);
            }

            System.out.print("\n"+prompt);
            input = in.nextLine().trim();
         }
         
         // Close the database connection
         in.close();
         System.out.println("Database connection closed.");
         conn.close();

      } catch (SQLException ex) {
         System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n",
                           ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
      }
   }

   //Claculate split based on defined rules
   static double splitOccur(double closingOld, double openingNew){
      if(Math.abs((closingOld/openingNew) - 2.0) < .20) return 2.0;
      else if(Math.abs((closingOld/openingNew) - 3.0) <.30) return 3.0;
      else if(Math.abs((closingOld/openingNew) -1.5) < .15) return 1.5;
      else return 1.0;
  }
   
   static boolean getName(String ticker) throws SQLException {
      try{
         String query = "select Name from company where Ticker = ?";
         PreparedStatement psmt = conn.prepareStatement(query);
         psmt.setString(1, ticker);
         ResultSet ret = psmt.executeQuery();

         if (ret.next()){
            System.out.printf("%s\n",ret.getString(1));
            return true;
         }
         else System.out.println("Stock for " + ticker + " not found in database.");

      } catch(SQLException e){
         System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n",
         e.getMessage(), e.getSQLState(), e.getErrorCode());
      }
      return false;
   }

   static Deque<StockData> getStockData(String ticker, String start, String end) {

      Deque<StockData> result = new ArrayDeque<>();
      try {
         PreparedStatement psmt;
         //Change query depending on if we are given a range of dates or not
         if (start != null && end != null){
            String query = "select * from pricevolume where Ticker = ? and (? <= TransDate) and (TransDate <= ?) order by TransDate DESC";
            psmt = conn.prepareStatement(query);
            psmt.setString(1, ticker);
            psmt.setString(2, start);
            psmt.setString(3, end);           
         }
         else{
            String query = "select * from pricevolume where Ticker = ? order by TransDate DESC";
            psmt = conn.prepareStatement(query);
            psmt.setString(1, ticker);
         }

         ResultSet ret = psmt.executeQuery();
         while(ret.next()){

            //convert the number values to proper data type
            String[] numHolder = new String[] {ret.getString(3),ret.getString(4),ret.getString(5),ret.getString(6),ret.getString(8)};
            double[] numerics = Stream.of(numHolder).mapToDouble(t -> Double.parseDouble(t)).toArray();

            int traded = Integer.parseInt(ret.getString(7).trim());            
            LocalDate pDate = LocalDate.parse(ret.getString(2), formatter);

            //create the stock object using convereted data types
            StockData stock = new StockData(ticker, pDate, numerics[0], numerics[1], numerics[2], numerics[3], traded, numerics[4]);
            result.addFirst(stock);
         }
      } catch (SQLException e){
         System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n",
         e.getMessage(), e.getSQLState(), e.getErrorCode());
      }

      // Identify splits and adjust the data while iterating
      double closingOld = -1;
      double openingNew= -1;
      double split = 1;
      int sCounter = 0;
      double mult = 1.0;
      
      Iterator<StockData> itr = result.descendingIterator();
      while(itr.hasNext()){
         StockData stock = itr.next();

         //used to have store the first stock value and skip rest
         // as there was nothing to compare to before this
         if(openingNew < 0){
            openingNew = stock.getOpen();
            continue;
         }

         //get the current closing and process it against last stocks opening
         // This is backwards due to the way of the input being in descending order(next day comes before previous)
         closingOld = stock.getClosing();
         split = splitOccur(closingOld, openingNew);
         
         //Adjust the multiplier for as we ecnounter splits
         mult *= split;
         
         if(split > 1){
            sCounter++;
            String type = "";
            if(split == 1.5) type = "3:2";
            else type = (int)split + ":1";
            stock.setSplit(String.format("%-10s %-10s %10.2f %5s %10.2f\n", type +" split on",stock.getDate(),closingOld,"-->",openingNew));
            System.out.print(stock.getSplit());
         }

         //roll over the current stock to be considered previous
         openingNew = stock.getOpen();
         
         //Apply the adjustment using our multiplier
         stock.setOpen(stock.getOpen()/mult);
         stock.setHigh(stock.getHigh()/mult);
         stock.setLow(stock.getLow()/mult);
         stock.setClosing(stock.getClosing()/mult);
      }
      System.out.println(sCounter + " Splits in " + result.size() + " days");
      return result;
   }
   
   static void doStrategy(String ticker, Deque<StockData> data) {
      //If not enough data points disregard starategy
      if(data.size() < 51){
         System.out.println("No trading done \nNet cash: 0");
      }
      else{    
         
         //initialize variables to process the strategy
         double cash = 0;
         double shares = 0;
         Deque<Double> fData = new ArrayDeque<>(); //Used to store and calculate the moving 50 day avg
         boolean buy = false;
         StockData dPrior = data.removeFirst(); //Holds the prior or d-1 stock
         
         //store the intitail 50 data points
         fData.add(dPrior.getClosing());
         for(int i = 2; i <= 50; i++){
            dPrior = data.removeFirst();
            fData.add(dPrior.getClosing());           
         }

         int transactions = 0;
         double avg;
         while(data.size()>= 2){
            avg = movingAvg(fData); //calculate the avg 

            //setup our current stock values
            StockData d = data.removeFirst();
            double close = d.getClosing();
            double open = d.getOpen();

            //Since we need to use d+1 price to trade if buy occurs
            //We use an boolean 'buy' to notify us when we move over
            if(buy){
               shares += 100;
               cash -= (100 * open);
               buy = false;
               transactions++;
            }
            if(close < avg && (close/open) < 0.97000001) buy = true;

            //sell transaction
            else if(shares >= 100 && open > avg && (open / dPrior.getClosing()) > 1.00999999){
               shares -= 100;
               cash += 100 * ((open + close)/2);
               transactions++;
            }
            
            //adjust data points for the moving window to get new avg on the next day
            fData.removeFirst();
            fData.add(close);
            dPrior = d;
         }
         cash -= 8.0 * transactions; //Take out the fee for all the transactions

         StockData last = data.removeFirst();
         if(shares > 0) cash += (shares * last.getOpen());

         System.out.println("Transactions executed: " + transactions);
         System.out.printf("Net cash: %.2f\n",cash);
      }
   }

   static Double movingAvg(Deque<Double> d){
      Double ret = 0.0;
      for(Double n: d){
         ret += n;
      }
      ret = ret/50.0;
      return ret;
   }
}
