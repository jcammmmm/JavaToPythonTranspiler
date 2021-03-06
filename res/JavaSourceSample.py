class JavaSourceSample:
  crossVariable = ''
  obsFactor = 9

  def __init__(self, lazyIdentity=0):
    self.lazyIdentity = lazyIdentity

  def instanceFun(self):
    print(self.lazyIdentity)

  def staticFun():
    print(JavaSourceSample.obsFactor)

  def read():
    data = input()
    print(data)

  def show(self, j, smthg, o):
    x = 0
    d = 4.1
    b = 10.1
    s = "cadena"
    n = 9292.2
    print(d)
    print(s)
    print(n)

  def ifStatement(self):
    temperatura = 24
    condicion1 = True
    condicion2 = False
    if (temperatura>25):
      if (temperatura>25):
        print("A la playa!!!")
      else:
        print("Esperando al buen tiempo...")
      
    if (temperatura<=25):
      print("Esperando al buen tiempo...")
    if (temperatura>25):
      print("A la playa!!!")
    else:
      print("Esperando al buen tiempo...")
    
    if (condicion1==True):
      print("A")
    else:
      if (condicion2==False):
        print("B")
      else:
        if (condicion2):
          if (temperatura>25):
            print("A la playa!!!")
          else:
            print("Esperando al buen tiempo...")
          
        else:
          print("D")
        
      
    

  def whileLoop(self):
    print("Test para 'while':")
    i = 2
    b = True
    while i<10:
      print(i)
      self.forLoop()
      i = i+1
    while b:
      print(i)
      self.forLoop()
      b =  False

  def doWhileLoop(self):
    print("Test para 'doWhile':")
    i = 1
    while True:
      print('{}{}'.format("iteracion: ", i))
      i = i+1
      if not i<=3:
        break

  def forLoop(self):
    i = 2
    while i<500 and i%1==0:
      print(i/2)
      i=i*i
      
    j = 12
    j = 2
    while not (j>30):
      print('{}{}'.format("El nuevo valor es: ", j))
      j=j*j
      
    k = 2
    while k<10:
      pass
      k=k*k
      

  def switchExample(self):
    day = 5
    day2 = 2
    dayString = ''
    dayString2 = ''
    if (day == 1):
      if (day == 1):
        dayString="Lunes"
      elif (day == 2):
        dayString="2"
      else:
        dayString="3"
    elif (day == 2):
      dayString="Martes"
    elif (day == 3):
      dayString="Miercoles"
    elif (day == 4):
      dayString="Jueves"
    elif (day == 5):
      dayString="Viernes"
    elif (day == 6):
      dayString="Sabado"
    elif (day == 7):
      dayString="Domingo"
    else:
      dayString="Dia inv??lido"
    if (day2 == 1):
      dayString2="1"
    elif (day2 == 2):
      dayString2="2"
    else:
      dayString2="3"
    print(dayString)
    print(dayString2)

  def arrayExample(self):
    arrayCaracteres = []
    arrayCaracteres = [None]*10
    arrayCaracteres2 = [None]*10
    arrayCaracteres3 = [[None]*10]*10
    x = arrayCaracteres[2]
    matriz = []
    matriz = [[None]*2]*2
    y = matriz[1][1]
    arrayChar = ['a','b','c','d','e']
    arrayInt = [[1,2,3,4],[5,6,7,8]]
    print(x)
    print(y)
    print(arrayChar)
    print(arrayCaracteres2)
    print(arrayCaracteres3)
    print(arrayInt)

  def tryStatement(self):
    try:
      numerador = 0
      denominador = 0
      resultado = 0
      print(resultado)
    except ArithmeticError as ae :
      print("No se puede dividir por cero")
    finally:
      print("Proceso finalizado")

  def main(args):
    demo = '{}{}{}{}'.format("Hello: ", 1, ", World: ", 2)
    print(demo)
    j = JavaSourceSample()
    j.show(3, "algo", args)
    print("------------------------------")
    j.forLoop()
    print("..............................")
    j.instanceFun()
    print("------------------------------")
    j.whileLoop()
    print("..............................")
    JavaSourceSample.staticFun()
    print("------------------------------")
    j.switchExample()
    print("..............................")
    j.ifStatement()
    print("------------------------------")
    j.doWhileLoop()
    print("..............................")
    JavaSourceSample.read()
    print("------------------------------")
    j.tryStatement()
    print("------------------------------")
    j.arrayExample()

import sys
if __name__ == '__main__':
  JavaSourceSample.main(sys.argv)
