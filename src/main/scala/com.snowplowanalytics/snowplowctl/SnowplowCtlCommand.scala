package com.snowplowanalytics.snowplowctl

import cats.implicits._
import com.monovore.decline._
import Config._
import org.json4s.JsonAST.JValue

/** Main command hierarchy */
sealed trait SnowplowCtlCommand

object SnowplowCtlCommand {

  /** Manifest-related subcommands with their specific arguments */
  sealed trait ManifestSubcommand
  case object Dump extends ManifestSubcommand
  case object Create extends ManifestSubcommand
  case class GetItem(id: String, json: Boolean) extends ManifestSubcommand
  case class Resolve(id: String, state: ResolvableState) extends ManifestSubcommand
  case class SkipItem(id: String, app: String, version: Option[String], instance: Option[String]) extends ManifestSubcommand
  case class DeleteItem(id: String) extends ManifestSubcommand
  case class Import(manifestFile: String) extends ManifestSubcommand

  /** Container with common required arguments and subcommand-specific ones */
  case class ManifestCommand(tableName: String, config: ManifestSubcommand, resolver: Option[String]) extends SnowplowCtlCommand
  case object ShowVersion extends SnowplowCtlCommand

  // manifest dump
  val dumpSubcommand: Opts[ManifestSubcommand] =
    Opts.subcommand("dump", "Dump manifest records as JSON")(Opts(Dump))

  // manifest create
  val createSubcommand: Opts[ManifestSubcommand] =
    Opts.subcommand("create", "Create DynamoDB table with expected structure")(Opts(Create))

  // manifest get-item
  val idOpt: Opts[String] =
    Opts.argument[String]("item-id")
  val jsonFlag: Opts[Boolean] =
    Opts.flag("json", "Dump as JSON").orFalse
  val getItem: Opts[ManifestSubcommand] =
    Opts.subcommand("get-item", "Retrieve all records for particular item")((idOpt, jsonFlag).mapN { (id, json) => GetItem(id, json) })

  // manifest skip
  val appNameOpt: Opts[String] =
    Opts.argument[String]("application-name")
  val appVersionOpt: Opts[Option[String]] =
    Opts.argument[String]("application-version").orNone
  val instanceIdOpt: Opts[Option[String]] =
    Opts.argument[String]("instance-id").orNone
  val skip: Opts[ManifestSubcommand] =
    Opts.subcommand("skip", "Add SKIP state to omit item for subset of applications")((idOpt, appNameOpt, appVersionOpt, instanceIdOpt).mapN(SkipItem))

  // manifest resolve
  val stateOpt: Opts[ResolvableState] =
    Opts.argument[ResolvableState]("STATE")
  val resolve: Opts[ManifestSubcommand] =
    Opts.subcommand("resolve", "Add RESOLVE state for blocking record")((idOpt, stateOpt).mapN { (id, state) => Resolve(id, state) })

  // manifest delete
  val delete: Opts[ManifestSubcommand] =
    Opts.subcommand("delete", "Delete Item entirely (not recommended)")(idOpt.map(DeleteItem))

  // import
  val manifestFile: Opts[String] =
    Opts.argument[String]("manifest-file")
  val importRecords: Opts[ManifestSubcommand] =
    Opts.subcommand("import", "Import records from file produced by dump")(manifestFile.map(Import))

  // manifest
  val manifestTableOpt: Opts[String] = Opts.option[String]("table-name", "Amazon DynamoDB table")
  val resolverOpt: Opts[Option[String]] = Opts.option[String]("resolver", "Iglu Resolver config path").orNone
  val allSubcommands: Opts[ManifestSubcommand] = createSubcommand
    .orElse(importRecords)
    .orElse(dumpSubcommand)
    .orElse(resolve)
    .orElse(getItem)
    .orElse(skip)
    .orElse(delete)
  val manifestSubcommand: Opts[SnowplowCtlCommand] =
    Opts.subcommand("manifest", "Manipulating Snowplow Processing Manifest") {
      (manifestTableOpt, allSubcommands, resolverOpt).mapN(ManifestCommand)
    }

  // main
  val showVersion: Opts[SnowplowCtlCommand] =
    Opts.flag("version", "Show version and exit").map(_ => ShowVersion)
  val snowplowCtl: Command[SnowplowCtlCommand] =
    Command(generated.ProjectMetadata.name, "Snowplow CTL")(showVersion.orElse(manifestSubcommand))

  def parse(args: Seq[String]): Either[Help, SnowplowCtlCommand] =
    snowplowCtl.parse(args)
}
