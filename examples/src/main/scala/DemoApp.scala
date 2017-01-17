import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

object DemoApp {
  //
  val SUCCESS = 127
  val FAILURE = 255
}

class DemoApp(val testdir: Path) {

  def doSuccess(args: Array[String]): Int = {
    DemoApp.SUCCESS
  }

  def doFailure(args: Array[String]): Int = {
    DemoApp.FAILURE
  }

  def doFlaky(args: Array[String]): Int = {
    if (args(0) == "bang") {
      throw new RuntimeException("BANG!")
    }
    else if (args(0) == "fail") {
      DemoApp.FAILURE
    } else {
      DemoApp.SUCCESS
    }
  }

  def doArgsCount(args: Array[String]): Int = {
    args.length
  }

  def doTxt(args: Array[String]): Int = {
    val output = Paths.get(testdir.toString, args(0))
    Files.write(output, args
      .mkString("hello\n", "\n", "\nworld\n")
      .getBytes(StandardCharsets.UTF_8))
    DemoApp.SUCCESS
  }

  def doXml(args: Array[String]): Int = {
    val output = Paths.get(testdir.toString, args(0))
    Files.write(output, args
      .mkString("<hello><arg>", "</arg><arg>", "</arg></hello>\n")
      .getBytes(StandardCharsets.UTF_8))
    DemoApp.SUCCESS
  }

  def doTxtXml(args: Array[String]): Int = {
    val result =
      if (args(1) == "txt") {
        doTxt(args)
      } else if (args(1) == "xml") {
        doXml(args)
      } else {
        DemoApp.FAILURE
      }
    result
  }
}
