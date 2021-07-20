class JavaSourceSample:
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
    while i<100:
      print(i/2)
      i=i*i
    j = 12
    while j<30:
      pass
      j=j*j
    k = 2
    while k<10:
      pass
      k=k*k

import sys
if __name__ == '__main__':
  JavaSourceSample.main(sys.argv)
