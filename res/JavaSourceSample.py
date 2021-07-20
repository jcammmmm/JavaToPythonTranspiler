class JavaSourceSample:
  def main(args):
    print("Hello world!")
    j = JavaSourceSample()
    j.show(3,"algo",args)

  def show(self, j, smthg, o):
    x = 0
    d = 4.1
    b = 10.1
    s = "cadena"
    n = 9292.2
    print(d)
    print(s)
    print(n)

import sys
if __name__ == '__main__':
  JavaSourceSample.main(sys.argv)
