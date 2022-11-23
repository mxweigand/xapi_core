package de.hsuifa.xapi.xapi_core;

import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.GraphBase;
import org.apache.jena.util.iterator.ExtendedIterator;

import org.json.JSONArray;


public class VirtualAboxGraph extends GraphBase {

    private RestClient restClient;
	
	/**
     * constructor
     * @param restClient
     */
    public VirtualAboxGraph(RestClient restClient) {

        // init rest client
        this.restClient = restClient;

	}

	@Override
	protected ExtendedIterator<Triple> graphBaseFind(Triple triplePattern) {

        // convert triple to json array
        JSONArray requestJson = TripleConverter.tripleToJson(triplePattern);

        // init
        JSONArray returnJson = null;

        // fire request via REST
        try {

            returnJson = restClient.getAboxTriples(requestJson);
 
        } catch (Exception e) {
            
            //
            e.printStackTrace();
        
        }

        // convert json to triple Iterator 
        ExtendedIterator<Triple> returnIterator = TripleConverter.jsonToTripleIterator(returnJson);

        // return 
        return returnIterator;

	}

    // TODO: remove dummy return, should probably use deafult method with findAll()
	@Override
	protected int graphBaseSize() {
		System.out.println("not implemented yet");
		return 11111;
	}

	@Override
	public void performAdd(Triple t) {
		System.out.println("not implemented yet");
	}

	@Override
	public void performDelete(Triple t) {
		System.out.println("not implemented yet");
	}

}