package com.grookage.leia.aerospike.client;

import com.aerospike.client.*;
import com.aerospike.client.policy.*;
import com.aerospike.client.query.IndexType;
import com.grookage.leia.aerospike.exception.LeiaAeroErrorCode;
import com.grookage.leia.aerospike.storage.AerospikeStorageConstants;
import com.grookage.leia.models.exception.LeiaException;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@UtilityClass
@Slf4j
public class AerospikeClientUtils {

	private static final List<String> BIN_INDEXES =
			List.of(AerospikeStorageConstants.SCHEMA_BIN,
					AerospikeStorageConstants.SCHEMA_STATE_BIN,
					AerospikeStorageConstants.NAMESPACE_BIN,
					AerospikeStorageConstants.ORG_BIN,
					AerospikeStorageConstants.TENANT_BIN);

	/*
	  Having to do this to have control at the code level, and also checkIndex doesn't exist.
	 */
	private static void createIndex(final String namespace,
	                                final IAerospikeClient client) {

		BIN_INDEXES.forEach(bin -> {
			try {
				final var indexTask = client.createIndex(null, namespace, AerospikeStorageConstants.SCHEMA_SET,
						bin, bin, IndexType.STRING);
				indexTask.waitTillComplete();
				log.debug("Index successfully created for namespace {} with indexName {} and binName {}",
						namespace,
						bin, bin);
			} catch (final AerospikeException ex) {
				if (ex.getResultCode() != ResultCode.INDEX_ALREADY_EXISTS) {
					log.error(
							"The index creation has failed for indexName {} with bin {} with the exception",
							bin, bin, ex);
					throw LeiaException.error(LeiaAeroErrorCode.INDEX_CREATION_FAILED);
				}
			}
		});
	}

	public static IAerospikeClient getIClient(final AerospikeConfig config) {
		log.info("Starting Aerospike client");
		final var clientPolicy = getClientPolicy(config);
		final var aerospikeClient = new AerospikeClient(clientPolicy, config.getHosts()
				.stream()
				.map(
						connection -> new Host(connection.getHost(), connection.getTls(), connection.getPort()))
				.toArray(Host[]::new));
		if (!config.isIndexCreationDisabled()) {
			createIndex(config.getNamespace(), aerospikeClient);
		}
		log.info("Started the Aerospike Client after creating indexes");
		return aerospikeClient;
	}

	public static ClientPolicy getClientPolicy(final AerospikeConfig config) {
		final var clientPolicy = new ClientPolicy();
		final var readPolicy = getReadPolicy(config);
		final var writePolicy = getWritePolicy(config);
		final var scanPolicy = getScanPolicy(config);
		final var batchPolicy = getBatchPolicy(config);
		final var queryPolicy = getQueryPolicy(config);

		clientPolicy.user = config.getUsername();
		clientPolicy.password = config.getPassword();
		clientPolicy.maxConnsPerNode = config.getMaxConnectionsPerNode();
		clientPolicy.readPolicyDefault = readPolicy;
		clientPolicy.writePolicyDefault = writePolicy;
		clientPolicy.scanPolicyDefault = scanPolicy;
		clientPolicy.batchPolicyDefault = batchPolicy;
		clientPolicy.queryPolicyDefault = queryPolicy;
		clientPolicy.failIfNotConnected = true;
		clientPolicy.threadPool = Executors.newFixedThreadPool(config.getThreadPoolSize());

		if (config.isTlsEnabled()) {
			clientPolicy.tlsPolicy = new TlsPolicy();
		}
		return clientPolicy;
	}

	public QueryPolicy getQueryPolicy(final AerospikeConfig config) {
		final var queryPolicy = new QueryPolicy();
		queryPolicy.readModeAP = ReadModeAP.ONE;
		queryPolicy.replica = Replica.MASTER_PROLES;
		queryPolicy.maxConcurrentNodes = config.getBatchMaxConcurrentNodes();
		return queryPolicy;
	}

	public BatchPolicy getBatchPolicy(final AerospikeConfig config) {
		final var batchPolicy = new BatchPolicy();
		batchPolicy.maxConcurrentThreads = config.getBatchMaxConcurrentNodes();
		batchPolicy.allowInline = true;
		return batchPolicy;
	}

	private static ScanPolicy getScanPolicy(final AerospikeConfig config) {
		final var scanPolicy = new ScanPolicy();
		scanPolicy.concurrentNodes = true;
		scanPolicy.includeBinData = true;
		scanPolicy.maxConcurrentNodes = config.getScanMaxConcurrentNodes();
		return scanPolicy;
	}

	private static WritePolicy getWritePolicy(final AerospikeConfig config) {
		final var writePolicy = new WritePolicy();
		writePolicy.maxRetries = config.getRetries();
		writePolicy.readModeAP = ReadModeAP.ALL;
		writePolicy.replica = Replica.MASTER_PROLES;
		writePolicy.sleepBetweenRetries = config.getSleepBetweenRetries();
		writePolicy.commitLevel = CommitLevel.COMMIT_ALL;
		writePolicy.totalTimeout = config.getTimeout();
		writePolicy.sendKey = true;
		return writePolicy;
	}

	private static Policy getReadPolicy(final AerospikeConfig config) {
		final var readPolicy = new Policy();
		readPolicy.maxRetries = config.getRetries();
		readPolicy.readModeAP = ReadModeAP.ONE;
		readPolicy.replica = Replica.MASTER_PROLES;
		readPolicy.sleepBetweenRetries = config.getSleepBetweenRetries();
		readPolicy.totalTimeout = config.getTimeout();
		readPolicy.sendKey = true;
		return readPolicy;
	}

	@SneakyThrows
	public static byte[] compress(final byte[] bytes) {
		final var bos = new ByteArrayOutputStream();
		final var gzip = new GZIPOutputStream(bos);
		gzip.write(bytes);
		gzip.close();
		return bos.toByteArray();
	}

	@SneakyThrows
	public static String retrieve(final byte[] value) {
		final var gis = new GZIPInputStream(
				new ByteArrayInputStream(
						value
				));
		final var bf = new BufferedReader(new InputStreamReader(gis));
		final var stringBuilder = new StringBuilder();
		String line;
		while ((line = bf.readLine()) != null) {
			stringBuilder.append(line);
		}
		return stringBuilder.toString();
	}
}
