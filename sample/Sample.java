import java.util.ArrayList;
import java.io.*;

public class Sample {

    public static void main(String[] args) {
        
        //Set interace and operation names here
        String interfaceName = "";
        String operationName = "";
        SoapGen sGen = null;
        
        try {
            sGen = new SoapGen("ServiceMediationFramework v3.0.0.wsdl");
            
            //Only if no interface and operation names are set
            interfaceName = sGen.getInterfaceNames().get(0);
            operationName = sGen.getOperationNames(interfaceName).get(0);
            
            ArrayList<String> messageFields = sGen.getMessageFieldNames(interfaceName, operationName);
            
            //Temp list to hold content
            ArrayList<String> messageContent = new ArrayList<String>();
            
            //Set field contents here
            messageContent.add("testclient");
            messageContent.add("12321");
            messageContent.add("Customer");
            messageContent.add("CustomerProfileInquiry");
            messageContent.add("<Service Message>");
            
            //Set field content list into SoapGen
            for(int i = 0; i < messageFields.size(); i++) {
                if(messageContent.get(i).contains("<")) {
                    sGen.setMessageField(interfaceName, operationName, messageFields.get(i), messageContent.get(i), true); 
                } else {
                    sGen.setMessageField(interfaceName, operationName, messageFields.get(i), messageContent.get(i)); 
                }
            }
            
            //Get output
            String requestMsg = sGen.generateSoapRequest(interfaceName, operationName);
            
            //Write to file
            File output = new File("output.txt");
            output.createNewFile();
            PrintStream writer = new PrintStream(new FileOutputStream(output));
            
            writer.print(requestMsg);
        } catch(Exception e) {
            
        } finally {
            if(sGen != null) {
                sGen.close();
            }    
        }
    
    }

}