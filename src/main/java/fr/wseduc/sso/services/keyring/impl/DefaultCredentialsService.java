/*
 * Copyright © Région Nord Pas de Calais-Picardie, Département 91, Région Aquitaine-Limousin-Poitou-Charentes, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.wseduc.sso.services.keyring.impl;

import fr.wseduc.sso.services.keyring.CredentialsService;
import fr.wseduc.sso.services.keyring.KeyRingService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.Blowfish;
import org.entcore.common.sql.Sql;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.security.GeneralSecurityException;

import static org.entcore.common.sql.SqlResult.*;

public class DefaultCredentialsService implements CredentialsService {

	private static final Logger log = LoggerFactory.getLogger(DefaultKeyRingService.class);
	private final String cryptKey;
	private Sql sql = Sql.getInstance();
	private KeyRingService keyRingService;

	public DefaultCredentialsService(String cryptKey) {
		this.cryptKey = cryptKey;
	}

	@Override
	public void insert(final String serviceId, JsonObject data, final Handler<Either<String, JsonObject>> handler) {
		encrypt(serviceId, data, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					sql.insert("sso." + serviceId, r.right().getValue(), "id", validUniqueResultHandler(handler));
				} else {
					handler.handle(r);
				}
			}
		});
	}

	private void encrypt(String serviceId, final JsonObject data, final Handler<Either<String, JsonObject>> handler) {
		keyRingService.description(serviceId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					JsonObject j = r.right().getValue();
					if (j != null && j.getJsonObject("form_schema") != null) {
						JsonObject schema = j.getJsonObject("form_schema");
						JsonObject output = new JsonObject();
						if (data.getString("user_id") != null) {
							output.put("user_id", data.getString("user_id"));
						}
						for (String attr: data.fieldNames()) {
							JsonObject p = schema.getJsonObject(attr);
							if (p != null) {
								String content = data.getString(attr);
								if (content == null) {
									handler.handle(new Either.Left<String, JsonObject>("missing.attribute " + attr));
									return;
								}
								if ("password".equals(p.getString("type"))) {
									try {
										output.put(attr, Blowfish.encrypt(content, cryptKey));
									} catch (GeneralSecurityException e) {
										log.error(e.getMessage(), e);
										handler.handle(new Either.Left<String, JsonObject>(e.getMessage()));
										return;
									}
								} else {
									output.put(attr, content);
								}
							}
						}
						handler.handle(new Either.Right<String, JsonObject>(output));
					} else {
						handler.handle(new Either.Left<String, JsonObject>("missing.form_schema"));
					}
				} else {
					handler.handle(r);
				}
			}
		});
	}

	private Handler<Message<JsonObject>> decryptResultHandler(
			final String serviceId, final Handler<Either<String, JsonArray>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				decryptResult(serviceId, event, handler);
			}
		};
	}

	private void decryptResult(String serviceId, Message<JsonObject> event, final Handler<Either<String, JsonArray>> handler) {
		final Either<String, JsonArray> res = validResult(event);
		final JsonArray a;
		if (res.isRight() && (a = res.right().getValue()) != null && a.size() > 0) {
			keyRingService.description(serviceId, new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> r) {
					if (r.isRight()) {
						JsonObject j = r.right().getValue();
						if (j != null && j.getJsonObject("form_schema") != null) {
							JsonObject schema = j.getJsonObject("form_schema");
							for (Object o : a) {
								if (! (o instanceof JsonObject)) continue;
								JsonObject json = (JsonObject) o;
								for (String attr: json.fieldNames()) {
									JsonObject p = schema.getJsonObject(attr);
									String c;
									if (p != null && "password".equals(p.getString("type")) && (c = json.getString(attr)) != null) {
										try {
											json.put(attr, Blowfish.decrypt(c, cryptKey));
										} catch (GeneralSecurityException e) {
											log.error(e.getMessage(), e);
										}
									}
								}
							}
							handler.handle(new Either.Right<String, JsonArray>(a));
						} else {
							handler.handle(res);
						}
					} else {
						handler.handle(res);
					}
				}
			});
		} else {
			handler.handle(res);
		}
	}

	@Override
	public void getCredentials(String serviceId, String userId, Handler<Either<String, JsonArray>> handler) {
		sql.prepared(
				"SELECT c.*, s.url, s.name FROM sso." + serviceId + " AS c, sso.keyring AS s " +
						"WHERE c.user_id = ? AND s.service_id = ?",
				new JsonArray().add(userId).add(serviceId),
				decryptResultHandler(serviceId, handler)
		);
	}

	@Override
	public void update(final String serviceId, final String id, JsonObject data, final Handler<Either<String, JsonObject>> handler) {
		encrypt(serviceId, data, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					JsonObject d = r.right().getValue();
					StringBuilder s = new StringBuilder()
							.append("UPDATE sso.").append(serviceId).append(" SET ");
					JsonArray params = new JsonArray();
					for (String attr : d.fieldNames()) {
						s.append(attr).append(" = ?,");
						params.add(d.getValue(attr));
					}
					s.deleteCharAt(s.length() - 1);
					s.append(" WHERE id = ?;");
					params.add(Sql.parseId(id));
					sql.prepared(s.toString(), params, validRowsResultHandler(handler));
				} else {
					handler.handle(r);
				}
			}
		});
	}

	@Override
	public void delete(String serviceId, String id, Handler<Either<String, JsonObject>> handler) {
		sql.prepared("DELETE FROM sso." + serviceId + " WHERE id = ?",
				new JsonArray().add(Sql.parseId(id)), validRowsResultHandler(handler));
	}

	public void setKeyRingService(KeyRingService keyRingService) {
		this.keyRingService = keyRingService;
	}

}
