package riakka

import org.slf4j.{Logger, LoggerFactory}

// logger borrowed from Lift until we get a decent standard way of logging in Scala

trait LiftLogger {
  def isTraceEnabled: Boolean = false
  def trace(msg: => AnyRef): Unit = ()
  def trace(msg: => AnyRef, t: => Throwable): Unit = ()

  def assertLog(assertion: Boolean, msg: => String): Unit = ()

  def isDebugEnabled: Boolean = false
  def debug(msg: => AnyRef): Unit = ()
  def debug(msg: => AnyRef, t: => Throwable): Unit = ()

  def isErrorEnabled: Boolean = false
  def error(msg: => AnyRef): Unit = ()
  def error(msg: => AnyRef, t: => Throwable): Unit = ()

  def fatal(msg: AnyRef): Unit = ()
  def fatal(msg: AnyRef, t: Throwable): Unit = ()

  def level: LiftLogLevels.Value = LiftLogLevels.Off
  def level_=(level: LiftLogLevels.Value): Unit = ()
  def name: String = "Null"
  // def parent = logger.getParent

  def isInfoEnabled: Boolean = false
  def info(msg: => AnyRef): Unit = ()
  def info(msg: => AnyRef, t: => Throwable): Unit = ()

  def isEnabledFor(level: LiftLogLevels.Value): Boolean = false

  def isWarnEnabled: Boolean = false
  def warn(msg: => AnyRef): Unit = ()
  def warn(msg: => AnyRef, t: => Throwable): Unit = ()
}

/**
 * Adapter use internaly by lift as Logger, if Slf4jLogBoot is enabled.
 */
private[riakka] trait Logging extends LiftLogger {

  private val logger = getLogger(this)

  override def isTraceEnabled = logger.isTraceEnabled
  override def trace(msg: => AnyRef) = if (isTraceEnabled) logger.trace(String.valueOf(msg))
  override def trace(msg: => AnyRef, t: => Throwable) = if (isTraceEnabled) logger.trace(String.valueOf(msg), t)

  override def assertLog(assertion: Boolean, msg: => String) = if (assertion) info(msg)

  override def isDebugEnabled = logger.isDebugEnabled
  override def debug(msg: => AnyRef) = if (isDebugEnabled) logger.debug(String.valueOf(msg))
  override def debug(msg: => AnyRef, t: => Throwable) = if (isDebugEnabled) logger.debug(String.valueOf(msg), t)

  override def isErrorEnabled = logger.isErrorEnabled
  override def error(msg: => AnyRef) = if (isErrorEnabled) logger.error(String.valueOf(msg))
  override def error(msg: => AnyRef, t: => Throwable) = if (isErrorEnabled) logger.error(String.valueOf(msg), t)

  override def fatal(msg: AnyRef) = error(msg)
  override def fatal(msg: AnyRef, t: Throwable) = error(msg, t)

  override def level = LiftLogLevels.All

  override def isEnabledFor(level: LiftLogLevels.Value): Boolean = level match {
    case LiftLogLevels.All => isTraceEnabled
    case LiftLogLevels.Debug => isDebugEnabled
    case LiftLogLevels.Error => isErrorEnabled
    case LiftLogLevels.Warn => isWarnEnabled
    case LiftLogLevels.Fatal => isErrorEnabled
    case LiftLogLevels.Info => isInfoEnabled
    case LiftLogLevels.Trace => isTraceEnabled
    case LiftLogLevels.Off => !isErrorEnabled
  }

//  override def level_=(level: LiftLogLevels.Value) = logger.setLevel(liftToLog4J(level) )
  override def name = logger.getName

  override def isInfoEnabled = logger.isInfoEnabled
  override def info(msg: => AnyRef) = if (isInfoEnabled) logger.info(String.valueOf(msg))
  override def info(msg: => AnyRef, t: => Throwable) = if (isInfoEnabled) logger.info(String.valueOf(msg), t)

  override def isWarnEnabled = logger.isWarnEnabled
  override def warn(msg: => AnyRef) = if (isWarnEnabled) logger.warn(String.valueOf(msg))
  override def warn(msg: => AnyRef, t: => Throwable) = if (isWarnEnabled) logger.warn(String.valueOf(msg), t)

  def loggerNameForClass(className: String) = {
      if (className endsWith "$") className.substring(0, className.length - 1)
      else className
  }

  def getLogger(logging: AnyRef) = LoggerFactory.getLogger(loggerNameForClass(logging.getClass.getName))

}

object LiftLogLevels extends Enumeration {
  val All = Value(1, "All")
  val Trace = Value(3, "Trace")
  val Debug = Value(5, "Debug")
  val Warn = Value(7, "Warn")
  val Error = Value(9, "Error")
  val Fatal = Value(11, "Fatal")
  val Info = Value(13, "Info")
  val Off = Value(15, "Off")
}