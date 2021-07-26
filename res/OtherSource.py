class OtherSource:

  def __init__(self, ):
    pass

  def main(args):
    os = OtherSource()
    demo = '{}{}{}{}'.format("uno: ", 1, "; dos: ", 2)
    print(demo)

import sys
if __name__ == '__main__':
  OtherSource.main(sys.argv)
