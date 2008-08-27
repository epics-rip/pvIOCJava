/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

import org.epics.ioc.util.IOCXMLListener;
import org.epics.ioc.util.IOCXMLReader;
import org.epics.ioc.util.IOCXMLReaderFactory;
import org.epics.ioc.util.MessageType;

/**
 * Factory to convert an xml file to a Database Definition and put it in a database.
 * The only public method is convert.
 * @author mrk
 *
 */
public class XMLToCalcSupport {
    
    

    public static void main(String[] args) {
        if(args.length==1 && args[0].equals("?")) {
            usage(null);
            return;
        }
        int nargs = args.length;
        if(nargs!=1) {
            usage("illegal number of args");
            return;
        }
        String fileName = args[0];
        String packageName = null;
        String pathName = null;
        File file = new File(fileName);
        if(file==null) {
            usage("filename not a file");
            return;
        }
        if(!file.exists()) {
            usage("filename does not exist");
            return;
        }
        pathName = file.getParent();
        int index = pathName.indexOf("src");
        if(index<0) {
            usage("can't create packageName. No src directory");
        }
        index += 4;
        if(index>=fileName.length()) {
            usage("can't create packageName. Nothing after src");
        }
        String rest = pathName.substring(index);
        packageName = rest.replace(File.separatorChar, '.');
        fileName = file.getName();
        Listener listener = new Listener();
        listener.start(fileName,packageName,pathName);
    }
    
    static void usage(String message) {
        if(message!=null) System.err.println(message);
        System.out.println("Usage: fileName [packageName [pathName]]");
    }
    
    private static class Listener implements IOCXMLListener {
        
        private void start(String fileName,String packageName,String pathName) {
            this.packageName = packageName;
            this.pathName = pathName;
            int index = fileName.lastIndexOf("Generate.xml");
            if(index<1) {
                System.err.println("Illegal file. Must end with Generate.xml");
                return;
            }
            String supportFileName = pathName + File.separator + fileName.substring(0,index) + "Support.xml";
            File file = new File(supportFileName);
            try {
                supportWriter = new PrintWriter(new FileWriter(file));
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return;
            }
            supportWriter.println("<?xml version=\"1.0\" ?>");
            supportWriter.println("<IOCDatabase>");
            iocxmlReader = IOCXMLReaderFactory.getReader();
            iocxmlReader.parse("JavaIOCCalculation",
                    pathName + File.separator + fileName,
                    this);
        }
        
        private IOCXMLReader iocxmlReader;
        private String packageName = null;
        private String pathName = null;
        private PrintWriter supportWriter = null;
        
        private enum State {
            idle,
            calculate,
            importState,
            arg,
            value,
            compute,
            process
        } 
        private State state = State.idle;
        
        private String calculateName = null;
        private ArrayList<String> importList = new ArrayList<String>();
        private String valueType = null;
        private String valueElementType = null;
        
        private static class ArgInfo {
            String name;
            String type;
            String elementType;
            ArgInfo(String name,String type,String elementType) {
                this.name = name;
                this.type = type;
                this.elementType = elementType;
            }
        }
        
        
        private ArrayList<ArgInfo> argList = new ArrayList<ArgInfo>();
        private StringBuilder computeBuilder = new StringBuilder();
        private StringBuilder processBuilder = new StringBuilder();
        
        private PrintWriter calculateWriter = null;
        
        public void endDocument(){
            supportWriter.println("</IOCDatabase>");
            supportWriter.close();
        }       

