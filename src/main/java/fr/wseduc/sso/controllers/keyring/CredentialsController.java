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

package fr.wseduc.sso.controllers.keyring;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.sso.filters.CredentialsFilter;
import fr.wseduc.sso.filters.KeyringFilter;
import fr.wseduc.sso.services.keyring.CredentialsService;
import fr.wseduc.sso.services.keyring.KeyRingService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class CredentialsController extends BaseController {

	private KeyRingService keyRingService;
	private CredentialsService credentialsService;

	@Post("/keyring/:serviceId")
	@ResourceFilter(KeyringFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void insertCredential(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(final JsonObject body) {
							final String serviceId = request.params().get("serviceId");
							keyRingService.description(serviceId, new Handler<Either<String, JsonObject>>() {
								@Override
								public void handle(Either<String, JsonObject> r) {
									if (r.isRight() && r.right().getValue().getJsonObject("form_schema") != null) {
										if (formDataValidation(r, body, request)) return;
										body.put("user_id", user.getUserId());
										credentialsService.insert(serviceId, body, notEmptyResponseHandler(request, 201));
									} else {
										badRequest(request, "invalid.service.id");
									}
								}
							});
						}
					});
				} else {
					unauthorized(request, "session.not.found");
				}
			}
		});
	}

	@Put("/keyring/:serviceId/:id")
	@ResourceFilter(CredentialsFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void updateCredential(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject body) {
				final String serviceId = request.params().get("serviceId");
				keyRingService.description(serviceId, new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> r) {
						if (r.isRight() && r.right().getValue().getJsonObject("form_schema") != null) {
							if (formDataValidation(r, body, request)) return;
							credentialsService.update(serviceId, request.params().get("id"), body,
									notEmptyResponseHandler(request));
						} else {
							badRequest(request, "invalid.service.id");
						}
					}
				});
			}
		});
	}

	private boolean formDataValidation(Either<String, JsonObject> r, JsonObject body, HttpServerRequest request) {
		JsonObject schema = r.right().getValue().getJsonObject("form_schema");
		for (String attr: new HashSet<>(body.fieldNames())) {
			if (!schema.containsKey(attr)) {
				body.remove(attr);
			}
		}
		for (String attr: schema.fieldNames()) {
			if (!body.containsKey(attr)) {
				badRequest(request, "missing.attr." + attr);
				return true;
			}
		}
		return false;
	}

	@Get("/keyring/credentials/:serviceId")
	@ResourceFilter(KeyringFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void getCredentials(final HttpServerRequest request) {
		final String serviceId = request.params().get("serviceId");
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					credentialsService.getCredentials(serviceId, user.getUserId(), arrayResponseHandler(request));
				} else {
					unauthorized(request, "session.not.found");
				}
			}
		});
	}

	@Get("/keyring/edit/:serviceId")
	@ResourceFilter(KeyringFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void editView(final HttpServerRequest request) {
		final String serviceId = request.params().get("serviceId");
		keyRingService.description(serviceId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					JsonObject params = r.right().getValue();
					JsonObject schema = params.getJsonObject("form_schema");
					if (schema != null) {
						JsonArray form = new JsonArray();
						for (String key : schema.fieldNames()) {
							JsonObject j = schema.getJsonObject(key);
							if (j != null) {
								form.add(j.put("name", key));
							}
						}
						params.put("form", form);
					}
					renderView(request, params, "keyring/credentials.html", null);
				} else {
					notFound(request);
				}
			}
		});
	}

	@Get("/keyring/access/:serviceId")
	@ResourceFilter(KeyringFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void access(final HttpServerRequest request) {
		final String serviceId = request.params().get("serviceId");
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					credentialsService.getCredentials(serviceId, user.getUserId(), new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> r) {
							if (r.isRight()) {
								JsonArray res = r.right().getValue();
								if (res != null && res.size() == 1) {
									renderView(request, formatMustacheObject(res.getJsonObject(0), serviceId),
											"keyring/authenticate.html", null);
								} else {
									editView(request);
								}
							} else {
								leftToResponse(request, r.left());
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private JsonObject formatMustacheObject(JsonObject json, String serviceId) {
		JsonObject j = new JsonObject();
		if (json != null) {
			j.put("url", json.getString("url"));
			j.put("name", json.getString("name"));
			j.put("service_id", serviceId);
			json.remove("url");
			json.remove("name");
			JsonArray form = new JsonArray();
			for (String attr : json.fieldNames()) {
				if (!"user_id".equals(attr) && !"id".equals(attr)) {
					form.add(new JsonObject().put("name", attr).put("value", json.getString(attr)));
				}
			}
			j.put("form", form);
		}
		return j;
	}

	@Delete("/keyring/:serviceId/:id")
	@ResourceFilter(CredentialsFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void deleteCredential(final HttpServerRequest request) {
		credentialsService.delete(request.params().get("serviceId"), request.params().get("id"), defaultResponseHandler(request, 204));
	}

	public void setKeyRingService(KeyRingService keyRingService) {
		this.keyRingService = keyRingService;
	}

	public void setCredentialsService(CredentialsService credentialsService) {
		this.credentialsService = credentialsService;
	}

}
