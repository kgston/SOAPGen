import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;

import kingston.InputValidator;
import kingston.FileBrowser;
import com.smutbank.SoapGen;

import com.eviware.soapui.impl.support.definition.support.InvalidDefinitionException;

public class SoapGenUI {
    
    private static final String NAME = "soapGen v1.1.0";
    private static SoapGen wsdl;
    
    public static void main(String[] args) {
        
        try {
            System.out.println("Loading SoapGen...");
            wsdl = new SoapGen();
        } catch(Exception e) {
            
        }
        
        Scanner sc = new Scanner(System.in);
        //wsdl = null;
        //load splash
        System.out.println();
        printPrettyBox("Welcome to " + NAME);
        System.out.println();
        
        //Initilize starting path
        boolean startPathExists = false;
        String startPath = null;
        if(args.length == 1) {
            startPath = args[0];
            startPathExists = true;
        }
        
        while(!startPathExists) {
            System.out.println("Let's start by entering the path to your WSDL directory or hit [.] for current dir:");
            System.out.print(">>> ");
            startPath = sc.nextLine();
            
            if(!new File(startPath).exists()) {
                System.out.println("Specified path does not exists");
                System.out.println();
            } else {
                startPathExists = true;
            }
        }
        System.out.println();
        
        boolean exit = false;
        while(!exit) {
            //get WSDL path
            boolean isWsdl = false;
            while(!isWsdl) {
                String wsdlPath = FileBrowser.browseForFile(startPath);
                //wsdl = null;
                
                //If exit command received from FileBrowser, it will call close() command
                if(wsdlPath != null) {
                    if(wsdlPath.length() == 0) {
                        close();
                    }
                }
                
                //Try to load file
                try {
                    long startTime = System.nanoTime();
                    System.out.println("Loading WSDL...");
                    System.out.println(wsdlPath);
                    wsdl.loadWSDL(wsdlPath);
                    System.out.println("Time took to load WSDL: " + (System.nanoTime() - startTime) / 1000000.00 + "ms");
                    isWsdl = true;
                } catch(InvalidDefinitionException e) { //If file is not WSDL
                    wsdlPath = null;
                    System.out.println();
                    System.out.println("File selected is not a WSDL file");
                    System.out.println();
                } catch(Exception e) { //If the user tries to be funny... ha ha ha..
                    e.printStackTrace();
                }
                System.out.println();
            }
            //Start soapGen menu, WSDL loaded sucessfully, 
            //Main Menu
            soapGenMain();
        }
        //Exit programme
        close();
    }
    
    private static void soapGenMain() {
        //launch WSDL interface selection
        soapGenRoot();
        
    }
    