        public void startElement(String qName,Map<String,String> attributes)
        {
            switch(state) {
            case idle:
                if(qName.equals("calculate")) {
                    state = State.calculate;
                    calculateName = attributes.get("name");
                    if(calculateName==null) {
                        iocxmlReader.message("name not specified",
                                MessageType.error);
                        state= State.idle;
                        return;
                    }
                    importList.clear();
                    valueType = null;
                    argList.clear();
                    computeBuilder.setLength(0);
                    processBuilder.setLength(0);
                } else {
                    iocxmlReader.message("startElement " + qName + " not understood",
                            MessageType.error);
                }
                break;
            case calculate:
                if(qName.equals("arg")) {
                    String argName = attributes.get("name");
                    if(argName==null) {
                        iocxmlReader.message("name not specified",
                                MessageType.error);
                        state = State.idle;
                        return;
                    }
                    String argType = attributes.get("type");
                    if(argType==null) {
                        iocxmlReader.message("type not specified",
                                MessageType.error);
                        state = State.idle;
                        return;
                    }
                    String argElementType = null;
                    if(argType.equals("array")) {
                        argElementType = attributes.get("elementType");
                        if(argElementType==null) {
                            iocxmlReader.message("elementType not specified",
                                    MessageType.error);
                            state = State.idle;
                            return;
                        }
                    }
                    ArgInfo argInfo = new ArgInfo(argName,argType,argElementType);
                    argList.add(argInfo);
                    state = State.arg;
                } else if(qName.equals("import")) {
                    String value = attributes.get("packageName");
                    if(value==null) {
                        iocxmlReader.message("packageName not specified",
                                MessageType.error);
                    } else {
                        importList.add(value);
                    }
                    state = State.importState;
                    
                } else if(qName.equals("value")) {
                    valueType = attributes.get("type");
                    if(valueType==null) {
                        iocxmlReader.message("type not specified",
                                MessageType.error);
                        state = State.idle;
                        return;
                    }
                    if(valueType.equals("array")) {
                        valueElementType = attributes.get("elementType");
                        if(valueElementType==null) {
                            iocxmlReader.message("elementType not specified",
                                    MessageType.error);
                            state = State.idle;
                            return;
                        }
                    }
                    state = State.value;
                } else if(qName.equals("compute")) {
                    state = State.compute;
                } else if(qName.equals("process")) {
                    state = State.process;
                } else {
                    iocxmlReader.message("startElement " + qName + " not understood",
                            MessageType.error);
                }
                break;
            default:
                iocxmlReader.message("startElement " + qName + " not understood",
                        MessageType.error);
                break;
            }
        }
        
        public void endElement(String qName)
        {
            switch(state) {
            case arg:
                state = State.calculate;
                break;
            case value:
                state = State.calculate;
            case compute:
                if(qName.equals("compute")) state = State.calculate;
                break;
            case importState:
                if(qName.equals("import")) state = State.calculate;
                break;
            case process:
                if(qName.equals("process")) state = State.calculate;
                break;
            case calculate:
                if(qName.equals("calculate")) {
                    generateSupport();
                    state = State.idle;
                } else {
                    iocxmlReader.message("startElement " + qName + " not understood",
                            MessageType.error);
                }
                break;
            default:
                break;
            }
        }
        
        public void characters(char[] ch, int start, int length)
        {
            if(state!=State.compute && state!=State.process) return; 
            String string = String.copyValueOf(ch, start, length);
            switch(state) {
            case compute:
                computeBuilder.append(string);
                break;
            case process:
                processBuilder.append(string);
                break;
            default:
                break;
            }
            
        }

        public void message(String message,MessageType messageType) {
            System.out.println(messageType.name() + " " + message);
        }
        
        private void generateSupport() {
            String firstChar = calculateName.substring(0, 1).toUpperCase();
            String name = firstChar + calculateName.substring(1);
            String fileName = name + "CalculatorFactory";
            String sourceFileName = pathName + File.separator +  fileName + ".java";
            File file = new File(sourceFileName);
            try {
                calculateWriter = new PrintWriter(new FileWriter(file));
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return;
            }
            preamble();
            classStart();
            argdefs();
            valuedefs();
            getMethods();
            setMethods();
            if(processBuilder.length()==0) {
                processMethodDefault();
            } else {
                processMethod();
            }
            computeMethod();
            calculateWriter.println("    }");
            calculateWriter.println("}");
            calculateWriter.close();
            supportWriter.println();
            supportWriter.println(
                    "<support name = \"" + calculateName + "Calculator\""
                    + " factoryName = \"" + packageName + "." + fileName
                    + "\" />");
        }
        
        private void preamble() {
            calculateWriter.println("/* generated code */");
            if(packageName.length()>0)
                calculateWriter.println("package " + packageName + ";");
            calculateWriter.println();
            if(!packageName.equals("org.epics.ioc.support.calc")) 
                calculateWriter.println("import org.epics.ioc.support.calc.*;");
            calculateWriter.println("import org.epics.ioc.db.*;");
            calculateWriter.println("import org.epics.ioc.pv.*;");
            calculateWriter.println("import org.epics.ioc.util.*;");
            calculateWriter.println("import org.epics.ioc.support.*;");
            calculateWriter.println();
            if(importList.size()>0) {
                for(String value : importList) {
                    calculateWriter.println("import " + value + ";");
                }
                calculateWriter.println();
            }
        }
        
