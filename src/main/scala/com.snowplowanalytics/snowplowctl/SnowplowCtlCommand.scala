package com.snowplowanalytics.snowplowctl

import cats.implicits._

import com.monovore.decline._

import com.snowplowanalytics.manifest.core.Application

import Config._

/** Main command hierarchy */
sealed trait SnowplowCtlCommand

object SnowplowCtlCommand {

  /** Common containers */
  case class AwsConfig(awsProfile: Option[String], awsRegion: Option[String])

  /** Manifest-related subcommands with their specific arguments */
  sealed trait ManifestSubcommand
  case object Dump extends ManifestSubcommand
  case object Create extends ManifestSubcommand
  case class GetItem(id: String, json: Boolean) extends ManifestSubcommand
  case class Resolve(id: String, state: ResolvableState) extends ManifestSubcommand
  case class SkipItem(id: String, app: String, version: Option[String], instance: Option[String]) extends ManifestSubcommand
  case class DeleteItem(id: String) extends ManifestSubcommand
  case class Import(manifestFile: String) extends ManifestSubcommand
  case class Query(processedBy: Option[Application], requestedBy: Option[Application]) extends ManifestSubcommand

  /** Container with common required arguments and subcommand-specific ones */
  case class ManifestCommand(table: String,
                             config: ManifestSubcommand,
                             resolver: Option[String],
                             awsConfig: AwsConfig) extends SnowplowCtlCommand
  case object ShowVersion extends SnowplowCtlCommand

  // manifest dump
  private val dumpSubcommand: Opts[ManifestSubcommand] =
    Opts.subcommand("dump", "Dump manifest records as JSON")(Opts(Dump))

  // manifest create
  private val createSubcommand: Opts[ManifestSubcommand] =
    Opts.subcommand("create", "Create DynamoDB table with expected structure")(Opts(Create))

  // manifest get-item
  private val idOpt: Opts[String] =
    Opts.argument[String]("item-id")
  private val jsonFlag: Opts[Boolean] =
    Opts.flag("json", "Dump as JSON").orFalse
  private val getItem: Opts[ManifestSubcommand] =
    Opts.subcommand("get-item", "Retrieve all records for particular item")((idOpt, jsonFlag).mapN { (id, json) => GetItem(id, json) })

  // manifest skip
  private val appNameOpt: Opts[String] =
    Opts.argument[String]("application-name")
  private val appVersionOpt: Opts[Option[String]] =
    Opts.argument[String]("application-version").orNone
  private val instanceIdOpt: Opts[Option[String]] =
    Opts.argument[String]("instance-id").orNone
  private val skip: Opts[ManifestSubcommand] =
    Opts.subcommand("skip", "Add SKIP state to omit item for subset of applications")((idOpt, appNameOpt, appVersionOpt, instanceIdOpt).mapN(SkipItem))

  // manifest resolve
  private val stateOpt: Opts[ResolvableState] =
    Opts.argument[ResolvableState]("STATE")
  private val resolve: Opts[ManifestSubcommand] =
    Opts.subcommand("resolve", "Add RESOLVE state for blocking record")((idOpt, stateOpt).mapN { (id, state) => Resolve(id, state) })

  // manifest delete
  private val delete: Opts[ManifestSubcommand] =
    Opts.subcommand("delete", "Delete Item entirely (not recommended)")(idOpt.map(DeleteItem))

  // import
  private val manifestFile: Opts[String] =
    Opts.argument[String]("manifest-file")
  private val importRecords: Opts[ManifestSubcommand] =
    Opts.subcommand("import", "Import records from file produced by dump")(manifestFile.map(Import))

  // query
  private val processedByOpt: Opts[Option[Application]] =
    Opts.option[Application]("processed-by", "Application added PROCESSED state (use '*' as wildcard for id)").orNone
  private val requestedByOpt: Opts[Option[Application]] =
    Opts.option[Application]("requested-by", "Application that wants to process (use '*' as wildcard for id)").orNone
  private val query: Opts[Query] =
    Opts.subcommand("query", "Query manifest items by applications")((processedByOpt, requestedByOpt).mapN(Query))


  // manifest
  private val manifestTableOpt: Opts[String] = Opts.option[String]("table-name", "Amazon DynamoDB table")
  private val resolverOpt: Opts[Option[String]] = Opts.option[String]("resolver", "Iglu Resolver config path").orNone
  private val awsProfileOpt: Opts[Option[String]] = Opts.option[String]("profile", "AWS profile name").orNone
  private val awsRegionOpt: Opts[Option[String]] = Opts.option[String]("region", "AWS region").orNone
  private val allSubcommands: Opts[ManifestSubcommand] = createSubcommand
    .orElse(importRecords)
    .orElse(dumpSubcommand)
    .orElse(resolve)
    .orElse(getItem)
    .orElse(skip)
    .orElse(delete)
    .orElse(query)
  private val manifestSubcommand: Opts[SnowplowCtlCommand] =
    Opts.subcommand("manifest", "Manipulating Snowplow Processing Manifest") {
      val awsConfig = (awsProfileOpt, awsRegionOpt).mapN(AwsConfig)
      (manifestTableOpt, allSubcommands, resolverOpt, awsConfig).mapN(ManifestCommand)
    }

  // main
  private val showVersion: Opts[SnowplowCtlCommand] =
    Opts.flag("version", "Show version and exit").map(_ => ShowVersion)
  private val snowplowCtl: Command[SnowplowCtlCommand] =
    Command(generated.ProjectMetadata.name, "Snowplow CTL")(showVersion.orElse(manifestSubcommand))

  def parse(args: Seq[String]): Either[Help, SnowplowCtlCommand] =
    snowplowCtl.parse(args)
}
