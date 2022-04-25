package com.odenzo.base.testkit

object TestTags:
  val notInCI   = new munit.Tag("notInCI")
  val database  = new munit.Tag("database")
  val careful   = new munit.Tag("databaseRW")
  val migration = new munit.Tag("migration")
