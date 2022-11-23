package de.hsuifa.xapi.xapi_core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_ANY;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TripleConverter {
	
    public static JSONArray tripleToJson(Triple triple) {
    
        // init
        JSONArray tripleAsJson = new JSONArray();

        // convert s, p, o
        tripleAsJson.put(nodeToJson(triple.getSubject()));
        tripleAsJson.put(nodeToJson(triple.getPredicate()));
        tripleAsJson.put(nodeToJson(triple.getObject()));

        // return
        return tripleAsJson;

    }

    private static JSONArray nodeToJson(Node node) {

        JSONArray nodeAsJson = new JSONArray();    
        
		// if any
		if (node.getClass() == Node_ANY.class) {
			nodeAsJson.put(true);
            nodeAsJson.put(JSONObject.NULL);
            nodeAsJson.put(JSONObject.NULL);
		}

		// if uri
		else if (node.isURI()) {
            nodeAsJson.put(false);
            nodeAsJson.put(0);
            nodeAsJson.put(node.getURI());
		}
				
		// if literal 
		else if (node.isLiteral()) {
			
			nodeAsJson.put(false);

			// if error
			if (node.getLiteralDatatype()==null) { 
				return nullNodeJson();
			}

			// if string
			else if (node.getLiteralDatatype()==XSDDatatype.XSDstring) { 
				nodeAsJson.put(1);
			}

			// if double
			else if (node.getLiteralDatatype()==XSDDatatype.XSDdouble) { 
				nodeAsJson.put(2);
			}

			// if integer
			else if (node.getLiteralDatatype()==XSDDatatype.XSDinteger) { 
				nodeAsJson.put(3);
			}

			// if boolean
			else if (node.getLiteralDatatype()==XSDDatatype.XSDboolean) { 
				nodeAsJson.put(4);
			}

			// if there is no mapping (i.e. the previous 4 types didnt match) return null
			else { 
				return nullNodeJson(); 
			}

			nodeAsJson.put(node.getLiteralValue()); 

		}

		else {
			// TODO: better error handling
            return nullNodeJson();
		}

        return nodeAsJson;
    
    }

	private static JSONArray nullNodeJson() {

		JSONArray nullNodeJson = new JSONArray(); 
		nullNodeJson.put(JSONObject.NULL);
		nullNodeJson.put(JSONObject.NULL);
		nullNodeJson.put(JSONObject.NULL);
		return nullNodeJson;

	}

	public static List<Triple> jsonToTripleList (JSONArray tripleListAsJsonArray) {

		List<Triple> returnList = new ArrayList<Triple>();
		
		// if list is empty, return empty triple list
		if (tripleListAsJsonArray.get(0).equals(JSONObject.NULL)) { return returnList;}

		// iterate over contents of json array
		Iterator<Object> jsonIterator = tripleListAsJsonArray.iterator();
		while(jsonIterator.hasNext()) {

			// get next object
			Object tripleObject = jsonIterator.next();
			
			if (tripleObject instanceof JSONArray) {
				
				// convert to triple
				Triple triple = jsonToTriple((JSONArray) tripleObject);
				// skip if null
				if (triple == null) { continue; }
				// add to list
				returnList.add(triple);

			}
			
			// skip if object not instance of JSONArray
			else { 
				continue;
			}

		}

		// return
		return returnList;

	}

	public static ExtendedIterator<Triple> jsonToTripleIterator (JSONArray tripleListAsJsonArray) {

		// convert to list first, then wrap as extended iterator and return
		List<Triple> returnList = jsonToTripleList(tripleListAsJsonArray);
		return WrappedIterator.create(returnList.iterator());

	}

	private static Triple jsonToTriple (JSONArray tripleAsJsonArray) {

		// continue only inf exacly 3 entries
		if (tripleAsJsonArray.length()!=3) { return null; }
		
		// get all nodes and return as triple, abort if any node is null
		try {
			Node subject = jsonToNode(tripleAsJsonArray.getJSONArray(0));
			Node predicate = jsonToNode(tripleAsJsonArray.getJSONArray(1));
			Node object = jsonToNode(tripleAsJsonArray.getJSONArray(2));
			if (subject == null | predicate == null | object == null) { return null; }
			return new Triple(subject, predicate, object);
		} 
		// if JSONEception is thrown, return null
		catch (JSONException je) {
			je.printStackTrace();
			return null;
		}
		
	}

	private static Node jsonToNode (JSONArray nodeAsJsonArray) {

		Node returnNode = null;

		// return null if null
		// or if any, as it is not possible in an answer 
		if (nodeAsJsonArray.get(0).equals(JSONObject.NULL) || nodeAsJsonArray.getBoolean(0)) {
			// do nothing to return null at the end
		}

		// if uri or literal
		else if (!(nodeAsJsonArray.getBoolean(0))) {

			switch(nodeAsJsonArray.getInt(1)) {	
				// uri
				case 0:
					returnNode = NodeFactory.createURI(nodeAsJsonArray.getString(2));
					break;
				// string
				case 1:
					returnNode = NodeFactory.createLiteralByValue(nodeAsJsonArray.getString(2),XSDDatatype.XSDstring);
					break;
				// double
				case 2:
					returnNode = NodeFactory.createLiteralByValue(nodeAsJsonArray.getDouble(2),XSDDatatype.XSDdouble);
					break;
				// integer				
				case 3:
					returnNode = NodeFactory.createLiteralByValue(nodeAsJsonArray.getInt(2),XSDDatatype.XSDinteger);
					break;
				// boolean
				case 4:
					returnNode = NodeFactory.createLiteralByValue(nodeAsJsonArray.getBoolean(2),XSDDatatype.XSDboolean);
					break;
				default:
					break;
			}

		}

		return returnNode;

	}

}
