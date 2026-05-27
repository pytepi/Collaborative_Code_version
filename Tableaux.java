import java.util.HashMap;
import java.util.LinkedList;

public class Tableaux {

    Expression rootFormula;
    Branch start;
    LinkedList<Branch> branches;

    LinkedList<Branch> unfinishedBranches;

    public Tableaux() {
        branches = new LinkedList<>();
        unfinishedBranches = new LinkedList<>();
    }

    public void doTableaux(Expression startExpression) {
        rootFormula = startExpression;
        start = new Branch(new TableauxPart(startExpression, null, null, null), null);
        unfinishedBranches.add(start);

        while (!unfinishedBranches.isEmpty()) {
            Branch current = unfinishedBranches.getFirst();

            LinkedList<Branch> result = current.nextStep();
            if (result == null) {
                branches.add(current);
                unfinishedBranches.remove(current);
            } else {
                for (int i = 0; i < result.size(); i++) {
                    if (!unfinishedBranches.contains(result.get(i))) {
                        unfinishedBranches.add(result.get(i));
                    }
                }
            }
        }
    }

    

    public String toString() {
        String s = "\n";
        for (int i = 0; i < branches.size(); i++) {
            s += "Branch nr: " + i;
            s += branches.get(i).toString();
            s += "\n";
        }
        return s;
    }

}

class Branch {

    LinkedList<TableauxPart> tableauxParts;
    LinkedList<TableauxPart> upNext;
    TableauxPart currentStep;
    Branch from;
    

    HashMap<Expression, LinkedList<TableauxRules>> exprRulesApplied;
    LinkedList<String> nomsInBranch;

    boolean notEInEffect = false;
    LinkedList<TableauxPart> origNotEParts;

    boolean notDiamondInEffect = false;
    LinkedList<TableauxPart> origNotDiamondParts;

    

    public Branch(TableauxPart startTab, Branch from) {
        this.tableauxParts = new LinkedList<>();
        this.tableauxParts.add(startTab);
        this.currentStep = startTab;

        this.upNext = new LinkedList<>();
        upNext.add(startTab);

        this.from = from;

        if (this.from == null) {
            exprRulesApplied = new HashMap<Expression, LinkedList<TableauxRules>>() {
            };
            nomsInBranch = new LinkedList<>();
            origNotEParts = new LinkedList<>();
            origNotDiamondParts = new LinkedList<>();
        } else {
            exprRulesApplied = this.from.exprRulesApplied;
            nomsInBranch = this.from.nomsInBranch;
            origNotEParts = this.from.origNotEParts;
            origNotDiamondParts = this.from.origNotDiamondParts;
        }
    }

    public void addToBranch(TableauxPart part, boolean upNext) {
        if (currentStep.seekExpressionInBranch(part.expr)) {
            return;
        }
        
        this.currentStep = part;
        this.tableauxParts.add(part);
        
        if (upNext) {
            this.upNext.add(part);
        }
    }

    public void documentRule(Expression expr, TableauxRules ruleApplied) {
        if (exprRulesApplied.get(expr) == null) {
            exprRulesApplied.put(expr, new LinkedList<>());
        }
        exprRulesApplied.get(expr).add(ruleApplied);
    }

    public void applyAnd(Expression parent, Expression left, Expression right, TableauxPart current,
        LinkedList<Branch> returnBranches, TableauxRules ruleApplied) {
        TableauxPart andLeft = new TableauxPart(left, this.currentStep, current, ruleApplied);
        addToBranch(andLeft, true);
        TableauxPart andRight = new TableauxPart(right, this.currentStep, current, ruleApplied);
        addToBranch(andRight, true);
        returnBranches.add(this);
    }

    public void applyOr(Expression parent, Expression left, Expression right, TableauxPart current,
            LinkedList<Branch> returnBranches, TableauxRules ruleApplied) {
        documentRule(parent, ruleApplied);
        TableauxPart orLeft = new TableauxPart(left, this.currentStep, current, ruleApplied,
                this.currentStep.lineNumber + 1);
        TableauxPart orRight = new TableauxPart(right, this.currentStep, current, ruleApplied,
                this.currentStep.lineNumber + 2);
        returnBranches.add(this);
        
        returnBranches.add(new Branch(orLeft, this));
        returnBranches.add(new Branch(orRight, this));
    }

