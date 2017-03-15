package example;

import com.domo.sdk.Client;
import com.domo.sdk.datasets.DataSetClient;
import com.domo.sdk.datasets.model.Column;
import com.domo.sdk.datasets.model.CreateDataSetRequest;
import com.domo.sdk.datasets.model.DataSet;
import com.domo.sdk.datasets.model.Schema;
import com.domo.sdk.request.Config;
import com.domo.sdk.users.UserClient;
import com.domo.sdk.users.model.User;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import okhttp3.logging.HttpLoggingInterceptor;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import static com.domo.sdk.datasets.model.ColumnType.LONG;
import static com.domo.sdk.datasets.model.ColumnType.STRING;
import static com.domo.sdk.request.Scope.DATA;
import static com.domo.sdk.request.Scope.USER;

/**
 * Created by clintchecketts on 3/14/17.
 */
public class WorkshopTasks {

    private Client client;

    @Before
    public void setup(){
        //Create client (step 1 & 2)
        client = Client.create(Config.with()
                .scope(DATA)
                .clientId("316f11c5-b768-4ef8-8404-d1ec508557ec")
                .clientSecret("cb780e210c75194487d1ec40143ce40f3fef178bf1bf509a4b9bcb370f302d85")
                .scope(USER, DATA)
                .httpLoggingLevel(HttpLoggingInterceptor.Level.BODY)
                .build());
    }


    @Test
    public void handsOnWorkShop() throws IOException {
        //Step 3
        UserClient userClient = client.userClient();
        User clint = userClient.get(669096686);
        System.out.println(clint);

        DataSetClient dsClient = client.dataSetClient();

        //Step 4 - Create DS
        CreateDataSetRequest createRequest = new CreateDataSetRequest();
        createRequest.setName("Sample DataSet");
        createRequest.setDescription("Just some data");
        createRequest.setSchema(new Schema(Lists.newArrayList(new Column(STRING, "name"))));

        DataSet ds = dsClient.create(createRequest);
        System.out.println("Created:"+ds);

        //Step 5 - Get DS
        DataSet ds2 = dsClient.get(ds.getId());
        System.out.println("Get:"+ds2);

        //Step 6 - Update DS
        ds2.getSchema().setColumns(Lists.newArrayList(
                new Column(STRING, "1st Letters"),
                new Column(STRING, "2nd Letters"),
                new Column(STRING, "3rd Letters")));
        dsClient.update(ds2);

        //Step 7 - Import data
        String input = "\"a\",\"b\",\"c\"\n\"d\",\"e\",\"f\"\n\"g\",\"h\",\"i\"\n\"j\",\"k\",\"l\"\n\"m\",\"n\",\"o\"\n\"p\",\"q\",\"r\"";
        dsClient.importData(ds.getId(),input);
    }




    @Test
    public void heroCsv() throws Exception {
        //Read and filter CSV
        CSVReader reader = new CSVReader(new FileReader("datasets/comicBooks/heroAndVillianStats.csv"));

        File csvFile = File.createTempFile("goodguys",".csv");
        System.out.println(csvFile.getAbsolutePath());

        CSVWriter csvWriter =new CSVWriter(new FileWriter(csvFile));

        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            if (nextLine[1].equals("good")) {

                csvWriter.writeNext(nextLine);
                System.out.println(Arrays.toString(nextLine));

            }
        }

        csvWriter.close();


        //Upload new filtered DS
        DataSetClient dsClient = client.dataSetClient();

        CreateDataSetRequest createRequest = new CreateDataSetRequest();
        createRequest.setName("Good guys!");
        createRequest.setDescription("Good guy attributes");
        createRequest.setSchema(new Schema(Lists.newArrayList(
                new Column(STRING, "Name"),
                new Column(STRING, "Alignment"),
                new Column(LONG, "Intelligence"),
                new Column(LONG, "Strength"),
                new Column(LONG, "Speed"),
                new Column(LONG, "Durability"),
                new Column(LONG, "Power"),
                new Column(LONG, "Combat"))));

        DataSet ds = dsClient.create(createRequest);

        //Import Data
        dsClient.importData(ds.getId(),csvFile);


    }






    @Test
    public void sqliteExample() throws Exception {
        File csvFile = File.createTempFile("pokemon",".csv");
        System.out.println(csvFile.getAbsolutePath());

        //Query and populate CSV File
        executePokemonQuery(csvFile, "select * from pokemon_species", (csv, rs) -> {

            String identifier =  rs.getString("identifier");
            String id =  rs.getString("id");

            csv.writeNext(new String[]{id, identifier});
        });


        //Create DS
        DataSetClient dsClient = client.dataSetClient();

        CreateDataSetRequest createRequest = new CreateDataSetRequest();
        createRequest.setName("Pokemon!");
        createRequest.setDescription("Pokemon Names");
        createRequest.setSchema(new Schema(Lists.newArrayList(
                new Column(LONG, "id"),
                new Column(STRING, "name"))));

        DataSet ds = dsClient.create(createRequest);

        //Import Data
        dsClient.importData(ds.getId(),csvFile);
    }








    @FunctionalInterface
    public interface ResultSetConsumer {
        void accept(CSVWriter csv, ResultSet t) throws SQLException;
    }

    private void executePokemonQuery(File csvFile, String query, ResultSetConsumer callback) throws ClassNotFoundException, IOException {
        CSVWriter csvWriter =new CSVWriter(new FileWriter(csvFile));


        // load the sqlite-JDBC driver using the current class loader
        Class.forName("org.sqlite.JDBC");

        Connection connection = null;
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:datasets/pokemon/pokedex.sqlite");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery("select * from pokemon_species");
            while(rs.next()) {
                callback.accept(csvWriter, rs);
            }
        }
        catch(SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            //close the writer
            csvWriter.close();

            try {
                if(connection != null)
                    connection.close();
            } catch(SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }




}
