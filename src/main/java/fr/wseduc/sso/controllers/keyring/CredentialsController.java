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
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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
									if (r.isRight() && r.right().getValue().getObject("form_schema") != null) {
										if (formDataValidation(r, body, request)) return;
										body.putString("user_id", user.getUserId());
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
						if (r.isRight() && r.right().getValue().getObject("form_schema") != null) {
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
		JsonObject schema = r.right().getValue().getObject("form_schema");
		for (String attr: new HashSet<>(body.getFieldNames())) {
			if (!schema.containsField(attr)) {
				body.removeField(attr);
			}
		}
		for (String attr: schema.getFieldNames()) {
			if (!body.containsField(attr)) {
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
					JsonObject schema = params.getObject("form_schema");
					if (schema != null) {
						JsonArray form = new JsonArray();
						for (String key : schema.getFieldNames()) {
							JsonObject j = schema.getObject(key);
							if (j != null) {
								form.add(j.putString("name", key));
							}
						}
						params.putArray("form", form);
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
									renderView(request, formatMustacheObject(res.<JsonObject>get(0), serviceId),
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
			j.putString("url", json.getString("url"));
			j.putString("name", json.getString("name"));
			j.putString("service_id", serviceId);
			json.removeField("url");
			json.removeField("name");
			JsonArray form = new JsonArray();
			for (String attr : json.getFieldNames()) {
				if (!"user_id".equals(attr) && !"id".equals(attr)) {
					form.add(new JsonObject().putString("name", attr).putString("value", json.getString(attr)));
				}
			}
			j.putArray("form", form);
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