    public void applyNotE(TableauxPart origNotEPart, Nominal nominal) {
        Expression origNotEExpr = null;
        try {
            origNotEExpr = ((E) ((Satisfier) ((Not) origNotEPart.expr).proposition).proposition).proposition;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        TableauxPart newNotE = new TableauxPart(new Not(new Satisfier(nominal, origNotEExpr)), this.currentStep,
                origNotEPart, TableauxRules.NOTE);
        addToBranch(newNotE, false);
        upNext.add(newNotE);
    }

    public void applyNotDiamond(TableauxPart origNotDiamondPart) {
        Diamond notSatisDiamond = (Diamond) ((Satisfier) ((Not) origNotDiamondPart.expr).proposition).proposition;
        Nominal desiredNominal = ((Satisfier) ((Not) origNotDiamondPart.expr).proposition).referencePoint;
        Nominal result = this.currentStep.seekNotDiamondRule(desiredNominal.identifier);
        if (result != null) {
            TableauxPart notSatisOldNominal = new TableauxPart(
                    new Not(new Satisfier(result, notSatisDiamond.proposition)), this.currentStep, origNotDiamondPart,
                    TableauxRules.NOTDIAMOND);
            addToBranch(notSatisOldNominal, true);
        }
    }

    public void newNominal(String id, TableauxPart from, TableauxPart source) {
        nomsInBranch.add(id);
        TableauxPart ref = new TableauxPart(new Satisfier(new Nominal(id), new Nominal(id)), from, source,
                TableauxRules.REF);
        addToBranch(ref, false);
        if (notEInEffect) {
            for (int i = 0; i < origNotEParts.size(); i++) {
                applyNotE(origNotEParts.get(i), new Nominal(id));
            }
        }
        if (notDiamondInEffect) {
            for (int i = 0; i < origNotDiamondParts.size(); i++) {
                applyNotDiamond(origNotDiamondParts.get(i));
            }
        }
    }

    public void applyNomRule(TableauxPart target, String targetId) {
        LinkedList<Satisfier> foundNomRelations = this.currentStep.seekAllNominalRelations(targetId, null);
        LinkedList<Satisfier> foundNomRuleFormula = this.currentStep.seekAllNomRuleFormula(targetId, null);

        if (foundNomRelations.size() > 0 && foundNomRuleFormula.size() > 0) {
            for (int i = 0; i < foundNomRelations.size(); i++) {
                String oldNom = foundNomRelations.get(i).referencePoint.identifier;
                String newNom = ((Nominal) foundNomRelations.get(i).proposition).identifier;

                if (oldNom == newNom) {
                    continue;
                }

                for (int j = 0; j < foundNomRuleFormula.size(); j++) {
                    Expression innerPart = foundNomRuleFormula.get(j).proposition;
                    TableauxRules rule = innerPart.getType() == ExpressionTypes.DIAMOND ? TableauxRules.NOM2
                            : TableauxRules.NOM1;
                    TableauxPart newRelation = new TableauxPart(new Satisfier(new Nominal(newNom), innerPart),
                            this.currentStep, target, rule);

                    boolean expressionInBranch = this.currentStep.seekExpressionInBranch(newRelation.expr);
                    if (expressionInBranch) {
                        continue;
                    }
                    addToBranch(newRelation, true);
                }

            }
        }
    }

    public LinkedList<Branch> nextStep() {

        LinkedList<Branch> returnBranches = new LinkedList<>();

        if (upNext.isEmpty()) {
            return null;
        }

        TableauxPart current = upNext.getFirst();
        upNext.removeFirst();

        Expression expr = current.expr;
        ExpressionTypes type = expr.getType();

        exprRulesApplied.put(expr, new LinkedList<>());

        for (String nominalId : nomsInBranch) {
            applyNomRule(current, nominalId);
        }

        switch (type) {
            case AND:
                And and = (And) expr;
                applyAnd(expr, and.propositionLeft, and.propositionRight, current, returnBranches, TableauxRules.AND);
                break;
            case OR:
                Or or = (Or) expr;
                applyOr(expr, or.propositionLeft, or.propositionRight, current, returnBranches, TableauxRules.OR);
                break;
            case SATISFIER:
                Expression satisProp = ((Satisfier) expr).proposition;
                Nominal satisNominal = (Nominal) ((Satisfier) expr).referencePoint;
                if (!nomsInBranch.contains(satisNominal.identifier)) {
                    newNominal(satisNominal.identifier, current, current);
                }
                switch (satisProp.getType()) {
                    case NOT:
                        Not statisNot = (Not) satisProp;
                        TableauxPart notSatis = new TableauxPart(
                                new Not(new Satisfier(satisNominal, statisNot.proposition)), this.currentStep, current,
                                TableauxRules.NOT);
                        addToBranch(notSatis, true);
                        returnBranches.add(this);
                        break;
                    case AND:
                        And satisAnd = (And) satisProp;
                        applyAnd(expr, new Satisfier(satisNominal, satisAnd.propositionLeft),
                                new Satisfier(satisNominal, satisAnd.propositionRight), current, returnBranches,
                                TableauxRules.AND);
                        break;
                    case OR:
                        Or satisOr = (Or) satisProp;
                        applyOr(expr, new Satisfier(satisNominal, satisOr.propositionLeft),
                                new Satisfier(satisNominal, satisOr.propositionRight), current, returnBranches,
                                TableauxRules.OR);
                        break;
                    case SATISFIER:
                        TableauxPart satisSatis = new TableauxPart(satisProp, this.currentStep, current,
                                TableauxRules.SATIS);
                        addToBranch(satisSatis, true);
                        returnBranches.add(this);
                        break;
                    case DIAMOND:
                        Diamond diamond = (Diamond) satisProp;
                        boolean diamondHasLoneNominal = diamond.proposition.getType() == ExpressionTypes.NOMINAL;
                        boolean branchHasDiamondRuleExpression = this.currentStep.seekLoopCondition(diamond.proposition,
                                satisNominal.identifier, false);
                        if (!diamondHasLoneNominal && !branchHasDiamondRuleExpression) {
                            String id = satisNominal.identifier;
                            String newNominalId = "D" + id + id;

                            TableauxPart newNominalSatis = new TableauxPart(
                                    new Satisfier(new Nominal(newNominalId), diamond.proposition), this.currentStep,
                                    current, TableauxRules.DIAMOND, current.lineNumber + 1);
                            addToBranch(newNominalSatis, true);
                            TableauxPart newDiamondNominal = new TableauxPart(
                                    new Satisfier(satisNominal, new Diamond(new Nominal(newNominalId))),
                                    this.currentStep, current, TableauxRules.DIAMOND, current.lineNumber + 2);
                            addToBranch(newDiamondNominal, false);

                            newNominal(newNominalId, this.currentStep, newNominalSatis);
                        }
                        returnBranches.add(this);
                        break;
                    case E:
                        E e = (E) satisProp;
                        boolean eHasLoneNominal = e.proposition.getType() == ExpressionTypes.NOMINAL;
                        boolean branchHasERuleExpression = this.currentStep.seekLoopCondition(e.proposition,
                                satisNominal.identifier, false);
                        if (!eHasLoneNominal && !branchHasERuleExpression) {
                            String id = satisNominal.identifier;
                            TableauxPart newESatis = new TableauxPart(
                                    new Satisfier(new Nominal("E" + id + id), e.proposition), this.currentStep, current,
                                    TableauxRules.E);
                            addToBranch(newESatis, true);
                            newNominal("E" + id + id, this.currentStep, newESatis);
                        }
                        returnBranches.add(this);
                        break;
                    case NOMINAL:
                        Nominal satisLoneNominal = (Nominal) satisProp;
                        if (!nomsInBranch.contains(satisLoneNominal.identifier)) {
                            newNominal(satisLoneNominal.identifier, current, current);
                        }
                        returnBranches.add(this);
                        break;
                    case PROPOSITIONAL_SYMBOL:
                        returnBranches.add(this);
                        break;
                    default:
                        System.err.println("Attempted to break down: " + current.expr
                                + " but encountered an unexpeted ExpressionType in Satisfier(...): "
                                + satisProp.getType());
                        returnBranches.add(this);
                        break;
                }
                break;
            case NOT:
                Expression notProp = ((Not) expr).proposition;
                switch (notProp.getType()) {
                    case NOT:
                        Not notNot = (Not) notProp;
                        TableauxPart prop = new TableauxPart(notNot.proposition, this.currentStep, current,
                                TableauxRules.NOTNOT);
                        addToBranch(prop, true);
                        returnBranches.add(this);
                        break;
                    case AND:
                        And notAnd = (And) notProp;
                        applyOr(expr, new Not(notAnd.propositionLeft), new Not(notAnd.propositionRight), current,
                                returnBranches, TableauxRules.NOTAND);
                        break;
                    case OR:
                        Or notOr = (Or) notProp;
                        applyAnd(expr, new Not(notOr.propositionLeft), new Not(notOr.propositionRight), current,
                                returnBranches, TableauxRules.NOTOR);
                        break;
                    case SATISFIER:
                        Satisfier notSatis = (Satisfier) notProp;
                        Nominal notSatisNominal = notSatis.referencePoint;
                        if (!nomsInBranch.contains(notSatisNominal.identifier)) {
                            newNominal(notSatisNominal.identifier, current, current);
                        }
                        if (notSatis.proposition.getType() == ExpressionTypes.NOT) {
                            Not notSatisNot = (Not) notSatis.proposition;
                            TableauxPart notNotSatis = new TableauxPart(
                                    new Satisfier(notSatis.referencePoint, notSatisNot.proposition), this.currentStep,
                                    current, TableauxRules.NOTNOT);
                            addToBranch(notNotSatis, true);
                            returnBranches.add(this);
                        } else if (notSatis.proposition.getType() == ExpressionTypes.AND) {
                            And notSatisAnd = (And) notSatis.proposition;
                            applyOr(expr, new Not(new Satisfier(notSatis.referencePoint, notSatisAnd.propositionLeft)),
                                    new Not(new Satisfier(notSatis.referencePoint, notSatisAnd.propositionRight)),
                                    current, returnBranches, TableauxRules.NOTAND);
                        } else if (notSatis.proposition.getType() == ExpressionTypes.OR) {
                            Or notSatisOr = (Or) notSatis.proposition;
                            applyAnd(expr, new Not(new Satisfier(notSatis.referencePoint, notSatisOr.propositionLeft)),
                                    new Not(new Satisfier(notSatis.referencePoint, notSatisOr.propositionRight)),
                                    current, returnBranches, TableauxRules.NOTOR);
                        } else if (notSatis.proposition.getType() == ExpressionTypes.SATISFIER) {
                            Satisfier notSatisSatis = (Satisfier) notSatis.proposition;
                            TableauxPart innerNotSatis = new TableauxPart(new Not(notSatisSatis), this.currentStep,
                                    current, TableauxRules.NOTSATIS);
                            addToBranch(innerNotSatis, true);
                            returnBranches.add(this);
                        } else if (notSatis.proposition.getType() == ExpressionTypes.DIAMOND) {
                            applyNotDiamond(current);
                            notDiamondInEffect = true;
                            origNotDiamondParts.add(current);
                            returnBranches.add(this);
                        } else if (notSatis.proposition.getType() == ExpressionTypes.E) {
                            applyNotE(current, notSatis.referencePoint);
                            this.notEInEffect = true;
                            this.origNotEParts.add(current);
                        } else if (notSatis.proposition.getType() == ExpressionTypes.NOMINAL) {
                            Nominal satisLoneNominal = (Nominal) notSatis.proposition;
                            if (!nomsInBranch.contains(satisLoneNominal.identifier)) {
                                newNominal(satisLoneNominal.identifier, current, current);
                            }
                            returnBranches.add(this);
                        } else if (notSatis.proposition.getType() == ExpressionTypes.PROPOSITIONAL_SYMBOL) {
                            returnBranches.add(this);
                        } else {
                            System.err.println("Attempted to break down: " + current.expr
                                    + " but encountered unexpected Expression type in Not(Satisfier(...)): "
                                    + notSatis.proposition.getType());

                            returnBranches.add(this);
                        }
                        break;
                    case NOMINAL:
                        Nominal notNominal = (Nominal) notProp;
                        if (!nomsInBranch.contains(notNominal.identifier)) {
                            newNominal(notNominal.identifier, current, current);
                        }
                        break;
                    default:
                        System.err.println("Attempted to break down: " + current.expr
                                + "but encountered an unexpeted ExpressionType in Not(...):" + notProp.getType());
                        returnBranches.add(this);
                        break;
                }
                break;
            case NOMINAL:
                Nominal nominal = (Nominal) expr;
                if (!nomsInBranch.contains(nominal.identifier)) {
                    newNominal(nominal.identifier, current, current);
                }
                break;
            default:
                System.err.println("Attempted to break down: " + current.expr
                        + "but encountered an unexpeted ExpressionType: " + type);
                returnBranches.add(this);
                break;
        }

        return returnBranches;
    }

    public String toString() {
        LinkedList<String> s = new LinkedList<>();
        TableauxPart next = this.currentStep;
        while (next != null) {
            s.add(next.toString());
            if (tableauxParts.contains(next.from)) {
                next = next.from;
            } else
                next = null;
        }
        String string = "";
        for (int i = s.size() - 1; i >= 0; i--) {
            string += "\n " + s.get(i);
        }
        return string;
    }

}

class TableauxPart {
    Expression expr;
    TableauxPart from;
    TableauxPart source;
    int lineNumber;
    TableauxRules ruleApplied;

