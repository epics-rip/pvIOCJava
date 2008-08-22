/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
import java.util.ArrayList;
import java.util.Stack;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVByte;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVDataFactory;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVFloat;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVLong;
import org.epics.ioc.pv.PVProperty;
import org.epics.ioc.pv.PVPropertyFactory;
import org.epics.ioc.pv.PVShort;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmFactory;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;



/**
 * Factory that provides support for expressions.
 * @author mrk
 *
 */
public abstract class ExpressionCalculatorFactory  {

    public static Support create(DBStructure dbStructure) {
        return new ExpressionCalculator(dbStructure);
    }

    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final  String supportName = "expressionCalculator";
    private static Convert convert = ConvertFactory.getConvert();
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    private static boolean dumpTokenList = false;
    private static boolean dumpExpression = false;
    
    private static class ExpressionCalculator extends AbstractSupport {
        
        private ExpressionCalculator(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        
        
        private DBStructure dbStructure = null;
        private PVStructure pvStructure;
        private AlarmSupport alarmSupport = null;
        private DBField dbValue = null;
        private CalcArgArraySupport calcArgArraySupport = null;
        
        private Expression expression = null;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            DBField dbParent = dbStructure.getParent();
            PVField pvParent = dbParent.getPVField();
            PVField pvValue = pvProperty.findProperty(pvParent,"value");
            if(pvValue==null) { // try parent of parent. 
                pvValue = pvParent.getParent();
                if(pvValue!=null) pvValue = pvProperty.findProperty(pvValue,"value");
            }
            if(pvValue==null) {
                pvStructure.message("value field not found", MessageType.error);
                return;
            }
            dbValue = dbStructure.getDBRecord().findDBField(pvValue);
            PVField pvField = pvProperty.findProperty(pvParent,"calcArgArray");
            if(pvField==null) {
                pvStructure.message("calcArgArray field not found", MessageType.error);
                return;
            }
            DBField dbField = dbStructure.getDBRecord().findDBField(pvField);
            Support support = dbField.getSupport();
            if(!(support instanceof CalcArgArraySupport)) {
                pvStructure.message("calcArgArraySupport not found", MessageType.error);
                return;
            }
            calcArgArraySupport = (CalcArgArraySupport)support;
            PVString pvExpression = dbStructure.getPVStructure().getStringField("expression");
            if(pvExpression==null) return;
            Parse parse = new Parse(pvExpression);
            expression = parse.parse();
            if(expression==null) return;
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            expression = null;
            alarmSupport = null;
            dbValue = null;
            super.uninitialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.support.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            try {
                if(expression.operator!=null) {
                    expression.operator.compute();
                }
                PVField pvResult = expression.pvResult;
                PVField pvValue = dbValue.getPVField();
                if(pvResult!=pvValue) convert.copyScalar(pvResult, pvValue);
                dbValue.postPut();
            } catch (ArithmeticException e) {
                alarmSupport.setAlarm(e.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
        private enum Operation {
            unaryPlus,
            unaryMinus,
            bitwiseComplement,
            booleanNot,
            multiplication,
            division,
            remainder,
            plus,
            minus,
            stringPlus,
            leftShift,
            rightShiftSignExtended,
            rightShiftZeroExtended,
            lessThan,
            lessThanEqual,
            greaterThan,
            greaterThanEqual,
            equalEqual,
            notEqual,
            bitwiseAnd,
            booleanAnd,
            bitwiseXOR,
            booleanXOR,
            bitwiseOr,
            booleanOr,
            conditionalAnd,
            conditionalOr,
            ternaryIf,
        }
        
        private enum OperandType {
            none,
            integer,
            bool,
            number,
            string,
            any,
        }
        
        private enum Associativity {
            left,right
        }
        
        static private class OperationSemantics {
            Operation operation;
            String op;
            int precedence;
            Associativity associativity;
            OperandType leftOperand;
            OperandType rightOperand;
            public OperationSemantics(Operation operation,String op,
                    int precedence, Associativity associativity,
                    OperandType leftOperand, OperandType rightOperand) {
                super();
                this.operation = operation;
                this.op = op;
                this.precedence = precedence;
                this.associativity = associativity;
                this.leftOperand = leftOperand;
                this.rightOperand = rightOperand;
            }
            
        }
        
        private static final OperationSemantics[] operationSemantics = 
        {
            new OperationSemantics(Operation.unaryPlus,"+",12,Associativity.right,OperandType.none,OperandType.number),
            new OperationSemantics(Operation.unaryMinus,"-",12,Associativity.right,OperandType.none,OperandType.number),
            new OperationSemantics(Operation.bitwiseComplement,"~",12,Associativity.right,OperandType.none,OperandType.integer),
            new OperationSemantics(Operation.booleanNot,"!",12,Associativity.right,OperandType.none,OperandType.bool),
            new OperationSemantics(Operation.multiplication,"*",11,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.division,"/",11,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.remainder,"%",11,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.plus,"+",10,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.minus,"-",10,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.stringPlus,"+",10,Associativity.left,OperandType.string,OperandType.any),
            new OperationSemantics(Operation.leftShift,"<<",9,Associativity.left,OperandType.integer,OperandType.integer),
            new OperationSemantics(Operation.rightShiftSignExtended,">>",9,Associativity.left,OperandType.integer,OperandType.integer),
            new OperationSemantics(Operation.rightShiftZeroExtended,">>>",9,Associativity.left,OperandType.integer,OperandType.integer),
            new OperationSemantics(Operation.lessThan,"<",8,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.lessThanEqual,"<=",8,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.greaterThan,">",8,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.greaterThanEqual,">=",8,Associativity.left,OperandType.number,OperandType.number),
            new OperationSemantics(Operation.equalEqual,"==",7,Associativity.left,OperandType.any,OperandType.any),
            new OperationSemantics(Operation.notEqual,"!=",7,Associativity.left,OperandType.any,OperandType.any),
            new OperationSemantics(Operation.bitwiseAnd,"&",6,Associativity.left,OperandType.integer,OperandType.integer),
            new OperationSemantics(Operation.booleanAnd,"&",6,Associativity.left,OperandType.bool,OperandType.bool),
            new OperationSemantics(Operation.bitwiseXOR,"^",5,Associativity.left,OperandType.integer,OperandType.integer),
            new OperationSemantics(Operation.booleanXOR,"^",5,Associativity.left,OperandType.bool,OperandType.bool),
            new OperationSemantics(Operation.bitwiseOr,"|",4,Associativity.left,OperandType.integer,OperandType.integer),
            new OperationSemantics(Operation.booleanOr,"|",4,Associativity.left,OperandType.bool,OperandType.bool),
            new OperationSemantics(Operation.conditionalAnd,"&&",3,Associativity.left,OperandType.bool,OperandType.bool),
            new OperationSemantics(Operation.conditionalOr,"||",2,Associativity.left,OperandType.bool,OperandType.bool),
            new OperationSemantics(Operation.ternaryIf,"?:",1,Associativity.right,OperandType.bool,OperandType.any),
            
        };
        
        private enum MathFunction {
            abs,
            acos,
            asin,
            atan,
            atan2,
            cbrt,
            ceil,
            cos,
            cosh,
            exp,
            expm1,
            floor,
            hypot,
            IEEEremainder,
            log,
            log10,
            log1p,
            max,
            min,
            pow,
            random,
            rint,
            round,
            signum,
            sin,
            sinh,
            sqrt,
            tan,
            tanh,
            toDegrees,
            toRadians,
            ulp,
        }
        
        static private class MathFunctionSemantics {
            MathFunction mathFunction;
            int nargs;
            boolean isRandom;
            
            public MathFunctionSemantics(MathFunction mathFunction,int nargs,boolean isRandom) {
                super();
                this.mathFunction = mathFunction;
                this.nargs = nargs;
                this.isRandom = isRandom;
            }
        }
        
        private static final MathFunctionSemantics[] mathFunctionSemantics = 
        {
            new MathFunctionSemantics(MathFunction.abs,1,false),
            new MathFunctionSemantics(MathFunction.acos,1,false),
            new MathFunctionSemantics(MathFunction.asin,1,false),
            new MathFunctionSemantics(MathFunction.atan,1,false),
            new MathFunctionSemantics(MathFunction.atan2,2,false),
            new MathFunctionSemantics(MathFunction.cbrt,1,false),
            new MathFunctionSemantics(MathFunction.ceil,1,false),
            new MathFunctionSemantics(MathFunction.cos,1,false),
            new MathFunctionSemantics(MathFunction.cosh,1,false),
            new MathFunctionSemantics(MathFunction.exp,1,false),
            new MathFunctionSemantics(MathFunction.expm1,1,false),
            new MathFunctionSemantics(MathFunction.floor,1,false),
            new MathFunctionSemantics(MathFunction.hypot,2,false),
            new MathFunctionSemantics(MathFunction.IEEEremainder,2,false),
            new MathFunctionSemantics(MathFunction.log,1,false),
            new MathFunctionSemantics(MathFunction.log10,1,false),
            new MathFunctionSemantics(MathFunction.log1p,1,false),
            new MathFunctionSemantics(MathFunction.max,2,false),
            new MathFunctionSemantics(MathFunction.min,2,false),
            new MathFunctionSemantics(MathFunction.pow,2,false),
            new MathFunctionSemantics(MathFunction.random,0,true),
            new MathFunctionSemantics(MathFunction.rint,1,false),
            new MathFunctionSemantics(MathFunction.round,1,false),
            new MathFunctionSemantics(MathFunction.signum,1,false),
            new MathFunctionSemantics(MathFunction.sin,1,false),
            new MathFunctionSemantics(MathFunction.sinh,1,false),
            new MathFunctionSemantics(MathFunction.sqrt,1,false),
            new MathFunctionSemantics(MathFunction.tan,1,false),
            new MathFunctionSemantics(MathFunction.tanh,1,false),
            new MathFunctionSemantics(MathFunction.toDegrees,1,false),
            new MathFunctionSemantics(MathFunction.toRadians,1,false),
            new MathFunctionSemantics(MathFunction.ulp,1,false),
        };
        
        private enum TokenType {
            unaryOperator,
            binaryOperator,
            ternaryOperator,
            mathFunction,
            booleanConstant,
            integerConstant,
            realConstant,
            stringConstant,
            mathConstant,
            variable,
            comma,
            leftParen,
            rightParen;
            
            boolean isOperator() {
                if( (ordinal() >= TokenType.unaryOperator.ordinal()) && (ordinal() <= TokenType.mathFunction.ordinal()) ) {
                    return true;
                }
                return false;
            }
            boolean isFunction() {
                if( (ordinal() >= TokenType.mathFunction.ordinal()) && (ordinal() <= TokenType.mathFunction.ordinal()) ) {
                    return true;
                }
                return false;
            }
            boolean isConstant() {
                if( (ordinal() >= TokenType.booleanConstant.ordinal()) && (ordinal() <= TokenType.mathConstant.ordinal()) ) {
                    return true;
                }
                return false;
            }
            boolean isOperand() {
                if( (ordinal() >= TokenType.booleanConstant.ordinal()) && (ordinal() <= TokenType.variable.ordinal()) ) {
                    return true;
                }
                return false;
            }
        }
        
        private static class Token {
            TokenType type = null;
            String value = null;
        }
        
        private interface Operator {
            public boolean createPVResult(String fieldName);
            public void compute();
        }
         
        private static class Expression {
            Operator operator = null;
            Expression[] expressionArguments = null;
            PVField pvResult = null;
            Token token = null;
            
            void computeArguments() {
                for(Expression expressionArgument: expressionArguments) {
                    if(expressionArgument==null) continue;
                    Operator operator = expressionArgument.operator;
                    if(operator==null) continue;
                    operator.compute();
                }
            }
        }
        
        private static class OperatorExpression extends Expression{
            OperationSemantics operationSemantics = null;
        }

        
        private static class MathFunctionExpression extends Expression{
            MathFunctionSemantics functionSemantics = null;
        }
        
        private class Parse {
            
            private Parse(PVString pvExpression) {
                this.pvExpression = pvExpression;
            }
            
            Expression parse() {
                if(!createTokenList()) return null;
                if(dumpTokenList) printTokenList("after createTokenList");
                int numLeftParan = 0;
                int numRightParan = 0;
                for(Token token : tokenList) {
                    if(token.type==TokenType.leftParen) numLeftParan++;
                    if(token.type==TokenType.rightParen) numRightParan++;
                    if(numRightParan>numLeftParan) {
                        pvExpression.message("parse failure mismatched parentheses ", MessageType.error);
                        return null;
                    }
                }
                if(numLeftParan!=numRightParan) {
                    pvExpression.message("parse failure mismatched parentheses ", MessageType.error);
                    return null;
                }
                if(!createTokenListWithPrecedence()) return null;
                if(dumpTokenList)printTokenList("after createTokenListWithPrecedence");
                Expression expression = createExpression();
                if(dumpExpression) {
                    pvExpression.message("after parse expression is",MessageType.info);
                    printExpression(expression,"");
                }
                return expression;
            }
            
            
            private PVString pvExpression = null;
            private ArrayList<Token> tokenList = null;
            

            private boolean createTokenList() {
                tokenList = new ArrayList<Token>();
                int next = 0;
                String expression = pvExpression.get();
                int length = expression.length();
                StringBuilder string = new StringBuilder(expression.length());
                boolean inString = false;
                for(int index=0; index<length; index++) {
                    char nextChar = expression.charAt(index);
                    if(nextChar=='"') {
                        string.append(nextChar);
                        inString = !inString;
                        continue;
                    }
                    if(inString) {
                        string.append(nextChar);
                        continue;
                    }
                    if(nextChar!=' ' && !Character.isISOControl(nextChar)) string.append(nextChar);
                }
                expression = string.toString();
                length = expression.length();
                while(true) {
                    if(next>=length) break;
                    Token token = new Token();
                    String value = null;
                    char nextChar = expression.charAt(next);
                    if(nextChar==')') {
                        token.type = TokenType.rightParen;
                        value = ")";
                    } else if(nextChar=='(') {
                        token.type = TokenType.leftParen;
                        value = "(";
                    } else if(nextChar==',') {
                        token.type = TokenType.comma;
                        value = ",";
                    } else if(nextChar=='?' || nextChar==':') {
                        token.type = TokenType.ternaryOperator;
                        value = expression.substring(next, next+1);
                    } else {
                        if(isNumericConstant(expression,next)) {
                            if ((value = getIntegerConstant(expression,next))!=null) {
                                token.type = TokenType.integerConstant;
                            } else if ((value = getRealConstant(expression,next))!=null) {
                                token.type = TokenType.realConstant;
                            } else {
                                throw new IllegalStateException("logic error.");
                            }
                        } else if((value = getBooleanConstant(expression,next))!=null) {
                            token.type = TokenType.booleanConstant;
                        } else if ((value = getStringConstant(expression,next))!=null) {
                            token.type = TokenType.stringConstant;
                        } else if ((value = getVar(expression,next))!=null) {
                            int n = next + value.length();
                            nextChar = 0;
                            if(n<length) nextChar = expression.charAt(n);
                            if(nextChar!='.') {
                                token.type = TokenType.variable;
                            } else {
                                next = n + 1;
                                String functionName = null;
                                if(next<length) {
                                    if(value.equals("Math")) {
                                        token.type = TokenType.mathFunction;
                                        functionName= getVar(expression,next);
                                    }
                                }
                                if(functionName==null) {
                                    pvExpression.message("parse failure unknown function at " + expression.substring(next), MessageType.error);
                                    return false;
                                }
                                if(functionName.equals("PI") || functionName.equals("E")) {
                                    token.type = TokenType.mathConstant;
                                }
                                value = functionName;
                            }
                        } else if ((value=getUnaryOp(expression,next))!=null) {
                            token.type = TokenType.unaryOperator;
                        } else if ((value=getBinaryOp(expression,next))!=null) {
                            token.type = TokenType.binaryOperator;

                        } else {
                            pvExpression.message("parse failure at " + expression.substring(next), MessageType.error);
                            printTokenList("after parse failure");
                            return false;
                        }
                    }
                    if(value.length()==0) {
                        pvExpression.message("zero length string caused parse failure at " + expression.substring(next), MessageType.error);
                        return false;
                    }
                    token.value = value;
                    tokenList.add(token);
                    next += value.length();
                    if(token.type==TokenType.stringConstant) next+= 2;
                }
                return true;
            }
            
            private boolean isNumericConstant(String string, int offset) {
                int len = string.length();
                char firstChar = string.charAt(offset);
                boolean isDigit = Character.isDigit(firstChar);
                if(isDigit) return true;
                boolean isPlusMinus = (firstChar=='+' || firstChar=='-') ? true : false;
                boolean isPeriod = (firstChar=='.') ? true : false;
                char nextChar = 0;
                boolean nextCharIsDigit = false;
                if(offset<len-1) {
                    nextChar = string.charAt(offset+1);
                    nextCharIsDigit = Character.isDigit(nextChar);
                }
                if(isPeriod && nextCharIsDigit) return true;
                if(!isPlusMinus) return false;
                if(!nextCharIsDigit && nextChar!='.') return false;
                if(offset==0) {
                    if(nextCharIsDigit||nextChar=='.') return true;
                    return false;
                }
                Token token = tokenList.get(tokenList.size()-1);
                TokenType type = token.type;
                if(type.isOperator()) return true;
                if(type.isConstant()) return false;
                if(type==TokenType.comma || type==TokenType.leftParen) return true;
                if(type!=TokenType.variable && type!=TokenType.rightParen) {
                    throw new IllegalStateException("logic error.");
                }
                return false;
            }
            
            private String getIntegerConstant(String string, int offset) {
                int len = string.length();
                char first = string.charAt(offset);
                int next = offset;
                if(first=='+' || first=='-') {
                    if(len<=next+1) return null;
                    next++;
                    first = string.charAt(next);
                }
                if(!Character.isDigit(first)) return null;
                if(len<=next+1) {
                    return string.substring(offset,next+1);
                }
                boolean gotX = false;
                while(++next < len) {
                    char now = string.charAt(next);
                    if(Character.isDigit(now)) continue;
                    if(now=='X' || now=='x') {
                        if(string.charAt(offset)!='0') break;
                        gotX = true; continue;
                    }
                    if(gotX) {
                        if(now>='a' && now <='f') continue;
                        if(now>='A' && now <='F') continue;
                    }
                    if(now=='L'){ next++; break;}
                    if(now=='.') return null;
                    if(now=='E' || now=='e') return null;
                    if(now=='D' || now=='d' || now=='F' || now=='f') return null;
                    break;
                }
                return string.substring(offset, next);
            }
            
            private String getRealConstant(String string, int offset) {
                int len = string.length();
                char first = string.charAt(offset);
                int next = offset;
                boolean gotPeriod = false;
                if(first=='+' || first=='-') {
                    if(len<=next+1) return null;
                    next++;
                    first = string.charAt(next);
                }
                if(first=='.') {
                    if(len<=next+1) return null;
                    gotPeriod = true;
                    next++;
                    first = string.charAt(next);
                }
                if(!Character.isDigit(first)) return null;
                if(len<=next+1) {
                    return string.substring(offset,next+1);
                }
                boolean gotE = false;
                
                while(++next < len) {
                    char now = string.charAt(next);
                    if(Character.isDigit(now)) continue;
                    if(now=='D' || now=='d' || now=='F' || now=='f') {
                        next++; break;
                    }
                    if(gotE) {
                        if(now=='-') continue;
                        if(now=='+') continue;
                        break;
                    } else {
                        if(!gotPeriod && now=='.') {
                            gotPeriod = true;
                            continue;
                        }
                        if(now=='e' || now=='E') {
                            gotE = true;
                            continue;
                        }
                    }
                    break;
                }
                return string.substring(offset, next);
            }
            
            private String getBooleanConstant(String string, int offset) {
                int len = string.length();
                if(len<offset+4) return null;
                String str = string.substring(offset, offset+4);
                if(str.equals("true")) return str;
                if(len<offset+5) return null;
                str = string.substring(offset, offset+5);
                if(str.equals("false")) return str;
                return null;
            }
            
            private String getStringConstant(String string, int offset) {
                int len = string.length();
                char charNext = string.charAt(offset);
                if(charNext!='"') return null;
                int next = offset;
                while(next++ < len-1) {
                    charNext = string.charAt(next);
                    if(charNext=='"') return string.substring(offset+1, next);
                    if(charNext=='\\') next++;
                }
                return null;
            }

            private String getVar(String string, int offset) {
                int codePoint = string.codePointAt(offset);
                if(!Character.isJavaIdentifierStart(codePoint)) return null;
                int len = Character.charCount(codePoint);
                int next = offset + len;
                while(next<string.length()) {
                    codePoint = string.codePointAt(next);
                    if(!Character.isJavaIdentifierPart(codePoint)) {
                        String value = string.substring(offset, next);
                        return value;
                    }
                    next += Character.charCount(codePoint);
                }
                String value = string.substring(offset);
                return value;
            }
            
            private String getUnaryOp(String string, int offset) {
                char offsetChar = string.charAt(offset);
                if(offsetChar!='-' && offsetChar!='+' && offsetChar!='~' && offsetChar!='!') return null;
                if(offset==0) return string.substring(offset, offset+1);
                char prevChar = string.charAt(offset-1);
                if(prevChar=='(' || prevChar=='-' || prevChar=='+' || prevChar=='~' || prevChar=='!') return string.substring(offset, offset+1);
                return null;
            }
            
            private String getBinaryOp(String string, int offset) {
                char nextChar = string.charAt(offset);
                char nextNextChar = ((offset+1)<string.length()) ? string.charAt(offset+1) : 0;
                char nextNextNextChar = ((offset+2)<string.length()) ? string.charAt(offset+2) : 0;
                if(nextChar=='+') {
                    return new String("+");
                }
                if(nextChar=='-') {
                    return new String("-");
                }
                if(nextChar=='*') {
                    return new String("*");
                }
                if(nextChar=='/') {
                    return new String("/");
                }
                if(nextChar=='%') {
                    return new String("/");
                }
                if(nextChar=='^') {
                    return new String("^");
                }
                if(nextChar=='<') {
                    String value = "<";
                    if(nextNextChar=='=') value += "=";
                    if(nextNextChar=='<') value += "<";
                    return value;
                }
                if(nextChar=='>') {
                    String value = ">";
                    if(nextNextChar=='=') value += "=";
                    if(nextNextChar=='>') {
                        value += ">";
                        if(nextNextNextChar=='>') value+= ">";
                    }
                    return value;
                }
                if(nextChar=='=') {
                    String value = "=";
                    if(nextNextChar=='=') value += "=";
                    return value;
                }
                if(nextChar=='!') {
                    String value = "!";
                    if(nextNextChar=='=') value += "=";
                    return value;
                }
                if(nextChar=='|') {
                    String value = "|";
                    if(nextNextChar=='|') value += "|";
                    return value;
                }
                if(nextChar=='&') {
                    String value = "&";
                    if(nextNextChar=='&') value += "&";
                    return value;
                }
                return null;
            }

            private boolean createTokenListWithPrecedence() {
                int length = tokenList.size();
                if(length==0) {
                    pvExpression.message("parse failure expression has no tokens", MessageType.error);
                    return false;
                }
                Token token = tokenList.get(0);
                TokenType type = token.type;
                // make sure the entire expression enclosed in ()
                if(type!=TokenType.leftParen || tokenList.get(length-1).type!=TokenType.rightParen) {
                    token = new Token();
                    token.type = TokenType.leftParen;
                    token.value = "(";
                    tokenList.add(0, token);
                    token = new Token();
                    token.type = TokenType.rightParen;
                    token.value = ")";
                    tokenList.add(token);
                    length += 2;
                }
                int next = 0;
                for(int precedence = 12; precedence>0; precedence--) {
                    // right to left first
                    next = length-1;
                    while(next >0) {
                        token = tokenList.get(next);
                        type = token.type;
                        if(type==TokenType.binaryOperator || type==TokenType.unaryOperator) {
                            OperationSemantics semantics = null;
                            for(OperationSemantics sem : ExpressionCalculator.operationSemantics) {
                                if(sem.precedence<precedence) break;
                                if(sem.precedence!=precedence) continue;
                                if(sem.associativity!=Associativity.right) continue;
                                if(!sem.op.equals(token.value)) continue;
                                if(type==TokenType.unaryOperator) {
                                    if(sem.leftOperand==OperandType.none) {
                                        semantics = sem; break;
                                    }
                                } else  {
                                    if(sem.leftOperand!=OperandType.none) {
                                        semantics = sem; break;
                                    }
                                }
                            }
                            if(semantics!=null) {
                                if(type==TokenType.binaryOperator) {
                                    int ret = insertBinaryOperationParans(next);
                                    if(ret<0) return false;
                                    if(ret>0) {
                                        next --; length +=2;
                                    }
                                } else {
                                    int ret = insertUnaryOperationParans(next);
                                    if(ret<0) return false;
                                    if(ret>0) {
                                        length +=2;
                                    }
                                }
                                
                            }
                        }
                        next--;
                    }
                    // left to right
                    next = 0;
                    while(next<length) {
                        token = tokenList.get(next);
                        type = token.type;
                        if(type==TokenType.binaryOperator || type==TokenType.unaryOperator) {
                            OperationSemantics semantics = null;
                            for(OperationSemantics sem : ExpressionCalculator.operationSemantics) {
                                if(sem.precedence<precedence) break;
                                if(sem.precedence!=precedence) continue;
                                if(sem.associativity!=Associativity.left) continue;
                                if(!sem.op.equals(token.value)) continue;
                                if(type==TokenType.unaryOperator) {
                                    if(sem.leftOperand==OperandType.none) {
                                        semantics = sem; break;
                                    }
                                } else  {
                                    if(sem.leftOperand!=OperandType.none) {
                                        semantics = sem; break;
                                    }
                                }
                            }
                            if(semantics!=null) {
                                if(type==TokenType.binaryOperator) {
                                    int ret = insertBinaryOperationParans(next);
                                    if(ret<0) return false;
                                    if(ret>0) {
                                        next ++; length +=2;
                                    }
                                } else {
                                    int ret = insertUnaryOperationParans(next);
                                    if(ret<0) return false;
                                    if(ret>0) {
                                        next++; length +=2;
                                    }
                                }
                            }
                        }
                        next++;
                    }
                }
                // ternaryIf is special case associativity is right to left
                int prev = length-1;
                while(prev >0) {
                    token = tokenList.get(prev);
                    if(token.value.equals("?")) {
                        int ret= insertTernaryIfParans(prev);
                        if(ret<0) return false;
                        if(ret>0) {
                            prev--; length +=2;
                        }
                    }
                    prev--;
                }
                return true;
            }
            
            private int insertUnaryOperationParans(int offset) {
                int length = tokenList.size();
                Token token = null;
                TokenType type = null;
                if(offset>0) {
                    TokenType prev = tokenList.get(offset-1).type;
                    if(prev==TokenType.leftParen) return 0;
                }
                token = new Token();
                token.type = TokenType.leftParen;
                token.value = "(";
                tokenList.add(offset , token);
                length = tokenList.size();
                int next = offset + 2;
                int parenDepth = 0;
                while(next<length) {
                    token = tokenList.get(next);
                    type = token.type;
                    if(type.isFunction()) {
                        next++;
                        continue;
                    }
                    if(type==TokenType.leftParen) {
                        parenDepth++; next++; continue;
                    }
                    if(type==TokenType.rightParen) {
                        if(parenDepth<=0) {
                            pvExpression.message("parse failure bad expression ", MessageType.error);
                            return -1;
                        }
                        parenDepth--; next++;
                        if(parenDepth>0) continue;
                        break;
                    }
                    if(parenDepth>0) {
                        next++; continue;
                    }
                    next++;
                    break;
                }
                if(next>=length) {
                    pvExpression.message("parse failure bad expression ", MessageType.error);
                    return -1;
                }
                token = new Token();
                token.type = TokenType.rightParen;
                token.value = ")";
                tokenList.add(next , token);
                return 1;
            }
            
            private int insertBinaryOperationParans(int offset) {
                int length = tokenList.size();
                Token token = null;
                TokenType type = null;
                int prev = offset - 1;
                int parenDepth = 0;
                boolean gotPrev = false;
                if(prev>=0) {
                    token = tokenList.get(prev);
                    type = token.type;
                    if(type.isOperand()) gotPrev = true;
                }
                while(!gotPrev && prev>=0) {
                    token = tokenList.get(prev);
                    type = token.type;
                    if(type==TokenType.rightParen) {
                        parenDepth++; prev--; continue;
                    }
                    if(type==TokenType.leftParen) {
                        parenDepth--;
                        if(parenDepth==0) {
                            gotPrev = true;
                            break;
                        }
                    }
                    if(parenDepth>0) {
                        prev--; continue;
                    }
                    pvExpression.message("parse failure bad expression ", MessageType.error);
                    return -1;
                }
                int next = offset + 1;
                boolean gotNext = false;
                if(next<length && tokenList.get(next).type.isOperand()) gotNext = true;
                while(!gotNext && next<length) {
                    token = tokenList.get(next);
                    type = token.type;
                    if(type.isFunction()) {
                        next++;
                        continue;
                    }
                    if(type==TokenType.leftParen) {
                        parenDepth++; next++; continue;
                    }
                    if(type==TokenType.rightParen) {
                        if(parenDepth==0) {
                            pvExpression.message("parse failure bad expression ", MessageType.error);
                            return -1;
                        }
                        parenDepth--;
                        if(parenDepth==0) {
                            gotNext = true;
                            break;
                        }
                    }
                    if(parenDepth>0) {
                        next++; continue;
                    }
                    pvExpression.message("parse failure bad expression ", MessageType.error);
                    return -1;
                }
                TokenType beforePrevType = null;
                TokenType afterNextType = null;
                if(prev>0) {
                    beforePrevType = tokenList.get(prev-1).type;
                }
                if(next<length-1) {
                    afterNextType = tokenList.get(next+1).type;
                }
                if(beforePrevType==TokenType.leftParen && afterNextType==TokenType.rightParen) {
                    if(prev<2) return 0;
                    if(!tokenList.get(prev-2).type.isFunction()) return 0;
                }
                token = new Token();
                token.type = TokenType.rightParen;
                token.value = ")";
                tokenList.add(next+1 , token);
                token = new Token();
                token.type = TokenType.leftParen;
                token.value = "(";
                tokenList.add(prev , token);
                return 1;
            }
            
            private int insertTernaryIfParans(int offset) {
                // see if already inclosed in () 
                int length = tokenList.size();
                int prev = offset-1;
                Token token = null;
                TokenType type = null;
                int parenDepth = 0;
                while(prev>0) {
                    token = tokenList.get(prev);
                    type = token.type;
                    if(type==TokenType.rightParen) {
                        parenDepth++; prev--; continue;
                    }
                    if(type==TokenType.leftParen) {
                        if(parenDepth>0) {
                            parenDepth--; prev--; continue;
                        }
                        return 0;
                    }
                    if(parenDepth>0) {
                        prev--; continue;
                    }
                    prev-- ; break;
                }
                token = tokenList.get(prev);
                if(token.type==TokenType.leftParen) return 0;
                Token newToken = new Token();
                newToken.type = TokenType.leftParen;
                newToken.value = "(";
                tokenList.add(prev , newToken);
                int next = offset+1;
                // look for first :
                while(next++<length) {
                    token = tokenList.get(next);
                    if(token.value.equals(":")) break;
                }
                parenDepth = 0;
                while(next++<length) {
                    token = tokenList.get(next);
                    type = token.type;
                    if(type==TokenType.leftParen) {
                        parenDepth++; continue;
                    }
                    if(type==TokenType.rightParen && parenDepth>0) {
                        parenDepth--; continue;
                    }
                    if(parenDepth>0) continue;
                    newToken = new Token();
                    newToken.type = TokenType.rightParen;
                    newToken.value = ")";
                    tokenList.add(next+1 , newToken);
                    return 1;
                }
                pvExpression.message("parse failure bad expression ", MessageType.error);
                return -1;
            }

            private Expression createExpression() {
                Stack<Expression> infixExpStack = new Stack<Expression>();
                Stack<Expression> expStack = new Stack<Expression>();
                while(!tokenList.isEmpty()) {
                    Token nextToken= tokenList.remove(0);
                    TokenType type = nextToken.type;
                    if(type==TokenType.leftParen || type==TokenType.comma) continue;
                    if(type==TokenType.rightParen) {
                        if(infixExpStack.size()>0) {
                            unwindExpStack(infixExpStack.pop(),expStack);
                        }
                        continue;
                    }
                    if(type.isOperand()) {
                        Expression exp = new Expression();
                        exp.token = nextToken;
                        if(!unwindExpStack(exp,expStack)) return null;
                        continue;
                    }
                    
                    if(type.isOperator()) {
                        Expression exp = null;
                        switch(type) {
                        case mathFunction:  {
                            exp = new MathFunctionExpression();
                            exp.token = nextToken;
                            break;
                        }
                        case unaryOperator:
                        case binaryOperator:
                        {
                            exp = new OperatorExpression();
                            exp.token = nextToken;
                            break;
                        }

                        case ternaryOperator: {
                            if(nextToken.value.equals("?")) break;
                            exp = new OperatorExpression();
                            exp.token = new Token();
                            exp.token.type = TokenType.ternaryOperator;
                            exp.token.value = "?:";
                            break;
                        }
                        default:
                            throw new IllegalStateException("logic error.");
                        }
                        if(exp!=null) infixExpStack.push(exp);
                        continue;
                    }
                    
                }
                boolean ok = true;
                if(infixExpStack.size()!=0) {
                    pvExpression.message("logic error infixExpStack not empty",MessageType.error);
                    printTokenList("tokenList");
                    printExpStack("infixExpStack",infixExpStack);
                    printExpStack("expStack",expStack);
                    ok = false;
                }
                if(expStack.size()!=1) {
                    pvExpression.message("logic error expStack should only have one element",MessageType.error);
                    printTokenList("tokenList");
                    printExpStack("infixExpStack",infixExpStack);
                    printExpStack("expStack",expStack);
                    ok = false;
                }
                if(!ok) return null;
                Expression expression = expStack.firstElement();
                pruneExpStack(expression);
                return expression;
            }
            
            private boolean unwindExpStack(Expression exp,Stack<Expression> expStack) {
                Token token = exp.token;
                TokenType type = token.type;
                switch(type) {
                case variable: {
                    PVField pvField = null;
                    String name = token.value;
                    if(name.equals("value")) {
                        pvField = dbValue.getPVField();
                    } else {
                        pvField = calcArgArraySupport.getPVField(name);
                    }
                    if(pvField==null) {
                        pvStructure.message("variable " + name + " not found", MessageType.error);
                        return false;
                    }
                    exp.pvResult = pvField;
                    expStack.push(exp);
                    return true;
                }
                case booleanConstant: {
                    Boolean scalar = Boolean.valueOf(token.value);
                    Field field = fieldCreate.createField("result", Type.pvBoolean);
                    PVBoolean pv = (PVBoolean)pvDataCreate.createPVField(pvStructure, field);
                    pv.put(scalar);
                    pv.setMutable(false);
                    exp.pvResult = pv;
                    expStack.push(exp);
                    return true;
                }
                case integerConstant: {
                    PVField pvField = null;
                    String value = token.value;
                    int length = value.length();
                    char lastChar = value.charAt(length-1);
                    Type pvType = (lastChar=='L') ? Type.pvLong : Type.pvInt;
                    Field field = fieldCreate.createField("result", pvType);
                    pvField = pvDataCreate.createPVField(pvStructure, field);
                    if(pvType==Type.pvLong) {
                        Long scalar = new Long(0);
                        try {
                            scalar = Long.decode(value.substring(0, length-1));
                        } catch (NumberFormatException e) {
                            pvStructure.message(e.getMessage(), MessageType.error);
                            return false;
                        }
                        PVLong pv = (PVLong)pvField;
                        pv.put(scalar);
                    } else {
                        Long scalar = new Long(0);
                        try {
                            scalar = Long.decode(value);
                        } catch (NumberFormatException e) {
                            pvStructure.message(e.getMessage(), MessageType.error);
                            return false;
                        }
                        PVInt pv = (PVInt)pvField;
                        pv.put((int)(long)scalar);
                    }
                    pvField.setMutable(false);
                    exp.pvResult = pvField;
                    expStack.push(exp);
                    return true;
                }
                case realConstant: {
                    PVField pvField = null;
                    String value = token.value;
                    int length = value.length();
                    char lastChar = value.charAt(length-1);
                    Type pvType = (lastChar=='F') ? Type.pvFloat : Type.pvDouble;
                    Field field = fieldCreate.createField("result", pvType);
                    pvField = pvDataCreate.createPVField(pvStructure, field);
                    if(pvType==Type.pvFloat) {
                        Float scalar = Float.valueOf(value);
                        PVFloat pv = (PVFloat)pvField;
                        pv.put(scalar);
                    } else {
                        Double scalar = Double.valueOf(value);
                        PVDouble pv = (PVDouble)pvField;
                        pv.put(scalar);
                    }
                    pvField.setMutable(false);
                    exp.pvResult = pvField;
                    expStack.push(exp);
                    return true;
                }
                case stringConstant: {
                    String scalar = token.value;
                    Field field = fieldCreate.createField("result", Type.pvString);
                    PVString pv = (PVString)pvDataCreate.createPVField(pvStructure, field);
                    pv.put(scalar);
                    pv.setMutable(false);
                    exp.pvResult = pv;
                    expStack.push(exp);
                    return true;
                }
                case mathConstant: {
                    String functionName = exp.token.value;
                    Field field = fieldCreate.createField("result", Type.pvDouble);
                    PVDouble pv = (PVDouble)pvDataCreate.createPVField(pvStructure, field);
                    double value = (functionName.equals("E")) ? Math.E : Math.PI;
                    pv.put(value);
                    pv.setMutable(false);
                    exp.pvResult = pv;
                    expStack.push(exp);
                    return true;
                }

                case mathFunction: {
                    MathFunctionExpression funcExp = (MathFunctionExpression)exp;
                    String functionName = exp.token.value;
                    MathFunctionSemantics functionSemantics = null;
                    for(MathFunctionSemantics semantics: mathFunctionSemantics) {
                        if(semantics.mathFunction.name().equals(functionName)) {
                            functionSemantics = semantics;
                            break;
                        }
                    }
                    if(functionSemantics==null) {
                        pvStructure.message(
                                "unsupported Math function " + functionName,
                                MessageType.error);
                        return false;
                    }
                    funcExp.functionSemantics = functionSemantics;
                    int nargs = functionSemantics.nargs;
                    exp.expressionArguments = new Expression[nargs];
                    int iarg = nargs;
                    while(--iarg>=0) {
                        exp.expressionArguments[iarg] = expStack.pop();
                    }
                    funcExp.operator = MathFactory.create(pvStructure,funcExp);
                    if(!funcExp.operator.createPVResult("result")) return false;
                    expStack.push(exp);
                    return true;
                }
                case unaryOperator: {
                    OperatorExpression opExp = (OperatorExpression)exp;
                    int size = expStack.size();
                    if(size<1) {
                        pvStructure.message(
                                " nargs " + size + " illegal for unaryOperator",
                                MessageType.error);
                        return false;
                    }
                    exp.expressionArguments = new Expression[1];
                    exp.expressionArguments[0] = expStack.pop();
                    PVField argResult = exp.expressionArguments[0].pvResult;
                    Type argType = argResult.getField().getType();
                    opExp.operationSemantics = null;
                    outer:
                    for(OperationSemantics sem : ExpressionCalculator.operationSemantics) {
                        if(!sem.op.equals(token.value)) continue;
                        if(sem.leftOperand!=OperandType.none) continue;
                        switch(sem.rightOperand) {
                        case none: continue;
                        case integer:
                            if(argType.isInteger()) {
                                opExp.operationSemantics = sem;
                                break outer;
                            }
                            continue;
                        case bool:
                            if(argType==Type.pvBoolean) {
                                opExp.operationSemantics = sem;
                                break outer;
                            }
                            continue;
                        case number:
                            if(argType.isNumeric()) {
                                opExp.operationSemantics = sem;
                                break outer;
                            }
                            continue;
                        case string:
                            if(argType==Type.pvString) {
                                opExp.operationSemantics = sem;
                                break outer;
                            }
                            continue;
                        case any:
                            opExp.operationSemantics = sem;
                            break outer;
                        }
                    }
                    if(opExp.operationSemantics==null) {
                        pvStructure.message(
                            "unsupported unary operation " + token.value,
                            MessageType.error);
                        return false;
                    }
                    opExp.operator = OperatorFactory.create(pvStructure, opExp);
                    if(opExp.operator==null) {
                        pvStructure.message(
                                "unsupported unary operation " + token.value,
                                MessageType.error);
                            return false;
                    }
                    if(!opExp.operator.createPVResult("result")) return false;
                    expStack.push(exp);
                    return true;
                }
                case binaryOperator: {
                    OperatorExpression opExp = (OperatorExpression)exp;
                    int size = expStack.size();
                    if(size<2) {
                        pvStructure.message(
                                " nargs " + size + " illegal for binaryOperator",
                                MessageType.error);
                        return false;
                    }
                    exp.expressionArguments = new Expression[2];
                    exp.expressionArguments[1] = expStack.pop();
                    exp.expressionArguments[0] = expStack.pop();
                    PVField argResult = exp.expressionArguments[0].pvResult;
                    Type argType = argResult.getField().getType();
                    opExp.operationSemantics = null;
                    outer:
                    for(OperationSemantics sem : ExpressionCalculator.operationSemantics) {
                        if(!sem.op.equals(token.value)) continue;
                        if(sem.leftOperand==OperandType.none) continue;
                        switch(sem.rightOperand) {
                        case none: continue;
                        case integer:
                            if(argType.isInteger()) {
                                opExp.operationSemantics = sem;
                                break outer;
                            }
                            continue;
                        case bool:
                            if(argType==Type.pvBoolean) {
                                opExp.operationSemantics = sem;
                                break outer;
                            }
                            continue;
                        case number:
                            if(argType.isNumeric()) {
                                opExp.operationSemantics = sem;
                                break outer;
                            }
                            continue;
                        case string:
                            if(argType==Type.pvString) {
                                opExp.operationSemantics = sem;
                                break outer;
                            }
                            continue;
                        case any:
                            opExp.operationSemantics = sem;
                            break outer;
                        }
                    }
                    if(opExp.operationSemantics==null) {
                        pvStructure.message(
                            "unsupported unary operation " + token.value,
                            MessageType.error);
                        return false;
                    }
                    opExp.operator = OperatorFactory.create(pvStructure, opExp);
                    if(opExp.operator==null) {
                        pvStructure.message(
                                "unsupported unary operation " + token.value,
                                MessageType.error);
                            return false;
                    }
                    if(!opExp.operator.createPVResult("result")) return false;
                    expStack.push(exp);
                    return true;
                }
                case ternaryOperator: {
                    OperatorExpression opExp = (OperatorExpression)exp;
                    int size = expStack.size();
                    if(size<3) {
                        pvStructure.message(
                                " nargs " + size + " illegal for ternaryOperator",
                                MessageType.error);
                        return false;
                    }
                    exp.expressionArguments = new Expression[3];
                    exp.expressionArguments[2] = expStack.pop();
                    exp.expressionArguments[1] = expStack.pop();
                    exp.expressionArguments[0] = expStack.pop();
                    Type arg0Type = exp.expressionArguments[0].pvResult.getField().getType();
                    Type arg1Type = exp.expressionArguments[1].pvResult.getField().getType();
                    Type arg2Type = exp.expressionArguments[2].pvResult.getField().getType();
                    if(arg0Type!=Type.pvBoolean) {
                        pvStructure.message("arg0 must be boolean",MessageType.error);
                        return false;
                    }
                    if(arg1Type!=arg2Type) {
                        pvStructure.message("arg1 and arg2 must be same type",MessageType.error);
                        return false;
                    }
                    opExp.operator = new TernaryIf(pvStructure,opExp);
                    if(!opExp.operator.createPVResult("result")) return false;
                    expStack.push(exp);
                    return true;
                }
                default:
                    throw new IllegalStateException("logic error " + type);
                }
            }
            
            private void printTokenList(String message) {
                if(tokenList.isEmpty()) {
                    System.out.println(message + " tokenList is empty");
                } else {
                    System.out.println(message + " tokenList");
                }
                int nlev = 0;
                for(Token token : tokenList) {
                    if(token.type==TokenType.rightParen) nlev--;
                    String blanks = "";
                    for (int i=0; i<nlev; i++) blanks += "  ";
                    System.out.println(blanks + token.type.name() + " " + token.value);
                    if(token.type == TokenType.leftParen) nlev++;

                }
            }
            
            private void pruneExpStack(Expression expression) {
                Expression[] expressionArguments = expression.expressionArguments;
                Operator operator = expression.operator;
                int numConstantArgs = 0;
                int numArgs = 0;
                if(expressionArguments!=null) {
                    numArgs = expressionArguments.length;
                    for(Expression argExp: expressionArguments) {
                        pruneExpStack(argExp);
                        if(argExp.pvResult.isMutable()) continue;
                        if(!(argExp instanceof MathFunctionExpression)) {
                            numConstantArgs++;
                            continue;
                        }
                        MathFunctionExpression mathFunExp = (MathFunctionExpression)argExp;
                        if(!mathFunExp.functionSemantics.isRandom) numConstantArgs++;
                    }
                }
                if(numArgs>0 && operator!=null && numConstantArgs==numArgs) {
                    boolean okToPrune = true;
                    if(expression instanceof MathFunctionExpression) {
                        MathFunctionExpression mathFunExp = (MathFunctionExpression)expression;
                        if(mathFunExp.functionSemantics.isRandom) okToPrune = false;
                    }
                    if(okToPrune) {
                        operator.compute();
                        expression.pvResult.setMutable(false);
                        expression.operator = null;
                        expression.expressionArguments = new Expression[0];
                    }
                }
            }
            
            private void printExpStack(String message,Stack<Expression> expStack) {
                if(expStack.isEmpty()) {
                    System.out.println(message + " is empty");
                    return;
                }
                System.out.println(message);
                for(Expression exp : expStack) {
                    printExpression(exp,"  ");
                }
            }
            
            private void printExpression(Expression expression,String prefix) {
                if(expression==null) {
                    System.out.println(prefix + " is null");
                    return;
                }
                if(expression.operator!=null) {
                    if(expression instanceof OperatorExpression) {
                        OperatorExpression operatorExpression = (OperatorExpression)expression;
                        OperationSemantics operationSemantics = operatorExpression.operationSemantics;
                        String operationName = "operationSemantics is null";
                        if(operationSemantics!=null) operationName = operationSemantics.operation.name();
                        System.out.println(prefix + "OperatorExpression " + operationName);
                    } else if(expression instanceof MathFunctionExpression) {
                        MathFunctionExpression functionExpression = (MathFunctionExpression)expression;
                        MathFunctionSemantics functionSemantics = functionExpression.functionSemantics;
                        String functionName = "function is null";
                        if(functionSemantics!=null) functionName = functionSemantics.mathFunction.name();
                        System.out.println(prefix + "MathFunctionExpression " + functionName);
                    }
                }
                Token token = expression.token;
                System.out.println(prefix + token.type.name() +" " + token.value );
                Expression[] args = expression.expressionArguments;
                if(args!=null) {
                    for(Expression arg : args) {
                        printExpression(arg,"  " + prefix);
                    }
                }
                PVField pvResult = expression.pvResult;
                if(pvResult==null) {
                    System.out.println(prefix + "pvResult is null");
                    return;
                }
                Field field = pvResult.getField();
                System.out.println(prefix + field.getFieldName() + " " + field.getType().name());
                return;
            }
        }
        
        
        
        private static class OperatorFactory {
            static Operator create(
                    PVStructure parent,
                    OperatorExpression operatorExpression)
            {
                OperationSemantics operationSemantics = operatorExpression.operationSemantics;
                switch(operationSemantics.operation) {
                case unaryPlus:
                    return new UnaryPlus(parent,operatorExpression);
                case unaryMinus:
                    return new UnaryMinus(parent,operatorExpression);
                case bitwiseComplement:
                    return new BitwiseComplement(parent,operatorExpression);
                case booleanNot:
                    return new BooleanNot(parent,operatorExpression);
                case multiplication:
                    return new Multiplication(parent,operatorExpression);
                case division:
                    return new Division(parent,operatorExpression);
                case remainder:
                    return new Remainder(parent,operatorExpression);
                case plus:
                    return new Plus(parent,operatorExpression);
                case minus:
                    return new Minus(parent,operatorExpression);
                case stringPlus:
                    return new StringPlus(parent,operatorExpression);
                case leftShift:
                    return new LeftShift(parent,operatorExpression);
                case rightShiftSignExtended:
                    return new RightShiftSignExtended(parent,operatorExpression);
                case rightShiftZeroExtended:
                    return new RightShiftZeroExtended(parent,operatorExpression);
                case lessThan:
                    return new LessThan(parent,operatorExpression);
                case lessThanEqual:
                    return new LessThanEqual(parent,operatorExpression);
                case greaterThan:
                    return new GreaterThan(parent,operatorExpression);
                case greaterThanEqual:
                    return new GreaterThanEqual(parent,operatorExpression);
                case equalEqual:
                    return new EqualEqual(parent,operatorExpression);
                case notEqual:
                    return new NotEqual(parent,operatorExpression);
                case bitwiseAnd:
                    return new BitwiseAnd(parent,operatorExpression);
                case booleanAnd:
                    return new BooleanAnd(parent,operatorExpression);
                case bitwiseXOR:
                    return new BitwiseXOR(parent,operatorExpression);
                case booleanXOR:
                    return new BooleanXOR(parent,operatorExpression);
                case bitwiseOr:
                    return new BitwiseOr(parent,operatorExpression);
                case booleanOr:
                    return new BooleanOr(parent,operatorExpression);
                case conditionalAnd:
                    return new ConditionalAnd(parent,operatorExpression);
                case conditionalOr:
                    return new ConditionalOr(parent,operatorExpression);
                case ternaryIf :
                    return new TernaryIf(parent,operatorExpression);
                }
                return null;
            }
        }
        
        static class UnaryPlus implements Operator {
            
            private OperatorExpression operatorExpression;
            UnaryPlus(PVStructure parent,OperatorExpression operatorExpression) {
                this.operatorExpression = operatorExpression;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#createPVResult(java.lang.String)
             */
            public boolean createPVResult(String fieldName) {
                operatorExpression.pvResult = operatorExpression.expressionArguments[0].pvResult;
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                return;
            }
        }
        
        static class UnaryMinus implements Operator {
            private PVStructure parent;
            private OperatorExpression operatorExpression;
            private PVField argPV;
            
            private PVField resultPV;
            private Type resultType;
            
            UnaryMinus(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                argPV = operatorExpression.expressionArguments[0].pvResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#createPVResult(java.lang.String)
             */
            public boolean createPVResult(String fieldName) {
                Field argField = argPV.getField();
                Type argType = argField.getType();
                if(!argType.isNumeric()) {
                    parent.message(
                            "For operator + "  + argPV.getFullFieldName() + " is not numeric",
                            MessageType.fatalError);
                    return false;
                }
                resultType = argField.getType();
                Field resultField = fieldCreate.createField(fieldName, resultType);
                resultPV = pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: 
                    convert.fromByte(resultPV, (byte)-(convert.toByte(argPV))); break;
                case pvShort: 
                    convert.fromShort(resultPV, (short)-(convert.toShort(argPV))); break;
                case pvInt: 
                    convert.fromInt(resultPV, (int)-(convert.toInt(argPV))); break;
                case pvLong: 
                    convert.fromLong(resultPV, (long)-(convert.toLong(argPV))); break;
                case pvFloat: 
                    convert.fromFloat(resultPV, (float)-(convert.toFloat(argPV))); break;
                case pvDouble: 
                    convert.fromDouble(resultPV, (double)-(convert.toDouble(argPV))); break;
                default:
                    throw new IllegalStateException("logic error. unknown pvType");
                }
                return;
            }
        }
        
        static class BitwiseComplement implements Operator {
            private PVStructure parent;
            private OperatorExpression operatorExpression;
            private PVField argPV;
            
            private PVField resultPV;
            private Type resultType;
            
            BitwiseComplement(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                argPV = operatorExpression.expressionArguments[0].pvResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#createPVResult(java.lang.String)
             */
            public boolean createPVResult(String fieldName) {
                Field argField = argPV.getField();
                Type argType = argField.getType();
               
                if(!argType.isInteger()) {
                    parent.message(
                            "For operator ~ "  + argPV.getFullFieldName() + " is not integer",
                            MessageType.fatalError);
                    return false;
                }
                resultType = argField.getType();
                Field resultField = fieldCreate.createField(fieldName, resultType);
                resultPV = pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: 
                    convert.fromByte(resultPV, (byte)~(convert.toByte(argPV))); break;
                case pvShort: 
                    convert.fromShort(resultPV, (short)~(convert.toShort(argPV))); break;
                case pvInt: 
                    convert.fromInt(resultPV, (int)~(convert.toInt(argPV))); break;
                case pvLong: 
                    convert.fromLong(resultPV, (long)~(convert.toLong(argPV))); break;
                default:
                    throw new IllegalStateException("logic error. unknown pvType");
                }
                return;
            }
        }
        
        static class BooleanNot implements Operator {
            private PVStructure parent;
            private OperatorExpression operatorExpression;
            PVField pvField = null;
            private PVBoolean argPV = null;
            private PVBoolean resultPV = null;
            
            BooleanNot(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                pvField = operatorExpression.expressionArguments[0].pvResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#createPVResult(java.lang.String)
             */
            public boolean createPVResult(String fieldName) {
                if(pvField.getField().getType()!=Type.pvBoolean) {
                    parent.message(
                            "For operator ! " + argPV.getFullFieldName() + " is not boolean",
                            MessageType.fatalError);
                    return false;
                }
                argPV = (PVBoolean)pvField;
                Field resultField = fieldCreate.createField(fieldName, Type.pvBoolean);
                resultPV = (PVBoolean)pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                resultPV.put(!argPV.get());
            }
        }
        abstract static class NumericBinaryBase implements Operator {
            protected PVStructure parent;
            protected OperationSemantics operationSemantics;
            protected OperatorExpression operatorExpression;

            protected PVField arg0PV;
            protected Field arg0Field;
            protected Type arg0Type;
            protected PVField arg1PV;
            protected Field arg1Field;
            protected Type arg1Type;
            protected PVField resultPV;
            protected Field resultField;
            protected Type resultType;


            NumericBinaryBase(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#createPVResult(java.lang.String)
             */
            public boolean createPVResult(String fieldName) {
                arg0PV = operatorExpression.expressionArguments[0].pvResult;
                arg0Field = arg0PV.getField();
                arg0Type = arg0Field.getType();
                arg1PV = operatorExpression.expressionArguments[1].pvResult;
                arg1Field = arg1PV.getField();
                arg1Type = arg1Field.getType();
                if(!convert.isCopyScalarCompatible(arg0PV.getField(),arg1PV.getField())) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName()
                            + " not compatible with " +arg1PV.getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                int ind0 = arg0PV.getField().getType().ordinal();
                int ind1 = arg1PV.getField().getType().ordinal();
                int ind = ind0;
                if(ind<ind1) ind = ind1;
                resultType = Type.values()[ind];
                resultField = fieldCreate.createField(fieldName, resultType);
                resultPV = pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            abstract public void compute();
        }
        
        static class Multiplication extends NumericBinaryBase {
            
            Multiplication(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    byte value = (byte)(arg0 * arg1);
                    ((PVByte)resultPV).put(value);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    short value = (short)(arg0 * arg1);
                    ((PVShort)resultPV).put(value);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    int value = (int)(arg0 * arg1);
                    ((PVInt)resultPV).put(value);
                    return;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    long value = (long)(arg0 * arg1);
                    ((PVLong)resultPV).put(value);
                    return;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    float value = (float)(arg0 * arg1);
                    ((PVFloat)resultPV).put(value);
                    return;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    double value = (double)(arg0 * arg1);
                    ((PVDouble)resultPV).put(value);
                    return;
                }

                }

            }
        }
        
        static class Division extends NumericBinaryBase {
           
            Division(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
               
            }
           
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    byte value = (byte)(arg0 / arg1);
                    ((PVByte)resultPV).put(value);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    short value = (short)(arg0 / arg1);
                    ((PVShort)resultPV).put(value);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    int value = (int)(arg0 / arg1);
                    ((PVInt)resultPV).put(value);
                    return;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    long value = (long)(arg0 / arg1);
                    ((PVLong)resultPV).put(value);
                    return;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    float value = (float)(arg0 / arg1);
                    ((PVFloat)resultPV).put(value);
                    return;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    double value = (double)(arg0 / arg1);
                    ((PVDouble)resultPV).put(value);
                    return;
                }

                }

            }
        }
        
        static class Remainder extends NumericBinaryBase {
           
            Remainder(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    byte value = (byte)(arg0 % arg1);
                    ((PVByte)resultPV).put(value);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    short value = (short)(arg0 % arg1);
                    ((PVShort)resultPV).put(value);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    int value = (int)(arg0 % arg1);
                    ((PVInt)resultPV).put(value);
                    return;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    long value = (long)(arg0 % arg1);
                    ((PVLong)resultPV).put(value);
                    return;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    float value = (float)(arg0 % arg1);
                    ((PVFloat)resultPV).put(value);
                    return;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    double value = (double)(arg0 % arg1);
                    ((PVDouble)resultPV).put(value);
                    return;
                }

                }

            }
        }
        