        private void classStart() {
            String firstChar = calculateName.substring(0, 1).toUpperCase();
            String className = firstChar + calculateName.substring(1);
            calculateWriter.println("public class " + className + "CalculatorFactory {");
            calculateWriter.println(
                "    public static Support create(DBStructure dbStructure) {");
            calculateWriter.println(
                "        return new " + className + "Calculator(dbStructure);");
            calculateWriter.println(
                    "    }");
            calculateWriter.println();
            calculateWriter.println(
            "    private static String supportName = \"" + calculateName + "Calculator\";");
            calculateWriter.println();
            calculateWriter.println(
            "    private static class " + className +"Calculator extends AbstractCalculatorSupport");
            calculateWriter.println(
            "    {");
            calculateWriter.println(
            "        private " + className +"Calculator(DBStructure dbStructure) {");
            calculateWriter.println(
                    "            super(supportName,dbStructure);");
            calculateWriter.println(
                    "        }");
            calculateWriter.println();
        }
        
        private void argdefs() {
            calculateWriter.println();
            String blanks = "        ";
            int num = argList.size();
            if(num==0) {
                calculateWriter.println(
                        blanks
                        + "private ArgType[] argTypes = new ArgType[0];");
                return;
            }
            calculateWriter.println(
                blanks
                + "private ArgType[] argTypes = new ArgType[] {");
            for(int i=0; i<num; i++) {
                ArgInfo argInfo = argList.get(i);
                if(!argInfo.type.equals("array")) {
                    calculateWriter.print(
                            blanks + "    " 
                            + "new ArgType(\"" + argInfo.name + "\","
                            + getTypeDef(argInfo.type) + ",null)"
                    );
                } else {
                    calculateWriter.print(
                            blanks + "    " 
                            + "new ArgType(\"" + argInfo.name + "\","
                            + "Type.pvArray,"
                            + getTypeDef(argInfo.elementType) + ")"
                    );
                }
                if(i<num-1) {
                    calculateWriter.println(",");
                } else {
                    calculateWriter.println();
                }
            }
            calculateWriter.println(blanks + "};");
            for(ArgInfo argInfo : argList) {
                String name = argInfo.name;
                if(!argInfo.type.equals("array")) {
                    String pvDef = getPVDef(argInfo.type);
                    calculateWriter.println(
                            blanks
                            + "private " + pvDef + " " + name + "PV = null;");
                    calculateWriter.println(
                            blanks
                            + "private " + argInfo.type + " " + name + ";");
                } else {
                    String pvDef = getElementPVDef(argInfo.elementType);
                    calculateWriter.println(
                            blanks + "private " + pvDef + " " + argInfo.name +"PV = null;");
                    String dataDef = pvDef.substring(2) + "Data";
                    calculateWriter.println(
                            blanks + "private " + dataDef + " " + argInfo.name + "Data = new " + dataDef + "();");
                    calculateWriter.println(
                            blanks + "private " + valueElementType + "[] " + argInfo.name +";");
                    calculateWriter.println(
                            blanks + "private int " + argInfo.name + "Length;");
                    
                }
            }
        }
        
        private void valuedefs() {
            calculateWriter.println();
            String blanks = "        ";
            calculateWriter.println(
                blanks + "private DBField valueDB = null;");
            if(!valueType.equals("array")) {
                calculateWriter.println(
                    blanks + "private " + getPVDef(valueType) + " valuePV = null;");
                calculateWriter.println(
                        blanks + "private " + valueType + " value;");
            } else {
                String pvDef = getElementPVDef(valueElementType);
                calculateWriter.println(
                        blanks + "private " + pvDef + " valuePV = null;");
                String dataDef = pvDef.substring(2) + "Data";
                calculateWriter.println(
                        blanks + "private " + dataDef + " valueData = new " + dataDef + "();");
                calculateWriter.println(
                        blanks + "private " + valueElementType + "[] value;");
                calculateWriter.println(
                        blanks + "private int valueLength;");
                
            }
        }
        
        private void getMethods() {
            calculateWriter.println();
            String blanks = "        ";
            calculateWriter.println(
                blanks + "protected ArgType[] getArgTypes() { return argTypes;}");
            calculateWriter.println();
            calculateWriter.println(
                    blanks + "protected Type getValueType() { return " + getTypeDef(valueType) +";}");
        }
        
        private void setMethods() {
            calculateWriter.println();
            String blanks = "        ";
            calculateWriter.println(
                blanks + "protected void setArgPVFields(PVField[] pvArgs) {");
            for(int ind =0; ind< argList.size(); ind++) {
                ArgInfo argInfo = argList.get(ind);
                if(!argInfo.type.equals("array")) {
                    calculateWriter.println(
                            blanks + "    "
                            + argInfo.name + "PV = ("
                            + getPVDef(argInfo.type) + ")pvArgs["
                            + ind + "];");
                } else {
                    calculateWriter.println(
                            blanks + "    "
                            + argInfo.name + "PV = ("
                            + getElementPVDef(argInfo.elementType) + ")pvArgs["
                            + ind + "];");
                }
            }
            calculateWriter.println(
                    blanks + "};");
            calculateWriter.println();
            calculateWriter.println(
                    blanks + "protected void setValueDBField(DBField dbValue) {");
            calculateWriter.println(
                    blanks + "    "
                    +"this.valueDB = dbValue;");
            if(!valueType.equals("array")) {
                calculateWriter.println(
                        blanks + "    "
                        + "valuePV = ("
                        + getPVDef(valueType) + ")dbValue.getPVField();");
            } else {
                calculateWriter.println(
                        blanks + "    "
                        + "valuePV = ("
                        + getElementPVDef(valueElementType) + ")dbValue.getPVField();");
            }
            calculateWriter.println(
                    blanks + "};");
        }
        