    // Line number set explicidly
    public TableauxPart(Expression e, TableauxPart from, TableauxPart source, TableauxRules ruleApplied,
            int lineNumber) {
        this.expr = e;
        // From is a reference to the "step" it is from, it has nothing to do with what
        // expression the current tableauxPart is a subexpression of
        this.from = from;
        // Souce is the expression this tableaux part is made from
        this.source = source;
        // Set the line number
        this.lineNumber = lineNumber;
        // Rule applied to get this tableauxPart
        this.ruleApplied = ruleApplied;
    }

    // Line number gotten from from
    public TableauxPart(Expression e, TableauxPart from, TableauxPart source, TableauxRules ruleApplied) {
        this.expr = e;
        // From is a reference to the "step" it is from, it has nothing to do with what
        // expression the current tableauxPart is a subexpression of
        this.from = from;
        // Souce is the expression this tableaux part is made from
        this.source = source;
        // Set the line number
        if (from == null) {
            this.lineNumber = 1;
        } else
            this.lineNumber = from.lineNumber + 1;
        // Rule applied to get this tableauxPart
        this.ruleApplied = ruleApplied;
    }

    public String toString() {
        if (from != null && source != null) {
            return expr.toString() + "\t" + "Line: " + lineNumber + ", Rule: " + ruleApplied + " from: "
                    + source.lineNumber;
        }
        return expr.toString() + "\t" + "Line: " + lineNumber;
    }

