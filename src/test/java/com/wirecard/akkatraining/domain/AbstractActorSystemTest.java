package com.wirecard.akkatraining.domain;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractActorSystemTest {

  private ActorSystem system;

  @Before
  public void setUp() throws Exception {
    system = ActorSystem.create();
    before();
  }

  protected void before() {
    // nothing
  }

  @After
  public void tearDown() throws Exception {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  ActorSystem system() {
    return system;
  }
}
