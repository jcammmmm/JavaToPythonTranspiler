# Suposiones
El codigo fuente en Java se supone que tiene las siguientes características:   
    - No se usan parametros anotados en las funciones    
    - No se usa el spread operator e.g. String...   
    - El codigo java solo hace uso de utilidades basicas de su librerias estandar
    - Sólo transpila programas de un solo archivo Java sin clases anidadas
        * El tema de invocación de métodos es más sencillo
    - Todos los metodos de la fuente en Java deben ser estáticos

En codigos de fuente sencillos de Java se debe instanciar la clase principal dentro del
método estático principal `main()` para poder ejecutar el programa y no abusar de los 
métodos estáticos y conservar métodos y atributos de instancia sin la necesidad de la 
verbosidad de la palabra `static` cada vez que se define un identificador.

# Caracteristicas
El transpilador hace distincion entre los métodos que son estáticos y los que no

# Estructura
El atributo de clase estático `tabDepth` controla la identación a medida que se procesa
el codigo fuente de java. Es un entero que se incrementa o decrementa a través
de expresiones del tipo `tabDepth++` o `tabDepth--`   

Cada vez que se entra a un bloque, esto es, a lo encerrado entre corchetes {},
se aumenta la identacion. Cada vez que se sale se decrementa en una unidad.

Para buscar navegar rapidamente por la implementacion basta con buscar el comentario
`// impl.`. Todos los metodos que han sido implementados tienen esta marca en la primer
linea del cuerpo.