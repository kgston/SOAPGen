package com.smutbank;

import com.eviware.soapui.impl.wsdl.support.soap.SoapMessageBuilder;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlContext;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlImporter;
import com.eviware.soapui.model.iface.Operation;
import com.eviware.soapui.model.iface.MessagePart;
import com.eviware.soapui.support.XmlHolder;
import com.eviware.soapui.impl.wsdl.WsdlContentPart;
import com.eviware.soapui.impl.wsdl.WsdlHeaderPart;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaParticle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 *  <p>SoapGen is a simplified SOAP request message generator built on soapUI 4.5.0 compiled sources. This class was created with the intention of simplifying the generation and population of SOAP message fields directly from a WSDL at runtime without the need for an intermediate java code to be compiled and run. Using the soapUI classes directly will be tedious to understand and execute as the usage methods are not very well documented. In addition, it uses a mix of its own developed classes to read WSDLs and Apache's xmlbeans classes to parse schemas. SoapGen attempts to consolidate and create a set of simple APIs that is user friendly to easily parse a WSDL and generate its request messages.
 *  <p>Message fields within SoapGen are stored within HashMaps nested 2 levels down. Level 0 represents the interface name to operation HashMap mapping, level 1 represents the operation name to message fields HashMap mapping and level 2 represents the actual mapping of the message fields to the data itself. The nested HashMaps are generated upon instantiating the SoapGen object. 
 *  <p> The method in which SoapGen populates the SOAP request message is by using the soapUI classes and methods to generate out the blank template request message, following which it is reparsed using an XML parser found within the soapUI. The data is then populated into the relavent fields via XPATH commands and then rendered back into the string. 
 *  <p>
 *  How generate a SOAP request:
 *  <ol>
 *  <li>Create a new SoapGen object</li>
 *  <li>Passin WSDL URL using the <code>loadWSDL</code> method</li>
 *  <li>You can get the interfaces and operations and its message fields by using <code>getInterfaceNames(), getOperationNames()</code> and <code>getMessageFieldNames()</code></li>
 *  <li>With the correct names in memory, you can set the fields required by using the <code>setMessageField()</code> method. Note that if you are passing XML data in or embedding a SOAP message within a SOAP message, declare <code>true</code> in the <code>isXml</code> parameter; this will append the CDATA tag</li>
 *  <li>After you are done setting the fields, call <code>generateSoapRequest()</code> to build the SOAP request message</li>
 *  <li>If you need to retrieve destination information, use <code>getEndpoints()</code> and <code>getSoapAction()</code></li>
 *  <li>Please refer to SoapGenUI source code to see actual implementation examples</li>
 *  </ol>
 *  <p>Author: Kingston Chan
 *  <p>Version: 1.1.0
 *  <p>Email: kwchan.2010@sis.smu.edu.sg
 *  @author Kingston Chan
 *  @version 1.1.0
 *  
 */

public class SoapGen {
    
    private WsdlProject project;
    private WsdlInterface[] wsdlInterfaces;
    private HashMap<String, HashMap<String, HashMap<String, String>>> interfaceToOperationToMessageMap;
    
    /**
     *Default constructor to pre-initialize SoapGen.
     *
     *
     */
    
    public SoapGen() throws Exception{
        project = new WsdlProject();
    }
    
    /**
     *Loads the specified WSDL in wsdlUrl into SoapGen
     *
     *@param wsdlUrl Absolute or relative URL or path to the WSDL object
     *@throws Throws an exception if wsdlUrl is not a WSDL object
     *
     */
    public void loadWSDL(String wsdlUrl) throws Exception{
        project = new WsdlProject();
        wsdlInterfaces = WsdlImporter.importWsdl(project, wsdlUrl);
        rebuildAllMessageFields();
    }
    
    //Gets actual interface object given its name
    private WsdlInterface getInterface(String interfaceName) {
        for(WsdlInterface iFace: wsdlInterfaces) {
            if(iFace.getName().equals(interfaceName)) {
                return iFace;
            }
        }
        return null;
    }
    
    //Gets actual operation object given a interface and operaton name
    private WsdlOperation getOperation(String interfaceName, String operationName) {
        WsdlInterface selectedInterface = getInterface(interfaceName);
        if(selectedInterface != null) {
            for (Operation operation : selectedInterface.getOperationList()) {
                WsdlOperation op = (WsdlOperation) operation;
                if(op.getName().equals(operationName)) {
                    return op;
                }
            }
        }
        return null;
    }
    
    //Checks if the given interface name is valid
    private boolean isInterfaceNameValid(String interfaceName) {
        if(getInterface(interfaceName) != null) {
            return true;
        }
        return false;
    }
    
    //Checks if the given operation name is valid
    private boolean isOperationNameValid(String interfaceName, String operationName) {
        if(getOperation(interfaceName, operationName) != null) {
            return true;
        }
        return false;
    }
    
