import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.io.*;
import java.io.File;

class QueryRunner implements Runnable
{
    //  Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket)
    {
        this.socketConnection =  clientSocket;
    }

    public void run()
    {
        String url = "jdbc:postgresql://localhost:5432/dbms_project";
        String user = "postgres";
        String password = "123456";
        Connection c = null;
        Statement stmt = null;
      try
        {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(url, user, password);
            System.out.println("Opened database successfully");
            stmt = c.createStatement();
            //  Reading data from client
            InputStreamReader inputStream = new InputStreamReader(socketConnection
                                                                  .getInputStream()) ;
            BufferedReader bufferedInput = new BufferedReader(inputStream) ;
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection
                                                                     .getOutputStream()) ;
            BufferedWriter bufferedOutput = new BufferedWriter(outputStream) ;
            PrintWriter printWriter = new PrintWriter(bufferedOutput, true) ;
            String clientCommand = "" ;
            String sql = "ALTER DATABASE dbms_project SET DEFAULT_TRANSACTION_ISOLATION TO 'serializable';";
            //stmt.executeQuery(sql);
            // Read client query from the socket endpoint
            clientCommand = bufferedInput.readLine(); 

            while( ! clientCommand.equals("#")){
                
                System.out.println("Recieved data <" + clientCommand + "> from client : " 
                                    + socketConnection.getRemoteSocketAddress().toString());

                String s = "";
                s = clientCommand;
                StringTokenizer st = new StringTokenizer(s, " ");
                int a = Integer.parseInt(st.nextToken());
                String[] arr = new String[a];
                for (int i = 0; i < a; i++) {
                    arr[i] = (st.nextToken());
                    if (i != a - 1) {
                        arr[i] = arr[i].substring(0, arr[i].length() - 1);
                    }
                }
                int trNo = Integer.parseInt(st.nextToken());
                String tDate = st.nextToken();
                String type = st.nextToken();
                int remaining = 0;
                if(type.equals("AC")){
                    sql = "select ac_remaining as ac from trains_running where doj = ? and train_number = ?";
                    PreparedStatement stmt1 = c.prepareStatement(sql);
                    stmt1.setInt(2, trNo);
                    stmt1.setString(1, tDate);

                    ResultSet rs = stmt1.executeQuery();
                    
                    while(rs.next()){
                        remaining = rs.getInt("ac");
                    }
                    System.out.println(remaining);
                    if(remaining < a){
                        printWriter.println("No ticket booked\n");
                        printWriter.println("-------------------------------------------------------------------------------------------\n");
                        System.out.println("No ticket booked "+ trNo);
                        clientCommand = bufferedInput.readLine(); 
                        continue;
                    }
                    sql = "UPDATE trains_running set ac_remaining = ? where doj = ? and train_number = ?;";
                    PreparedStatement stmt2 = c.prepareStatement(sql);
                    stmt2.setInt(1, remaining - a);
                    stmt2.setString(2, tDate);
                    stmt2.setInt(3, trNo);
                    stmt2.execute();
                }
                else{
                    sql = "select sl_remaining as sl from trains_running where doj = ? and train_number = ?";
                    PreparedStatement stmt1 = c.prepareStatement(sql);
                    stmt1.setInt(2, trNo);
                    stmt1.setString(1, tDate);

                    ResultSet rs = stmt1.executeQuery();
                    while(rs.next()){
                        remaining = rs.getInt("sl");
                    }
                    System.out.println(remaining);

                    if(remaining < a){
                        printWriter.println("No ticket booked\n");
                        printWriter.println("-------------------------------------------------------------------------------------------\n");
                        System.out.println("No ticket booked "+ trNo);
                        clientCommand = bufferedInput.readLine(); 
                        continue;
                    }
                    sql = "UPDATE trains_running set sl_remaining = ? where doj = ? and train_number = ?;";
                    PreparedStatement stmt2 = c.prepareStatement(sql);
                    stmt2.setInt(1, remaining - a);
                    stmt2.setString(2, tDate);
                    stmt2.setInt(3, trNo);
                    stmt2.execute();
                }
                
                int date = (tDate.charAt(8)-'0') * 10 + tDate.charAt(9) - '0';
                int month = (tDate.charAt(5)-'0') * 10 + tDate.charAt(6) - '0';
                int year = (tDate.charAt(2)-'0') * 10 + tDate.charAt(3) - '0';
                String pnr = "" + trNo + ((remaining - 1)/18 + 1) + (((remaining-1) % 18) + 1) + date + month + year;
                while(pnr.length() < 12){
                    pnr = pnr + '_';
                }
                
                
                for(int i = 0; i < a; i++){
                    PreparedStatement stmt1 = c.prepareStatement("call booking(?,?,?,?,?,?);");
                    stmt1.setInt(1,remaining--);
                    stmt1.setString(2, pnr);
                    stmt1.setString(3,arr[i]);
                    stmt1.setInt(4,trNo);
                    stmt1.setString(5,tDate);
                    stmt1.setString(6,type);                    
                    stmt1.execute();
                    stmt1.close();
                }   

                sql = "select * from tickets_booked where pnr = ?";
                PreparedStatement stmt1 = c.prepareStatement(sql);
                stmt1.setString(1, pnr);                

                ResultSet rs = stmt1.executeQuery();
                printWriter.println("PNR\t\t\t\tName\tCoach\tBerth\tType\tTrainNo\tDate\n");
                while (rs.next()) {
                    String pnr_ = rs.getString("pnr");
                    String name = rs.getString("name");
                    int coach  = rs.getInt("coach_number");
                    int berth  = rs.getInt("berth_number");
                    String b_type = rs.getString("berth_type");
                    int trNum  = rs.getInt("train_number");
                    String doj = rs.getString("doj");
                    printWriter.println(pnr_+ "\t" + name + "\t" + coach + "\t" + berth + "\t" + b_type + "\t" + trNum +"\t" + doj+"\n");                   
                }
                
                printWriter.println("-------------------------------------------------------------------------------------------\n");
                rs.close();
                stmt1.close();
                clientCommand = bufferedInput.readLine(); 
            }
            inputStream.close();
            bufferedInput.close();
            outputStream.close();
            bufferedOutput.close();
            printWriter.close();
            socketConnection.close();
        }
        catch(Exception e)
        {
            return;
        }
    }
}

/**
 * Main Class to controll the program flow
 */
public class ServiceModule 
{
    // Server listens to port
    static int serverPort = 7008;
    // Max no of parallel requests the server can process
    static int numServerCores = 5 ;         
    //------------ Main----------------------
    public static void main(String[] args) throws IOException 
    {    
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);
        
        try (//Creating a server socket to listen for clients
        ServerSocket serverSocket = new ServerSocket(serverPort)) {
            Socket socketConnection = null;
            
            // Always-ON server
            while(true)
            {
                System.out.println("Listening port : " + serverPort 
                                    + "\nWaiting for clients...");
                socketConnection = serverSocket.accept();   // Accept a connection from a client
                System.out.println("Accepted client :" 
                                    + socketConnection.getRemoteSocketAddress().toString() 
                                    + "\n");
                //  Create a runnable task
                Runnable runnableTask = new QueryRunner(socketConnection);
                //  Submit task for execution   
                executorService.submit(runnableTask);   
            }
        }
    }
}

