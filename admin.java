import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.io.*;
import java.io.File;
import java.util.Scanner;

public class admin {
    public static void main(String args[]) {

        String url = "jdbc:postgresql://localhost:5432/dbms_project";
        String user = "postgres";
        String password = "123456";
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(url, user, password);
            System.out.println("Opened database successfully");
            stmt = c.createStatement();

            String sql = "drop table if exists trains_running; "+
                    "create table trains_running(" +
                    "train_number integer not null," +
                    "doj varchar(10) not null, " +
                    "ac integer not null," +
                    "sl integer not null," +
                    "ac_remaining integer not null, " +
                    "sl_remaining integer not null," +
                    "primary key (train_number, doj));";

            stmt.executeUpdate(sql);

            sql = "create or replace procedure train_schedule_admin("+
                    "train_number integer,"+
                    "doj varchar(10),"+
                    "ac integer,"+
                    "sl integer) \n"+
                    "language plpgsql "+
                    "as $$ \n"+
                    "begin "+
                        "insert into trains_running values(train_number, doj, ac, sl, 18*ac, 24*sl);"+
                    " end; \n"+
                    "$$;";
            stmt.executeUpdate(sql);

            sql = " drop table if exists tickets_booked; " +
                "create table tickets_booked(" +
                    "pnr varchar(20) not null," +
                    "name text not null, " +
                    "coach_number integer not null," +
                    "berth_number integer not null," +
                    "berth_type varchar(2) not null," +
                    "train_number integer not null," +
                    "doj varchar(10) not null," +
                    "primary key (pnr, coach_number, berth_number, berth_type));";
            stmt.executeUpdate(sql);

            sql = "create or replace procedure booking("+
                "IN remaining integer,"+
                "IN pnr text,"+
                "IN r_name text,"+
                "IN r_train_number integer,"+
                "IN r_doj varchar(10),"+
                "IN r_coach_choice text) "+
            "language plpgsql "+
            "as $$ "+
            "declare "+
             "available integer default(0);"+
             "coach integer;"+
             "berth_number integer;"+
             "berth_type text; "+
            "begin "+
                "if r_coach_choice = 'AC' then "+
                    "available = remaining;"+
                    "berth_number = MOD(available-1, 18) + 1;"+
                    "coach = (available - 1)/18 + 1;"+
                
                    "if MOD(berth_number, 6) = 1 OR MOD(berth_number, 6) = 2 then berth_type = 'LB';"+
                    "elsif MOD(berth_number, 6) = 3 OR MOD(berth_number, 6) = 4 then berth_type = 'UB';"+
                    "elsif MOD(berth_number, 6) = 5 then berth_type = 'SL';"+
                    "elsif MOD(berth_number, 6) = 0 then berth_type = 'SU';"+
                    "end if;"+
                "end if;"+
            
                "if r_coach_choice = 'SL' then "+
                    "available = remaining;"+
                    "berth_number = MOD(available-1, 24)+1;"+
                    "coach = (available - 1)/24 + 1;"+
            
                    "if MOD(berth_number, 8) = 1 OR MOD(berth_number, 8) = 4 then berth_type = 'LB';"+
                    "elsif MOD(berth_number, 8) = 2 OR MOD(berth_number, 8) = 5 then berth_type = 'MB';"+
                    "elsif MOD(berth_number, 8) = 3 OR MOD(berth_number, 8) = 6 then berth_type = 'UB';"+
                    "elsif MOD(berth_number, 8) = 7 then berth_type = 'SL';"+
                    "elsif MOD(berth_number, 8) = 0 then berth_type = 'SU';"+
                    "end if;"+
                "end if;"+
                "insert into tickets_booked values(pnr, r_name, coach, berth_number, berth_type, r_train_number, r_doj);"+
            "end"+
            "$$;";
            stmt.executeUpdate(sql);               
            stmt.close();

            c.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
}
