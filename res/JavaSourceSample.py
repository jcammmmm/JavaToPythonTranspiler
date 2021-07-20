class JavaSourceSample:
  def main(self, args):
    print("Hello world!")
    self.show(3,"algo",args)

  def show(self, j, smthg, o):
    x = 0
    d = 4.4
    b = 10.1
    s = "cadena"
    n = 9292.2
    print(d)
    print(s)
    print(n)

import sys
if __name__ == '__main__':
  javaSourceSample = JavaSourceSample()
  javaSourceSample.main(sys.argv)
