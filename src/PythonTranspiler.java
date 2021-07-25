import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.sql.SQLOutput;
import java.util.*;

public class PythonTranspiler implements Java8ParserListener {

    // control de la generacion de la traduccion
    private static final String TAB = "  ";                                      // tamanio de una identacion
    private static final String INSTANCE_VAR_PLACEHOLDER = "?instance_vars";     // placeholder para insertar constructor con variables de instancia
    private static final String CLASS_VAR_PLACEHOLDER = "?class_vars";           // placeholder para insertar variables de clase

    private String compilationUnitName;         // nombre del archivo Java
    private int tabDepth;                       // profundidad de la identacion
    private Stack<Integer> switchState = new Stack<>(); // 0 = initial if, 1 = elif, 2 = else
    private boolean scannerWasDeclared = false;
    private Stack<String> switchVariable= new Stack<>();

    // control de codigo fuente de salida
    private final StringBuilder transpiledSource;
    private final boolean enableDebugOutput;

    // control de variables del codigo fuente origen
    private Map<String, String> classVariables;
    private Map<String, String> instanceVariables;
    private Set<String> classMethods;
    private Set<String> instanceMethods;


    /**
     * Para no tener que estar escribiendo System.out.println(o.toString())
     * @param o el objeto a mostrar
     */
    public void print(Object o) {
        System.out.println(o.toString());
    }

    public PythonTranspiler(String filename) {
        // impl.
        compilationUnitName = filename;
        tabDepth = 0;

        transpiledSource = new StringBuilder();
        enableDebugOutput = false;

        classVariables = new HashMap<>();
        instanceVariables = new HashMap<>();
        classMethods = new HashSet<>();
        instanceMethods = new HashSet<>();
        System.out.println("Transpiling this compilation unit (filename): " + filename);
    }

    /**
     * Agrega codigo fuente Python traducido a un StringBuilder.
     * @param src codigo fuente
     * @param addNewline indica si debe ir en la misma linea, o en la siguiente.
     */
    public void appendToTranspiledSrc(String src, boolean addNewline) {
        // impl.
        // TODO: src = fixBloatSpaces(src);
        if (addNewline) {
            transpiledSource
                    .append(TAB.repeat(tabDepth))
                    .append(src)
                    .append("\n");
        } else {
            transpiledSource.append(src);
        }

        if (enableDebugOutput)
            System.out.println(src);
    }

    /**
     * Agrega una linea de codigo fuente Python sin 'newline' al final, de esta manera la
     * siguiente linea va al lado (en la misma linea).
     * @param src codigo fuente traducido
     */
    public void append(String src) {
        // impl.
        appendToTranspiledSrc(src, false);
    }

    /**
     * Agrega una linea de codigo fuente Python con 'newline' al final, de esta manera la
     * siguiente linea va debajo.
     * @param src codigo fuente traducido
     */
    public void appendln(String src) {
        // impl.
        appendToTranspiledSrc(src, true);
    }

    /**
     * Un getter para el contenido del StringBuilder que esta concatenando la traduccion.
     * Se realizan traducciones offline y se mejorar estrucuturalmente el codigo de salida.
     * @return
     */
    public String getTranspiledSource() {
        // impl.
        String pythonSource = transpiledSource.toString();
        pythonSource = insertVariables(pythonSource);      // inserta variables
        pythonSource = removePlaceHolders(pythonSource);   // elimina cualquier placeholder
        return pythonSource;
    }

    /**
     * Obtiene un valor de inicializacion por defecto segun el tipo de variable.
     * @param type
     * @return
     */
    public String getInitValue(String type) {
        // impl.
        switch (type) {
            case "double":
            case "Double":
                return "0.0";
            case "int":
            case "Integer":
                return "0";
            case "char":
            case "Char":
            case "String":
                return "''";
            case "boolean":
            case "Boolean":
                return "False";
            default:
                return "None";
        }
    }

    /**
     * Empieza una busqueda hacia la raiz del arbol buscanto si el contexto que se pasa como inicio es hijo del contexto
     * que se nombra como cadena.
     * Se usa para controlar la transpilacion en los casos en los que la funcion de entrada o salida de regla deba ejecutarse
     * según sea su padre o de donde provenga.
     * @param startCtx contexto objetivo a verificar paternidad
     * @param parentContextName
     * @return
     */
    public boolean hasParent(ParserRuleContext startCtx, String parentContextName) {
        // impl.
        ParserRuleContext currCtx = startCtx.getParent();
        boolean parentFound = false;
        try {
            while (true) {
                String parentGoal = "Java8Parser$" + parentContextName;
                currCtx = currCtx.getParent();
                String currParent = currCtx.getParent().getClass().getName();
                if (currParent.equals(parentGoal)) {
                    parentFound = true;
                    break;
                }
            }
        } catch (NullPointerException npe) {
            parentFound = false;
        }
        return parentFound;
    }

    /**
     * Traduce los operadores logicos de una expresion booleana en Java a Python
     * @param boolExpr
     * @return
     */
    public String replaceBooleanOps(String boolExpr) {
        // impl.
        boolExpr = boolExpr.replace("||", " or ");
        boolExpr = boolExpr.replace("&&", " and ");
        boolExpr = boolExpr.replace("!", " not ");
        return fixBloatSpaces(boolExpr);
    }

    /**
     * Contiene otros metodos que realizan relleno con declaracion de variables. Como
     * en python la declaracion de variables de instancia se hace con el constructor,
     * también puede implicar la creacion de traductores para la equivalencia semantica.
     *
     * https://docs.python.org/3/tutorial/classes.html#class-and-instance-variables
     */
    private String insertVariables(String pythonSource) {
        // impl.
        pythonSource = insertClassVariables(pythonSource);
        pythonSource = insertInstanceVariables(pythonSource); // crea constructor
        return pythonSource;
    }

    /**
     * Inserta las variables estaticas a la traduccion en los lugares del los placeholders.
     * @param pythonSource resultado de la traduccion online del codigo fuente
     * @return
     */
    private String insertClassVariables(String pythonSource) {
        // impl.
        tabDepth++;
        StringBuilder varDeclarations = new StringBuilder();
        for (Map.Entry<String, String> varDcl : classVariables.entrySet())
            varDeclarations.append(String.format("%s%s = %s\n", TAB.repeat(tabDepth), varDcl.getKey(), varDcl.getValue()));
        tabDepth--;
        return pythonSource.replace(CLASS_VAR_PLACEHOLDER, varDeclarations.toString());
    }

    /**
     * Inserta las variables de instancia a la traduccion en los lugares del los placeholders. Se hace doble iteracion
     * para mayor claridad del codigo.
     * @param pythonSource resultado de la traduccion online del codigo fuente.
     */
    private String insertInstanceVariables(String pythonSource) {
        // impl.
        StringBuilder constructorDcl = new StringBuilder();
        // Declaracion del constructor
        tabDepth++; // identacion para el __init__()
        List<String> constructorArgs = new LinkedList<>();
        for (Map.Entry<String, String> varDcl : instanceVariables.entrySet())
            constructorArgs.add(String.format("%s=%s", varDcl.getKey(), varDcl.getValue()));
        constructorDcl.append(String.format("%sdef __init__(self, %s):\n", TAB.repeat(tabDepth), String.join(", ", constructorArgs)));

        // Cuerpo del constructor
        tabDepth++; // identacion para el cuerpo
        for (Map.Entry<String, String> varDcl : instanceVariables.entrySet())
            constructorDcl.append(String.format("%sself.%s = %s", TAB.repeat(tabDepth), varDcl.getKey(), varDcl.getKey()));
        constructorDcl.append("\n");
        tabDepth--;
        tabDepth--;

        pythonSource = pythonSource.replace(INSTANCE_VAR_PLACEHOLDER, constructorDcl.toString());
        return pythonSource;
    }

    /**
     * Elimina cualquier place holder que no se haya reemplazado en la fase offline de la
     * traduccion.
     * @param pythonSource
     * @return
     */
    private String removePlaceHolders(String pythonSource) {
        // impl.
        pythonSource = pythonSource.replace(CLASS_VAR_PLACEHOLDER, "");
        pythonSource = pythonSource.replace(INSTANCE_VAR_PLACEHOLDER, "");
        return pythonSource;
    }

    /**
     * TODO
     * Cambia varios espacios en blanco por uno solo, separa operador de asignaciones y arregla cualquier
     * tema estetico de espacios del codigo fuente.
     * @param src
     * @return
     */
    public String fixBloatSpaces(String src) {
        // impl.
        src = src.replace("  ", " "); // quitar espacios en blanco que sobran
        src = src.replaceAll("=", " = ").trim();
        src = src.replaceAll(" =  = ", "==").trim(); // fix para el operador
        src = src.replaceAll(">  =", ">=").trim(); // fix para el operador
        src = src.replaceAll("<  =", "<=").trim(); // fix para el operador
        return src;
    }

    @Override
    public void enterLiteral(Java8Parser.LiteralContext ctx) {

    }

    @Override
    public void exitLiteral(Java8Parser.LiteralContext ctx) {

    }

    @Override
    public void enterPrimitiveType(Java8Parser.PrimitiveTypeContext ctx) {

    }

    @Override
    public void exitPrimitiveType(Java8Parser.PrimitiveTypeContext ctx) {

    }

    @Override
    public void enterNumericType(Java8Parser.NumericTypeContext ctx) {

    }

