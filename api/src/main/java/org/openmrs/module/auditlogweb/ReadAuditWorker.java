/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb;

import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ReadAuditWorker {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private ReadAuditService readAuditService;
	
	private final BlockingQueue<ReadAuditLog> queue = new LinkedBlockingQueue<>(10000);
	
	private Thread workerThread;
	
	private volatile boolean running = true;
	
	@PostConstruct
	public void init() {
		log.info("Starting ReadAuditWorker background thread");
		workerThread = new Thread(() -> run(), "ReadAuditWorkerThread");
		workerThread.setDaemon(true);
		workerThread.start();
	}
	
	@PreDestroy
	public void destroy() {
		log.info("Stopping ReadAuditWorker background thread");
		running = false;
		if (workerThread != null) {
			workerThread.interrupt();
		}
	}
	
	public void submitTask(ReadAuditLog readAuditLog) {
		boolean isAdded = queue.offer(readAuditLog);
		if (!isAdded) {
			log.error("Queue is full!, can't submit new read audit task ");
		}
	}
	
	private void run() {
		while (running) {
			try {
				ReadAuditLog item = queue.take();
				List<ReadAuditLog> batch = new ArrayList<>();
				batch.add(item);
				
				// It will drain any additional queued logs that can go up to 49 more, making it a max batch of 50
				queue.drainTo(batch, 49);
				saveBatch(batch);
			}
			catch (InterruptedException e) {
				log.debug("ReadAuditWorker thread interrupted, shutting down");
				Thread.currentThread().interrupt();
				break;
			}
			catch (Exception e) {
				log.error("Error in ReadAuditWorker execution loop", e);
			}
		}
	}
	
	private void saveBatch(List<ReadAuditLog> batch) {
		if (batch.isEmpty()) {
			return;
		}
		
		boolean isBatchLogsSaved = false;
		try {
			Context.openSession();
			readAuditService.logReadAudits(batch);
			isBatchLogsSaved = true;
		}
		catch (Exception e) {
			log.warn("Failed to save read audit logs in batch, falling back to one-by-one save", e);
		}
		finally {
			Context.closeSession();
		}
		
		if (!isBatchLogsSaved) {
			for (ReadAuditLog logEntry : batch) {
				try {
					Context.openSession();
					readAuditService.logReadAudit(logEntry);
				}
				catch (Exception ex) {
					log.error("Failed to save individual read audit log in fallback", ex);
				}
				finally {
					Context.closeSession();
				}
			}
		}
	}
}
