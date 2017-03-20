package example;

import com.domo.sdk.Client;
import com.domo.sdk.datasets.DataSetClient;
import com.domo.sdk.datasets.model.Column;
import com.domo.sdk.datasets.model.CreateDataSetRequest;
import com.domo.sdk.datasets.model.DataSet;
import com.domo.sdk.datasets.model.Schema;
import com.domo.sdk.request.Config;
import com.domo.sdk.streams.model.StreamDataSet;
import com.domo.sdk.streams.model.StreamDataSetRequest;
import com.domo.sdk.streams.model.StreamExecution;
import com.domo.sdk.streams.model.StreamUploadMethod;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.domo.sdk.datasets.model.ColumnType.LONG;
import static com.domo.sdk.datasets.model.ColumnType.STRING;
import static com.domo.sdk.request.Scope.DATA;
import static com.domo.sdk.request.Scope.USER;

/**
 * Created by clintchecketts on 3/14/17.
 */
@SuppressWarnings("Duplicates")
public class WorkshopTasksFinal {

    private Client client;

    @Before
    public void setup(){
        //Create client (step 1 & 2)
        client = Client.create(Config.with()
                .clientId("<clientId>")
                .clientSecret("<clientSecret>")
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
    public void streamExample_starWarsCsv() throws Exception {

        //Build the DataSet request object
        CreateDataSetRequest ds = new CreateDataSetRequest();
        ds.setName("Star Wars Planets");
        ds.setDescription("Every planet from the Star Wars movies, books, and shows");

        //Populate the DataSet schema
        List<Column> columns = new ArrayList<>();
        columns.add(new Column(STRING, "Planet Name"));
        columns.add(new Column(STRING, "Position"));
        columns.add(new Column(STRING, "System"));
        columns.add(new Column(STRING, "Sector"));
        columns.add(new Column(STRING, "Planet Type"));
        columns.add(new Column(STRING, "Environment"));
        columns.add(new Column(STRING, "Length of Day"));
        columns.add(new Column(STRING, "Length of Year"));
        columns.add(new Column(STRING, "Sentient Species"));
        columns.add(new Column(STRING, "Other Species"));
        columns.add(new Column(STRING, "Capital City"));
        columns.add(new Column(STRING, "Region"));
        columns.add(new Column(STRING, "World"));
        columns.add(new Column(STRING, "Atmosphere"));
        columns.add(new Column(STRING, "Gravity"));
        columns.add(new Column(STRING, "Diameter"));

        Schema schema = new Schema(columns);
        ds.setSchema(schema);

        //Build the Stream request object
        StreamDataSetRequest streamRequest = new StreamDataSetRequest();
        streamRequest.setDataset(ds);
        streamRequest.setUpdateMethod(StreamUploadMethod.APPEND);

        //Create the Stream DataSet in Domo
        StreamDataSet stream = client.streamDataSetClient().createStreamDataset(streamRequest);

        //Create a Stream Execution to begin a multi-part upload
        StreamExecution execution = client.streamDataSetClient().createStreamExecution(stream.getId());

        //Get the current path of the parts to upload
        File csvDirectory = new File("datasets/starwars");
        int partNum = 1;
        if(csvDirectory.isDirectory()){
            //noinspection ConstantConditions
            for(String path:csvDirectory.list()) {
                System.out.println("Uploading part: " + csvDirectory.getAbsolutePath()+"/"+ path);
                File part = new File(csvDirectory, path);
                client.streamDataSetClient().uploadDataPart(stream.getId(), execution.getId(), partNum, part);
                partNum++;
            }
        }

        //Commit the execution to mark the upload as completed
        client.streamDataSetClient().commitStreamExecution(stream.getId(), execution.getId());
    }


    @Test
    public void pokemonSQLiteExample() throws Exception {
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
