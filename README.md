# JavaToPythonTranspiler

## Manual de compilacion

1. Abrir el proyecto con IntelliJ
2. Click derecho en la carpeta del proyecto y clickear en open module settings.
3. Seleccionar Sources, seleccionamos la carpeta gen y le damos Mark as: Sources.
4. (Si se tiene descargado jar de antlr 4.9.2 pasar a paso 6). Descargar ANTLR de https://www.antlr.org/dowload.html
5. En la seccion ANTLR toll and Java Target seleccionar Complete ANTLR 4.9.2 Java binaries jar.
6. Seleccionamos Dependencies y se oprime en añadir JARs or Directory.
7. Buscamos el jar de ANTLR y se oprime ok, se marca la casilla del jay y se le da apply y ok.
8. En la carpeta in se agregan todos los archivos que se quieren traducir.
9. En `src\Main.java` en el arreglo `samples` se agrega la ruta de los archivos que se quieren traducir.
10. Ya podemos ejecutar el proyecto y los archivos traducidos se encontraran en la carpeta `res\`

## Suposiones
El codigo fuente en Java se supone que tiene las siguientes características:   
- No se usan parametros anotados en las funciones    
- No se usa el spread operator e.g. String...   
- El codigo java solo hace uso de utilidades basicas de su librerias estandar   
- Sólo transpila programas de un solo archivo Java sin clases anidadas    
    * El tema de invocación de métodos es más sencillo    
- El uso de los corchetes para delimitar bloques es obligatorio. Esto puede afectar
  la identacion del codigo fuente en algunos casos. Por ejemplo en `for (i = 0; i < 2; i++) print(i)` 
- (Soportado!) Todos los metodos de la fuente en Java deben ser estáticos   
- Expresiones i++ no se soportan.
- En excepciones solo es soportado el `ArithmeticException`, `EOFException`  y `Exception` 
### Ciclos
- Solo se admiten expresiones booleanas dentro de la condicion de terminacion
  del ciclo.
#### *for*
- La variable principal de iteracion de un ciclo for se declara dentro del 
  ciclo y se realiza mediante los tipos `int` o `double`. Esto para lograr
  compatibilidad con los ciclos de python.
  Sólo se permite una sola variable declarada en el ciclo. No se permiten
  listas de declaraciones o asignaciones (ver `statementExpressionList`) en
  la gramática.



En codigos de fuente sencillos de Java se debe instanciar la clase principal dentro del
método estático principal `main()` para poder ejecutar el programa y no abusar de los 
métodos estáticos y conservar métodos y atributos de instancia sin la necesidad de la 
verbosidad de la palabra `static` cada vez que se define un identificador.

# Caracteristicas
- Se reconocen como diferentes los tipos estaticos y los de instancia. Se realiza la
  traduccion de acuerdo a: https://docs.python.org/3/tutorial/classes.html#class-and-instance-variables
- El transpilador hace distincion entre los métodos que son estáticos y los que no.
- El metodo reconoce bloques vacíos y los traduce como `pass`.
- Siempre se añade una traducción para que el metodo main se ejecute.
- El manejo de tipos y operadores está en su fase inicial, por lo que se recomienda
  que el codigo fuente Java indique apropiadamente los tipos de los operandos, porque
  expresiones como `double/int` podría NO dar entero como esperaria el programador.
- El traductor cuida la estética de los operadores, por lo que hace una curación
  de espacios en blanco cada vez que se agrega una nueva linea.
- El metodo `main` se debe ser el método que se encuentra más abajo de todos los métodos y 
  todos los métodos estáticos debe aparecer primero antes que los de clase. Esto con el 
  fin de que la invocacion de los metodos se haga de manera correcta al momento de la
  traduccion. Este es un TODO que consiste en hacer la traduccion de los métodos de forma
  offline para precindir del ordenamiento que se indica. 

# Estructura
El atributo de clase estático `tabDepth` controla la identación a medida que se procesa
el codigo fuente de java. Es un entero que se incrementa o decrementa a través
de expresiones del tipo `tabDepth++` o `tabDepth--`   

Cada vez que se entra a un bloque, esto es, a lo encerrado entre corchetes {},
se aumenta la identacion. Cada vez que se sale se decrementa en una unidad.

Para buscar navegar rapidamente por la implementacion basta con buscar el comentario
`// impl.`. Todos los metodos que han sido implementados tienen esta marca en la primer
linea del cuerpo.

La traduccion tiene una fase online y otra offline. En la online se va realizando la traduccion
a medida que se va procesando el codigo fuente. En la offline se reemplazan porciones de texto
con contenido que va recopilando a medida que se realiza la compilacion online. La traduccion
offline se usa en el caso de atributos de clase y de instancia porque en Python y Java esta
característica se maneja de formas diferentes.


# Notas Dev.
- No agregar tildes a ningun codigo fuente.
