/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.operator.events;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.javaoperatorsdk.operator.processing.event.DefaultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatefulSetEventSource extends AbstractEventSource implements Watcher<StatefulSet> {

 private static final Logger log = LoggerFactory.getLogger(StatefulSetEventSource.class);

 private final KubernetesClient client;

 public StatefulSetEventSource(KubernetesClient client) {
  this.client = client;
  registerWatch(client);
 }

 private void registerWatch(KubernetesClient client) {
  System.out.println("StatefulSetEventSource.registerWatch");
  client
          .apps()
          .statefulSets()
          .inAnyNamespace()
          // FIXME make this parameterized
          .withLabel("app.kubernetes.io/managed-by", "wildfly-operator")
          .watch(this);
 }

 @Override
 public void eventReceived(Action action, StatefulSet statefulSet) {
  log.info(
          "Event received for action: {}, StatefulSet: {} (rr={})",
          action.name(),
          statefulSet.getMetadata().getName(),
          statefulSet.getStatus().getReadyReplicas());

  if (action == Action.ERROR) {
   log.warn(
           "Skipping {} event for custom resource uid: {}, version: {}",
           action);
   return;
  }
  eventHandler.handleEvent(new DefaultEvent(statefulSet.getMetadata().getOwnerReferences().get(0).getUid(), this));
 }

 @Override
 public void onClose(WatcherException e) {
  if (e == null) {
   return;
  }
  if (e.isHttpGone()) {
   log.warn("Received error for watch, will try to reconnect.", e);
   registerWatch(client);
  } else {
   // Note that this should not happen normally, since fabric8 client handles reconnect.
   // In case it tries to reconnect this method is not called.
   log.error("Unexpected error happened with watch. Will exit.", e);
   System.exit(1);
  }
 }
}