        private void processMethodDefault() {
            calculateWriter.println();
            String blanks = "        ";
            calculateWriter.println(
                    blanks
                    + "public void process(SupportProcessRequester supportProcessRequester) {"
            );
            String bodyBlanks = blanks + "    ";
            if(!valueType.equals("array")) {
                calculateWriter.println(
                        bodyBlanks
                        + "value = valuePV.get();");
            } else {
                calculateWriter.println(
                        bodyBlanks
                        + "valueLength = valuePV.getLength();");
                calculateWriter.println(
                        bodyBlanks
                        + "valuePV.get(0,valueLength,valueData);");
                calculateWriter.println(
                        bodyBlanks
                        + "value = valueData.data;");
            }
            for(ArgInfo argInfo : argList) {
                String argName = argInfo.name;
                if(!argInfo.type.equals("array")) {
                    calculateWriter.println(
                            bodyBlanks
                            + argName + " = " + argName + "PV.get();");
                } else {
                    calculateWriter.println(
                            bodyBlanks
                            + argName + "Length = " + argName + "PV.getLength();");
                    calculateWriter.println(
                            bodyBlanks
                            + argName + "PV.get(0," + argName + "Length," + argName + "Data);");
                    calculateWriter.println(
                            bodyBlanks
                            + argName + " = " + argName + "Data.data;");
                }
            }
            calculateWriter.println(
                    bodyBlanks
                    + "compute();");
            if(!valueType.equals("array")) {
                calculateWriter.println(
                        bodyBlanks
                        + "valuePV.put(value);");
            }
            calculateWriter.println(
                    bodyBlanks
                    + "valueDB.postPut();");
            calculateWriter.println(
                    bodyBlanks
                    + "supportProcessRequester.supportProcessDone(RequestResult.success);");
            calculateWriter.println(
                    blanks + "}");
        }
        
        private void processMethod() {
            calculateWriter.println();
            String blanks = "        ";
            calculateWriter.println(
                    blanks
                    + "public void process(SupportProcessRequester supportProcessRequester) {"
                );
            calculateWriter.print(processBuilder.toString());
            calculateWriter.println(
                    blanks + "};");
        }

        private void computeMethod() {
            calculateWriter.println();
            String body = computeBuilder.toString();
            String blanks = "        ";
            if(body!=null && body.length()>0) {
                calculateWriter.println(
                        blanks
                        + "private void compute() {"
                );
                calculateWriter.print(body);
                calculateWriter.println();
                calculateWriter.println(blanks + "}");
            }

        }
       
        private String getTypeDef(String type) {
            if(type.equals("boolean")) return "Type.pvBoolean";
            if(type.equals("byte")) return "Type.pvByte";
            if(type.equals("short")) return "Type.pvShort";
            if(type.equals("int")) return "Type.pvInt";
            if(type.equals("float")) return "Type.pvFloat";
            if(type.equals("double")) return "Type.pvDouble";
            if(type.equals("string")) return "Type.pvString";
            if(type.equals("array")) return "Type.pvArray";
            throw new IllegalStateException("logic error");
        }
        
        private String getPVDef(String type) {
            if(type.equals("boolean")) return "PVBoolean";
            if(type.equals("byte")) return "PVByte";
            if(type.equals("short")) return "PVShort";
            if(type.equals("int")) return "PVInt";
            if(type.equals("float")) return "PVFloat";
            if(type.equals("double")) return "PVDouble";
            if(type.equals("string")) return "PVString";
            throw new IllegalStateException("logic error");
        }
        
        private String getElementPVDef(String type) {
            if(type.equals("boolean")) return "PVBooleanArray";
            if(type.equals("byte")) return "PVByteArray";
            if(type.equals("short")) return "PVShortArray";
            if(type.equals("int")) return "PVIntArray";
            if(type.equals("float")) return "PVFloatArray";
            if(type.equals("double")) return "PVDoubleArray";
            if(type.equals("string")) return "PVStringArray";
            throw new IllegalStateException("logic error");
        }
       
    }
}