    public boolean seekExpressionInBranch(Expression model) {
        if (expr.equals(model)) {
            return true;
        }
        if (from == null) {
            return false;
        }
        return from.seekExpressionInBranch(model);
    }

    public LinkedList<Satisfier> seekAllNominalRelations(String targetId, LinkedList<Satisfier> foundRelation) {
        LinkedList<Satisfier> returnNoms = foundRelation;
        if (foundRelation == null) {
            returnNoms = new LinkedList<>();
        }
        if (expr.getType() == ExpressionTypes.SATISFIER) {
            Satisfier inner = (Satisfier) expr;
            Boolean isLoneNominal = inner.proposition.getType() == ExpressionTypes.NOMINAL;
            Boolean isTargetReference = inner.referencePoint.identifier.equals(targetId);
            if (isLoneNominal && isTargetReference) {
                returnNoms.add(inner);
            }
        }
        if (from == null) {
            return returnNoms;
        }
        return from.seekAllNominalRelations(targetId, returnNoms);
    }

    public LinkedList<Satisfier> seekAllNomRuleFormula(String targetId, LinkedList<Satisfier> foundNomRuleFormula) {
        LinkedList<Satisfier> returnNoms = foundNomRuleFormula;
        if (foundNomRuleFormula == null) {
            returnNoms = new LinkedList<>();
        }
        if (expr.getType() == ExpressionTypes.SATISFIER) {
            Satisfier inner = (Satisfier) expr;
            Boolean isLoneNominal = inner.proposition.getType() == ExpressionTypes.NOMINAL;
            Boolean isLonePropSymbol = inner.proposition.getType() == ExpressionTypes.PROPOSITIONAL_SYMBOL;
            Boolean isDiamond = inner.proposition.getType() == ExpressionTypes.DIAMOND;
            Boolean diamondHasLonePropSymbol = false;
            if (isDiamond) {
                diamondHasLonePropSymbol = ((Diamond) inner.proposition).proposition
                        .getType() == ExpressionTypes.NOMINAL;
            }
            Boolean isTargetReference = inner.referencePoint.identifier.equals(targetId);
            if (((isDiamond && diamondHasLonePropSymbol) || isLonePropSymbol || isLoneNominal) && isTargetReference) {
                returnNoms.add(inner);
            }
        }
        if (from == null) {
            return returnNoms;
        }
        return from.seekAllNomRuleFormula(targetId, returnNoms);
    }

