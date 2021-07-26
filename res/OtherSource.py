class OtherSource:

  def __init__(self, ):
    pass

  def main(args):
    os = OtherSource()
    i = 2
    while i<500 and i%1==0:
      print(i/2)
      i=i*i
      

import sys
if __name__ == '__main__':
  OtherSource.main(sys.argv)
