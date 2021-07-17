# Suposiones
El codigo fuente en Java se supone que tiene las siguientes características:   
    - No se usan parametros anotados en las funciones    
    - No se usa el spread operator e.g. String...   
    - El codigo java solo hace uso de utilidades basicas de su librerias estandar
    - Sólo transpila programas de un solo archivo Java sin clases anidadas
        * El tema de invocación de métodos es más sencillo
    - Todos los metodos de la fuente en Java deben ser estáticos

# Estructura
El atributo de clase estático `tabDepth` controla la identación a medida que se procesa
el codigo fuente de java. Es un entero que se incrementa o decrementa a través
de expresiones del tipo `tabDepth++` o `tabDepth--`   

Cada vez que se entra a un bloque, esto es, a lo encerrado entre corchetes {},
se aumenta la identacion. Cada vez que se sale se decrementa en una unidad.

Para buscar navegar rapidamente por la implementacion basta con buscar el comentario
`// impl.`. Todos los metodos que han sido implementados tienen esta marca en la primer
linea del cuerpo.