    private static void soapGenRoot() {
        boolean exit = false;
        int interfaceIndex = 0;
        while(!exit) {
            ArrayList<String> wsdlRootHeader = new ArrayList<String>();
            wsdlRootHeader.add("WSDL Root");
            printPrettyBox(wsdlRootHeader);
            System.out.println();
            
            System.out.println("Select an interface:");
            ArrayList<String> interfaceNames = wsdl.getInterfaceNames();
            for(int i = 1; i <= interfaceNames.size(); i++) {
                System.out.println(i + ". " + interfaceNames.get(i - 1));
            }
            System.out.println();
            System.out.println("Select an interface to use | Exit [0]:");
            System.out.print(">>> ");
            try {
                interfaceIndex = InputValidator.getInt(0, interfaceNames.size(), false, true);
                if(interfaceIndex == 0) {
                    exit = true;
                } else {
                    soapGenInterface(interfaceNames.get(interfaceIndex - 1));
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
                System.out.println();
            }
            System.out.println();
        }
    }
    
    private static void soapGenInterface(String interfaceName) {
        boolean exit = false;
        int operationIndex = 0;
        while(!exit) {
            ArrayList<String> wsdlInterfaceHeader = new ArrayList<String>();
            wsdlInterfaceHeader.add("WSDL Interface");
            wsdlInterfaceHeader.add("");
            wsdlInterfaceHeader.add("Interface selected: " + interfaceName);
            wsdlInterfaceHeader.add("");
            wsdlInterfaceHeader.add("Endpoints:");
            for(String endpoint: wsdl.getEndpoints(interfaceName)) {
                wsdlInterfaceHeader.add(endpoint);
            }
            printPrettyBox(wsdlInterfaceHeader);
            System.out.println();
            
            ArrayList<String> operationNames = wsdl.getOperationNames(interfaceName);
            for(int i = 1; i <= operationNames.size(); i++) {
                System.out.println(i + ". " + operationNames.get(i - 1));
            }
            System.out.println();
            System.out.println("Select an operation | Exit [0]:");
            System.out.print(">>> ");
            try {
                operationIndex = InputValidator.getInt(0, operationNames.size(), false, true);
                if(operationIndex == 0) {
                    exit = true;
                } else {
                    soapGenOperation(interfaceName, operationNames.get(operationIndex - 1));
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
            System.out.println();
        }
    }
    
    private static void soapGenOperation(String interfaceName, String operationName) {
        boolean exit = false;
        int fieldIndex = 0;
        while(!exit) {
            ArrayList<String> wsdlOperationHeader = new ArrayList<String>();
            wsdlOperationHeader.add("WSDL Operation");
            wsdlOperationHeader.add("");
            wsdlOperationHeader.add("Interface selected: " + interfaceName);
            wsdlOperationHeader.add("Operation selected: " + operationName);
            wsdlOperationHeader.add("");
            wsdlOperationHeader.add("SOAP Action:");
            wsdlOperationHeader.add(wsdl.getSoapAction(interfaceName, operationName));
            printPrettyBox(wsdlOperationHeader);
            System.out.println();
            
            ArrayList<String> messageFields = wsdl.getMessageFieldNames(interfaceName, operationName);
            for(int i = 1; i <= messageFields.size(); i++) {
                System.out.println(i + ". " + messageFields.get(i - 1));
                if(i == messageFields.size()) {
                    System.out.println(i + 1 + ". Print SOAP Request");
                }
            }
            System.out.println();
            System.out.println("Select a field to populate | Exit [0]:");
            System.out.print(">>> ");
            try {
                fieldIndex = InputValidator.getInt(0, messageFields.size() + 1, false, true);
                if(fieldIndex == 0) {
                    exit = true;
                } else if(fieldIndex == messageFields.size() + 1) {
                    System.out.println();
                    System.out.println(wsdl.generateSoapRequest(interfaceName, operationName));
                } else {
                    String data = "";
                    System.out.print("Data >>> ");
                    Scanner sc = new Scanner(System.in);
                    data = sc.nextLine();
                    if(data.contains("<")) {
                        wsdl.setMessageField(interfaceName, operationName, messageFields.get(fieldIndex - 1), data, true); 
                    } else {
                        wsdl.setMessageField(interfaceName, operationName, messageFields.get(fieldIndex - 1), data); 
                    }
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
            System.out.println();
        }
    }
    
    private static void printPrettyBox(String text) {
        ArrayList<String> printList = new ArrayList<String>();
        printList.add(text);
        printPrettyBox(printList);
    }
    
    private static void printPrettyBox(ArrayList<String> printList) {
        ArrayList<String> formattedPrint = new ArrayList<String>();
        int maxLength = 0;
        for(int i = 0; i < printList.size(); i++) {
            String line = printList.get(i);
            if(line.length() > 69) {
                String curLine = line.substring(0, 69);
                printList.remove(i);
                printList.add(i, curLine);
                maxLength = 69;
                String nextLine = line.substring(69);
                printList.add(i + 1, nextLine);
            }else if(line.length() > maxLength) {
                maxLength = line.length();
            }
        }
        
        for(String line: printList) {
            while(line.length() < maxLength) {
                line+= " ";
            }
            formattedPrint.add("#  " + line + "  #");
        }
        
        String bufferLine = "";
        while(bufferLine.length() < maxLength) {
            bufferLine+= " ";
        }
        bufferLine =  "#  " + bufferLine + "  #";
        formattedPrint.add(0, bufferLine);
        formattedPrint.add(bufferLine);
        
        String borderLine = "";
        
        while(borderLine.length() < maxLength) {
            borderLine+= "#";
        }
        borderLine =  "###" + borderLine + "###";
        formattedPrint.add(0, borderLine);
        formattedPrint.add(borderLine);
        
        for(String i: formattedPrint) {
            System.out.println(i);
        }
    }
    
    public static void close() {
        System.out.println("Exiting...");
        System.exit(0);
    }

}