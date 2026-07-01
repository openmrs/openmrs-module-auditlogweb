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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.ReadAuditService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ReadAuditWorkerTest {

	@Mock
	private ReadAuditService readAuditService;

	@InjectMocks
	private ReadAuditWorker worker;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void shouldStartDaemonThreadOnInit() throws Exception {
		worker.init();

		Field threadField = ReadAuditWorker.class.getDeclaredField("workerThread");
		threadField.setAccessible(true);
		Thread thread = (Thread) threadField.get(worker);

		assertNotNull(thread);
		assertTrue(thread.isAlive());
		assertTrue(thread.isDaemon());
		assertEquals("ReadAuditWorkerThread", thread.getName());

		worker.destroy();
	}

	@Test
	void shouldStopDaemonThreadOnDestroy() throws Exception {
		worker.init();

		Field threadField = ReadAuditWorker.class.getDeclaredField("workerThread");
		threadField.setAccessible(true);
		Thread thread = (Thread) threadField.get(worker);

		assertNotNull(thread);
		assertTrue(thread.isAlive());

		worker.destroy();

		Field runningField = ReadAuditWorker.class.getDeclaredField("running");
		runningField.setAccessible(true);
		boolean running = (boolean) runningField.get(worker);

		assertFalse(running);
		assertTrue(thread.isInterrupted() || !thread.isAlive());
	}

	@Test
	void shouldSubmitTaskToQueue() throws Exception {
		ReadAuditLog logEntry = new ReadAuditLog();
		worker.submitTask(logEntry);

		Field queueField = ReadAuditWorker.class.getDeclaredField("queue");
		queueField.setAccessible(true);
		BlockingQueue<ReadAuditLog> queue = (BlockingQueue<ReadAuditLog>) queueField.get(worker);

		assertEquals(1, queue.size());
		assertEquals(logEntry, queue.peek());
	}

	@Test
	void shouldNotSubmitTaskWhenQueueIsFull() throws Exception {
		BlockingQueue<ReadAuditLog> smallQueue = new LinkedBlockingQueue<>(1);
		Field queueField = ReadAuditWorker.class.getDeclaredField("queue");
		queueField.setAccessible(true);
		queueField.set(worker, smallQueue);

		ReadAuditLog logEntry1 = new ReadAuditLog();
		ReadAuditLog logEntry2 = new ReadAuditLog();

		worker.submitTask(logEntry1);
		worker.submitTask(logEntry2);

		assertEquals(1, smallQueue.size());
		assertEquals(logEntry1, smallQueue.peek());
	}

	@Test
	void shouldSaveBatchSuccessfully() throws Exception {
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			ReadAuditLog logEntry1 = new ReadAuditLog();
			ReadAuditLog logEntry2 = new ReadAuditLog();
			List<ReadAuditLog> batch = Arrays.asList(logEntry1, logEntry2);

			Method saveBatchMethod = ReadAuditWorker.class.getDeclaredMethod("saveBatch", List.class);
			saveBatchMethod.setAccessible(true);
			saveBatchMethod.invoke(worker, batch);

			context.verify(Context::openSession);
			context.verify(Context::closeSession);

			verify(readAuditService).logReadAudits(batch);
			verify(readAuditService, never()).logReadAudit(any(ReadAuditLog.class));
		}
	}

	@Test
	void shouldFallbackToIndividualSavesOnBatchFailure() throws Exception {
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			ReadAuditLog logEntry1 = new ReadAuditLog();
			ReadAuditLog logEntry2 = new ReadAuditLog();
			List<ReadAuditLog> batch = Arrays.asList(logEntry1, logEntry2);

			doThrow(new RuntimeException("Batch failed")).when(readAuditService).logReadAudits(batch);

			Method saveBatchMethod = ReadAuditWorker.class.getDeclaredMethod("saveBatch", List.class);
			saveBatchMethod.setAccessible(true);
			saveBatchMethod.invoke(worker, batch);

			verify(readAuditService).logReadAudit(logEntry1);
			verify(readAuditService).logReadAudit(logEntry2);

			context.verify(Context::openSession);
			context.verify(Context::closeSession);
		}
	}

	@Test
	void shouldContinueSavingIndividualLogsEvenIfOneFailsInFallback() throws Exception {
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			ReadAuditLog logEntry1 = new ReadAuditLog();
			ReadAuditLog logEntry2 = new ReadAuditLog();
			List<ReadAuditLog> batch = Arrays.asList(logEntry1, logEntry2);

			doThrow(new RuntimeException("Batch failed")).when(readAuditService).logReadAudits(batch);
			doThrow(new RuntimeException("Entry 1 failed")).when(readAuditService).logReadAudit(logEntry1);

			Method saveBatchMethod = ReadAuditWorker.class.getDeclaredMethod("saveBatch", List.class);
			saveBatchMethod.setAccessible(true);
			saveBatchMethod.invoke(worker, batch);

			verify(readAuditService).logReadAudit(logEntry1);
			verify(readAuditService).logReadAudit(logEntry2);

			context.verify(Context::openSession);
			context.verify(Context::closeSession);
		}
	}

	@Test
	void shouldProcessQueuedTasksInRunLoop() throws Exception {
		ReadAuditLog logEntry1 = new ReadAuditLog();
		worker.submitTask(logEntry1);

		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			doAnswer(invocation -> {
				Field runningField = ReadAuditWorker.class.getDeclaredField("running");
				runningField.setAccessible(true);
				runningField.set(worker, false);
				return null;
			}).when(readAuditService).logReadAudits(anyList());

			Method runMethod = ReadAuditWorker.class.getDeclaredMethod("run");
			runMethod.setAccessible(true);
			runMethod.invoke(worker);

			verify(readAuditService).logReadAudits(Collections.singletonList(logEntry1));

			context.verify(Context::openSession);
			context.verify(Context::closeSession);
		}
	}

	@Test
	void shouldHandleInterruptedExceptionInRunLoop() throws Exception {
		Thread.currentThread().interrupt();

		Method runMethod = ReadAuditWorker.class.getDeclaredMethod("run");
		runMethod.setAccessible(true);
		runMethod.invoke(worker);

		assertTrue(Thread.interrupted());
	}
}