    public boolean seekLoopCondition(Expression target, String selfId, Boolean rightTargetRightNominal) {
        Boolean flag = rightTargetRightNominal;
        if (expr.getType() == ExpressionTypes.SATISFIER) {
            Satisfier inner = (Satisfier) expr;
            Boolean isOwnNominal = ((Nominal) inner.referencePoint).identifier.equals(selfId);
            Boolean isTarget = inner.proposition.equals(target);
            if (isTarget && !isOwnNominal && rightTargetRightNominal) {
                return true;
            } else if (isTarget && isOwnNominal) {
                flag = true;
            }
        } else if (expr.getType() == ExpressionTypes.NOT) {
            Not inner = (Not) expr;
            if (inner.proposition.getType() == ExpressionTypes.SATISFIER) {
                Satisfier notInner = (Satisfier) inner.proposition;
                Boolean isOwnNominal = ((Nominal) notInner.referencePoint).identifier.equals(selfId);
                Boolean isTarget = notInner.proposition.equals(target);
                if (isTarget && !isOwnNominal && rightTargetRightNominal) {
                    return true;
                } else if (isTarget && isOwnNominal) {
                    flag = true;
                }
            }
        }
        if (from == null) {
            return false;
        }
        return from.seekLoopCondition(target, selfId, flag);
    }

    public Nominal seekNotDiamondRule(String nominalID) {
        if (expr.getType() == ExpressionTypes.SATISFIER) {
            Satisfier inner = (Satisfier) expr;
            Nominal nominal = (Nominal) inner.referencePoint;
            if (nominal.identifier.equals(nominalID) && inner.proposition.getType() == ExpressionTypes.DIAMOND) {
                Diamond innerInner = (Diamond) inner.proposition;
                if (innerInner.proposition.getType() == ExpressionTypes.NOMINAL) {
                    return (Nominal) innerInner.proposition;
                }
            }
        }
        if (from == null) {
            return null;
        }
        return from.seekNotDiamondRule(nominalID);
    }
}

enum TableauxRules {
    AND,
    OR,
    NOTNOT,
    NOTOR,
    NOTAND,
    NOT,
    SATIS,
    NOTSATIS,
    DIAMOND,
    NOTDIAMOND,
    E,
    NOTE,
    REF,
    NOM1,
    NOM2
}