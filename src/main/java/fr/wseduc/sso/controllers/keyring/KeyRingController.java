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
import fr.wseduc.sso.services.keyring.KeyRingService;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class KeyRingController extends BaseController {

	private KeyRingService keyRingService;

	@Get("/keyring/admin-console")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void adminView(HttpServerRequest request) {
		renderView(request);
	}

	@Post("/keyring")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void addService(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "createService", new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject data) {
				keyRingService.create(data, notEmptyResponseHandler(request, 201));
			}
		});
	}

	@Put("/keyring/:serviceId")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void alterService(final HttpServerRequest request) {
		// TODO add validation
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject data) {
				keyRingService.alter(request.params().get("serviceId"), data, notEmptyResponseHandler(request));
			}
		});
	}

	@Get("/keyring/:serviceId")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void descService(HttpServerRequest request) {
		keyRingService.description(request.params().get("serviceId"), notEmptyResponseHandler(request));
	}

	@Get("/keyring")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void listServices(HttpServerRequest request) {
		keyRingService.listServices(arrayResponseHandler(request));
	}

	@Delete("/keyring/:serviceId")
	@ResourceFilter(SuperAdminFilter.class)
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void dropService(HttpServerRequest request) {
		keyRingService.drop(request.params().get("serviceId"), defaultResponseHandler(request, 204));
	}

	public void setKeyRingService(KeyRingService keyRingService) {
		this.keyRingService = keyRingService;
	}

}