    /**
     *Get the names of the endpoints associated with specified interface
     *
     *@param interfaceName Name of the interface
     *@return Array consisting of all endpoint/s
     *
     */
    public String[] getEndpoints(String interfaceName) {
        return getInterface(interfaceName).getEndpoints();
    }
    
    
    /**
     *Get the name of the SOAP action associated with specified interface and operation
     *
     *@param interfaceName Name of the interface
     *@param operationName Name of the operation
     *@return The SOAP action
     *
     */
    public String getSoapAction(String interfaceName, String operationName) {
        return getOperation(interfaceName, operationName).getAction();
    }
    
    /**
     *Resets all message fields for all interfaces and operations
     *
     *
     */
    public void rebuildAllMessageFields() {
        //Generate new HashMap structure object
        interfaceToOperationToMessageMap = new HashMap<String, HashMap<String, HashMap<String, String>>>();
        //Pre populate keys and values with interace & operation names with correct mappings, does not pre populate message field names however.
        for(WsdlInterface iFace: wsdlInterfaces) {
            HashMap<String, HashMap<String, String>> operationToMessageMap = new HashMap<String, HashMap<String, String>>();
            for(String opName: getOperationNames(iFace.getName())) {
                operationToMessageMap.put(opName, new HashMap<String, String>());
            }
            interfaceToOperationToMessageMap.put(iFace.getName(), operationToMessageMap);
        }
    }
    
