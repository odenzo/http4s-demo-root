package com.odenzo.base

import scribe.filter.*
import scribe.output.{Color, ColoredOutput}
import scribe.{Level, Logger, Priority}

/** Scribe has run-time configuration. This is designed to control when developing the codec library and also when using. This is my
  * experiment and learning on how to control The default config fvor scribe is INFO See com.odenzo.ripple.bincodec package information for
  * usage.
  */
object ScribeJSLoggingConfig extends Logger:

  /** Helper to filter out messages in the packages given below the given level I am not sure this works with the global scribe object or
    * not. Usage:
    * {{{
    *   scribe.
    * }}}
    * @return
    *   a filter that can be used with .withModifier()
    */
  def excludePackageSelction(packages: List[String], atOrAboveLevel: Level, priority: Priority): FilterBuilder =
    val ps: List[Filter] = packages.map(p => packageName.startsWith(p))
    val fb               = select(ps: _*).exclude(level < atOrAboveLevel).includeUnselected.copy(priority = priority)
    fb

  def excludeByClass(clazzes: List[Class[_]], minLevel: Level): FilterBuilder =
    val names   = clazzes.map(_.getName)
    val filters = names.map(n => className(n))
    select(filters: _*).include(level >= minLevel)

  def setAllToLevel(l: Level): Unit =
    scribe.Logger.root.clearHandlers().withHandler(minimumLevel = Some(l)).replace()

  def addModifiers(packages: List[String], l: Level): Unit =
    val pri = Priority.Normal // unnecessary since clearing existing modifiers, but handy for future.
    scribe.Logger.root.withModifier(ScribeLoggingConfig.excludePackageSelction(packages, l, pri)).replace()

  def mutePackage(p: String, l: Level = Level.Warn): Unit =
    ScribeLoggingConfig.addModifiers(List(p), Level.Warn) // TODO: This really adding, check alter

  /** Set logging level of all com.odenzo.bincodec.* packages to the level (via filter) Default is for INFO level */
  def setBinCodecLogging(l: Level): Unit = mutePackage("com.odenzo.bincodec", l)

  import scribe.format.*

  def init(l: Level): Logger =
    val myFormatter: Formatter = formatter"$time $levelColoredPaddedRight [$fileName:$line] - ${white(messages)}$newLine"
    Logger.root.clearHandlers().withHandler(formatter = myFormatter).clearModifiers().withMinimumLevel(l).replace()

  /** Thisis tuned for MS Edge Dark Mode, Safari JS Console not vt100 color compatable! */
  def initJS(l: Level): Logger =

    val myFormatter: Formatter = formatter"$time $levelColoredForDark ${green(fileName)}:${green(line)} - ${white(messages)}$newLine"
    Logger.root.clearHandlers().withHandler(formatter = myFormatter).clearModifiers().withMinimumLevel(l).replace()

  def levelColoredForDark: FormatBlock = FormatBlock { logRecord =>
    val color = logRecord.level match
      case Level.Trace => Color.White
      case Level.Debug => Color.Green
      case Level.Info  => Color.Blue
      case Level.Warn  => Color.Yellow
      case Level.Error => Color.Red
      case _           => Color.Cyan
    new ColoredOutput(color, FormatBlock.Level.PaddedRight.format(logRecord))
  }
