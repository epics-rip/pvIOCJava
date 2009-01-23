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

import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVByte;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVFloat;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVLong;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVShort;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;




/**
 * Factory that provides support for expressions.
 * @author mrk
 *
 */
public abstract class ExpressionCalculatorFactory  {

    public static Support create(PVStructure pvStructure) {
        return new ExpressionCalculator(pvStructure);
    }
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static Convert convert = ConvertFactory.getConvert();
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    private static boolean dumpTokenList = false;
    private static boolean dumpExpression = false;
    
    private static class ExpressionCalculator extends AbstractSupport {
        
        private ExpressionCalculator(PVStructure pvStructure) {
            super("expressionCalculator",pvStructure);
            this.pvStructure = pvStructure;
        }
        
        
        private PVStructure pvStructure = null;
        private AlarmSupport alarmSupport = null;
        private PVScalar pvValue = null;
        private CalcArgs calcArgsSupport = null;
        
        private Expression expression = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            alarmSupport = AlarmSupportFactory.findAlarmSupport(pvStructure,recordSupport);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            PVField pvParent = pvStructure.getParent();
            PVField pvValue = pvProperty.findProperty(pvParent,"value");
            if(pvValue==null) { // try parent of parent. 
                pvValue = pvParent.getParent();
                if(pvValue!=null) pvValue = pvProperty.findProperty(pvValue,"value");
            }
            if(pvValue==null) {
                pvStructure.message("value field not found", MessageType.error);
                return;
            }
            if(pvValue.getField().getType()!=Type.scalar) {
                pvValue.message("ExpressionCalculator requires this field to be a scalar", MessageType.error);
                return;
            }
            this.pvValue = (PVScalar)pvValue;
            PVField pvField = pvProperty.findProperty(pvParent,"calcArgs");
            if(pvField==null) {
                pvStructure.message("calcArgs field not found", MessageType.error);
                return;
            }
            Support support = recordSupport.getSupport(pvField);
            if(!(support instanceof CalcArgs)) {
                pvStructure.message("calcArgsSupport not found", MessageType.error);
                return;
            }
            calcArgsSupport = (CalcArgs)support;
            PVString pvExpression = pvStructure.getStringField("expression");
            if(pvExpression==null) return;
            Parse parse = new Parse(pvExpression);
            expression = parse.parse();
            if(expression==null) return;
            super.initialize(recordSupport);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            expression = null;
            alarmSupport = null;
            pvValue = null;
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
                PVScalar pvResult = expression.pvResult;
                if(pvResult!=pvValue) convert.copyScalar(pvResult, pvValue);
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
            PVScalar pvResult = null;
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
                        pvField = pvValue;
                    } else {
                        pvField = calcArgsSupport.getPVField(name);
                    }
                    if(pvField==null) {
                        pvStructure.message("variable " + name + " not found", MessageType.error);
                        return false;
                    }
                    if(pvField.getField().getType()!=Type.scalar) {
                        pvStructure.message("ExpressionCalculator requires this to be a scalar", MessageType.error);
                        return false;
                    }
                    exp.pvResult = (PVScalar)pvField;
                    expStack.push(exp);
                    return true;
                }
                case booleanConstant: {
                    Boolean scalar = Boolean.valueOf(token.value);
                    PVBoolean pv = (PVBoolean)pvDataCreate.createPVScalar(pvStructure,"result", ScalarType.pvBoolean);
                    pv.put(scalar);
                    pv.setMutable(false);
                    exp.pvResult = pv;
                    expStack.push(exp);
                    return true;
                }
                case integerConstant: {
                    PVScalar pvScalar = null;
                    String value = token.value;
                    int length = value.length();
                    char lastChar = value.charAt(length-1);
                    ScalarType scalarType = (lastChar=='L') ? ScalarType.pvLong : ScalarType.pvInt;
                    pvScalar = pvDataCreate.createPVScalar(pvStructure,"result", scalarType);
                    if(scalarType==ScalarType.pvLong) {
                        Long scalar = new Long(0);
                        try {
                            scalar = Long.decode(value.substring(0, length-1));
                        } catch (NumberFormatException e) {
                            pvStructure.message(e.getMessage(), MessageType.error);
                            return false;
                        }
                        PVLong pv = (PVLong)pvScalar;
                        pv.put(scalar);
                    } else {
                        Long scalar = new Long(0);
                        try {
                            scalar = Long.decode(value);
                        } catch (NumberFormatException e) {
                            pvStructure.message(e.getMessage(), MessageType.error);
                            return false;
                        }
                        PVInt pv = (PVInt)pvScalar;
                        pv.put((int)(long)scalar);
                    }
                    pvScalar.setMutable(false);
                    exp.pvResult = pvScalar;
                    expStack.push(exp);
                    return true;
                }
                case realConstant: {
                    PVScalar pvScalar = null;
                    String value = token.value;
                    int length = value.length();
                    char lastChar = value.charAt(length-1);
                    ScalarType scalarType = (lastChar=='F') ? ScalarType.pvFloat : ScalarType.pvDouble;
                    pvScalar = pvDataCreate.createPVScalar(pvStructure,"result", scalarType);
                    if(scalarType==ScalarType.pvFloat) {
                        Float scalar = Float.valueOf(value);
                        PVFloat pv = (PVFloat)pvScalar;
                        pv.put(scalar);
                    } else {
                        Double scalar = Double.valueOf(value);
                        PVDouble pv = (PVDouble)pvScalar;
                        pv.put(scalar);
                    }
                    pvScalar.setMutable(false);
                    exp.pvResult = pvScalar;
                    expStack.push(exp);
                    return true;
                }
                case stringConstant: {
                    String scalar = token.value;
                    PVString pv = (PVString)pvDataCreate.createPVScalar(pvStructure,"result", ScalarType.pvString);
                    pv.put(scalar);
                    pv.setMutable(false);
                    exp.pvResult = pv;
                    expStack.push(exp);
                    return true;
                }
                case mathConstant: {
                    String functionName = exp.token.value;
                    PVDouble pv = (PVDouble)pvDataCreate.createPVScalar(pvStructure,"result", ScalarType.pvDouble);
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
                    PVScalar argResult = exp.expressionArguments[0].pvResult;
                    ScalarType argType = argResult.getScalar().getScalarType();
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
                            if(argType==ScalarType.pvBoolean) {
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
                            if(argType==ScalarType.pvString) {
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
                    PVScalar argResult = exp.expressionArguments[0].pvResult;
                    ScalarType argType = argResult.getScalar().getScalarType();
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
                            if(argType==ScalarType.pvBoolean) {
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
                            if(argType==ScalarType.pvString) {
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
                    ScalarType arg0Type = exp.expressionArguments[0].pvResult.getScalar().getScalarType();
                    ScalarType arg1Type = exp.expressionArguments[1].pvResult.getScalar().getScalarType();
                    ScalarType arg2Type = exp.expressionArguments[2].pvResult.getScalar().getScalarType();
                    if(arg0Type!=ScalarType.pvBoolean) {
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
                PVScalar pvResult = expression.pvResult;
                if(pvResult==null) {
                    System.out.println(prefix + "pvResult is null");
                    return;
                }
                Scalar scalar = pvResult.getScalar();
                System.out.println(prefix + scalar.getFieldName() + " " + scalar.getScalarType().name());
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
            private PVScalar argPV;
            
            private PVScalar resultPV;
            private ScalarType resultType;
            
            UnaryMinus(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                argPV = operatorExpression.expressionArguments[0].pvResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#createPVResult(java.lang.String)
             */
            public boolean createPVResult(String fieldName) {
                Scalar argScalar = argPV.getScalar();
                ScalarType argType = argScalar.getScalarType();
                if(!argType.isNumeric()) {
                    parent.message(
                            "For operator + "  + argPV.getFullFieldName() + " is not numeric",
                            MessageType.fatalError);
                    return false;
                }
                resultType = argScalar.getScalarType();
                resultPV = pvDataCreate.createPVScalar(parent, fieldName,resultType);
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
            private PVScalar argPV;
            
            private PVScalar resultPV;
            private ScalarType resultType;
            
            BitwiseComplement(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                argPV = operatorExpression.expressionArguments[0].pvResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.calc.ExpressionCalculatorFactory.ExpressionCalculator.Operator#createPVResult(java.lang.String)
             */
            public boolean createPVResult(String fieldName) {
                Scalar argScalar = argPV.getScalar();
                ScalarType argType = argScalar.getScalarType();
               
                if(!argType.isInteger()) {
                    parent.message(
                            "For operator ~ "  + argPV.getFullFieldName() + " is not integer",
                            MessageType.fatalError);
                    return false;
                }
                resultType = argScalar.getScalarType();
                resultPV = pvDataCreate.createPVScalar(parent,fieldName,resultType);
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
            private PVScalar pvField = null;
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
                if(pvField.getScalar().getScalarType()!=ScalarType.pvBoolean) {
                    parent.message(
                            "For operator ! " + argPV.getFullFieldName() + " is not boolean",
                            MessageType.fatalError);
                    return false;
                }
                argPV = (PVBoolean)pvField;
                resultPV = (PVBoolean)pvDataCreate.createPVScalar(parent,fieldName,ScalarType.pvBoolean);
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

            protected PVScalar arg0PV;
            protected Scalar arg0Field;
            protected ScalarType arg0Type;
            protected PVScalar arg1PV;
            protected Scalar arg1Field;
            protected ScalarType arg1Type;
            protected PVScalar resultPV;
            protected Scalar resultField;
            protected ScalarType resultType;


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
                arg0Field = arg0PV.getScalar();
                arg0Type = arg0Field.getScalarType();
                arg1PV = operatorExpression.expressionArguments[1].pvResult;
                arg1Field = arg1PV.getScalar();
                arg1Type = arg1Field.getScalarType();
                if(!convert.isCopyScalarCompatible(arg0Field,arg1Field)) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName()
                            + " not compatible with " +arg1PV.getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                int ind0 = arg0Type.ordinal();
                int ind1 = arg1Type.ordinal();
                int ind = ind0;
                if(ind<ind1) ind = ind1;
                resultType = ScalarType.values()[ind];
                resultPV = pvDataCreate.createPVScalar(parent,fieldName,resultType);
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

            private PVScalar arg0PV;
            private PVScalar arg1PV;
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
                resultPV = (PVString)pvDataCreate.createPVScalar(parent,fieldName, ScalarType.pvString);
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
            
            protected PVScalar arg0PV;
            protected PVScalar arg1PV;
            protected PVScalar resultPV;
            protected ScalarType resultType;

            ShiftBase(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            public boolean createPVResult(String fieldName) {
                arg0PV = operatorExpression.expressionArguments[0].pvResult;
                Scalar arg0Field = arg0PV.getScalar();
                ScalarType arg0Type = arg0Field.getScalarType();
                arg1PV = operatorExpression.expressionArguments[1].pvResult;
                Scalar arg1Field = arg1PV.getScalar();
                ScalarType arg1Type = arg1Field.getScalarType();
                if(!arg0Type.isInteger() || !arg1Type.isInteger()) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName()
                            + " not compatible with " +arg1PV.getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                resultType = arg0Type;
                resultPV = pvDataCreate.createPVScalar(parent,fieldName, arg0Type);
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

            protected PVScalar arg0PV;
            protected ScalarType scalarType;
            protected PVScalar arg1PV;
            protected PVBoolean resultPV;

            Relational(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            public boolean createPVResult(String fieldName) {
                Expression expressionArgument = operatorExpression.expressionArguments[0];
                arg0PV = expressionArgument.pvResult;
                ScalarType arg0Type = arg0PV.getScalar().getScalarType();
                expressionArgument = operatorExpression.expressionArguments[1];
                arg1PV = expressionArgument.pvResult;
                ScalarType arg1Type = arg1PV.getScalar().getScalarType();
                if(!convert.isCopyScalarCompatible(arg0PV.getScalar(),arg1PV.getScalar())) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + arg0PV.getFullFieldName()
                            + " not compatible with " +arg1PV.getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                int ind0 = arg0Type.ordinal();
                int ind1 = arg1Type.ordinal();
                scalarType = ScalarType.values()[Math.max(ind0,ind1)];
                resultPV = (PVBoolean)pvDataCreate.createPVScalar(parent,fieldName,ScalarType.pvBoolean);
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
                switch(scalarType) {
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
                switch(scalarType) {
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
                switch(scalarType) {
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
                switch(scalarType) {
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
                switch(scalarType) {
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
                switch(scalarType) {
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
            
            protected PVScalar arg0PV;
            protected PVScalar arg1PV;
            protected PVScalar resultPV;
            protected ScalarType resultType;

            BitwiseBase(PVStructure parent,OperatorExpression operatorExpression) {
                this.parent = parent;
                this.operatorExpression = operatorExpression;
                operationSemantics = operatorExpression.operationSemantics;
            }
            public boolean createPVResult(String fieldName) {
                arg0PV = operatorExpression.expressionArguments[0].pvResult;
                Scalar arg0Field = arg0PV.getScalar();
                ScalarType arg0Type = arg0Field.getScalarType();
                arg1PV = operatorExpression.expressionArguments[1].pvResult;
                Scalar arg1Field = arg1PV.getScalar();
                ScalarType arg1Type = arg1Field.getScalarType();
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
                resultPV = pvDataCreate.createPVScalar(parent,fieldName, resultType);
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
                PVScalar pvScalar = expressionArgument.pvResult;
                ScalarType scalarType = pvScalar.getScalar().getScalarType();
                if(scalarType!=ScalarType.pvBoolean) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + pvScalar.getFullFieldName()
                            + " is not boolean",
                            MessageType.fatalError);
                    return false;
                }
                arg0PV = (PVBoolean)pvScalar;
                expressionArgument = operatorExpression.expressionArguments[1];
                pvScalar = expressionArgument.pvResult;
                scalarType = pvScalar.getScalar().getScalarType();
                if(scalarType!=ScalarType.pvBoolean) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + pvScalar.getFullFieldName()
                            + " is not boolean",
                            MessageType.fatalError);
                    return false;
                }
                arg1PV = (PVBoolean)pvScalar;
                resultPV = (PVBoolean)pvDataCreate.createPVScalar(parent,fieldName, ScalarType.pvBoolean);
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
            private PVScalar[] argPVs = new PVScalar[2];
            private PVScalar pvResult;
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
                PVScalar argPV = expressionArgument.pvResult;
                Scalar argField = argPV.getScalar();
                ScalarType argType = argField.getScalarType();
                if(argType!=ScalarType.pvBoolean) {
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
                if(!convert.isCopyScalarCompatible(argPVs[0].getScalar(),argPVs[1].getScalar())) {
                    parent.message(
                            "For operator " + operationSemantics.operation.name()
                            + argPVs[0].getFullFieldName()
                            + " not compatible with " +argPVs[1].getFullFieldName(),
                            MessageType.fatalError);
                    return false;
                }
                int ind0 = argPVs[0].getScalar().getScalarType().ordinal();
                int ind1 = argPVs[1].getScalar().getScalarType().ordinal();
                int ind = ind0;
                if(ind<ind1) ind = ind1;
                ScalarType resultType = ScalarType.values()[ind];
                pvResult = pvDataCreate.createPVScalar(parent,fieldName, resultType);
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
            protected PVStructure parent = null;
            protected MathFunctionExpression mathFunctionExpression = null;
            protected PVDouble pvArg;
            protected PVDouble pvResult;
            

            MathDoubleOneArg(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                PVScalar pvField = mathFunctionExpression.expressionArguments[0].pvResult; 
                if(pvField.getScalar().getScalarType()!=ScalarType.pvDouble) {
                   pvField.message("arg type must be double", MessageType.error);
                   return false;
                }
                pvArg = (PVDouble)pvField;
                pvResult = (PVDouble)pvDataCreate.createPVScalar(parent,fieldName, ScalarType.pvDouble);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            
            abstract public void compute();
        }
        
        abstract static class MathDoubleTwoArg implements Operator {
            protected PVStructure parent = null;
            protected MathFunctionExpression mathFunctionExpression = null;
            protected PVDouble pvArg0;
            protected PVDouble pvArg1;
            protected PVDouble pvResult;
            

            MathDoubleTwoArg(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=2) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                PVScalar pvScalar = mathFunctionExpression.expressionArguments[0].pvResult;
                if(pvScalar.getScalar().getScalarType()!=ScalarType.pvDouble) {
                   pvScalar.message("arg type must be double", MessageType.error);
                   return false;
                }
                pvArg0 = (PVDouble)pvScalar;
                pvScalar = mathFunctionExpression.expressionArguments[1].pvResult;
                if(pvScalar.getScalar().getScalarType()!=ScalarType.pvDouble) {
                   pvScalar.message("arg type must be double", MessageType.error);
                   return false;
                }
                pvArg1 = (PVDouble)pvScalar;
                pvResult = (PVDouble)pvDataCreate.createPVScalar(parent,fieldName,ScalarType.pvDouble);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            
            abstract public void compute();
        }
        
        
        
        static class MathAbs implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVStructure parent;
            private PVScalar pvArg;
            private PVScalar pvResult;
            private ScalarType scalarType;
            
            MathAbs(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }

           
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvArg = mathFunctionExpression.expressionArguments[0].pvResult;
                scalarType = pvArg.getScalar().getScalarType();
                if(scalarType!=ScalarType.pvInt && scalarType!=ScalarType.pvLong && scalarType!=ScalarType.pvFloat && scalarType!=ScalarType.pvDouble) {
                    pvArg.message("illegal arg type", MessageType.error);
                    return false;
                }
                pvResult = pvDataCreate.createPVScalar(parent,fieldName, scalarType);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }

            public void compute() {
                mathFunctionExpression.computeArguments();
                switch(scalarType) {
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
            MathAcos(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.acos(pvArg.get()));
            }
        }
        static class MathAsin extends MathDoubleOneArg {
            MathAsin(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.asin(pvArg.get()));
            }
        }
        static class MathAtan extends MathDoubleOneArg {
            MathAtan(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.atan(pvArg.get()));
            }
        }
        static class MathAtan2 extends MathDoubleTwoArg {
            MathAtan2(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.atan2(pvArg0.get(),pvArg1.get()));
            }
        }
        static class MathCbrt extends MathDoubleOneArg {
            MathCbrt(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.cbrt(pvArg.get()));
            }
        }
        static class MathCeil extends MathDoubleOneArg {
            MathCeil(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.ceil(pvArg.get()));
            }
        }
        static class MathCos extends MathDoubleOneArg {
            MathCos(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.cos(pvArg.get()));
            }
        }
        static class MathCosh extends MathDoubleOneArg {
            MathCosh(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.cosh(pvArg.get()));
            }
        }
        static class MathExp extends MathDoubleOneArg {
            MathExp(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.exp(pvArg.get()));
            }
        }
        static class MathExpm1 extends MathDoubleOneArg {
            MathExpm1(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.expm1(pvArg.get()));
            }
        }
        static class MathFloor extends MathDoubleOneArg {
            MathFloor(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.floor(pvArg.get()));
            }
        }
        static class MathHypot extends MathDoubleTwoArg {
            MathHypot(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.hypot(pvArg0.get(),pvArg1.get()));
            }
        }
        static class MathIEEEremainder extends MathDoubleTwoArg {
            MathIEEEremainder(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.IEEEremainder(pvArg0.get(),pvArg1.get()));
            }
        }
        static class MathLog extends MathDoubleOneArg {
            MathLog(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.log(pvArg.get()));
            }
        }
        static class MathLog10 extends MathDoubleOneArg {
            MathLog10(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.log10(pvArg.get()));
            }
        }
        static class MathLog1p extends MathDoubleOneArg {
            MathLog1p(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.log1p(pvArg.get()));
            }
        }
        static class MathMax implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVStructure parent;
            private PVScalar pv0Arg;
            private PVScalar pv1Arg;
            private PVScalar pvResult;
            private ScalarType scalarType;
            
            MathMax(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=2) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pv0Arg = mathFunctionExpression.expressionArguments[0].pvResult;;
                ScalarType scalarType = pv0Arg.getScalar().getScalarType();
                if(scalarType!=ScalarType.pvInt && scalarType!=ScalarType.pvLong && scalarType!=ScalarType.pvFloat && scalarType!=ScalarType.pvDouble) {
                    pv0Arg.message("illegal arg type", MessageType.error);
                    return false;
                }
                this.scalarType = scalarType;
                pv1Arg = mathFunctionExpression.expressionArguments[1].pvResult;
                if(!pv1Arg.getScalar().getScalarType().isNumeric()) {
                    pv1Arg.message("illegal arg type", MessageType.error);
                    return false;
                }
                pvResult = pvDataCreate.createPVScalar(parent,fieldName, this.scalarType);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                switch(scalarType) {
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
            private PVStructure parent;
            private PVScalar pv0Arg;
            private PVScalar pv1Arg;
            private PVScalar pvResult;
            private ScalarType scalarType;
            
            MathMin(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=2) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pv0Arg = mathFunctionExpression.expressionArguments[0].pvResult;
                ScalarType scalarType = pv0Arg.getScalar().getScalarType();
                if(scalarType!=ScalarType.pvInt && scalarType!=ScalarType.pvLong && scalarType!=ScalarType.pvFloat && scalarType!=ScalarType.pvDouble) {
                    pv0Arg.message("illegal arg type", MessageType.error);
                    return false;
                }
                this.scalarType = scalarType;
                pv1Arg = mathFunctionExpression.expressionArguments[1].pvResult;
                scalarType = pv1Arg.getScalar().getScalarType();
                if(scalarType!=this.scalarType) {
                    pv1Arg.message("arg1 type must be the same as arg0", MessageType.error);
                    return false;
                }
                pvResult = pvDataCreate.createPVScalar(parent,fieldName, this.scalarType);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                switch(scalarType) {
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
            MathPow(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.pow(pvArg0.get(),pvArg1.get()));
            }
        }
        static class MathRandom implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVStructure parent;
            private PVDouble pvResult;
            
            MathRandom(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=0) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvResult = (PVDouble)pvDataCreate.createPVScalar(parent,fieldName, ScalarType.pvDouble);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                pvResult.put(Math.random());
            } 
        }
        static class MathRint extends MathDoubleOneArg {
            MathRint(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.rint(pvArg.get()));
            }
        }
        static class MathRound implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVStructure parent;
            private PVScalar pvArg;
            private PVScalar pvResult;
            private ScalarType argType;
            
            MathRound(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvArg = mathFunctionExpression.expressionArguments[0].pvResult;
                argType = pvArg.getScalar().getScalarType();
                if(argType!=ScalarType.pvFloat && argType!=ScalarType.pvDouble) {
                    pvArg.message("illegal arg type", MessageType.error);
                    return false;
                }
                ScalarType resultType = (argType==ScalarType.pvFloat) ? ScalarType.pvInt : ScalarType.pvLong;
                pvResult = pvDataCreate.createPVScalar(parent,fieldName, resultType);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                if(argType==ScalarType.pvFloat) {
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
            private PVStructure parent;
            private PVScalar pvArg;
            private PVScalar pvResult;
            private ScalarType scalarType;
            
            MathSignum(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }    
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvArg = mathFunctionExpression.expressionArguments[0].pvResult;
                scalarType = pvArg.getScalar().getScalarType();
                if(scalarType!=ScalarType.pvFloat && scalarType!=ScalarType.pvDouble) {
                    pvArg.message("illegal arg type", MessageType.error);
                    return false;
                }
                pvResult = pvDataCreate.createPVScalar(parent,fieldName, scalarType);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                if(scalarType==ScalarType.pvFloat) {
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
            MathSin(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.sin(pvArg.get()));
            }
        }
        static class MathSinh extends MathDoubleOneArg {
            MathSinh(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.sinh(pvArg.get()));
            }
        }
        static class MathSqrt extends MathDoubleOneArg {
            MathSqrt(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.sqrt(pvArg.get()));
            }
        }
        static class MathTan extends MathDoubleOneArg {
            MathTan(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.tan(pvArg.get()));
            }
        }
        static class MathTanh extends MathDoubleOneArg {
            MathTanh(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.tanh(pvArg.get()));
            }
        }
        static class MathToDegrees extends MathDoubleOneArg {
            MathToDegrees(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.toDegrees(pvArg.get()));
            }
        }
        static class MathToRadians extends MathDoubleOneArg {
            MathToRadians (PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                super(parent,mathFunctionExpression);
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                pvResult.put(Math.toRadians (pvArg.get()));
            }
        }
        static class MathUlp implements Operator {
            private MathFunctionExpression mathFunctionExpression;
            private PVStructure parent;
            private PVScalar pvArg;
            private PVScalar pvResult;
            private ScalarType scalarType;
            
            MathUlp(PVStructure parent,MathFunctionExpression mathFunctionExpression) {
                this.parent = parent;
                this.mathFunctionExpression = mathFunctionExpression;
            }
            public boolean createPVResult(String fieldName) {
                if(mathFunctionExpression.expressionArguments.length!=1) {
                    parent.message("illegal number of args", MessageType.error);
                    return false;
                }
                pvArg = mathFunctionExpression.expressionArguments[0].pvResult;
                scalarType = pvArg.getScalar().getScalarType();
                if(scalarType!=ScalarType.pvFloat && scalarType!=ScalarType.pvDouble) {
                    pvArg.message("illegal arg type", MessageType.error);
                    return false;
                }
                pvResult = pvDataCreate.createPVScalar(parent,fieldName, scalarType);
                mathFunctionExpression.pvResult = pvResult;
                return true;
            }
            public void compute() {
                mathFunctionExpression.computeArguments();
                if(scalarType==ScalarType.pvFloat) {
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