    /**
     *Resets all message fields for all operations in specified interface
     *
     *@param interfaceName Name of the interface
     *@return Returns true if completed successfully, else false
     *
     */
    public boolean resetAllMessageFieldsInInterface(String interfaceName) {
        if(isInterfaceNameValid(interfaceName)) {
            for(String opName: interfaceToOperationToMessageMap.get(interfaceName).keySet()) {
                if(!resetAllMessageFieldsInOperation(interfaceName, opName)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     *Resets all message fields for specified interface and operation
     *
     *@param interfaceName Name of the interface
     *@param operationName Name of the operation
     *@return Returns true if completed successfully, else false
     *
     */
    public boolean resetAllMessageFieldsInOperation(String interfaceName, String operationName) {
        if(isOperationNameValid(interfaceName, operationName)) {
            HashMap<String, HashMap<String, String>> operationToMessageMap = interfaceToOperationToMessageMap.get(interfaceName);
            operationToMessageMap.put(operationName, new HashMap<String, String>());
            return true;
        }
        return false;
    }
    
    /**
     *Gets an ArrayList of the names of all interfaces in the WSDL
     *
     *@return Returns a list of interface names
     *
     */
    public ArrayList<String> getInterfaceNames() {
        ArrayList<String> interfaceNames = new ArrayList<String>();
        for(WsdlInterface iFace: wsdlInterfaces) {
            interfaceNames.add(iFace.getName());
        }
        return interfaceNames;
    }
    
    /**
     *Gets an ArrayList of the names of all operations for a specified interface
     *
     *@param interfaceName Name of the interface
     *@return Returns a list of operation names
     *
     */
    public ArrayList<String> getOperationNames(String interfaceName) {
        ArrayList<String> operationNames = new ArrayList<String>();
        if(isInterfaceNameValid(interfaceName)) {
            for (Operation operation : getInterface(interfaceName).getOperationList()) {
                WsdlOperation op = (WsdlOperation) operation;
                operationNames.add(op.getName());
            }
        }  else {
            operationNames = null;
        }
        return operationNames;
    }
    
    /**
     *Gets an ArrayList of all message field names for a specified operation in an interface
     *
     *@param interfaceName Name of the interface
     *@param operationName Name of the operation
     *@return Returns a list of message field names
     *
     */
    public ArrayList<String> getMessageFieldNames(String interfaceName, String operationName) {
        ArrayList<String> messageFieldNames = new ArrayList<String>();
        //check names valid
        if(isOperationNameValid(interfaceName, operationName)) {
            //retrieve operation object
            WsdlOperation curOp = getOperation(interfaceName, operationName);
            //Get message fields holder object
            MessagePart[] messageFields = curOp.getDefaultRequestParts();
            for(MessagePart field: messageFields) {
                //System.out.println("FieldName: " + field.getName());
                //For each field, check the schema type for complex schema types
                MessagePart.ContentPart mpp = (MessagePart.ContentPart) field;
                SchemaType sType = mpp.getSchemaType();
                //System.out.println("Name: " + sType.getName());
                //System.out.println("SimpleType: " + sType.isSimpleType());
                //If simple objects, like int and string, just add field names into ArrayList
                if(sType.isSimpleType()) {
                    messageFieldNames.add(field.getName());
                }  else {
                    ArrayList<String> schemaPaths = new ArrayList<String>();
                    String curPath = "";
                    //Internal top level element name can be declared either way depending if the schema is embedded in the WSDL or as a attachment file
                    if(sType.getContainerField() != null) {
                        curPath = sType.getContainerField().getName().getLocalPart();
                    } else {
                        curPath = sType.getName().getLocalPart();
                    }
                    //System.out.println("curPath: " + curPath);
                    //Start recursive path mapping within the schema structure
                    buildSchemaPathBreakdown(sType.getContentModel(), schemaPaths, curPath);
                    messageFieldNames.addAll(schemaPaths);
                }
            }
        }
        return messageFieldNames;
    }
    
    //A recursive function designed to travese and walk down the schema tree to map its structure and ending fields. Takes in a starting schema particle or a "schema element", a reference to an ArrayList to store newly found paths and the string representation of its parent path from the WSDL message fields
    private void buildSchemaPathBreakdown(SchemaParticle element, ArrayList<String> paths, String curPath) {
        //Takes the parent path and appends to it to form the current path
        if(element.getName() != null) {
            curPath+= "/" + element.getName().getLocalPart();
        }
        //System.out.println("CurrentPath: " + curPath);
        //System.out.println("ParticleType: " + element.getParticleType());
        //If the element/particle is a complex type
        if(element.getParticleType() <= 3) {
            for(SchemaParticle child: element.getParticleChildren()) {
                buildSchemaPathBreakdown(child, paths, curPath);
            }
        } else if(element.getParticleType() == 4) {
            //System.out.println("IsSimpleType: " + element.getType().isSimpleType());
            //Check for a new embedded complex schema type
            if(!element.getType().isSimpleType()) {
                if(element.getType().getContentModel() != null) {
                    buildSchemaPathBreakdown(element.getType().getContentModel(), paths, curPath);
                } else {
                    //model is simple but has attribute type.. needs to be fixed.. ignores attributes for now..
                    paths.add(curPath);
                }
            } else {
                paths.add(curPath);
            }
        } else {
            //do nothing for wildcard;
        }
    }
    
    /**
     *Sets the value for a specific message field
     *
     *@param interfaceName Name of the interface
     *@param operationName Name of the operation
     *@param fieldName Name of the message field
     *@param data The data to be stored in the message field
     *@return Returns true if completed successfully, else false
     *
     */
    public boolean setMessageField(String interfaceName, String operationName, String fieldName, String data) {
        return setMessageField(interfaceName, operationName, fieldName, data, false);
    }
    
    /**
     *Sets the value for a specific message field
     *
     *@param interfaceName Name of the interface
     *@param operationName Name of the operation
     *@param fieldName Name of the message field
     *@param data The data to be stored in the message field
     *@param isXml Appends a CDATA tag around the data parameter
     *@return Returns true if completed successfully, else false
     *
     */
    public boolean setMessageField(String interfaceName, String operationName, String fieldName, String data, boolean isXml) {
        if(isXml) {
            data = "<![CDATA[" + data + "]]>";
        }
        for(String field: getMessageFieldNames(interfaceName, operationName)) {
            if(field.equals(fieldName)) {
                HashMap<String, String> messageFields = interfaceToOperationToMessageMap.get(interfaceName).get(operationName);
                messageFields.put(fieldName, data);
                return true;
            }
        }
        return false;
    }
    
    /**
     *Generates the resultant SOAP request message for a specified interface and operation
     *
     *@param interfaceName Name of the interface
     *@param operationName Name of the operation
     *@return Returns a String representation of the XML based SOAP request message
     *
     */
    public String generateSoapRequest(String interfaceName, String operationName) {
        //Call soapUI API for blank soap request
        String blankSoapRequest = getOperation(interfaceName, operationName).createRequest(true);
        //parse blank soap request
        try {
            XmlHolder soapXml = new XmlHolder(blankSoapRequest);
            //retrieve mapping for each field populated
            HashMap<String, String> messageFields = interfaceToOperationToMessageMap.get(interfaceName).get(operationName);
            //form xpath query ignoring namespaces
            for(String field: messageFields.keySet()) {
                String xpath = "//*:" + field;
                if(field.contains("/")) {
                    String[] fieldParts = field.split("/");
                    xpath = "/";
                    for(String fieldPart: fieldParts) {
                        xpath+= "/*:" + fieldPart;
                    }
                }
                //sets data to element
                if(messageFields.get(field).length() > 0) {
                    soapXml.setNodeValue(xpath, messageFields.get(field));
                } else {
                    //if blank, remove node
                    soapXml.removeDomNodes(xpath);
                }
            }
            //renders newly populated soap request
            return soapXml.getXml();
        } catch(Exception e) {
        }
        return null;
    }
    
    /**
     * Command to exit the system
     */
    public void close() {
        //System.exit(0);
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for(Thread t : threadSet) {
            //System.out.println(t.getName());
            if(t.getName().contains("Timer") || t.getName().contains("Image Fetcher") || t.getName().contains("Thread")) {
                t.stop();
            }
        }
    }

}