        static class Plus extends NumericBinaryBase {

            Plus(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    byte value = (byte)(arg0 + arg1);
                    ((PVByte)resultPV).put(value);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    short value = (short)(arg0 + arg1);
                    ((PVShort)resultPV).put(value);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    int value = (int)(arg0 + arg1);
                    ((PVInt)resultPV).put(value);
                    return;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    long value = (long)(arg0 + arg1);
                    ((PVLong)resultPV).put(value);
                    return;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    float value = (float)(arg0 + arg1);
                    ((PVFloat)resultPV).put(value);
                    return;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    double value = (double)(arg0 + arg1);
                    ((PVDouble)resultPV).put(value);
                    return;
                }

                }

            }
        }
        
        static class Minus extends NumericBinaryBase  {
           
            Minus(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    byte value = (byte)(arg0 - arg1);
                    ((PVByte)resultPV).put(value);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    short value = (short)(arg0 - arg1);
                    ((PVShort)resultPV).put(value);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    int value = (int)(arg0 - arg1);
                    ((PVInt)resultPV).put(value);
                    return;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    long value = (long)(arg0 - arg1);
                    ((PVLong)resultPV).put(value);
                    return;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    float value = (float)(arg0 - arg1);
                    ((PVFloat)resultPV).put(value);
                    return;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    double value = (double)(arg0 - arg1);
                    ((PVDouble)resultPV).put(value);
                    return;
                }

                }

            }
        }
        
        static class StringPlus implements Operator {
            private PVStructure parent;
            private OperatorExpression operatorExpression;

            private PVField arg0PV;
            private PVField arg1PV;
            private PVString resultPV;

            StringPlus(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#createPVResult(java.lang.String)
             */
            public boolean createPVResult(String fieldName) {
                arg0PV = operatorExpression.expressionArguments[0].pvResult;
                arg1PV = operatorExpression.expressionArguments[1].pvResult;
                Field resultField = fieldCreate.createField(fieldName, Type.pvString);
                resultPV = (PVString)pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#compute()
             */
            public void compute() {
                operatorExpression.computeArguments();
                String value = convert.getString(arg0PV) + convert.getString(arg1PV);
                resultPV.put(value);
            }
        }
        
        abstract static class ShiftBase implements Operator {
            protected PVStructure parent;
            protected OperatorExpression operatorExpression;
            protected OperationSemantics operationSemantics;
            
            protected PVField arg0PV;
            protected PVField arg1PV;
            protected PVField resultPV;
            protected Type resultType;

            ShiftBase(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            public boolean createPVResult(String fieldName) {
                arg0PV = operatorExpression.expressionArguments[0].pvResult;
                Field arg0Field = arg0PV.getField();
                Type arg0Type = arg0Field.getType();
                arg1PV = operatorExpression.expressionArguments[1].pvResult;
                Field arg1Field = arg1PV.getField();
                Type arg1Type = arg1Field.getType();
                if(!arg0Type.isInteger() || !arg1Type.isInteger()) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName()
                            + " not compatible with " +arg1PV.getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                resultType = arg0Type;
                Field resultField = fieldCreate.createField(fieldName, arg0Type);
                resultPV = pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            abstract public void compute();
        }
        
        static class LeftShift extends ShiftBase {
            LeftShift(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                byte shift = convert.toByte(arg1PV);
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte value = (byte)(arg0<<shift);
                    ((PVByte)resultPV).put(value);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short value = (short)(arg0<<shift);
                    ((PVShort)resultPV).put(value);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int value = (int)(arg0<<shift);
                    ((PVInt)resultPV).put(value);
                    return;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long value = (long)(arg0<<shift);
                    ((PVLong)resultPV).put(value);
                    return;
                }
                }
            }
        }
        
        static class RightShiftSignExtended extends ShiftBase {
            RightShiftSignExtended(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                byte shift = convert.toByte(arg1PV);
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte value = (byte)(arg0>>shift);
                    ((PVByte)resultPV).put(value);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short value = (short)(arg0>>shift);
                    ((PVShort)resultPV).put(value);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int value = (int)(arg0>>shift);
                    ((PVInt)resultPV).put(value);
                    return;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long value = (long)(arg0>>shift);
                    ((PVLong)resultPV).put(value);
                    return;
                }
                }
            }
        }
        
        static class RightShiftZeroExtended extends ShiftBase  {
            RightShiftZeroExtended(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                byte shift = convert.toByte(arg1PV);
                switch(resultType) {
                case pvByte: {
                    int arg0 = convert.toByte(arg0PV);
                    arg0 &= 0x0ff;
                    byte value = (byte)(arg0>>>shift);
                    ((PVByte)resultPV).put(value);
                    return;
                }
                case pvShort: {
                    int arg0 = convert.toShort(arg0PV);
                    arg0 &= 0x0ffff;
                    short value = (short)(arg0>>>shift);
                    ((PVShort)resultPV).put(value);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int value = (arg0>>>shift);
                    ((PVInt)resultPV).put(value);
                    return;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long value = (long)(arg0>>>shift);
                    ((PVLong)resultPV).put(value);
                    return;
                }
                }
            }
        }
        
        abstract static class Relational implements Operator {
            protected PVStructure parent;
            protected OperationSemantics operationSemantics;
            protected OperatorExpression operatorExpression;

            protected PVField arg0PV;
            protected Type type;
            protected PVField arg1PV;
            protected PVBoolean resultPV;

            Relational(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            public boolean createPVResult(String fieldName) {
                Expression expressionArgument = operatorExpression.expressionArguments[0];
                arg0PV = expressionArgument.pvResult;
                Type arg0Type = arg0PV.getField().getType();
                expressionArgument = operatorExpression.expressionArguments[1];
                arg1PV = expressionArgument.pvResult;
                Type arg1Type = arg1PV.getField().getType();
                if(!convert.isCopyScalarCompatible(arg0PV.getField(),arg1PV.getField())) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName()
                            + " not compatible with " +arg1PV.getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                int ind0 = arg0Type.ordinal();
                int ind1 = arg1Type.ordinal();
                type = Type.values()[Math.max(ind0,ind1)];
                Field resultField = fieldCreate.createField(fieldName, Type.pvBoolean);
                resultPV = (PVBoolean)pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            abstract public void compute();
        }
        
        static class LessThan extends Relational {

            LessThan(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                boolean result = false;
                switch(type) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    result = (arg0<arg1) ? true : false;
                    break;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    result = (arg0<arg1) ? true : false;
                    break;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    result = (arg0<arg1) ? true : false;
                    break;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    result = (arg0<arg1) ? true : false;
                    break;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    result = (arg0<arg1) ? true : false;
                    break;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    result = (arg0<arg1) ? true : false;
                    break;
                }
                default:
                    parent.message(
                            "unsupported operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName(),
                            MessageType.fatalError);
                    return;
                }
                resultPV.put(result);
            }
        }

        static class LessThanEqual extends Relational {

            LessThanEqual(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                boolean result = false;
                switch(type) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    result = (arg0<=arg1) ? true : false;
                    break;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    result = (arg0<=arg1) ? true : false;
                    break;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    result = (arg0<=arg1) ? true : false;
                    break;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    result = (arg0<=arg1) ? true : false;
                    break;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    result = (arg0<=arg1) ? true : false;
                    break;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    result = (arg0<=arg1) ? true : false;
                    break;
                }
                default:
                    parent.message(
                            "unsupported operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName(),
                            MessageType.fatalError);
                    return;
                }
                resultPV.put(result);
            }
        }

        static class GreaterThan extends Relational {

            GreaterThan(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                boolean result = false;
                switch(type) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    result = (arg0>arg1) ? true : false;
                    break;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    result = (arg0>arg1) ? true : false;
                    break;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    result = (arg0>arg1) ? true : false;
                    break;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    result = (arg0>arg1) ? true : false;
                    break;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    result = (arg0>arg1) ? true : false;
                    break;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    result = (arg0>arg1) ? true : false;
                    break;
                }
                default:
                    parent.message(
                            "unsupported operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName(),
                            MessageType.fatalError);
                    return;
                }
                resultPV.put(result);
            }
        }

        static class GreaterThanEqual extends Relational {

            GreaterThanEqual(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                boolean result = false;
                switch(type) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    result = (arg0>=arg1) ? true : false;
                    break;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    result = (arg0>=arg1) ? true : false;
                    break;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    result = (arg0>=arg1) ? true : false;
                    break;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    result = (arg0>=arg1) ? true : false;
                    break;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    result = (arg0>=arg1) ? true : false;
                    break;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    result = (arg0>=arg1) ? true : false;
                    break;
                }
                default:
                    parent.message(
                            "unsupported operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName(),
                            MessageType.fatalError);
                    return;
                }
                resultPV.put(result);
            }
        }

        static class EqualEqual extends Relational {

            EqualEqual(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                boolean result = false;
                switch(type) {
                case pvBoolean: {
                    boolean arg0 = ((PVBoolean)arg0PV).get();
                    boolean arg1 = ((PVBoolean)arg1PV).get();
                    result = (arg0==arg1) ? true : false;
                    break;
                }
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    result = (arg0==arg1) ? true : false;
                    break;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    result = (arg0==arg1) ? true : false;
                    break;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    result = (arg0==arg1) ? true : false;
                    break;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    result = (arg0==arg1) ? true : false;
                    break;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    result = (arg0==arg1) ? true : false;
                    break;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    result = (arg0==arg1) ? true : false;
                    break;
                }
                case pvString: {
                    PVString arg0 = (PVString)arg0PV;
                    String arg1 = convert.getString(arg1PV,0);
                    result = (arg0.get().equals(arg1)) ? true : false;
                    break;
                }
                default:
                    parent.message(
                            "unsupported operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName(),
                            MessageType.fatalError);
                    return;
                }
                resultPV.put(result);
            }
        }

        static class NotEqual extends Relational {

            NotEqual(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                boolean result = false;
                switch(type) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    result = (arg0!=arg1) ? true : false;
                    break;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    result = (arg0!=arg1) ? true : false;
                    break;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    result = (arg0!=arg1) ? true : false;
                    break;
                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    result = (arg0!=arg1) ? true : false;
                    break;
                }
                case pvFloat: {
                    float arg0 = convert.toFloat(arg0PV);
                    float arg1 = convert.toFloat(arg1PV);
                    result = (arg0!=arg1) ? true : false;
                    break;
                }
                case pvDouble: {
                    double arg0 = convert.toDouble(arg0PV);
                    double arg1 = convert.toDouble(arg1PV);
                    result = (arg0!=arg1) ? true : false;
                    break;
                }
                default:
                    parent.message(
                            "unsupported operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName(),
                            MessageType.fatalError);
                    return;
                }
                resultPV.put(result);
            }
        }
        
        abstract static class BitwiseBase implements Operator {
            protected PVStructure parent;
            protected OperationSemantics operationSemantics;
            protected OperatorExpression operatorExpression;
            
            protected PVField arg0PV;
            protected PVField arg1PV;
            protected PVField resultPV;
            protected Type resultType;

            BitwiseBase(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            public boolean createPVResult(String fieldName) {
                arg0PV = operatorExpression.expressionArguments[0].pvResult;
                Field arg0Field = arg0PV.getField();
                Type arg0Type = arg0Field.getType();
                arg1PV = operatorExpression.expressionArguments[1].pvResult;
                Field arg1Field = arg1PV.getField();
                Type arg1Type = arg1Field.getType();
                if(!arg0Type.isInteger() || !arg1Type.isInteger()) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName()
                            + " not compatible with " +arg1PV.getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                resultType = arg0Type;
                if(arg1Type.ordinal()>arg0Type.ordinal()) {
                    resultType = arg1Type;
                }
                Field resultField = fieldCreate.createField(fieldName, resultType);
                resultPV = pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            
            abstract public void compute();
        }
        
        static class BitwiseAnd extends BitwiseBase {

            BitwiseAnd(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    byte result = (byte)(arg0&arg1);
                    convert.fromByte(resultPV, result);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    short result = (short)(arg0&arg1);
                    convert.fromShort(resultPV, result);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    int result = (int)(arg0&arg1);
                    convert.fromInt(resultPV, result);
                    return;

                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    long result = (long)(arg0&arg1);
                    convert.fromLong(resultPV, result);
                    return;

                }
                }
            }
        }
        
        static class BitwiseXOR extends BitwiseBase {

            BitwiseXOR(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    byte result = (byte)(arg0^arg1);
                    convert.fromByte(resultPV, result);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    short result = (short)(arg0^arg1);
                    convert.fromShort(resultPV, result);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    int result = (int)(arg0^arg1);
                    convert.fromInt(resultPV, result);
                    return;

                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    long result = (long)(arg0^arg1);
                    convert.fromLong(resultPV, result);
                    return;

                }
                }
            }
        }
        
        static class BitwiseOr extends BitwiseBase {

            BitwiseOr(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                switch(resultType) {
                case pvByte: {
                    byte arg0 = convert.toByte(arg0PV);
                    byte arg1 = convert.toByte(arg1PV);
                    byte result = (byte)(arg0|arg1);
                    convert.fromByte(resultPV, result);
                    return;
                }
                case pvShort: {
                    short arg0 = convert.toShort(arg0PV);
                    short arg1 = convert.toShort(arg1PV);
                    short result = (short)(arg0|arg1);
                    convert.fromShort(resultPV, result);
                    return;
                }
                case pvInt: {
                    int arg0 = convert.toInt(arg0PV);
                    int arg1 = convert.toInt(arg1PV);
                    int result = (int)(arg0|arg1);
                    convert.fromInt(resultPV, result);
                    return;

                }
                case pvLong: {
                    long arg0 = convert.toLong(arg0PV);
                    long arg1 = convert.toLong(arg1PV);
                    long result = (long)(arg0|arg1);
                    convert.fromLong(resultPV, result);
                    return;

                }
                }
            }
        }
        
        abstract static class BooleanBase implements Operator {
            protected PVStructure parent;
            protected OperationSemantics operationSemantics;
            protected OperatorExpression operatorExpression;
            
            protected PVBoolean arg0PV;
            protected PVBoolean arg1PV;
            protected PVBoolean resultPV;

            BooleanBase(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            public boolean createPVResult(String fieldName) {
                Expression expressionArgument = operatorExpression.expressionArguments[0];
                PVField pvField = expressionArgument.pvResult;
                Type type = pvField.getField().getType();
                if(type!=Type.pvBoolean) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + pvField.getFullFieldName()
                            + " is not boolean",
                            MessageType.fatalError);
                    return false;
                }
                arg0PV = (PVBoolean)pvField;
                expressionArgument = operatorExpression.expressionArguments[1];
                pvField = expressionArgument.pvResult;
                type = pvField.getField().getType();
                if(type!=Type.pvBoolean) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + pvField.getFullFieldName()
                            + " is not boolean",
                            MessageType.fatalError);
                    return false;
                }
                arg1PV = (PVBoolean)pvField;
                Field resultField = fieldCreate.createField(fieldName, Type.pvBoolean);
                resultPV = (PVBoolean)pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = resultPV;
                return true;
            }
            
            protected Operator getArgOperator(int argIndex) {
                Expression expressionArgument = operatorExpression.expressionArguments[argIndex];
                return expressionArgument.operator;
            }
            
            abstract public void compute();
        }
        
        static class BooleanAnd extends BooleanBase {
            BooleanAnd(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                resultPV.put(arg0PV.get()&arg1PV.get());
            }
        }
        static class BooleanXOR extends BooleanBase {
            BooleanXOR(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                resultPV.put(arg0PV.get()^arg1PV.get());
            }
        }
        static class BooleanOr extends BooleanBase {
            BooleanOr(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                operatorExpression.computeArguments();
                resultPV.put(arg0PV.get()|arg1PV.get());
            }
        }
        static class ConditionalAnd extends BooleanBase {
            ConditionalAnd(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                Operator operator = super.getArgOperator(0);
                if(operator!=null) operator.compute();
                boolean value = arg0PV.get();
                if(value) {
                    operator = super.getArgOperator(1);
                    if(operator!=null)operator.compute();
                    value  = value&&arg1PV.get();
                }
                resultPV.put(value);
            }
        }
        static class ConditionalOr extends BooleanBase {
            ConditionalOr(PVStructure parent,OperatorExpression operatorExpression) {
                super(parent,operatorExpression);
            }
            public void compute() {
                Operator operator = super.getArgOperator(0);
                if(operator!=null) operator.compute();
                boolean value = arg0PV.get();
                if(!value) {
                    operator = super.getArgOperator(1);
                    if(operator!=null)operator.compute();
                    value  = value||arg1PV.get();
                }
                resultPV.put(value);
            }
        }
        
        static class TernaryIf implements Operator {
            private PVStructure parent = null;
            private OperationSemantics operationSemantics;
            private OperatorExpression operatorExpression;
            private PVBoolean ifPV;
            private PVField[] argPVs = new PVField[2];
            private PVField pvResult;
            private Operator ifOperator;
            private Operator[] argOperators = new Operator[2];

            TernaryIf(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            public boolean createPVResult(String fieldName) {
                Expression expressionArgument = operatorExpression.expressionArguments[0];
                ifOperator = expressionArgument.operator;
                PVField argPV = expressionArgument.pvResult;
                Field argField = argPV.getField();
                Type argType = argField.getType();
                if(argType!=Type.pvBoolean) {
                    parent.message(
                            "if clause is not type boolean",
                            MessageType.fatalError);
                    return false;
                }
                ifPV = (PVBoolean)argPV;
                expressionArgument = operatorExpression.expressionArguments[1];
                argPVs[0] = expressionArgument.pvResult;
                argOperators[0] = expressionArgument.operator;
                expressionArgument = operatorExpression.expressionArguments[2];
                argPVs[1] = expressionArgument.pvResult;
                argOperators[1] = expressionArgument.operator;
                if(!convert.isCopyScalarCompatible(argPVs[0].getField(),argPVs[1].getField())) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + argPVs[0].getFullFieldName()
                            + " not compatible with " +argPVs[1].getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                int ind0 = argPVs[0].getField().getType().ordinal();
                int ind1 = argPVs[1].getField().getType().ordinal();
                int ind = ind0;
                if(ind<ind1) ind = ind1;
                Type resultType = Type.values()[ind];
                Field resultField = fieldCreate.createField(fieldName, resultType);
                pvResult = pvDataCreate.createPVField(parent, resultField);
                operatorExpression.pvResult = pvResult;
                return true;

            }

            public void compute() {
                if(ifOperator!=null) ifOperator.compute();
                boolean value = ifPV.get();
                int index = value ? 0 : 1;
                Operator operator = argOperators[index];
                if(operator!=null) operator.compute();
                convert.copyScalar(argPVs[index], pvResult);
            }
        }
        
        private static class MathFactory {
            static Operator create(
                    PVStructure parent,
                    MathFunctionExpression mathFunctionExpression)
            {
                MathFunction function = mathFunctionExpression.functionSemantics.mathFunction;
                switch(function) {
                case abs: return new MathAbs(parent,mathFunctionExpression);
                case acos: return new MathAcos(parent,mathFunctionExpression);
                case asin: return new MathAsin(parent,mathFunctionExpression);
                case atan: return new MathAtan(parent,mathFunctionExpression);
                case atan2: return new MathAtan2(parent,mathFunctionExpression);
                case cbrt: return new MathCbrt(parent,mathFunctionExpression);
                case ceil: return new MathCeil(parent,mathFunctionExpression);
                case cos: return new MathCos(parent,mathFunctionExpression);
                case cosh: return new MathCosh(parent,mathFunctionExpression);
                case exp: return new MathExp(parent,mathFunctionExpression);
                case expm1: return new MathExpm1(parent,mathFunctionExpression);
                case floor: return new MathFloor(parent,mathFunctionExpression);
                case hypot: return new MathHypot(parent,mathFunctionExpression);  
                case IEEEremainder: return new MathIEEEremainder(parent,mathFunctionExpression);
                case log: return new MathLog(parent,mathFunctionExpression);
                case log10: return new MathLog10(parent,mathFunctionExpression);
                case log1p: return new MathLog1p(parent,mathFunctionExpression);
                case max: return new MathMax(parent,mathFunctionExpression);
                case min: return new MathMin(parent,mathFunctionExpression);
                case pow: return new MathPow(parent,mathFunctionExpression);
                case random: return new MathRandom(parent,mathFunctionExpression);
                case rint: return new MathRint(parent,mathFunctionExpression);
                case round: return new MathRound(parent,mathFunctionExpression);
                case signum: return new MathSignum(parent,mathFunctionExpression);
                case sin: return new MathSin(parent,mathFunctionExpression);
                case sinh: return new MathSinh(parent,mathFunctionExpression);
                case sqrt: return new MathSqrt(parent,mathFunctionExpression);
                case tan: return new MathTan(parent,mathFunctionExpression);
                case tanh: return new MathTanh(parent,mathFunctionExpression);
                case toDegrees: return new MathToDegrees(parent,mathFunctionExpression);
                case toRadians: return new MathToRadians(parent,mathFunctionExpression);
                case ulp: return new MathUlp(parent,mathFunctionExpression);
                }
                return null;
            }
        }
        
        abstract static class MathDoubleOneArg implements Operator {
            protected PVField parent = null;
            protected MathFunctionExpression mathFunctionExpression = null;
            protected PVDouble pvArg;
            protected PVDouble pvResult;
            

            MathDoubleOneArg(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                PVField pvField = mathFunctionExpression.expressionArguments[0].pvResult; 
                if(pvField.getField().getType()!=Type.pvDouble) {
                   pvField.message("arg type must be double", MessageType.error);
                   return false;
                }
                pvArg = (PVDouble)pvField;
                Field resultField = fieldCreate.createField(fieldName, Type.pvDouble);
                pvResult = (PVDouble)pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            
            abstract public void compute();
        }
        
        abstract static class MathDoubleTwoArg implements Operator {
            protected PVField parent = null;
            protected MathFunctionExpression mathFunctionExpression = null;
            protected PVDouble pvArg0;
            protected PVDouble pvArg1;
            protected PVDouble pvResult;
            

            MathDoubleTwoArg(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=2) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                PVField pvField = mathFunctionExpression.expressionArguments[0].pvResult;
                if(pvField.getField().getType()!=Type.pvDouble) {
                   pvField.message("arg type must be double", MessageType.error);
                   return false;
                }
                pvArg0 = (PVDouble)pvField;
                pvField = mathFunctionExpression.expressionArguments[1].pvResult;
                if(pvField.getField().getType()!=Type.pvDouble) {
                   pvField.message("arg type must be double", MessageType.error);
                   return false;
                }
                pvArg1 = (PVDouble)pvField;
                Field resultField = fieldCreate.createField(fieldName, Type.pvDouble);
                pvResult = (PVDouble)pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            
            abstract public void compute();
        }
        
        
        
        static class MathAbs implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVField parent;
            private PVField pvArg;
            private PVField pvResult;
            private Type type;
            
            MathAbs(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }

           
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvArg = mathFunctionExpression.expressionArguments[0].pvResult;
                type = pvArg.getField().getType();
                if(type!=Type.pvInt && type!=Type.pvLong && type!=Type.pvFloat && type!=Type.pvDouble) {
                    pvArg.message("illegal arg type", MessageType.error);
                    return false;
                }
                Field resultField = fieldCreate.createField(fieldName, type);
                pvResult = pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }

            public void compute() {
                mathFunctionExpression.computeArguments();
                switch(type) {
                case pvInt: 
                    convert.fromInt(pvResult, Math.abs(convert.toInt(pvArg)));
                    break;
                case pvLong:
                    convert.fromLong(pvResult, Math.abs(convert.toLong(pvArg)));
                    break;
                case pvFloat:
                    convert.fromFloat(pvResult, Math.abs(convert.toFloat(pvArg)));
                    break;
                case pvDouble:
                    convert.fromDouble(pvResult, Math.abs(convert.toDouble(pvArg)));
                    break;
                }
            } 
        }
        
        static class MathAcos extends MathDoubleOneArg {
            MathAcos(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.acos(pvArg.get()));
            }
        }
        static class MathAsin extends MathDoubleOneArg {
            MathAsin(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.asin(pvArg.get()));
            }
        }
        static class MathAtan extends MathDoubleOneArg {
            MathAtan(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.atan(pvArg.get()));
            }
        }
        static class MathAtan2 extends MathDoubleTwoArg {
            MathAtan2(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.atan2(pvArg0.get(),pvArg1.get()));
            }
        }
        static class MathCbrt extends MathDoubleOneArg {
            MathCbrt(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.cbrt(pvArg.get()));
            }
        }
        static class MathCeil extends MathDoubleOneArg {
            MathCeil(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.ceil(pvArg.get()));
            }
        }
        static class MathCos extends MathDoubleOneArg {
            MathCos(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.cos(pvArg.get()));
            }
        }
        static class MathCosh extends MathDoubleOneArg {
            MathCosh(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.cosh(pvArg.get()));
            }
        }
        static class MathExp extends MathDoubleOneArg {
            MathExp(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.exp(pvArg.get()));
            }
        }
        static class MathExpm1 extends MathDoubleOneArg {
            MathExpm1(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.expm1(pvArg.get()));
            }
        }
        static class MathFloor extends MathDoubleOneArg {
            MathFloor(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.floor(pvArg.get()));
            }
        }
        static class MathHypot extends MathDoubleTwoArg {
            MathHypot(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.hypot(pvArg0.get(),pvArg1.get()));
            }
        }
        static class MathIEEEremainder extends MathDoubleTwoArg {
            MathIEEEremainder(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.IEEEremainder(pvArg0.get(),pvArg1.get()));
            }
        }
        static class MathLog extends MathDoubleOneArg {
            MathLog(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.log(pvArg.get()));
            }
        }
        static class MathLog10 extends MathDoubleOneArg {
            MathLog10(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.log10(pvArg.get()));
            }
        }
        static class MathLog1p extends MathDoubleOneArg {
            MathLog1p(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.log1p(pvArg.get()));
            }
        }
        static class MathMax implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVField parent;
            private PVField pv0Arg;
            private PVField pv1Arg;
            private PVField pvResult;
            private Type type;
            
            MathMax(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=2) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pv0Arg = mathFunctionExpression.expressionArguments[0].pvResult;;
                Type type = pv0Arg.getField().getType();
                if(type!=Type.pvInt && type!=Type.pvLong && type!=Type.pvFloat && type!=Type.pvDouble) {
                    pv0Arg.message("illegal arg type", MessageType.error);
                    return false;
                }
                this.type = type;
                pv1Arg = mathFunctionExpression.expressionArguments[1].pvResult;
                if(!pv1Arg.getField().getType().isNumeric()) {
                    pv1Arg.message("illegal arg type", MessageType.error);
                    return false;
                }
                Field resultField = fieldCreate.createField(fieldName, this.type);
                pvResult = pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                switch(type) {
                case pvInt:
                    convert.fromInt(pvResult,Math.max(convert.toInt(pv0Arg), convert.toInt(pv1Arg)));
                    break;
                case pvLong:
                    convert.fromLong(pvResult,Math.max(convert.toLong(pv0Arg), convert.toLong(pv1Arg)));
                    break;
                case pvFloat:
                    convert.fromFloat(pvResult,Math.max(convert.toFloat(pv0Arg), convert.toFloat(pv1Arg)));
                    break;
                case pvDouble:
                    convert.fromDouble(pvResult,Math.max(convert.toDouble(pv0Arg), convert.toDouble(pv1Arg)));
                    break;
                }
            } 
        }
        static class MathMin implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVField parent;
            private PVField pv0Arg;
            private PVField pv1Arg;
            private PVField pvResult;
            private Type type;
            
            MathMin(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=2) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pv0Arg = mathFunctionExpression.expressionArguments[0].pvResult;
                Type type = pv0Arg.getField().getType();
                if(type!=Type.pvInt && type!=Type.pvLong && type!=Type.pvFloat && type!=Type.pvDouble) {
                    pv0Arg.message("illegal arg type", MessageType.error);
                    return false;
                }
                this.type = type;
                pv1Arg = mathFunctionExpression.expressionArguments[1].pvResult;
                type = pv1Arg.getField().getType();
                if(type!=this.type) {
                    pv1Arg.message("arg1 type must be the same as arg0", MessageType.error);
                    return false;
                }
                Field resultField = fieldCreate.createField(fieldName, this.type);
                pvResult = pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                switch(type) {
                case pvInt:
                    convert.fromInt(pvResult,Math.min(convert.toInt(pv0Arg), convert.toInt(pv1Arg)));
                    break;
                case pvLong:
                    convert.fromLong(pvResult,Math.min(convert.toLong(pv0Arg), convert.toLong(pv1Arg)));
                    break;
                case pvFloat:
                    convert.fromFloat(pvResult,Math.min(convert.toFloat(pv0Arg), convert.toFloat(pv1Arg)));
                    break;
                case pvDouble:
                    convert.fromDouble(pvResult,Math.min(convert.toDouble(pv0Arg), convert.toDouble(pv1Arg)));
                    break;
                }
            } 
        }
        static class MathPow extends MathDoubleTwoArg {
            MathPow(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.pow(pvArg0.get(),pvArg1.get()));
            }
        }
        static class MathRandom implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVField parent;
            private PVDouble pvResult;
            
            MathRandom(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=0) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                Field resultField = fieldCreate.createField(fieldName, Type.pvDouble);
                pvResult = (PVDouble)pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                pvResult.put(Math.random());
            } 
        }
        static class MathRint extends MathDoubleOneArg {
            MathRint(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.rint(pvArg.get()));
            }
        }
        static class MathRound implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVField parent;
            private PVField pvArg;
            private PVField pvResult;
            private Type argType;
            
            MathRound(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvArg = mathFunctionExpression.expressionArguments[0].pvResult;
                argType = pvArg.getField().getType();
                if(argType!=Type.pvFloat && argType!=Type.pvDouble) {
                    pvArg.message("illegal arg type", MessageType.error);
                    return false;
                }
                Type resultType = (argType==Type.pvFloat) ? Type.pvInt : Type.pvLong;
                Field resultField = fieldCreate.createField(fieldName, resultType);
                pvResult = pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                if(argType==Type.pvFloat) {
                    PVFloat from = (PVFloat)pvArg;
                    PVInt to = (PVInt)pvResult;
                    to.put(Math.round(from.get()));
                } else {
                    PVDouble from = (PVDouble)pvArg;
                    PVLong to = (PVLong)pvResult;
                    to.put(Math.round(from.get()));
                }
            } 
        }
        static class MathSignum implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVField parent;
            private PVField pvArg;
            private PVField pvResult;
            private Type type;
            
            MathSignum(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }    
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvArg = mathFunctionExpression.expressionArguments[0].pvResult;
                type = pvArg.getField().getType();
                if(type!=Type.pvFloat && type!=Type.pvDouble) {
                    pvArg.message("illegal arg type", MessageType.error);
                    return false;
                }
                Field resultField = fieldCreate.createField(fieldName, type);
                pvResult = pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                if(type==Type.pvFloat) {
                    PVFloat from = (PVFloat)pvArg;
                    PVFloat to = (PVFloat)pvResult;
                    to.put(Math.signum(from.get()));
                } else {
                    PVDouble from = (PVDouble)pvArg;
                    PVDouble to = (PVDouble)pvResult;
                    to.put(Math.signum(from.get()));
                }
            } 
        }
        static class MathSin extends MathDoubleOneArg {
            MathSin(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.sin(pvArg.get()));
            }
        }
        static class MathSinh extends MathDoubleOneArg {
            MathSinh(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.sinh(pvArg.get()));
            }
        }
        static class MathSqrt extends MathDoubleOneArg {
            MathSqrt(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.sqrt(pvArg.get()));
            }
        }
        static class MathTan extends MathDoubleOneArg {
            MathTan(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.tan(pvArg.get()));
            }
        }
        static class MathTanh extends MathDoubleOneArg {
            MathTanh(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.tanh(pvArg.get()));
            }
        }
        static class MathToDegrees extends MathDoubleOneArg {
            MathToDegrees(PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.toDegrees(pvArg.get()));
            }
        }
        static class MathToRadians extends MathDoubleOneArg {
            MathToRadians (PVField parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.toRadians (pvArg.get()));
            }
        }
        static class MathUlp implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVField parent;
            private PVField pvArg;
            private PVField pvResult;
            private Type type;
            
            MathUlp(PVField parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvArg = mathFunctionExpression.expressionArguments[0].pvResult;
                type = pvArg.getField().getType();
                if(type!=Type.pvFloat && type!=Type.pvDouble) {
                    pvArg.message("illegal arg type", MessageType.error);
                    return false;
                }
                Field resultField = fieldCreate.createField(fieldName, type);
                pvResult = pvDataCreate.createPVField(parent, resultField);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                if(type==Type.pvFloat) {
                    PVFloat from = (PVFloat)pvArg;
                    PVFloat to = (PVFloat)pvResult;
                    to.put(Math.ulp(from.get()));
                } else {
                    PVDouble from = (PVDouble)pvArg;
                    PVDouble to = (PVDouble)pvResult;
                    to.put(Math.ulp(from.get()));
                }
            } 
        }
    }
}

