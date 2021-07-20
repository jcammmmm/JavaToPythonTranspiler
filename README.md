# Suposiones
El codigo fuente en Java se supone que tiene las siguientes características:   
- No se usan parametros anotados en las funciones    
- No se usa el spread operator e.g. String...   
- El codigo java solo hace uso de utilidades basicas de su librerias estandar   
- Sólo transpila programas de un solo archivo Java sin clases anidadas    
    * El tema de invocación de métodos es más sencillo    
- El uso de los corchetes para delimitar bloques es obligatorio. Esto puede afectar
  la identacion del codigo fuente en algunos casos. Por ejemplo en `for (i = 0; i < 2; i++) print(i)` 
- (Soportado!) Todos los metodos de la fuente en Java deben ser estáticos   

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
- El transpilador hace distincion entre los métodos que son estáticos y los que no.
- El metodo reconoce bloques vacíos y los traduce como `pass`.
- Siempre se añade una traducción para que el metodo main se ejecute.
- El manejo de tipos y operadores está en su fase inicial, por lo que se recomienda
  que el codigo fuente Java indique apropiadamente los tipos de los operandos, porque
  expresiones como `double/int` podría NO dar entero como esperaria el programador.
- El traductor tiene cuida la estética de los operadores, por lo que hace una curación
  de espacios en blanco cada vez que se agrega una nueva linea.

# Estructura
El atributo de clase estático `tabDepth` controla la identación a medida que se procesa
el codigo fuente de java. Es un entero que se incrementa o decrementa a través
de expresiones del tipo `tabDepth++` o `tabDepth--`   

Cada vez que se entra a un bloque, esto es, a lo encerrado entre corchetes {},
se aumenta la identacion. Cada vez que se sale se decrementa en una unidad.

Para buscar navegar rapidamente por la implementacion basta con buscar el comentario
`// impl.`. Todos los metodos que han sido implementados tienen esta marca en la primer
linea del cuerpo.

# Notas Dev.
- No agregar tildes a ningun codigo fuente.