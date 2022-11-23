package de.hsuifa.xapi.xapi_core;

import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryType;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraphFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class XapiCore {

    private Graph graph;
    private String mode;
    private Integer mainPort;
    private String queryTextFilePath;
    private String resultTextFilePath;
    private String matRdfFilePath;
    private String loadRdfFilePath ;
    private Boolean staticGraph;
    private List<XapiPlugin> xapiPlugins =new ArrayList<XapiPlugin>();
    private List<String[]> prefixMappingList = new ArrayList<String[]>();

    public XapiCore() {

        // read config file
        try { readConfigFile(); } 
        catch (Exception e) { e.printStackTrace(); return; }

    }

    public void run() {
    
        // generate graph
        try { generateGraph(); }
        catch (Exception e) { e.printStackTrace(); return; }

        // start server
        if (mode.equals("server")) {
            try { startServer(); }
            catch (Exception e) { e.printStackTrace(); return; }
        }
        // execute single query 
        else if (mode.equals("query")) {
            try { executeQuery(); }
            catch (Exception e) { e.printStackTrace(); return; }
        } 
        // materialize graph
        else if (mode.equals("materialize")) {
            try { materializeGraph(); }
            catch (Exception e) { e.printStackTrace(); return; }
        }
        // abort if unknown mode is given
        else {
            new Exception("invalid mode in config.json").printStackTrace(); return;
        }
        
    }

    private void readConfigFile() throws Exception {

        // read file
        JSONObject configJson = null;
        try {
            File configFile = new File("config.json");
            byte[] bytes = Files.readAllBytes(configFile.toPath());
            String configString = new String (bytes);
            configJson = new JSONObject(configString);
        } catch (Exception e) {
            throw new Exception("error trying to access config.json", e);
        }

        // assingn to variables
        try {
            mode = configJson.getString("mode");
            staticGraph = configJson.getBoolean("static-graph");
            mainPort = configJson.getInt("main-port");
            queryTextFilePath = configJson.getString("query-txt");
            resultTextFilePath = configJson.getString("result-txt");
            matRdfFilePath = configJson.getString("mat-rdf");
            loadRdfFilePath = configJson.getString("load-rdf");
            JSONArray plugins = configJson.getJSONArray("plugins");
            for (int i = 0; i < plugins.length(); i++) {
                JSONObject plugin = plugins.getJSONObject(i);
                String pluginName = plugin.getString("plugin-name");
                Integer pluginPort = plugin.getInt("plugin-port");
                JSONArray prefixMappings = plugin.getJSONArray("plugin-prefix-mappings");
                for (int j = 0; j < prefixMappings.length(); j++) {
                    JSONObject prefixMapping = prefixMappings.getJSONObject(i); 
                    String uri = prefixMapping.getString("uri");
                    String prefix = prefixMapping.getString("prefix");
                    prefixMappingList.add(new String[]{uri,prefix});
                }
                if (pluginName!=null && pluginPort!=null) {
                    xapiPlugins.add(new XapiPlugin(pluginName, pluginPort));
                }
            }           
        } catch (Exception e) {
            throw new Exception("error trying to read fields of config.json", e);
        }

        // print
        System.out.println("+++ config +++\n" + configJson.toString(2) + "\n+++ config +++\n");

    }

    private void generateGraph() {

        if (staticGraph) {
            
            // create model and read tbox from .rdf file
            Model model = ModelFactory.createDefaultModel();
            model.read(loadRdfFilePath, "RDFXML");
            graph = model.getGraph();

        } else {

            // init var
            List<Graph> graphList = new ArrayList<Graph>();
            // for each plugin
            for (XapiPlugin xapiPlugin: xapiPlugins) {
                // add tbox graph and virtual abox graph to graph list
                graphList.add(xapiPlugin.getUnionGraph());
            }
            // create a multiunion and return 
            graph = new MultiUnion(graphList.iterator());

        }

    }

    private void startServer() throws Exception {
	
        try {
            // set logging
            FusekiLogging.setLogging();
            // create and build server     
			FusekiServer server = FusekiServer
					.create()
					.port(mainPort)
					.loopback(true)
					.verbose(false)
					.enablePing(true)
					.add("/data", DatasetGraphFactory.wrap(graph))
					.build();
            // start server
			server.start();	
		} catch (Exception e) {
			throw new Exception("error trying to start the server", e);	
		}
		
	}

    private void materializeGraph() throws Exception {

        try {

            // get file or create new one
            File outputFile = new File(matRdfFilePath);
            if (outputFile.createNewFile()) {
                System.out.println("File created: " + outputFile.getName());
            } else {
                System.out.println("File " + outputFile.getName() + " already exists, overwriting it.");
            }            
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            // // create model from graph and write complete model to file 
            Model model = ModelFactory.createModelForGraph(graph);
            // apply all prefix mappigns
            for (String[] prefixMapping: prefixMappingList) {
                model.setNsPrefix(prefixMapping[1],prefixMapping[0]);
            }

            // execute ?s ?p ?o construct query to get the whole graph
            String queryString = "CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}";
            Query query = QueryFactory.create(queryString);
            try(QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                // write Outputs to file
                Model results = qexec.execConstruct();
                results.write(outputStream);
                outputStream.close();
            }

        } catch (Exception e) {
			throw new Exception("error trying execute the query", e);	
		}

    }

    private void executeQuery() throws Exception {
        
        try {

            // read contents of input file
            File queryStringFile = new File(queryTextFilePath);
            byte[] bytes = Files.readAllBytes(queryStringFile.toPath());
            String queryString = new String (bytes);
            
            // get output file or create new one
            File outputFile = new File(resultTextFilePath);
            if (outputFile.createNewFile()) {
                System.out.println("File created: " + outputFile.getName());
            } else {
                System.out.println("File " + outputFile.getName() + " already exists, overwriting it.");
            } 
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            // create and execute query
            Query query = QueryFactory.create(queryString);
            Model model = ModelFactory.createModelForGraph(graph);
            QueryExecution qexec = QueryExecutionFactory.create(query, model);
            
            // write results to output file
            if (query.queryType().equals(QueryType.SELECT)) {
                System.out.println();
                ResultSet results = qexec.execSelect();
                ResultSetFormatter.out(outputStream, results);
                outputStream.close();
            } else if (query.queryType().equals(QueryType.CONSTRUCT)) {
                Model results = qexec.execConstruct();
                results.write(outputStream);
                outputStream.close();
            } else {
                outputStream.close();
                throw new Exception("error: unknown QueryType");
            }
            System.out.println("done\n");

        } catch (Exception e) {
			throw new Exception("error trying execute the query", e);	
		}

    }

}