class JavaSourceSample:
  crossVariable = ''
  obsFactor = 9

  def __init__(self, lazyIdentity=0):
    self.lazyIdentity = lazyIdentity

  def main(args):
    print("Hello world!")
    j = JavaSourceSample()
    j.show(3,"algo",args)
    j.forLoop()

  def show(self, j, smthg, o):
    x = 0
    d = 4.1
    b = 10.1
    s = "cadena"
    n = 9292.2
    print(d)
    print(s)
    print(n)

  def forLoop(self, ):
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
      

import sys
if __name__ == '__main__':
  JavaSourceSample.main(sys.argv)