    @Override
    public void exitNumericType(Java8Parser.NumericTypeContext ctx) {

    }

    @Override
    public void enterIntegralType(Java8Parser.IntegralTypeContext ctx) {

    }

    @Override
    public void exitIntegralType(Java8Parser.IntegralTypeContext ctx) {

    }

    @Override
    public void enterFloatingPointType(Java8Parser.FloatingPointTypeContext ctx) {

    }

    @Override
    public void exitFloatingPointType(Java8Parser.FloatingPointTypeContext ctx) {

    }

    @Override
    public void enterReferenceType(Java8Parser.ReferenceTypeContext ctx) {

    }

    @Override
    public void exitReferenceType(Java8Parser.ReferenceTypeContext ctx) {

    }

    @Override
    public void enterClassOrInterfaceType(Java8Parser.ClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitClassOrInterfaceType(Java8Parser.ClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterClassType(Java8Parser.ClassTypeContext ctx) {

    }

    @Override
    public void exitClassType(Java8Parser.ClassTypeContext ctx) {

    }

    @Override
    public void enterClassType_lf_classOrInterfaceType(Java8Parser.ClassType_lf_classOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitClassType_lf_classOrInterfaceType(Java8Parser.ClassType_lf_classOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterClassType_lfno_classOrInterfaceType(Java8Parser.ClassType_lfno_classOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitClassType_lfno_classOrInterfaceType(Java8Parser.ClassType_lfno_classOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterInterfaceType(Java8Parser.InterfaceTypeContext ctx) {

    }

    @Override
    public void exitInterfaceType(Java8Parser.InterfaceTypeContext ctx) {

    }

    @Override
    public void enterInterfaceType_lf_classOrInterfaceType(Java8Parser.InterfaceType_lf_classOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitInterfaceType_lf_classOrInterfaceType(Java8Parser.InterfaceType_lf_classOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterInterfaceType_lfno_classOrInterfaceType(Java8Parser.InterfaceType_lfno_classOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitInterfaceType_lfno_classOrInterfaceType(Java8Parser.InterfaceType_lfno_classOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterTypeVariable(Java8Parser.TypeVariableContext ctx) {

    }

    @Override
    public void exitTypeVariable(Java8Parser.TypeVariableContext ctx) {

    }

    @Override
    public void enterArrayType(Java8Parser.ArrayTypeContext ctx) {

    }

    @Override
    public void exitArrayType(Java8Parser.ArrayTypeContext ctx) {

    }

    @Override
    public void enterDims(Java8Parser.DimsContext ctx) {

    }

    @Override
    public void exitDims(Java8Parser.DimsContext ctx) {

    }

    @Override
    public void enterTypeParameter(Java8Parser.TypeParameterContext ctx) {

    }

    @Override
    public void exitTypeParameter(Java8Parser.TypeParameterContext ctx) {

    }

    @Override
    public void enterTypeParameterModifier(Java8Parser.TypeParameterModifierContext ctx) {

    }

    @Override
    public void exitTypeParameterModifier(Java8Parser.TypeParameterModifierContext ctx) {

    }

    @Override
    public void enterTypeBound(Java8Parser.TypeBoundContext ctx) {

    }

    @Override
    public void exitTypeBound(Java8Parser.TypeBoundContext ctx) {

    }

    @Override
    public void enterAdditionalBound(Java8Parser.AdditionalBoundContext ctx) {

    }

    @Override
    public void exitAdditionalBound(Java8Parser.AdditionalBoundContext ctx) {

    }

    @Override
    public void enterTypeArguments(Java8Parser.TypeArgumentsContext ctx) {

    }

    @Override
    public void exitTypeArguments(Java8Parser.TypeArgumentsContext ctx) {

    }

    @Override
    public void enterTypeArgumentList(Java8Parser.TypeArgumentListContext ctx) {

    }

    @Override
    public void exitTypeArgumentList(Java8Parser.TypeArgumentListContext ctx) {

    }

    @Override
    public void enterTypeArgument(Java8Parser.TypeArgumentContext ctx) {

    }

    @Override
    public void exitTypeArgument(Java8Parser.TypeArgumentContext ctx) {

    }

    @Override
    public void enterWildcard(Java8Parser.WildcardContext ctx) {

    }

    @Override
    public void exitWildcard(Java8Parser.WildcardContext ctx) {

    }

    @Override
    public void enterWildcardBounds(Java8Parser.WildcardBoundsContext ctx) {

    }

    @Override
    public void exitWildcardBounds(Java8Parser.WildcardBoundsContext ctx) {

    }

    @Override
    public void enterPackageName(Java8Parser.PackageNameContext ctx) {

    }

    @Override
    public void exitPackageName(Java8Parser.PackageNameContext ctx) {

    }

    @Override
    public void enterTypeName(Java8Parser.TypeNameContext ctx) {

    }

    @Override
    public void exitTypeName(Java8Parser.TypeNameContext ctx) {

    }

    @Override
    public void enterPackageOrTypeName(Java8Parser.PackageOrTypeNameContext ctx) {

    }

    @Override
    public void exitPackageOrTypeName(Java8Parser.PackageOrTypeNameContext ctx) {

    }

    @Override
    public void enterExpressionName(Java8Parser.ExpressionNameContext ctx) {

    }

    @Override
    public void exitExpressionName(Java8Parser.ExpressionNameContext ctx) {

    }

    @Override
    public void enterMethodName(Java8Parser.MethodNameContext ctx) {

    }

    @Override
    public void exitMethodName(Java8Parser.MethodNameContext ctx) {

    }

    @Override
    public void enterAmbiguousName(Java8Parser.AmbiguousNameContext ctx) {

    }

    @Override
    public void exitAmbiguousName(Java8Parser.AmbiguousNameContext ctx) {

    }

    @Override
    public void enterCompilationUnit(Java8Parser.CompilationUnitContext ctx) {

    }

    @Override
    public void exitCompilationUnit(Java8Parser.CompilationUnitContext ctx) {
        // impl.
        appendln("import sys");
        appendln("if __name__ == '__main__':");
        appendln(String.format("%s%s.main(sys.argv)", TAB, compilationUnitName));
    }

    @Override
    public void enterPackageDeclaration(Java8Parser.PackageDeclarationContext ctx) {

    }

    @Override
    public void exitPackageDeclaration(Java8Parser.PackageDeclarationContext ctx) {

    }

    @Override
    public void enterPackageModifier(Java8Parser.PackageModifierContext ctx) {

    }

    @Override
    public void exitPackageModifier(Java8Parser.PackageModifierContext ctx) {

    }

    @Override
    public void enterImportDeclaration(Java8Parser.ImportDeclarationContext ctx) {

    }

    @Override
    public void exitImportDeclaration(Java8Parser.ImportDeclarationContext ctx) {

    }

    @Override
    public void enterSingleTypeImportDeclaration(Java8Parser.SingleTypeImportDeclarationContext ctx) {

    }

    @Override
    public void exitSingleTypeImportDeclaration(Java8Parser.SingleTypeImportDeclarationContext ctx) {

    }

    @Override
    public void enterTypeImportOnDemandDeclaration(Java8Parser.TypeImportOnDemandDeclarationContext ctx) {

    }

    @Override
    public void exitTypeImportOnDemandDeclaration(Java8Parser.TypeImportOnDemandDeclarationContext ctx) {

    }

    @Override
    public void enterSingleStaticImportDeclaration(Java8Parser.SingleStaticImportDeclarationContext ctx) {

    }

    @Override
    public void exitSingleStaticImportDeclaration(Java8Parser.SingleStaticImportDeclarationContext ctx) {

    }

    @Override
    public void enterStaticImportOnDemandDeclaration(Java8Parser.StaticImportOnDemandDeclarationContext ctx) {

    }

    @Override
    public void exitStaticImportOnDemandDeclaration(Java8Parser.StaticImportOnDemandDeclarationContext ctx) {

    }

    @Override
    public void enterTypeDeclaration(Java8Parser.TypeDeclarationContext ctx) {

    }

    @Override
    public void exitTypeDeclaration(Java8Parser.TypeDeclarationContext ctx) {

    }

    /**
     * Ademas de traducir la declaracion de la clase, agrega dos tokens para localizar
     * la ubicacion de las variables de instancia y clase. Los marcadores son:
     * - ?instance_vars
     * - ?class_vars
     *
     * @param ctx the parse tree
     */
    @Override
    public void enterClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {
        // impl.
        appendln("class " + compilationUnitName + ":");
        appendln(CLASS_VAR_PLACEHOLDER);
        appendln(INSTANCE_VAR_PLACEHOLDER);
    }

    @Override
    public void exitClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {

    }

    @Override
    public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {

    }

    @Override
    public void exitNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {

    }

    @Override
    public void enterClassModifier(Java8Parser.ClassModifierContext ctx) {

    }

    @Override
    public void exitClassModifier(Java8Parser.ClassModifierContext ctx) {

    }

    @Override
    public void enterTypeParameters(Java8Parser.TypeParametersContext ctx) {

    }

    @Override
    public void exitTypeParameters(Java8Parser.TypeParametersContext ctx) {

    }

    @Override
    public void enterTypeParameterList(Java8Parser.TypeParameterListContext ctx) {

    }

    @Override
    public void exitTypeParameterList(Java8Parser.TypeParameterListContext ctx) {

    }

    @Override
    public void enterSuperclass(Java8Parser.SuperclassContext ctx) {

    }

    @Override
    public void exitSuperclass(Java8Parser.SuperclassContext ctx) {

    }

    @Override
    public void enterSuperinterfaces(Java8Parser.SuperinterfacesContext ctx) {

    }

    @Override
    public void exitSuperinterfaces(Java8Parser.SuperinterfacesContext ctx) {

    }

    @Override
    public void enterInterfaceTypeList(Java8Parser.InterfaceTypeListContext ctx) {

    }

    @Override
    public void exitInterfaceTypeList(Java8Parser.InterfaceTypeListContext ctx) {

    }

    @Override
    public void enterClassBody(Java8Parser.ClassBodyContext ctx) {

    }

    @Override
    public void exitClassBody(Java8Parser.ClassBodyContext ctx) {

    }

    @Override
    public void enterClassBodyDeclaration(Java8Parser.ClassBodyDeclarationContext ctx) {

    }

    @Override
    public void exitClassBodyDeclaration(Java8Parser.ClassBodyDeclarationContext ctx) {

    }

    @Override
    public void enterClassMemberDeclaration(Java8Parser.ClassMemberDeclarationContext ctx) {

    }

    @Override
    public void exitClassMemberDeclaration(Java8Parser.ClassMemberDeclarationContext ctx) {

    }

    /**
     * Este metodo no realiza la traduccion directa del codigo Java. Acumula las traducciones en unos
     * diccionarios, que se procesan al final de la traduccion del archivo. Esto se hace para manejar
     * el caso de las variables de instancia que en Python no se manejan a traves de palabras reservadas,
     * sino con el manejo de constructores.
     * La esta traduccion con delay se hace tambien para mantener las declaraciones de atributos y
     * constructores en la parte inicial del archivo de codigo fuente.
     * @param ctx the parse tree
     */
    @Override
    public void enterFieldDeclaration(Java8Parser.FieldDeclarationContext ctx) {
        // impl.
        boolean isStatic = false;
        if (ctx.fieldModifier() != null)
            for (Java8Parser.FieldModifierContext mod : ctx.fieldModifier())
                if (mod.getText().contains("static"))
                    isStatic = true;

        // https://docs.python.org/3/tutorial/classes.html#class-and-instance-variables
        for (Java8Parser.VariableDeclaratorContext varDcl : ctx.variableDeclaratorList().variableDeclarator()) {
            String identifier = varDcl.variableDeclaratorId().getText();
            String value;
            if (varDcl.variableInitializer() == null) {
                // Se declara pero no se inicializa. Se asigna valor por defecto.
                String type = ctx.unannType().getText().trim();
                value = getInitValue(type);
            } else {
                value = varDcl.variableInitializer().getText();
            }

            // a medida que se van procesando los atributos de clase, se van agregando a en un hash
            // para cuando se acabe la traduccion del archivo se agregen al inicio luego de la declaracion
            // de clase del archivo de salida.
            if (isStatic) {
                classVariables.put(identifier, value);
            } else {
                instanceVariables.put(identifier, value);
            }
        }
    }

    @Override
    public void exitFieldDeclaration(Java8Parser.FieldDeclarationContext ctx) {

    }

    @Override
    public void enterFieldModifier(Java8Parser.FieldModifierContext ctx) {

    }

    @Override
    public void exitFieldModifier(Java8Parser.FieldModifierContext ctx) {

    }

    @Override
    public void enterVariableDeclaratorList(Java8Parser.VariableDeclaratorListContext ctx) {

    }

    @Override
    public void exitVariableDeclaratorList(Java8Parser.VariableDeclaratorListContext ctx) {

    }

    @Override
    public void enterVariableDeclarator(Java8Parser.VariableDeclaratorContext ctx) {

    }

    @Override
    public void exitVariableDeclarator(Java8Parser.VariableDeclaratorContext ctx) {

    }

    @Override
    public void enterVariableDeclaratorId(Java8Parser.VariableDeclaratorIdContext ctx) {

    }

    @Override
    public void exitVariableDeclaratorId(Java8Parser.VariableDeclaratorIdContext ctx) {

    }

    @Override
    public void enterVariableInitializer(Java8Parser.VariableInitializerContext ctx) {

    }

    @Override
    public void exitVariableInitializer(Java8Parser.VariableInitializerContext ctx) {

    }

    @Override
    public void enterUnannType(Java8Parser.UnannTypeContext ctx) {

    }

    @Override
    public void exitUnannType(Java8Parser.UnannTypeContext ctx) {

    }

    @Override
    public void enterUnannPrimitiveType(Java8Parser.UnannPrimitiveTypeContext ctx) {

    }

    @Override
    public void exitUnannPrimitiveType(Java8Parser.UnannPrimitiveTypeContext ctx) {

    }

    @Override
    public void enterUnannReferenceType(Java8Parser.UnannReferenceTypeContext ctx) {

    }

    @Override
    public void exitUnannReferenceType(Java8Parser.UnannReferenceTypeContext ctx) {

    }

    @Override
    public void enterUnannClassOrInterfaceType(Java8Parser.UnannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitUnannClassOrInterfaceType(Java8Parser.UnannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterUnannClassType(Java8Parser.UnannClassTypeContext ctx) {

    }

    @Override
    public void exitUnannClassType(Java8Parser.UnannClassTypeContext ctx) {

    }

    @Override
    public void enterUnannClassType_lf_unannClassOrInterfaceType(Java8Parser.UnannClassType_lf_unannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitUnannClassType_lf_unannClassOrInterfaceType(Java8Parser.UnannClassType_lf_unannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterUnannClassType_lfno_unannClassOrInterfaceType(Java8Parser.UnannClassType_lfno_unannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitUnannClassType_lfno_unannClassOrInterfaceType(Java8Parser.UnannClassType_lfno_unannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterUnannInterfaceType(Java8Parser.UnannInterfaceTypeContext ctx) {

    }

    @Override
    public void exitUnannInterfaceType(Java8Parser.UnannInterfaceTypeContext ctx) {

    }

    @Override
    public void enterUnannInterfaceType_lf_unannClassOrInterfaceType(Java8Parser.UnannInterfaceType_lf_unannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitUnannInterfaceType_lf_unannClassOrInterfaceType(Java8Parser.UnannInterfaceType_lf_unannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterUnannInterfaceType_lfno_unannClassOrInterfaceType(Java8Parser.UnannInterfaceType_lfno_unannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void exitUnannInterfaceType_lfno_unannClassOrInterfaceType(Java8Parser.UnannInterfaceType_lfno_unannClassOrInterfaceTypeContext ctx) {

    }

    @Override
    public void enterUnannTypeVariable(Java8Parser.UnannTypeVariableContext ctx) {

    }

    @Override
    public void exitUnannTypeVariable(Java8Parser.UnannTypeVariableContext ctx) {

    }

    @Override
    public void enterUnannArrayType(Java8Parser.UnannArrayTypeContext ctx) {

    }

    @Override
    public void exitUnannArrayType(Java8Parser.UnannArrayTypeContext ctx) {

    }

    @Override
    public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        // impl.
        tabDepth++;
        String funName = ctx.methodHeader().methodDeclarator().Identifier().getText();
        List<String> paramNames = new LinkedList<>();
        try {
            for (Java8Parser.FormalParameterContext formalParameter : ctx.methodHeader().methodDeclarator().formalParameterList().formalParameters().formalParameter())
            paramNames.add(formalParameter.variableDeclaratorId().getText());
        } catch (NullPointerException npe) {
            // hay npe cuando la funcion es un solo argumento
        }

        try {
            paramNames.add(ctx.methodHeader().methodDeclarator().formalParameterList().lastFormalParameter().formalParameter().variableDeclaratorId().getText());
        } catch (NullPointerException npe) {
            // hay npe cuando la funcion no tiene argumentos
        }

        boolean isStatic = false;
        for (Java8Parser.MethodModifierContext mod : ctx.methodModifier()) {
            if (mod.getText().contains("static"))
                isStatic = true;
        }

        String funParams = String.join(", ", paramNames);
        String funDcl;
        if (isStatic) {
            classMethods.add(funName);
            funDcl = String.format("def %s(%s):", funName, funParams);
        } else {
            instanceMethods.add(funName);
            if (funParams.isEmpty())
                funDcl = String.format("def %s(self):", funName);
            else
                funDcl = String.format("def %s(self, %s):", funName, funParams);
        }
        appendln(funDcl);
    }

    @Override
    public void exitMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        // impl.
        tabDepth--;
        appendln(""); // agrega un espacio entre funciones
    }

    @Override
    public void enterMethodModifier(Java8Parser.MethodModifierContext ctx) {

    }

    @Override
    public void exitMethodModifier(Java8Parser.MethodModifierContext ctx) {

    }

    @Override
    public void enterMethodHeader(Java8Parser.MethodHeaderContext ctx) {

    }

    @Override
    public void exitMethodHeader(Java8Parser.MethodHeaderContext ctx) {

    }

    @Override
    public void enterResult(Java8Parser.ResultContext ctx) {

    }

    @Override
    public void exitResult(Java8Parser.ResultContext ctx) {

    }

    @Override
    public void enterMethodDeclarator(Java8Parser.MethodDeclaratorContext ctx) {

    }

    @Override
    public void exitMethodDeclarator(Java8Parser.MethodDeclaratorContext ctx) {

    }

    @Override
    public void enterFormalParameterList(Java8Parser.FormalParameterListContext ctx) {

    }

    @Override
    public void exitFormalParameterList(Java8Parser.FormalParameterListContext ctx) {

    }

    @Override
    public void enterFormalParameters(Java8Parser.FormalParametersContext ctx) {

    }

    @Override
    public void exitFormalParameters(Java8Parser.FormalParametersContext ctx) {

    }

    @Override
    public void enterFormalParameter(Java8Parser.FormalParameterContext ctx) {

    }

    @Override
    public void exitFormalParameter(Java8Parser.FormalParameterContext ctx) {

    }

    @Override
    public void enterVariableModifier(Java8Parser.VariableModifierContext ctx) {

    }

    @Override
    public void exitVariableModifier(Java8Parser.VariableModifierContext ctx) {

    }

    @Override
    public void enterLastFormalParameter(Java8Parser.LastFormalParameterContext ctx) {

    }

    @Override
    public void exitLastFormalParameter(Java8Parser.LastFormalParameterContext ctx) {

    }

    @Override
    public void enterReceiverParameter(Java8Parser.ReceiverParameterContext ctx) {

    }

    @Override
    public void exitReceiverParameter(Java8Parser.ReceiverParameterContext ctx) {

    }

    @Override
    public void enterThrows_(Java8Parser.Throws_Context ctx) {

    }

    @Override
    public void exitThrows_(Java8Parser.Throws_Context ctx) {

    }

    @Override
    public void enterExceptionTypeList(Java8Parser.ExceptionTypeListContext ctx) {

    }

    @Override
    public void exitExceptionTypeList(Java8Parser.ExceptionTypeListContext ctx) {

    }

    @Override
    public void enterExceptionType(Java8Parser.ExceptionTypeContext ctx) {

    }

    @Override
    public void exitExceptionType(Java8Parser.ExceptionTypeContext ctx) {

    }

    @Override
    public void enterMethodBody(Java8Parser.MethodBodyContext ctx) {

    }

    @Override
    public void exitMethodBody(Java8Parser.MethodBodyContext ctx) {

    }

    @Override
    public void enterInstanceInitializer(Java8Parser.InstanceInitializerContext ctx) {

    }

    @Override
    public void exitInstanceInitializer(Java8Parser.InstanceInitializerContext ctx) {

    }

    @Override
    public void enterStaticInitializer(Java8Parser.StaticInitializerContext ctx) {

    }

    @Override
    public void exitStaticInitializer(Java8Parser.StaticInitializerContext ctx) {

    }

    @Override
    public void enterConstructorDeclaration(Java8Parser.ConstructorDeclarationContext ctx) {

    }

    @Override
    public void exitConstructorDeclaration(Java8Parser.ConstructorDeclarationContext ctx) {

    }

    @Override
    public void enterConstructorModifier(Java8Parser.ConstructorModifierContext ctx) {

    }

    @Override
    public void exitConstructorModifier(Java8Parser.ConstructorModifierContext ctx) {

    }

    @Override
    public void enterConstructorDeclarator(Java8Parser.ConstructorDeclaratorContext ctx) {

    }

    @Override
    public void exitConstructorDeclarator(Java8Parser.ConstructorDeclaratorContext ctx) {

    }

    @Override
    public void enterSimpleTypeName(Java8Parser.SimpleTypeNameContext ctx) {

    }

    @Override
    public void exitSimpleTypeName(Java8Parser.SimpleTypeNameContext ctx) {

    }

    @Override
    public void enterConstructorBody(Java8Parser.ConstructorBodyContext ctx) {

    }

    @Override
    public void exitConstructorBody(Java8Parser.ConstructorBodyContext ctx) {

    }

    @Override
    public void enterExplicitConstructorInvocation(Java8Parser.ExplicitConstructorInvocationContext ctx) {

    }

    @Override
    public void exitExplicitConstructorInvocation(Java8Parser.ExplicitConstructorInvocationContext ctx) {

    }

    @Override
    public void enterEnumDeclaration(Java8Parser.EnumDeclarationContext ctx) {

    }

    @Override
    public void exitEnumDeclaration(Java8Parser.EnumDeclarationContext ctx) {

    }

    @Override
    public void enterEnumBody(Java8Parser.EnumBodyContext ctx) {

    }

    @Override
    public void exitEnumBody(Java8Parser.EnumBodyContext ctx) {

    }

    @Override
    public void enterEnumConstantList(Java8Parser.EnumConstantListContext ctx) {

    }

    @Override
    public void exitEnumConstantList(Java8Parser.EnumConstantListContext ctx) {

    }

    @Override
    public void enterEnumConstant(Java8Parser.EnumConstantContext ctx) {

    }

    @Override
    public void exitEnumConstant(Java8Parser.EnumConstantContext ctx) {

    }

    @Override
    public void enterEnumConstantModifier(Java8Parser.EnumConstantModifierContext ctx) {

    }

    @Override
    public void exitEnumConstantModifier(Java8Parser.EnumConstantModifierContext ctx) {

    }

    @Override
    public void enterEnumBodyDeclarations(Java8Parser.EnumBodyDeclarationsContext ctx) {

    }

    @Override
    public void exitEnumBodyDeclarations(Java8Parser.EnumBodyDeclarationsContext ctx) {

    }

    @Override
    public void enterInterfaceDeclaration(Java8Parser.InterfaceDeclarationContext ctx) {

    }

    @Override
    public void exitInterfaceDeclaration(Java8Parser.InterfaceDeclarationContext ctx) {

    }

    @Override
    public void enterNormalInterfaceDeclaration(Java8Parser.NormalInterfaceDeclarationContext ctx) {

    }

    @Override
    public void exitNormalInterfaceDeclaration(Java8Parser.NormalInterfaceDeclarationContext ctx) {

    }

    @Override
    public void enterInterfaceModifier(Java8Parser.InterfaceModifierContext ctx) {

    }

    @Override
    public void exitInterfaceModifier(Java8Parser.InterfaceModifierContext ctx) {

    }

    @Override
    public void enterExtendsInterfaces(Java8Parser.ExtendsInterfacesContext ctx) {

    }

    @Override
    public void exitExtendsInterfaces(Java8Parser.ExtendsInterfacesContext ctx) {

    }

    @Override
    public void enterInterfaceBody(Java8Parser.InterfaceBodyContext ctx) {

    }

    @Override
    public void exitInterfaceBody(Java8Parser.InterfaceBodyContext ctx) {

    }

    @Override
    public void enterInterfaceMemberDeclaration(Java8Parser.InterfaceMemberDeclarationContext ctx) {

    }

    @Override
    public void exitInterfaceMemberDeclaration(Java8Parser.InterfaceMemberDeclarationContext ctx) {

    }

    @Override
    public void enterConstantDeclaration(Java8Parser.ConstantDeclarationContext ctx) {

    }

    @Override
    public void exitConstantDeclaration(Java8Parser.ConstantDeclarationContext ctx) {

    }

    @Override
    public void enterConstantModifier(Java8Parser.ConstantModifierContext ctx) {

    }

    @Override
    public void exitConstantModifier(Java8Parser.ConstantModifierContext ctx) {

    }

    @Override
    public void enterInterfaceMethodDeclaration(Java8Parser.InterfaceMethodDeclarationContext ctx) {

    }

    @Override
    public void exitInterfaceMethodDeclaration(Java8Parser.InterfaceMethodDeclarationContext ctx) {

    }

    @Override
    public void enterInterfaceMethodModifier(Java8Parser.InterfaceMethodModifierContext ctx) {

    }

    @Override
    public void exitInterfaceMethodModifier(Java8Parser.InterfaceMethodModifierContext ctx) {

    }

    @Override
    public void enterAnnotationTypeDeclaration(Java8Parser.AnnotationTypeDeclarationContext ctx) {

    }

    @Override
    public void exitAnnotationTypeDeclaration(Java8Parser.AnnotationTypeDeclarationContext ctx) {

    }

    @Override
    public void enterAnnotationTypeBody(Java8Parser.AnnotationTypeBodyContext ctx) {

    }

    @Override
    public void exitAnnotationTypeBody(Java8Parser.AnnotationTypeBodyContext ctx) {

    }

    @Override
    public void enterAnnotationTypeMemberDeclaration(Java8Parser.AnnotationTypeMemberDeclarationContext ctx) {

    }

    @Override
    public void exitAnnotationTypeMemberDeclaration(Java8Parser.AnnotationTypeMemberDeclarationContext ctx) {

    }

    @Override
    public void enterAnnotationTypeElementDeclaration(Java8Parser.AnnotationTypeElementDeclarationContext ctx) {

    }

    @Override
    public void exitAnnotationTypeElementDeclaration(Java8Parser.AnnotationTypeElementDeclarationContext ctx) {

    }

    @Override
    public void enterAnnotationTypeElementModifier(Java8Parser.AnnotationTypeElementModifierContext ctx) {

    }

    @Override
    public void exitAnnotationTypeElementModifier(Java8Parser.AnnotationTypeElementModifierContext ctx) {

    }

    @Override
    public void enterDefaultValue(Java8Parser.DefaultValueContext ctx) {

    }

    @Override
    public void exitDefaultValue(Java8Parser.DefaultValueContext ctx) {

    }

    @Override
    public void enterAnnotation(Java8Parser.AnnotationContext ctx) {

    }

    @Override
    public void exitAnnotation(Java8Parser.AnnotationContext ctx) {

    }

    @Override
    public void enterNormalAnnotation(Java8Parser.NormalAnnotationContext ctx) {

    }

    @Override
    public void exitNormalAnnotation(Java8Parser.NormalAnnotationContext ctx) {

    }

    @Override
    public void enterElementValuePairList(Java8Parser.ElementValuePairListContext ctx) {

    }

    @Override
    public void exitElementValuePairList(Java8Parser.ElementValuePairListContext ctx) {

    }

    @Override
    public void enterElementValuePair(Java8Parser.ElementValuePairContext ctx) {

    }

    @Override
    public void exitElementValuePair(Java8Parser.ElementValuePairContext ctx) {

    }

    @Override
    public void enterElementValue(Java8Parser.ElementValueContext ctx) {

    }

    @Override
    public void exitElementValue(Java8Parser.ElementValueContext ctx) {

    }

    @Override
    public void enterElementValueArrayInitializer(Java8Parser.ElementValueArrayInitializerContext ctx) {

    }

    @Override
    public void exitElementValueArrayInitializer(Java8Parser.ElementValueArrayInitializerContext ctx) {

    }

    @Override
    public void enterElementValueList(Java8Parser.ElementValueListContext ctx) {

    }

    @Override
    public void exitElementValueList(Java8Parser.ElementValueListContext ctx) {

    }

    @Override
    public void enterMarkerAnnotation(Java8Parser.MarkerAnnotationContext ctx) {

    }

    @Override
    public void exitMarkerAnnotation(Java8Parser.MarkerAnnotationContext ctx) {

    }

    @Override
    public void enterSingleElementAnnotation(Java8Parser.SingleElementAnnotationContext ctx) {

    }

    @Override
    public void exitSingleElementAnnotation(Java8Parser.SingleElementAnnotationContext ctx) {

    }

    @Override
    public void enterArrayInitializer(Java8Parser.ArrayInitializerContext ctx) {

    }

    @Override
    public void exitArrayInitializer(Java8Parser.ArrayInitializerContext ctx) {

    }

    @Override
    public void enterVariableInitializerList(Java8Parser.VariableInitializerListContext ctx) {

    }

    @Override
    public void exitVariableInitializerList(Java8Parser.VariableInitializerListContext ctx) {

    }

    @Override
    public void enterBlock(Java8Parser.BlockContext ctx) {
        // impl.
        tabDepth++;
        // Si el bloque esta vacio...
        if (ctx.blockStatements() == null)
            appendln("pass");

    }

    @Override
    public void exitBlock(Java8Parser.BlockContext ctx) {
        // impl.
        tabDepth--;
    }

    @Override
    public void enterBlockStatements(Java8Parser.BlockStatementsContext ctx) {
        if (ctx.getParent().getClass().getName() == "Java8Parser$SwitchBlockStatementGroupContext"){
            //appendln();
            tabDepth++;
        } else {

        }
    }

    @Override
    public void exitBlockStatements(Java8Parser.BlockStatementsContext ctx) {
        if (ctx.getParent().getClass().getName() == "Java8Parser$SwitchBlockStatementGroupContext"){
            tabDepth--;
        } else {

        }
    }

    @Override
    public void enterBlockStatement(Java8Parser.BlockStatementContext ctx) {

    }

    @Override
    public void exitBlockStatement(Java8Parser.BlockStatementContext ctx) {

    }

    @Override
    public void enterLocalVariableDeclarationStatement(Java8Parser.LocalVariableDeclarationStatementContext ctx) {

    }

    @Override
    public void exitLocalVariableDeclarationStatement(Java8Parser.LocalVariableDeclarationStatementContext ctx) {

    }

    @Override
    public void enterLocalVariableDeclaration(Java8Parser.LocalVariableDeclarationContext ctx) {
        // impl.
        if (hasParent(ctx, "ForStatementContext")) {
            // En el metodo 'basicForStatement' se maneja la variable principal de iteracion.
            // Esta condicion se deja para no imprimir nada, pues es una declaracion y el parser
            // coje por aqui.
        }
        else if (ctx.getText().contains("newScanner(")) {
            // Quiere decir que se ha declarado un objeto para leer, así que se deja la variable indicadora de entrada
            // en activa para no confundir con cualquier otro llamado de funcion. No se traduce porque en python
            // no es necesario declarar un objeto para leer desde la entrada estandar.
            scannerWasDeclared = true;
        }
        else {
            String type = ctx.unannType().getText();
            // se hace ciclo para declaraciones de la forma:
            // Type t1 = 93, t2, t3 = 4;
            for (Java8Parser.VariableDeclaratorContext variableDeclarator : ctx.variableDeclaratorList().variableDeclarator()) {
                String identifier = variableDeclarator.variableDeclaratorId().getText();
                String value;
                try {
                    // se hace llamado para intentar lanzar nullpointerexception para indicar que la variable no se inizializa y
                    // por tanto corresponde a nosotros inicializarla para compatibilidad con el lenguaje.
                    value = variableDeclarator.variableInitializer().getText();
                    if (value.contains("new"))
                        value = value.substring(3);
                    else if (scannerWasDeclared && (value.contains(".nextLine") || value.contains(".nextInt") || value.contains(".nextDouble") || value.contains(".nextFloat")))
                        value = "input()";
                } catch (NullPointerException npe) {
                    // este caso sucede cuando no se declara la variable y no se inicializa.
                    value = getInitValue(type);
                }
                appendln(String.format("%s = %s", identifier, value));
            }
        }
    }

    @Override
    public void exitLocalVariableDeclaration(Java8Parser.LocalVariableDeclarationContext ctx) {

    }

    @Override
    public void enterStatement(Java8Parser.StatementContext ctx) {

    }

    @Override
    public void exitStatement(Java8Parser.StatementContext ctx) {

    }

    @Override
    public void enterStatementNoShortIf(Java8Parser.StatementNoShortIfContext ctx) {

    }

    @Override
    public void exitStatementNoShortIf(Java8Parser.StatementNoShortIfContext ctx) {
        appendln("else:");
    }

    @Override
    public void enterStatementWithoutTrailingSubstatement(Java8Parser.StatementWithoutTrailingSubstatementContext ctx) {

    }

    @Override
    public void exitStatementWithoutTrailingSubstatement(Java8Parser.StatementWithoutTrailingSubstatementContext ctx) {

    }

    @Override
    public void enterEmptyStatement(Java8Parser.EmptyStatementContext ctx) {

    }

    @Override
    public void exitEmptyStatement(Java8Parser.EmptyStatementContext ctx) {

    }

    @Override
    public void enterLabeledStatement(Java8Parser.LabeledStatementContext ctx) {

    }

    @Override
    public void exitLabeledStatement(Java8Parser.LabeledStatementContext ctx) {

    }

    @Override
    public void enterLabeledStatementNoShortIf(Java8Parser.LabeledStatementNoShortIfContext ctx) {

    }

    @Override
    public void exitLabeledStatementNoShortIf(Java8Parser.LabeledStatementNoShortIfContext ctx) {

    }

    @Override
    public void enterExpressionStatement(Java8Parser.ExpressionStatementContext ctx) {
        if (hasParent(ctx, "SwitchBlockContext")) {
            //Para expresiones en switch, falta pulir pero por ahora todo bien
            String switchExpression = ctx.getText();
            char lastChar = switchExpression.charAt(switchExpression.length() - 1);
            if(lastChar==';'){
                switchExpression = switchExpression.substring(0, switchExpression.length() - 1);
            }
            appendln(switchExpression);
        } else {
            //
        }
    }

    @Override
    public void exitExpressionStatement(Java8Parser.ExpressionStatementContext ctx) {

    }

    @Override
    public void enterStatementExpression(Java8Parser.StatementExpressionContext ctx) {

    }

    @Override
    public void exitStatementExpression(Java8Parser.StatementExpressionContext ctx) {

    }

    @Override
    public void enterIfThenStatement(Java8Parser.IfThenStatementContext ctx) {
        String ifExpr = ctx.expression().getText();
        String ifDcl = String.format("if (%s):", ifExpr);
        appendln(ifDcl);
    }

    @Override
    public void exitIfThenStatement(Java8Parser.IfThenStatementContext ctx) {

    }

    @Override
    public void enterIfThenElseStatement(Java8Parser.IfThenElseStatementContext ctx) {
        boolean noShortIfParent = hasParent(ctx, "StatementContext");

        if (noShortIfParent) {
            tabDepth++;
        }
        String ifExpr = ctx.expression().getText();
        String ifDcl = String.format("if (%s):", ifExpr);
        appendln(ifDcl);

    }

    @Override
    public void exitIfThenElseStatement(Java8Parser.IfThenElseStatementContext ctx) {
        boolean noShortIfParent = hasParent(ctx, "StatementContext");
        if (noShortIfParent) {
            tabDepth--;
        }
    }

    @Override
    public void enterIfThenElseStatementNoShortIf(Java8Parser.IfThenElseStatementNoShortIfContext ctx) {
        String ifExpr = ctx.expression().getText();
        String ifDcl = String.format("if (%s):", ifExpr);
        appendln(ifDcl);
    }

    @Override
    public void exitIfThenElseStatementNoShortIf(Java8Parser.IfThenElseStatementNoShortIfContext ctx) {

    }

    @Override
    public void enterAssertStatement(Java8Parser.AssertStatementContext ctx) {

    }

    @Override
    public void exitAssertStatement(Java8Parser.AssertStatementContext ctx) {

    }

    @Override
    public void enterSwitchStatement(Java8Parser.SwitchStatementContext ctx) {
        switchVariable.push(ctx.expression().getText());
        switchState.push(0);
    }

    @Override
    public void exitSwitchStatement(Java8Parser.SwitchStatementContext ctx) {

    }

    @Override
    public void enterSwitchBlock(Java8Parser.SwitchBlockContext ctx) {

    }

    @Override
    public void exitSwitchBlock(Java8Parser.SwitchBlockContext ctx) {

    }

    @Override
    public void enterSwitchBlockStatementGroup(Java8Parser.SwitchBlockStatementGroupContext ctx) {

    }

    @Override
    public void exitSwitchBlockStatementGroup(Java8Parser.SwitchBlockStatementGroupContext ctx) {

    }

    @Override
    public void enterSwitchLabels(Java8Parser.SwitchLabelsContext ctx) {

    }

    @Override
    public void exitSwitchLabels(Java8Parser.SwitchLabelsContext ctx) {

    }

    @Override
    public void enterSwitchLabel(Java8Parser.SwitchLabelContext ctx) {
        String caseDefault = ctx.getChild(0).getText().split(" ")[0];
        int actualState = switchState.peek();
        if (caseDefault.equals("case")){
            if (actualState == 0){
                String value = ctx.getChild(1).getText();
                String switchLab = String.format("if (%s == %s):", switchVariable.peek(),value);
                switchState.pop();
                switchState.push(1);
                appendln(switchLab);
            } else{
                String value = ctx.getChild(1).getText();
                String switchLab = String.format("elif (%s == %s):", switchVariable.peek(),value);
                appendln(switchLab);
            }
        }else if (caseDefault.equals("default")){
            String value = ctx.getChild(1).getText();
            String switchLab = String.format("else:", switchVariable.pop(),value);
            switchState.pop();
            appendln(switchLab);
        }
    }

    @Override
    public void exitSwitchLabel(Java8Parser.SwitchLabelContext ctx) {

    }

    @Override
    public void enterEnumConstantName(Java8Parser.EnumConstantNameContext ctx) {

    }

    @Override
    public void exitEnumConstantName(Java8Parser.EnumConstantNameContext ctx) {

    }

    @Override
    public void enterWhileStatement(Java8Parser.WhileStatementContext ctx) {
        String whileExpr = ctx.expression().getText();
        String whileDcl = String.format("while %s:", whileExpr);
        appendln(whileDcl);
    }

    @Override
    public void exitWhileStatement(Java8Parser.WhileStatementContext ctx) {

    }

    @Override
    public void enterWhileStatementNoShortIf(Java8Parser.WhileStatementNoShortIfContext ctx) {

    }

    @Override
    public void exitWhileStatementNoShortIf(Java8Parser.WhileStatementNoShortIfContext ctx) {

    }

    @Override
    public void enterDoStatement(Java8Parser.DoStatementContext ctx) {

    }

    @Override
    public void exitDoStatement(Java8Parser.DoStatementContext ctx) {

    }

    @Override
    public void enterForStatement(Java8Parser.ForStatementContext ctx) {

    }

    @Override
    public void exitForStatement(Java8Parser.ForStatementContext ctx) {

    }

    @Override
    public void enterForStatementNoShortIf(Java8Parser.ForStatementNoShortIfContext ctx) {

    }

    @Override
    public void exitForStatementNoShortIf(Java8Parser.ForStatementNoShortIfContext ctx) {

    }

    /**
     * La conversion a ciclos se hace mediante un while porque el for en Python itera a traves de un lista de 'indices'
     * predefinida y no es común editarla. En cambio, el for de Java es posible que el cuerpo del ciclo altere la variable
     * de iteracion. Esta es la razon por la cual la conversion se hace con ciclos while.
     * @param ctx the parse tree
     */
    @Override
    public void enterBasicForStatement(Java8Parser.BasicForStatementContext ctx) {
        // impl.
        // VARIABLE DE ITERACION
        // Obtener variable de control de iteracion
        String identifier;
        String initVal;
        if (ctx.forInit() != null) {
            Java8Parser.LocalVariableDeclarationContext localVariableDeclaration = ctx.forInit().localVariableDeclaration();
            if (localVariableDeclaration != null) {
                Java8Parser.VariableDeclaratorContext variableDeclarator = localVariableDeclaration.variableDeclaratorList().variableDeclarator(0);
                identifier = variableDeclarator.variableDeclaratorId().getText();
                if (variableDeclarator.variableInitializer() != null) {
                    initVal = variableDeclarator.variableInitializer().getText();
                } else {
                    // La variable se ha declarado pero no se ha inicializado dentro del ciclo, por ejemplo:
                    // for(double k; k < 100; i--).
                    // Se supone inicializacion por defecto de la variable
                    String type = localVariableDeclaration.unannType().getText();
                    initVal = getInitValue(type);
                }
            } else {
                // Se asigna pero no se declara una nueva variable de control para el ciclo, por ejemplo:
                // for(k = 2; k < 100; k--) {}
                identifier = ctx.forInit().statementExpressionList().statementExpression(0).assignment().leftHandSide().getText();
                initVal = ctx.forInit().statementExpressionList().statementExpression(0).assignment().expression().getText();
            }
            appendln(String.format("%s = %s", identifier, initVal));
        } else {
            // No se ha declarado la variable de iteracion dentro del for loop. Por ejemplo:
            // for(; i < 10; i++)
            // Se supone que se ha declarado antes de esta proposion for.
        }

        // CONDICION DE PARADA
        // Traducir ciclo y expresion de finalizacion
        String endCondition = ctx.expression().getText();
        endCondition = replaceBooleanOps(endCondition);
        appendln(String.format("while %s:", endCondition));
    }

    @Override
    public void exitBasicForStatement(Java8Parser.BasicForStatementContext ctx) {
        // impl.
        // EXPRESION DE FINALIZACION
        // Salir del la estructura for implica haber salido del bloque interno, por lo que se aumenta y reduce la
        // identacion para que quede en el dominio del ciclo. Se recomienda leer la documentacion en este punto
        // si no se entiende en la seccion de 'estructura'.
        tabDepth++;
        // Saber si el incremento o decremeto es pre o post no interesa porque la instruccion siempre se ejecuta al
        // final tanto en el for original como en el while traducido. Por lo tanto no conviente hacer la verificacion.
        String updateSmnt = ctx.forUpdate().getText();
        // lo que hago es coger el incremento del for:
        // for (;; i++)
        // for (;; i = i + 2)
        if (updateSmnt.contains("++")) {
            String varName = updateSmnt.replace("+", "");
            updateSmnt = varName + "+= 1";
        } else if (updateSmnt.contains("--")) {
            String varName = updateSmnt.replace("-", "");
            updateSmnt = varName + "-= 1";
        }
        appendln(updateSmnt);
        appendln("");
        tabDepth--;
    }

    @Override
    public void enterBasicForStatementNoShortIf(Java8Parser.BasicForStatementNoShortIfContext ctx) {

    }

    @Override
    public void exitBasicForStatementNoShortIf(Java8Parser.BasicForStatementNoShortIfContext ctx) {

    }

    @Override
    public void enterForInit(Java8Parser.ForInitContext ctx) {

    }

    @Override
    public void exitForInit(Java8Parser.ForInitContext ctx) {

    }

    @Override
    public void enterForUpdate(Java8Parser.ForUpdateContext ctx) {

    }

    @Override
    public void exitForUpdate(Java8Parser.ForUpdateContext ctx) {

    }

    @Override
    public void enterStatementExpressionList(Java8Parser.StatementExpressionListContext ctx) {

    }

    @Override
    public void exitStatementExpressionList(Java8Parser.StatementExpressionListContext ctx) {

    }

    @Override
    public void enterEnhancedForStatement(Java8Parser.EnhancedForStatementContext ctx) {

    }

    @Override
    public void exitEnhancedForStatement(Java8Parser.EnhancedForStatementContext ctx) {

    }

    @Override
    public void enterEnhancedForStatementNoShortIf(Java8Parser.EnhancedForStatementNoShortIfContext ctx) {

    }

    @Override
    public void exitEnhancedForStatementNoShortIf(Java8Parser.EnhancedForStatementNoShortIfContext ctx) {

    }

    @Override
    public void enterBreakStatement(Java8Parser.BreakStatementContext ctx) {
        //Estaba vacío, pongo el condicional para que en caso de que alguien lo use, para switch debe ignorarse el break;
        if (hasParent(ctx, "SwitchBlockContext")){
            //
        } else {
            //
        }
    }

    @Override
    public void exitBreakStatement(Java8Parser.BreakStatementContext ctx) {

    }

    @Override
    public void enterContinueStatement(Java8Parser.ContinueStatementContext ctx) {

    }

    @Override
    public void exitContinueStatement(Java8Parser.ContinueStatementContext ctx) {

    }

    @Override
    public void enterReturnStatement(Java8Parser.ReturnStatementContext ctx) {

    }

    @Override
    public void exitReturnStatement(Java8Parser.ReturnStatementContext ctx) {

    }

    @Override
    public void enterThrowStatement(Java8Parser.ThrowStatementContext ctx) {

    }

    @Override
    public void exitThrowStatement(Java8Parser.ThrowStatementContext ctx) {

    }

    @Override
    public void enterSynchronizedStatement(Java8Parser.SynchronizedStatementContext ctx) {

    }

    @Override
    public void exitSynchronizedStatement(Java8Parser.SynchronizedStatementContext ctx) {

    }

    @Override
    public void enterTryStatement(Java8Parser.TryStatementContext ctx) {

    }

    @Override
    public void exitTryStatement(Java8Parser.TryStatementContext ctx) {

    }

    @Override
    public void enterCatches(Java8Parser.CatchesContext ctx) {

    }

    @Override
    public void exitCatches(Java8Parser.CatchesContext ctx) {

    }

    @Override
    public void enterCatchClause(Java8Parser.CatchClauseContext ctx) {

    }

    @Override
    public void exitCatchClause(Java8Parser.CatchClauseContext ctx) {

    }

    @Override
    public void enterCatchFormalParameter(Java8Parser.CatchFormalParameterContext ctx) {

    }

    @Override
    public void exitCatchFormalParameter(Java8Parser.CatchFormalParameterContext ctx) {

    }

    @Override
    public void enterCatchType(Java8Parser.CatchTypeContext ctx) {

    }

    @Override
    public void exitCatchType(Java8Parser.CatchTypeContext ctx) {

    }

    @Override
    public void enterFinally_(Java8Parser.Finally_Context ctx) {

    }

    @Override
    public void exitFinally_(Java8Parser.Finally_Context ctx) {

    }

    @Override
    public void enterTryWithResourcesStatement(Java8Parser.TryWithResourcesStatementContext ctx) {

    }

    @Override
    public void exitTryWithResourcesStatement(Java8Parser.TryWithResourcesStatementContext ctx) {

    }

    @Override
    public void enterResourceSpecification(Java8Parser.ResourceSpecificationContext ctx) {

    }

    @Override
    public void exitResourceSpecification(Java8Parser.ResourceSpecificationContext ctx) {

    }

    @Override
    public void enterResourceList(Java8Parser.ResourceListContext ctx) {

    }

    @Override
    public void exitResourceList(Java8Parser.ResourceListContext ctx) {

    }

    @Override
    public void enterResource(Java8Parser.ResourceContext ctx) {

    }

    @Override
    public void exitResource(Java8Parser.ResourceContext ctx) {

    }

    @Override
    public void enterPrimary(Java8Parser.PrimaryContext ctx) {

    }

    @Override
    public void exitPrimary(Java8Parser.PrimaryContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray(Java8Parser.PrimaryNoNewArrayContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray(Java8Parser.PrimaryNoNewArrayContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray_lf_arrayAccess(Java8Parser.PrimaryNoNewArray_lf_arrayAccessContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray_lf_arrayAccess(Java8Parser.PrimaryNoNewArray_lf_arrayAccessContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray_lfno_arrayAccess(Java8Parser.PrimaryNoNewArray_lfno_arrayAccessContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray_lfno_arrayAccess(Java8Parser.PrimaryNoNewArray_lfno_arrayAccessContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray_lf_primary(Java8Parser.PrimaryNoNewArray_lf_primaryContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray_lf_primary(Java8Parser.PrimaryNoNewArray_lf_primaryContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(Java8Parser.PrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primaryContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(Java8Parser.PrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primaryContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary(Java8Parser.PrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primaryContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary(Java8Parser.PrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primaryContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray_lfno_primary(Java8Parser.PrimaryNoNewArray_lfno_primaryContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray_lfno_primary(Java8Parser.PrimaryNoNewArray_lfno_primaryContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary(Java8Parser.PrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primaryContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary(Java8Parser.PrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primaryContext ctx) {

    }

    @Override
    public void enterPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(Java8Parser.PrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primaryContext ctx) {

    }

    @Override
    public void exitPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(Java8Parser.PrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primaryContext ctx) {

    }

    @Override
    public void enterClassInstanceCreationExpression(Java8Parser.ClassInstanceCreationExpressionContext ctx) {

    }

    @Override
    public void exitClassInstanceCreationExpression(Java8Parser.ClassInstanceCreationExpressionContext ctx) {

    }

    @Override
    public void enterClassInstanceCreationExpression_lf_primary(Java8Parser.ClassInstanceCreationExpression_lf_primaryContext ctx) {

    }

    @Override
    public void exitClassInstanceCreationExpression_lf_primary(Java8Parser.ClassInstanceCreationExpression_lf_primaryContext ctx) {

    }

    @Override
    public void enterClassInstanceCreationExpression_lfno_primary(Java8Parser.ClassInstanceCreationExpression_lfno_primaryContext ctx) {

    }

    @Override
    public void exitClassInstanceCreationExpression_lfno_primary(Java8Parser.ClassInstanceCreationExpression_lfno_primaryContext ctx) {

    }

    @Override
    public void enterTypeArgumentsOrDiamond(Java8Parser.TypeArgumentsOrDiamondContext ctx) {

    }

    @Override
    public void exitTypeArgumentsOrDiamond(Java8Parser.TypeArgumentsOrDiamondContext ctx) {

    }

    @Override
    public void enterFieldAccess(Java8Parser.FieldAccessContext ctx) {

    }

    @Override
    public void exitFieldAccess(Java8Parser.FieldAccessContext ctx) {

    }

    @Override
    public void enterFieldAccess_lf_primary(Java8Parser.FieldAccess_lf_primaryContext ctx) {

    }

    @Override
    public void exitFieldAccess_lf_primary(Java8Parser.FieldAccess_lf_primaryContext ctx) {

    }

    @Override
    public void enterFieldAccess_lfno_primary(Java8Parser.FieldAccess_lfno_primaryContext ctx) {

    }

    @Override
    public void exitFieldAccess_lfno_primary(Java8Parser.FieldAccess_lfno_primaryContext ctx) {

    }

    @Override
    public void enterArrayAccess(Java8Parser.ArrayAccessContext ctx) {

    }

    @Override
    public void exitArrayAccess(Java8Parser.ArrayAccessContext ctx) {

    }

    @Override
    public void enterArrayAccess_lf_primary(Java8Parser.ArrayAccess_lf_primaryContext ctx) {

    }

    @Override
    public void exitArrayAccess_lf_primary(Java8Parser.ArrayAccess_lf_primaryContext ctx) {

    }

    @Override
    public void enterArrayAccess_lfno_primary(Java8Parser.ArrayAccess_lfno_primaryContext ctx) {

    }

    @Override
    public void exitArrayAccess_lfno_primary(Java8Parser.ArrayAccess_lfno_primaryContext ctx) {

    }

    @Override
    public void enterMethodInvocation(Java8Parser.MethodInvocationContext ctx) {
        // impl.
        String args;
        try {
            String[] argVals = ctx.argumentList().getText().split(",");
            for (int i = 0; i < argVals.length; i++) {
                String val = argVals[i].trim();
                if (classVariables.containsKey(val)) {
                    argVals[i] = String.format("%s.%s", compilationUnitName, val);
                } else if (instanceVariables.containsKey(val)) {
                    argVals[i] = String.format("self.%s", val);
                }
            }
            args = String.join(", ", argVals);
        } catch (NullPointerException npe) {
            // se trata de una funcion sin argumentos
            args = "";
        }

        // convertir el llamado de la funcion
        if (ctx.getText().contains("System.out.println")) {
            appendln(String.format("print(%s)", args));
            return;
        } else {
            String methodInvocationExpr;
            if (ctx.methodName() == null) {
                // invocacion de metodos de instancia (no estaticos):
                // sucede cuando el llamado NO se hace directamente a traves del identificador del metodo como en
                // 'nombreDelMetodo(argumento, otro)' sino que se realiza a traves de una instancia como en
                // 'unaInstancia.nombreDelMetodo(argumento, otro)'
                String objName = ctx.typeName().getText();
                String methodName = ctx.Identifier().getText();
                methodInvocationExpr = String.format("%s.%s(%s)", objName, methodName, args);
            } else {
                String methodName = ctx.methodName().getText();
                boolean isAnStaticInvocation = classMethods.contains(methodName);
                if (isAnStaticInvocation) {
                    methodInvocationExpr = String.format("%s.%s(%s)", compilationUnitName, methodName, args);
                } else {
                    methodInvocationExpr = String.format("self.%s(%s)", methodName, args);
                }
            }
            appendln(methodInvocationExpr);
        }
    }

    @Override
    public void exitMethodInvocation(Java8Parser.MethodInvocationContext ctx) {

    }

    @Override
    public void enterMethodInvocation_lf_primary(Java8Parser.MethodInvocation_lf_primaryContext ctx) {

    }

    @Override
    public void exitMethodInvocation_lf_primary(Java8Parser.MethodInvocation_lf_primaryContext ctx) {

    }

    @Override
    public void enterMethodInvocation_lfno_primary(Java8Parser.MethodInvocation_lfno_primaryContext ctx) {

    }

    @Override
    public void exitMethodInvocation_lfno_primary(Java8Parser.MethodInvocation_lfno_primaryContext ctx) {

    }

    @Override
    public void enterArgumentList(Java8Parser.ArgumentListContext ctx) {

    }

    @Override
    public void exitArgumentList(Java8Parser.ArgumentListContext ctx) {

    }

    @Override
    public void enterMethodReference(Java8Parser.MethodReferenceContext ctx) {

    }

    @Override
    public void exitMethodReference(Java8Parser.MethodReferenceContext ctx) {

    }

    @Override
    public void enterMethodReference_lf_primary(Java8Parser.MethodReference_lf_primaryContext ctx) {

    }

    @Override
    public void exitMethodReference_lf_primary(Java8Parser.MethodReference_lf_primaryContext ctx) {

    }

    @Override
    public void enterMethodReference_lfno_primary(Java8Parser.MethodReference_lfno_primaryContext ctx) {

    }

    @Override
    public void exitMethodReference_lfno_primary(Java8Parser.MethodReference_lfno_primaryContext ctx) {

    }

    @Override
    public void enterArrayCreationExpression(Java8Parser.ArrayCreationExpressionContext ctx) {

    }

    @Override
    public void exitArrayCreationExpression(Java8Parser.ArrayCreationExpressionContext ctx) {

    }

    @Override
    public void enterDimExprs(Java8Parser.DimExprsContext ctx) {

    }

    @Override
    public void exitDimExprs(Java8Parser.DimExprsContext ctx) {

    }

    @Override
    public void enterDimExpr(Java8Parser.DimExprContext ctx) {

    }

    @Override
    public void exitDimExpr(Java8Parser.DimExprContext ctx) {

    }

    @Override
    public void enterConstantExpression(Java8Parser.ConstantExpressionContext ctx) {

    }

    @Override
    public void exitConstantExpression(Java8Parser.ConstantExpressionContext ctx) {

    }

    @Override
    public void enterExpression(Java8Parser.ExpressionContext ctx) {

    }

    @Override
    public void exitExpression(Java8Parser.ExpressionContext ctx) {

    }

    @Override
    public void enterLambdaExpression(Java8Parser.LambdaExpressionContext ctx) {

    }

    @Override
    public void exitLambdaExpression(Java8Parser.LambdaExpressionContext ctx) {

    }

    @Override
    public void enterLambdaParameters(Java8Parser.LambdaParametersContext ctx) {

    }

    @Override
    public void exitLambdaParameters(Java8Parser.LambdaParametersContext ctx) {

    }

    @Override
    public void enterInferredFormalParameterList(Java8Parser.InferredFormalParameterListContext ctx) {

    }

    @Override
    public void exitInferredFormalParameterList(Java8Parser.InferredFormalParameterListContext ctx) {

    }

    @Override
    public void enterLambdaBody(Java8Parser.LambdaBodyContext ctx) {

    }

    @Override
    public void exitLambdaBody(Java8Parser.LambdaBodyContext ctx) {

    }

    @Override
    public void enterAssignmentExpression(Java8Parser.AssignmentExpressionContext ctx) {

    }

    @Override
    public void exitAssignmentExpression(Java8Parser.AssignmentExpressionContext ctx) {

    }

    @Override
    public void enterAssignment(Java8Parser.AssignmentContext ctx) {
        if (hasParent(ctx, "WhileStatementContext")) {
            // se pasa directo porque se experan expresiones sencillas
            appendln(ctx.getText());
        } else {
            // no hago nado poque no se de donde vengo
        }
    }

    @Override
    public void exitAssignment(Java8Parser.AssignmentContext ctx) {

    }

    @Override
    public void enterLeftHandSide(Java8Parser.LeftHandSideContext ctx) {

    }

    @Override
    public void exitLeftHandSide(Java8Parser.LeftHandSideContext ctx) {

    }

    @Override
    public void enterAssignmentOperator(Java8Parser.AssignmentOperatorContext ctx) {

    }

    @Override
    public void exitAssignmentOperator(Java8Parser.AssignmentOperatorContext ctx) {

    }

    @Override
    public void enterConditionalExpression(Java8Parser.ConditionalExpressionContext ctx) {

    }

    @Override
    public void exitConditionalExpression(Java8Parser.ConditionalExpressionContext ctx) {

    }

    @Override
    public void enterConditionalOrExpression(Java8Parser.ConditionalOrExpressionContext ctx) {

    }

    @Override
    public void exitConditionalOrExpression(Java8Parser.ConditionalOrExpressionContext ctx) {

    }

    @Override
    public void enterConditionalAndExpression(Java8Parser.ConditionalAndExpressionContext ctx) {

    }

    @Override
    public void exitConditionalAndExpression(Java8Parser.ConditionalAndExpressionContext ctx) {

    }

    @Override
    public void enterInclusiveOrExpression(Java8Parser.InclusiveOrExpressionContext ctx) {

    }

    @Override
    public void exitInclusiveOrExpression(Java8Parser.InclusiveOrExpressionContext ctx) {

    }

    @Override
    public void enterExclusiveOrExpression(Java8Parser.ExclusiveOrExpressionContext ctx) {

    }

    @Override
    public void exitExclusiveOrExpression(Java8Parser.ExclusiveOrExpressionContext ctx) {

    }

    @Override
    public void enterAndExpression(Java8Parser.AndExpressionContext ctx) {

    }

    @Override
    public void exitAndExpression(Java8Parser.AndExpressionContext ctx) {

    }

    @Override
    public void enterEqualityExpression(Java8Parser.EqualityExpressionContext ctx) {

    }

    @Override
    public void exitEqualityExpression(Java8Parser.EqualityExpressionContext ctx) {

    }

    @Override
    public void enterRelationalExpression(Java8Parser.RelationalExpressionContext ctx) {

    }

    @Override
    public void exitRelationalExpression(Java8Parser.RelationalExpressionContext ctx) {

    }

    @Override
    public void enterShiftExpression(Java8Parser.ShiftExpressionContext ctx) {

    }

    @Override
    public void exitShiftExpression(Java8Parser.ShiftExpressionContext ctx) {

    }

    @Override
    public void enterAdditiveExpression(Java8Parser.AdditiveExpressionContext ctx) {

    }

    @Override
    public void exitAdditiveExpression(Java8Parser.AdditiveExpressionContext ctx) {

    }

    @Override
    public void enterMultiplicativeExpression(Java8Parser.MultiplicativeExpressionContext ctx) {

    }

    @Override
    public void exitMultiplicativeExpression(Java8Parser.MultiplicativeExpressionContext ctx) {

    }

    @Override
    public void enterUnaryExpression(Java8Parser.UnaryExpressionContext ctx) {

    }

    @Override
    public void exitUnaryExpression(Java8Parser.UnaryExpressionContext ctx) {

    }

    @Override
    public void enterPreIncrementExpression(Java8Parser.PreIncrementExpressionContext ctx) {

    }

    @Override
    public void exitPreIncrementExpression(Java8Parser.PreIncrementExpressionContext ctx) {

    }

    @Override
    public void enterPreDecrementExpression(Java8Parser.PreDecrementExpressionContext ctx) {

    }

    @Override
    public void exitPreDecrementExpression(Java8Parser.PreDecrementExpressionContext ctx) {

    }

    @Override
    public void enterUnaryExpressionNotPlusMinus(Java8Parser.UnaryExpressionNotPlusMinusContext ctx) {

    }

    @Override
    public void exitUnaryExpressionNotPlusMinus(Java8Parser.UnaryExpressionNotPlusMinusContext ctx) {

    }

    @Override
    public void enterPostfixExpression(Java8Parser.PostfixExpressionContext ctx) {

    }

    @Override
    public void exitPostfixExpression(Java8Parser.PostfixExpressionContext ctx) {

    }

    @Override
    public void enterPostIncrementExpression(Java8Parser.PostIncrementExpressionContext ctx) {

    }

    @Override
    public void exitPostIncrementExpression(Java8Parser.PostIncrementExpressionContext ctx) {

    }

    @Override
    public void enterPostIncrementExpression_lf_postfixExpression(Java8Parser.PostIncrementExpression_lf_postfixExpressionContext ctx) {

    }

    @Override
    public void exitPostIncrementExpression_lf_postfixExpression(Java8Parser.PostIncrementExpression_lf_postfixExpressionContext ctx) {

    }

    @Override
    public void enterPostDecrementExpression(Java8Parser.PostDecrementExpressionContext ctx) {

    }

    @Override
    public void exitPostDecrementExpression(Java8Parser.PostDecrementExpressionContext ctx) {

    }

    @Override
    public void enterPostDecrementExpression_lf_postfixExpression(Java8Parser.PostDecrementExpression_lf_postfixExpressionContext ctx) {

    }

    @Override
    public void exitPostDecrementExpression_lf_postfixExpression(Java8Parser.PostDecrementExpression_lf_postfixExpressionContext ctx) {

    }

    @Override
    public void enterCastExpression(Java8Parser.CastExpressionContext ctx) {

    }

    @Override
    public void exitCastExpression(Java8Parser.CastExpressionContext ctx) {

    }

    @Override
    public void visitTerminal(TerminalNode terminalNode) {

    }

    @Override
    public void visitErrorNode(ErrorNode errorNode) {

    }

    @Override
    public void enterEveryRule(ParserRuleContext parserRuleContext) {

    }

    @Override
    public void exitEveryRule(ParserRuleContext parserRuleContext) {

    }
}
