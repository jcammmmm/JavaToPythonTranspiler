class JavaSourceSample:
  crossVariable = ''
  obsFactor = 9

  def __init__(self, lazyIdentity=0):
    self.lazyIdentity = lazyIdentity

  def instanceFun(self):
    print(self.lazyIdentity)

  def staticFun():
    print(JavaSourceSample.obsFactor)

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
    condicion1 = true
    condicion2 = false
    if (temperatura>25):
      print("A la playa!!!")
    if (temperatura<=25):
      print("Esperando al buen tiempo...")
    if (temperatura>25):
      print("A la playa!!!")
    else:
      print("Esperando al buen tiempo...")
    if (condicion1):
      print("A")
    else:
      if (condicion2):
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
    while i<10:
      print(i)
      self.forLoop()
      i=i+1

  def forLoop(self):
    i = 2
    while i<500 and i%1==0:
      print(i/2)
      i=i*i
      
    j = 12
    j = 2
    while not (j>30):
      print(j)
      j=j*j
      
    k = 2
    while k<10:
      pass
      k=k*k
      

  def main(args):
    print("Hello world!")
    j = JavaSourceSample()
    j.show(3, "algo", args)
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    j.forLoop()
    print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    j.instanceFun()
    print("------------------------------")
    j.whileLoop()
    print("------------------------------")
    JavaSourceSample.staticFun()

import sys
if __name__ == '__main__':
  JavaSourceSample.main(sys.argv)
